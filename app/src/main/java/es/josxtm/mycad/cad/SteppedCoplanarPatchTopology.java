package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Read-only, patch-level half-edge adjacency for a stepped orthogonal shell.
 * A patch represents one oriented coplanar face with one or more boundary
 * loops. Original quad faces, edges and half-edges are never replaced.
 */
public final class SteppedCoplanarPatchTopology {
 private SteppedCoplanarPatchTopology(){}

 /** A boundary use still refers to its original, directed shell half-edge. */
 public static final class BoundaryUse {
  public final String halfEdgeId,edgeId,fromVertexId,toVertexId,twinHalfEdgeId;
  public final int adjacentPatchIndex;
  private BoundaryUse(VariableHeightOrthogonalBRep.HalfEdge use,int neighbor){
   halfEdgeId=use.id;edgeId=use.edgeId;fromVertexId=use.fromId;
   toVertexId=use.toId;twinHalfEdgeId=use.twinId;
   adjacentPatchIndex=neighbor;
  }
 }

 /** Analytic planar patch with its ordered outer/hole loops and neighboring patches. */
 public static final class PatchFace {
  public final int index,exteriorCount,holeCount;
  public final List<String> sourceFaceIds;
  public final List<List<BoundaryUse>> loops;
  public final List<Double> signedLoopAreas;
  public final double area,planeOffset;
  private final double[] normal;
  private PatchFace(int index,SteppedCoplanarFaceGroups.Patch source,
                    double[] normal,double offset,List<List<BoundaryUse>> loops){
   this.index=index;sourceFaceIds=source.faceIds;
   signedLoopAreas=source.signedLoopAreas;area=source.area;
   exteriorCount=source.exteriorCount;holeCount=source.holeCount;
   this.normal=normal.clone();planeOffset=offset;
   this.loops=Collections.unmodifiableList(loops);
  }
  /** Returns a defensive copy of the outward unit normal. */
  public double[] outwardNormal(){return normal.clone();}
 }

 public static final class Topology {
  public final List<PatchFace> patches;
  private final Map<String,Integer> sourceFaceToPatch;
  private Topology(List<PatchFace> patches,Map<String,Integer> mapping){
   this.patches=Collections.unmodifiableList(patches);
   sourceFaceToPatch=Collections.unmodifiableMap(new LinkedHashMap<>(mapping));
  }
  public int patchIndexForSourceFace(String id){
   Integer result=sourceFaceToPatch.get(id);
   if(result==null)throw new IllegalArgumentException("Unknown source face");
   return result;
  }
 }

