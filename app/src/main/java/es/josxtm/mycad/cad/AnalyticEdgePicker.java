package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Find the nearest ANALYTIC primitive edge to a known 3D surface hit.
 * The renderer performs ray/triangle picking first; this class classifies
 * its world-space hit using topology, not triangle edges or tessellation.
 *
 * Only primitive boxes/cylinders are supported. It is not screen-space
 * precision edge picking and does not support Boolean/STEP geometry.
 */
public final class AnalyticEdgePicker {
 private AnalyticEdgePicker(){}

 public static final class Hit {
  public final String bodyId,edgeId,kind;
  public final double distance;
  private final double[] closestPoint,sourceGeometry;
  private final PrimitiveBRepBody.Kind sourceKind;
  private Hit(PrimitiveBRepBody body,String edgeId,String kind,double distance,double[] closest){
   this.bodyId=body.bodyId;this.edgeId=edgeId;this.kind=kind;this.distance=distance;
   this.closestPoint=closest.clone();this.sourceKind=body.kind;
   sourceGeometry=new double[]{body.width,body.depth,body.height,
                               body.x,body.y,body.z,body.rx,body.ry,body.rz};
  }
  /** Prevent reuse of a touched edge point after any geometry/pose change. */
  boolean matchesGeometry(PrimitiveBRepBody body){
   if(body==null||!bodyId.equals(body.bodyId)||sourceKind!=body.kind)return false;
   double[] now={body.width,body.depth,body.height,
                 body.x,body.y,body.z,body.rx,body.ry,body.rz};
   for(int i=0;i<now.length;i++)
    if(Double.compare(sourceGeometry[i],now[i])!=0)return false;
   return true;
  }
  public double[] closestPoint(){return closestPoint.clone();}
 }

 private static double distance(double[] a,double[] b){
  double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2];
  return Math.sqrt(x*x+y*y+z*z);
 }
 private static double[] lineClosest(double[] a,double[] b,double[] p){
  double dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2];
  double length2=dx*dx+dy*dy+dz*dz;
  if(!Double.isFinite(length2)||length2<=0)
   throw new IllegalArgumentException("Degenerate analytic edge");
  double t=((p[0]-a[0])*dx+(p[1]-a[1])*dy+(p[2]-a[2])*dz)/length2;
  t=Math.max(0,Math.min(1,t));
  return new double[]{a[0]+t*dx,a[1]+t*dy,a[2]+t*dz};
 }
 /** Geometric hit tolerance in millimetres, not pixels. */
 public static double suggestedTolerance(PrimitiveBRepBody body){
  if(body==null)throw new IllegalArgumentException("Missing primitive");
  double min=Math.min(body.width,Math.min(body.depth,body.height));
  return Math.max(0.1,Math.min(3.0,min*0.035));
 }
 public static Hit nearestEdge(PrimitiveBRepBody body,double wx,double wy,double wz,
                               double tolerance){
  return nearest(body,null,wx,wy,wz,tolerance);
 }
 /**
  * Classify an edge ONLY among the boundary edges of the face already
  * identified by the visible-surface picker. Prevents a nearby edge on a
  * different face from stealing the selection on thin or small primitives.
  * The face ID must belong to this body; no silent global fallback.
  */
 public static Hit nearestEdgeOnFace(PrimitiveBRepBody body,String faceId,
                                    double wx,double wy,double wz,double tolerance){
  if(body==null||faceId==null||body.findFace(faceId)==null)
   throw new IllegalArgumentException("Unknown or foreign analytic face");
  return nearest(body,faceId,wx,wy,wz,tolerance);
 }
 private static Hit nearest(PrimitiveBRepBody body,String faceId,
                            double wx,double wy,double wz,double tolerance){
  if(body==null||!Double.isFinite(wx)||!Double.isFinite(wy)||
     !Double.isFinite(wz)||!Double.isFinite(tolerance)||tolerance<0)
   throw new IllegalArgumentException("Invalid world hit or tolerance");
  double[] point={wx,wy,wz};
  Hit winner=null;
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   CadKernel.Box b=new CadKernel.Box(body.bodyId,-body.width/2,-body.depth/2,0,
                                      body.width,body.depth,body.height);
   BoxBRep model=BoxBRep.from(new TransformedBody(b,body.pose()));
   Map<String,double[]> vertices=new HashMap<>();
   for(BoxBRep.Vertex v:model.vertices())vertices.put(v.id,v.position());
   for(BoxBRep.Edge edge:model.edges()){
    if(faceId!=null&&!edge.adjacentFaceIds().contains(faceId))continue;
    double[] candidate=lineClosest(vertices.get(edge.startId),
                                   vertices.get(edge.endId),point);
    winner=best(winner,new Hit(body,edge.id,"LINE",
                              distance(point,candidate),candidate));
   }
  }else{
   CylinderBRep model=CylinderBRep.from(body.bodyId,body.width,
                                        body.height,body.pose());
   double[] local=body.pose().inversePoint(wx,wy,wz);
   double angle=Math.atan2(local[1],local[0]);
   for(CylinderBRep.CircularEdge edge:model.edges()){
    if(faceId!=null&&!edge.adjacentFaceIds().contains(faceId))continue;
    double[] candidate=edge.pointAt(angle);
    winner=best(winner,new Hit(body,edge.id,"CIRCLE",
                              distance(point,candidate),candidate));
   }
  }
  return winner!=null&&winner.distance<=tolerance?winner:null;
 }
 /** Stable ordering for ambiguous vertex positions: independent of render order. */
 private static Hit best(Hit current,Hit next){
  if(!Double.isFinite(next.distance))
   throw new IllegalArgumentException("Nonfinite edge distance");
  if(current==null||next.distance<current.distance-1e-9)return next;
  if(Math.abs(next.distance-current.distance)<=1e-9&&
     next.edgeId.compareTo(current.edgeId)<0)return next;
  return current;
 }
}
