package es.josxtm.mycad.cad;

import java.util.*;

/** Selects only genuine boundary edges of an already visible B-Rep patch.
 * Coplanar quad seams and edges belonging only to hidden patches are excluded. */
public final class SteppedBoundaryEdgePicker {
 private SteppedBoundaryEdgePicker(){}
 public static final class Hit {
  public final String shellId,edgeId;
  public final int patchIndex,adjacentPatchIndex;
  public final double length,distance;
  private final float[] endpoints;
  private Hit(String shell,VariableHeightOrthogonalBRep.Edge edge,
      int patch,int adjacent,double distance,VariableHeightOrthogonalBRep.Vertex a,
      VariableHeightOrthogonalBRep.Vertex b){
   shellId=shell;edgeId=edge.id;patchIndex=patch;adjacentPatchIndex=adjacent;
   length=edge.length;this.distance=distance;
   endpoints=new float[]{(float)a.x,(float)a.y,(float)a.z,(float)b.x,(float)b.y,(float)b.z};
  }
  public float[] endpoints(){return endpoints.clone();}
 }
 public static Hit nearest(VariableHeightOrthogonalBRep shell,
      SteppedVisiblePatchBridge.Hit visible,double toleranceMm){
  if(shell==null||visible==null||!Double.isFinite(toleranceMm)||toleranceMm<0||
     !shell.id().equals(visible.shellId)||visible.localTriangle<0||
     visible.localTriangle/2>=shell.faces().size()||
     !visible.patch.sourceFaceIds.contains(shell.faces().get(visible.localTriangle/2).id))
   throw new IllegalArgumentException("Invalid or stale B-Rep surface hit");
  Map<String,VariableHeightOrthogonalBRep.Vertex> vertices=new HashMap<>();
  for(VariableHeightOrthogonalBRep.Vertex vertex:shell.vertices())vertices.put(vertex.id,vertex);
  Map<String,VariableHeightOrthogonalBRep.Edge> edges=new HashMap<>();
  for(VariableHeightOrthogonalBRep.Edge edge:shell.edges())edges.put(edge.id,edge);
  double[] point=visible.point();Hit best=null;
  for(List<SteppedCoplanarPatchTopology.BoundaryUse> loop:visible.patch.loops)
   for(SteppedCoplanarPatchTopology.BoundaryUse use:loop){
    VariableHeightOrthogonalBRep.Edge edge=edges.get(use.edgeId);
    if(edge==null)throw new IllegalArgumentException("Stale patch boundary");
    VariableHeightOrthogonalBRep.Vertex a=vertices.get(edge.firstVertexId),b=vertices.get(edge.secondVertexId);
    double dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z;
    double t=((point[0]-a.x)*dx+(point[1]-a.y)*dy+(point[2]-a.z)*dz)/(dx*dx+dy*dy+dz*dz);
    t=Math.max(0,Math.min(1,t));
    double distance=Math.hypot(Math.hypot(point[0]-a.x-t*dx,point[1]-a.y-t*dy),point[2]-a.z-t*dz);
    if(distance<=toleranceMm&&(best==null||distance<best.distance||
       (distance==best.distance&&edge.id.compareTo(best.edgeId)<0)))
     best=new Hit(shell.id(),edge,visible.patch.index,use.adjacentPatchIndex,distance,a,b);
   }
  return best;
 }
 /** Perspective 45-degree viewport, using the depth of the actual hit. */
 public static double touchTolerance(VariableHeightOrthogonalBRep shell,
     double ox,double oy,double oz,double[] point,int viewportHeight,double density){
  if(shell==null||point==null||point.length!=3||!Double.isFinite(ox)||
     !Double.isFinite(oy)||!Double.isFinite(oz))
   throw new IllegalArgumentException("Invalid viewport hit");
  for(double value:point)if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid hit point");
  double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
  double[] max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
  for(VariableHeightOrthogonalBRep.Vertex vertex:shell.vertices()){
   double[] p={vertex.x,vertex.y,vertex.z};
   for(int k=0;k<3;k++){min[k]=Math.min(min[k],p[k]);max[k]=Math.max(max[k],p[k]);}
  }
  return EdgeScreenTolerance.compute(Math.min(max[0]-min[0],Math.min(max[1]-min[1],max[2]-min[2])),
   Math.hypot(Math.hypot(point[0]-ox,point[1]-oy),point[2]-oz),viewportHeight,density);
 }
}