 public static Topology build(VariableHeightOrthogonalBRep shell){
  if(shell==null)throw new IllegalArgumentException("Missing stepped B-Rep");
  List<SteppedCoplanarFaceGroups.Patch> descriptions=SteppedCoplanarFaceGroups.describe(shell);
  Map<String,VariableHeightOrthogonalBRep.Face> faces=new HashMap<>();
  for(VariableHeightOrthogonalBRep.Face face:shell.faces())
   if(faces.put(face.id,face)!=null)throw new IllegalArgumentException("Duplicate face ID");
  Map<String,VariableHeightOrthogonalBRep.Vertex> vertices=new HashMap<>();
  for(VariableHeightOrthogonalBRep.Vertex vertex:shell.vertices())
   if(vertices.put(vertex.id,vertex)!=null)throw new IllegalArgumentException("Duplicate vertex ID");
  Map<String,VariableHeightOrthogonalBRep.HalfEdge> halfEdges=new HashMap<>();
  for(VariableHeightOrthogonalBRep.HalfEdge half:shell.halfEdges())
   if(halfEdges.put(half.id,half)!=null)throw new IllegalArgumentException("Duplicate half-edge ID");
  Map<String,Integer> owner=new LinkedHashMap<>();
  for(int i=0;i<descriptions.size();i++)
   for(String id:descriptions.get(i).faceIds)
    if(owner.put(id,i)!=null)throw new IllegalArgumentException("Source face in two patches");
  if(owner.size()!=faces.size())throw new IllegalArgumentException("Unassigned source faces");

  List<PatchFace> patches=new ArrayList<>();
  Map<String,BoundaryUse> exposed=new HashMap<>();
  double totalArea=0;
  for(int i=0;i<descriptions.size();i++){
   SteppedCoplanarFaceGroups.Patch source=descriptions.get(i);
   VariableHeightOrthogonalBRep.Face representative=faces.get(source.faceIds.get(0));
   if(representative==null)throw new IllegalArgumentException("Missing patch representative");
   double[] normal=representative.outwardNormal();
   double norm=Math.sqrt(normal[0]*normal[0]+normal[1]*normal[1]+normal[2]*normal[2]);
   if(!Double.isFinite(norm)||Math.abs(norm-1)>1e-6)
    throw new IllegalArgumentException("Invalid patch normal");
   VariableHeightOrthogonalBRep.Vertex first=vertices.get(representative.vertexIds().get(0));
   if(first==null)throw new IllegalArgumentException("Missing plane origin");
   double offset=normal[0]*first.x+normal[1]*first.y+normal[2]*first.z;
   for(String id:source.faceIds){
    VariableHeightOrthogonalBRep.Face face=faces.get(id);
    if(face==null)throw new IllegalArgumentException("Missing source face");
    double[] n=face.outwardNormal();
    if(normal[0]*n[0]+normal[1]*n[1]+normal[2]*n[2]<1-1e-8)
     throw new IllegalArgumentException("Patch normals differ");
    for(String vertexId:face.vertexIds()){
     VariableHeightOrthogonalBRep.Vertex v=vertices.get(vertexId);
     if(v==null||Math.abs(normal[0]*v.x+normal[1]*v.y+normal[2]*v.z-offset)>1e-5)
      throw new IllegalArgumentException("Noncoplanar patch vertex");
    }
   }
   List<List<BoundaryUse>> loops=new ArrayList<>();
   int loopIndex=0;
   for(List<String> ids:source.boundaryHalfEdgeLoops){
    List<BoundaryUse> loop=new ArrayList<>();
    for(String id:ids){
     VariableHeightOrthogonalBRep.HalfEdge half=halfEdges.get(id);
     if(half==null||!owner.get(half.faceId).equals(i))
      throw new IllegalArgumentException("Boundary use has wrong owner");
     VariableHeightOrthogonalBRep.HalfEdge twin=halfEdges.get(half.twinId);
     if(twin==null||!half.id.equals(twin.twinId)||
        !half.edgeId.equals(twin.edgeId)||
        !half.fromId.equals(twin.toId)||!half.toId.equals(twin.fromId))
      throw new IllegalArgumentException("Invalid boundary twin");
     Integer adjacent=owner.get(twin.faceId);
     if(adjacent==null||adjacent==i)
      throw new IllegalArgumentException("Boundary twin is not in neighboring patch");
     BoundaryUse use=new BoundaryUse(half,adjacent);
     if(exposed.put(use.halfEdgeId,use)!=null)
      throw new IllegalArgumentException("Duplicate exposed half-edge");
     loop.add(use);
    }
    if(loop.size()<4)throw new IllegalArgumentException("Short boundary loop");
    for(int j=0;j<loop.size();j++)
     if(!loop.get(j).toVertexId.equals(loop.get((j+1)%loop.size()).fromVertexId))
      throw new IllegalArgumentException("Disconnected patch loop");
    loops.add(Collections.unmodifiableList(loop));
    loopIndex++;
   }
   if(loopIndex!=source.signedLoopAreas.size()||source.exteriorCount<1)
    throw new IllegalArgumentException("Invalid patch loop classification");
   patches.add(new PatchFace(i,source,normal,offset,loops));
   totalArea+=source.area;
  }
  if(Math.abs(totalArea-shell.surfaceArea())>1e-5*Math.max(1,shell.surfaceArea()))
   throw new IllegalArgumentException("Patch surface area is not conserved");
  for(PatchFace patch:patches)
   for(List<BoundaryUse> loop:patch.loops)
    for(BoundaryUse use:loop){
     BoundaryUse twin=exposed.get(use.twinHalfEdgeId);
     if(twin==null||twin.adjacentPatchIndex!=patch.index||
        !twin.twinHalfEdgeId.equals(use.halfEdgeId)||
        !twin.fromVertexId.equals(use.toVertexId)||
        !twin.toVertexId.equals(use.fromVertexId))
      throw new IllegalArgumentException("Patch adjacency is not reciprocal");
    }
  return new Topology(patches,owner);
 }
}
