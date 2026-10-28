package es.josxtm.mycad.cad;

import java.util.Arrays;
import java.util.List;

/**
 * Resolves an already visible GPU mesh triangle to a stepped B-Rep patch.
 * The viewport mesh picker first handles occlusion across ALL scene objects;
 * this bridge never promotes an invisible analytic face over that mesh hit.
 * Candidates must be registered from the exact rendered mesh snapshot.
 */
public final class SteppedVisiblePatchBridge {
 private SteppedVisiblePatchBridge(){}

 public static final class Candidate {
  public final int firstTriangle,triangleCount;
  public final VariableHeightOrthogonalBRep shell;
  public final SteppedCoplanarPatchTopology.Topology topology;

  public Candidate(int firstTriangle,float[] renderedTriangles,
      VariableHeightOrthogonalBRep shell,
      SteppedCoplanarPatchTopology.Topology topology){
   if(firstTriangle<0||renderedTriangles==null||shell==null||topology==null||
      !shell.id().equals(topology.shellId)||
      renderedTriangles.length%9!=0||
      !Arrays.equals(renderedTriangles,shell.triangles()))
    throw new IllegalArgumentException("Stale or mismatched stepped mesh snapshot");
   triangleCount=renderedTriangles.length/9;
   if(triangleCount>Integer.MAX_VALUE-firstTriangle)
    throw new IllegalArgumentException("Global triangle index overflow");
   this.firstTriangle=firstTriangle;
   this.shell=shell;
   this.topology=topology;
  }
 }

 public static final class Hit {
  public final int candidateIndex,globalTriangle,localTriangle;
  public final String shellId;
  public final SteppedCoplanarPatchTopology.PatchFace patch;
  private final double[] point;
  private Hit(int index,int global,int local,Candidate candidate,
      SteppedPatchRayPicker.Hit exact){
   candidateIndex=index;globalTriangle=global;localTriangle=local;
   shellId=candidate.shell.id();patch=exact.patch;point=exact.point();
  }
  public double[] point(){return point.clone();}
 }

 /**
  * Returns null when the frontmost rendered triangle is not a registered
  * stepped shell or when its exact ray hit disagrees with the GPU mesh.
  * Rejects overlapping registration ranges to prevent ambiguous ownership.
  */
 public static Hit resolve(ViewportMeshRayPicker.Hit visible,
     List<Candidate> candidates){
  if(candidates==null)throw new IllegalArgumentException("Missing patch candidates");
  for(int i=0;i<candidates.size();i++){
   Candidate a=candidates.get(i);
   if(a==null)throw new IllegalArgumentException("Null patch candidate");
   for(int j=0;j<i;j++){
    Candidate b=candidates.get(j);
    if(a.firstTriangle<(long)b.firstTriangle+b.triangleCount&&
       b.firstTriangle<(long)a.firstTriangle+a.triangleCount)
     throw new IllegalArgumentException("Overlapping patch mesh ranges");
   }
  }
  if(visible==null)return null;
  for(int i=0;i<candidates.size();i++){
   Candidate candidate=candidates.get(i);
   long local=(long)visible.triangle-candidate.firstTriangle;
   if(local<0||local>=candidate.triangleCount)continue;
   SteppedCoplanarPatchTopology.PatchFace fromTriangle=
       SteppedPatchTrianglePicker.resolve(
           candidate.shell,candidate.topology,(int)local);
   SteppedPatchRayPicker.Hit exact=SteppedPatchRayPicker.pick(
       candidate.shell,candidate.topology,
       new double[]{visible.ox,visible.oy,visible.oz},
       new double[]{visible.dx,visible.dy,visible.dz});
   if(exact==null||exact.patch!=fromTriangle)return null;
   double[] point=exact.point();
   double separation=Math.hypot(Math.hypot(point[0]-visible.x,
       point[1]-visible.y),point[2]-visible.z);
   double tolerance=1e-4*Math.max(1,Math.abs(visible.distance));
   if(separation>tolerance||
      Math.abs(exact.distance-visible.distance)>tolerance)
    return null;
   return new Hit(i,visible.triangle,(int)local,candidate,exact);
  }
  return null;
 }
}
