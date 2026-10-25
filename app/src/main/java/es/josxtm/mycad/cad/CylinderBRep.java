package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Analytic boundary model for a right circular cylinder under an XYZ rigid pose.
 * The boundary has three faces (lateral, bottom and top) and two CLOSED circular
 * edges shared by the lateral face and one cap each. Circular edges are periodic,
 * not polylines: no fictitious seam vertex is introduced.
 *
 * Topological face/edge keys are based on the persistent body UUID, independent
 * of tessellation resolution. This is not a general-purpose Boolean B-Rep kernel.
 */
public final class CylinderBRep {
 public enum Kind { CYLINDER_SIDE, CYLINDER_BOTTOM, CYLINDER_TOP }

 public static final class CircularEdge {
  public final String id;
  public final Kind cap;
  public final double radius, circumference;
  private final RigidTransform pose;
  private final double localZ;
  private final List<String> adjacentFaces;
  private CircularEdge(String id,Kind cap,double radius,double z,
                       RigidTransform pose,List<String> faces){
   this.id=id;this.cap=cap;this.radius=radius;localZ=z;this.pose=pose;
   circumference=2*Math.PI*radius;
   adjacentFaces=Collections.unmodifiableList(new ArrayList<>(faces));
  }
  public double[] center(){return pose.point(0,0,localZ);}
  /** World-space point on the analytic circle, angle in radians. */
  public double[] pointAt(double angle){
   if(!Double.isFinite(angle))throw new IllegalArgumentException("Invalid angle");
   return pose.point(radius*Math.cos(angle),radius*Math.sin(angle),localZ);
  }
  public List<String> adjacentFaceIds(){return adjacentFaces;}
 }

 public static final class Face {
  public final String id;
  public final Kind kind;
  public final double area;
  private final RigidTransform pose;
  private final List<String> edgeIds;
  private Face(String id,Kind kind,double area,RigidTransform pose,List<String> edges){
   this.id=id;this.kind=kind;this.area=area;this.pose=pose;
   edgeIds=Collections.unmodifiableList(new ArrayList<>(edges));
  }
  public List<String> edgeIds(){return edgeIds;}
  /**
   * Outward unit normal at angle on lateral face. On planar caps angle is
   * ignored (but must be finite). Rotation follows RigidTransform X/Y/Z.
   */
  public double[] outwardNormalAt(double angle){
   if(!Double.isFinite(angle))throw new IllegalArgumentException("Invalid angle");
   final double[] start=pose.point(0,0,0);
   double[] dest;
   switch(kind){
    case CYLINDER_BOTTOM:dest=pose.point(0,0,-1);break;
    case CYLINDER_TOP:dest=pose.point(0,0,1);break;
    default:dest=pose.point(Math.cos(angle),Math.sin(angle),0);break;
   }
   return unit(sub(dest,start));
  }
 }

 private final String bodyId;
 private final double diameter,height,volume,surfaceArea;
 private final RigidTransform pose;
 private final List<CircularEdge> edges;
 private final List<Face> faces;

 private CylinderBRep(String bodyId,double diameter,double height,RigidTransform pose,
                      List<CircularEdge> edges,List<Face> faces){
  this.bodyId=bodyId;this.diameter=diameter;this.height=height;this.pose=pose;
  this.edges=Collections.unmodifiableList(new ArrayList<>(edges));
  this.faces=Collections.unmodifiableList(new ArrayList<>(faces));
  double radius=diameter/2;
  volume=Math.PI*radius*radius*height;
  surfaceArea=2*Math.PI*radius*(height+radius);
 }

 private static double[] sub(double[] a,double[] b){
  return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};
 }
 private static double length(double[] p){
  return Math.sqrt(p[0]*p[0]+p[1]*p[1]+p[2]*p[2]);
 }
 private static double[] unit(double[] p){
  double n=length(p);
  if(!Double.isFinite(n)||n<=0)throw new IllegalArgumentException("Invalid orientation");
  return new double[]{p[0]/n,p[1]/n,p[2]/n};
 }
 private static String faceId(String id,Kind kind){
  return id+"/face/"+kind.name();
 }
 private static String edgeId(String id,Kind cap){
  return id+"/edge/"+cap.name();
 }

 public static CylinderBRep from(String bodyId,double diameter,double height,
                                 RigidTransform pose){
  if(bodyId==null||bodyId.trim().isEmpty()||pose==null||
     !Double.isFinite(diameter)||!Double.isFinite(height)||
     diameter<=0||height<=0||diameter>10000||height>10000)
   throw new IllegalArgumentException("Invalid cylinder");
  // The editor's current world-coordinate and dimensional limits.
  if(Math.abs(pose.tx)>10000||Math.abs(pose.ty)>10000||Math.abs(pose.tz)>10000)
   throw new IllegalArgumentException("Cylinder outside editor limits");
  double radius=diameter/2;
  double capArea=Math.PI*radius*radius;
  double lateralArea=Math.PI*diameter*height;
  if(!Double.isFinite(capArea)||!Double.isFinite(lateralArea))
   throw new IllegalArgumentException("Cylinder area overflow");
  String side=faceId(bodyId,Kind.CYLINDER_SIDE),
   bottom=faceId(bodyId,Kind.CYLINDER_BOTTOM),
   top=faceId(bodyId,Kind.CYLINDER_TOP);
  String lowerEdge=edgeId(bodyId,Kind.CYLINDER_BOTTOM),
   upperEdge=edgeId(bodyId,Kind.CYLINDER_TOP);
  List<CircularEdge> edges=Arrays.asList(
   new CircularEdge(lowerEdge,Kind.CYLINDER_BOTTOM,radius,0,pose,
                    Arrays.asList(side,bottom)),
   new CircularEdge(upperEdge,Kind.CYLINDER_TOP,radius,height,pose,
                    Arrays.asList(side,top)));
  List<Face> faces=Arrays.asList(
   new Face(side,Kind.CYLINDER_SIDE,lateralArea,pose,
            Arrays.asList(lowerEdge,upperEdge)),
   new Face(bottom,Kind.CYLINDER_BOTTOM,capArea,pose,
            Collections.singletonList(lowerEdge)),
   new Face(top,Kind.CYLINDER_TOP,capArea,pose,
            Collections.singletonList(upperEdge)));
  return new CylinderBRep(bodyId,diameter,height,pose,edges,faces);
 }

 public String bodyId(){return bodyId;}
 public double diameter(){return diameter;}
 public double height(){return height;}
 public double volume(){return volume;}
 public double surfaceArea(){return surfaceArea;}
 public RigidTransform pose(){return pose;}
 public List<Face> faces(){return faces;}
 public List<CircularEdge> edges(){return edges;}
 public Face findFace(String faceId){
  for(Face face:faces)if(face.id.equals(faceId))return face;return null;
 }
 public CircularEdge findEdge(String edgeId){
  for(CircularEdge edge:edges)if(edge.id.equals(edgeId))return edge;return null;
 }

 /** Pure, atomic analytic face-offset plan. Positive moves OUTWARD.
  * SIDE: change radius. TOP: extend height, bottom remains fixed.
  * BOTTOM: extend height by moving origin along negative local Z so TOP stays fixed.
  * No mutation, UUIDs and other dimensions are preserved.
  */
 public CylinderBRep offsetFace(Kind face,double millimeters){
  if(face==null||!Double.isFinite(millimeters))
   throw new IllegalArgumentException("Invalid face offset");
  double d=diameter,h=height;
  RigidTransform nextPose=pose;
  switch(face){
   case CYLINDER_SIDE:d+=2*millimeters;break;
   case CYLINDER_TOP:h+=millimeters;break;
   case CYLINDER_BOTTOM:
    h+=millimeters;
    double[] origin=pose.point(0,0,-millimeters);
    nextPose=new RigidTransform(origin[0],origin[1],origin[2],
                                pose.rx,pose.ry,pose.rz);
    break;
   default:throw new IllegalArgumentException("Unsupported face");
  }
  return from(bodyId,d,h,nextPose);
 }

 /** Mesh is generated on demand; angular segments never affect logical IDs. */
 public float[] triangles(int segments){
  return CylinderSurface.triangles(diameter,height,pose,segments);
 }
}
