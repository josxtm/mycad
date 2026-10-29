package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Shared analytic B-Rep adapter for the two supported primitive solids.
 * All topological entities use the persistent body ID instead of renderer
 * triangle positions. No polygon meshes are treated as editable solids.
 *
 * Face offset produces an immutable proposed state (transaction-style). The
 * Android editor is responsible for undo checkpoint and committing the result.
 */
public final class PrimitiveBRepBody {
 public enum Kind { BOX, CYLINDER }

 public static final class Face {
  public final String id,kind;
  public final double area;
  private final List<String> edgeIds;
  private final double[] boxNormal;
  private final CylinderBRep.Face cylinder;
  Face(String id,String kind,double area,List<String> edges,double[] boxNormal,
       CylinderBRep.Face cylinder){
   this.id=id;this.kind=kind;this.area=area;
   edgeIds=Collections.unmodifiableList(new ArrayList<>(edges));
   this.boxNormal=boxNormal==null?null:boxNormal.clone();
   this.cylinder=cylinder;
  }
  public List<String> edgeIds(){return edgeIds;}
  /** Unit world normal. Cylinder sides vary with angular position (radians). */
  public double[] outwardNormalAt(double angle){
   if(!Double.isFinite(angle))throw new IllegalArgumentException("Invalid angle");
   return boxNormal!=null?boxNormal.clone():cylinder.outwardNormalAt(angle);
  }
 }

 public static final class Edge {
  public final String id,kind;
  public final double length;
  private final List<String> faceIds;
  Edge(String id,String kind,double length,List<String> faces){
   this.id=id;this.kind=kind;this.length=length;
   this.faceIds=Collections.unmodifiableList(new ArrayList<>(faces));
  }
  public List<String> adjacentFaceIds(){return faceIds;}
 }

 public final String bodyId;
 public final Kind kind;
 public final double width,depth,height,x,y,z,rx,ry,rz;
 private final List<Face> faces;
 private final List<Edge> edges;
 private final double volume,surfaceArea;

 private PrimitiveBRepBody(String bodyId,Kind kind,double width,double depth,
                          double height,RigidTransform pose){
  if(bodyId==null||bodyId.trim().isEmpty()||kind==null||pose==null||
     !Double.isFinite(width)||!Double.isFinite(depth)||!Double.isFinite(height)||
     width<=0||depth<=0||height<=0||width>10000||depth>10000||height>10000||
     Math.abs(pose.tx)>10000||Math.abs(pose.ty)>10000||Math.abs(pose.tz)>10000)
   throw new IllegalArgumentException("Invalid primitive");
  if(kind==Kind.CYLINDER&&Double.compare(width,depth)!=0)
   throw new IllegalArgumentException("Cylinder width and depth must be equal");
  this.bodyId=bodyId;this.kind=kind;
  this.width=width;this.depth=depth;this.height=height;
  x=pose.tx;y=pose.ty;z=pose.tz;rx=pose.rx;ry=pose.ry;rz=pose.rz;
  List<Face> f=new ArrayList<>();List<Edge> e=new ArrayList<>();
  if(kind==Kind.BOX){
   CadKernel.Box box=new CadKernel.Box(bodyId,-width/2,-depth/2,0,width,depth,height);
   BoxBRep solid=BoxBRep.from(new TransformedBody(box,pose));
   volume=solid.volume();surfaceArea=solid.surfaceArea();
   for(BoxBRep.Face face:solid.faces())
    f.add(new Face(face.id,face.slot.name(),face.area,face.edgeIds(),
                   face.outwardNormal(),null));
   for(BoxBRep.Edge edge:solid.edges())
    e.add(new Edge(edge.id,"LINE",edge.length,edge.adjacentFaceIds()));
  }else{
   CylinderBRep solid=CylinderBRep.from(bodyId,width,height,pose);
   volume=solid.volume();surfaceArea=solid.surfaceArea();
   for(CylinderBRep.Face face:solid.faces())
    f.add(new Face(face.id,face.kind.name(),face.area,face.edgeIds(),null,face));
   for(CylinderBRep.CircularEdge edge:solid.edges())
    e.add(new Edge(edge.id,"CIRCLE",edge.circumference,edge.adjacentFaceIds()));
  }
  faces=Collections.unmodifiableList(f);
  edges=Collections.unmodifiableList(e);
 }

 public static PrimitiveBRepBody box(String id,double width,double depth,double height,
                                    RigidTransform pose){
  return new PrimitiveBRepBody(id,Kind.BOX,width,depth,height,pose);
 }
 public static PrimitiveBRepBody cylinder(String id,double diameter,double height,
                                         RigidTransform pose){
  return new PrimitiveBRepBody(id,Kind.CYLINDER,diameter,diameter,height,pose);
 }
 public RigidTransform pose(){return new RigidTransform(x,y,z,rx,ry,rz);}
 public List<Face> faces(){return faces;}
 public List<Edge> edges(){return edges;}
 public double volume(){return volume;}
 public double surfaceArea(){return surfaceArea;}
 public Face findFace(String faceId){
  for(Face f:faces)if(f.id.equals(faceId))return f;
  return null;
 }
 public Edge findEdge(String edgeId){
  for(Edge e:edges)if(e.id.equals(edgeId))return e;
  return null;
 }

 /**
  * A face reference can be reused after dimensional edits if and only if the
  * body ID and face slot still exist. Unknown/cross-body keys are rejected.
  */
 public PrimitiveBRepBody offsetFace(String faceId,double mm){
  if(!Double.isFinite(mm)||mm==0||findFace(faceId)==null)
   throw new IllegalArgumentException("Invalid or stale face offset");
  if(kind==Kind.BOX){
   String suffix=faceId.substring((bodyId+"/face/").length());
   BoxFaceSelection.Face slot=BoxFaceSelection.Face.valueOf(suffix);
   FaceResize.Dimensions d=RotatedFaceResize.move(width,depth,height,
     x,y,z,rx,ry,rz,slot,mm);
   return box(bodyId,d.w,d.d,d.h,new RigidTransform(d.x,d.y,d.z,rx,ry,rz));
  }
  String suffix=faceId.substring((bodyId+"/face/").length());
  CylinderBRep.Kind slot=CylinderBRep.Kind.valueOf(suffix);
  CylinderBRep newCylinder=CylinderBRep.from(bodyId,width,height,pose()).offsetFace(slot,mm);
  return cylinder(bodyId,newCylinder.diameter(),newCylinder.height(),newCylinder.pose());
 }

 /**
  * Atomically moves the two faces meeting at an analytic primitive edge.
  *
  * Distances are signed outward offsets in millimetres, in the deterministic
  * order exposed by Edge.adjacentFaceIds(). A zero value holds that face fixed.
  * - Box edge: the two independent planar directions may be edited together.
  * - Cylinder circular rim: lateral direction changes diameter; axial
  *   direction extends the selected cap.
  *
  * Builds a NEW validated primitive; original body is never changed, including
  * when a second step fails. Edges/faces retain their persistent logical IDs.
  * This is not a bevel, fillet, or arbitrary B-Rep edge deformation.
  */
 public PrimitiveBRepBody offsetEdge(String edgeId,double firstMm,double secondMm){
  Edge target=findEdge(edgeId);
  if(target==null||!Double.isFinite(firstMm)||!Double.isFinite(secondMm)||
     (firstMm==0&&secondMm==0)||target.adjacentFaceIds().size()!=2)
   throw new IllegalArgumentException("Invalid or stale edge movement");
  List<String> adjacent=target.adjacentFaceIds();
  PrimitiveBRepBody proposed=this;
  if(firstMm!=0)proposed=proposed.offsetFace(adjacent.get(0),firstMm);
  if(secondMm!=0)proposed=proposed.offsetFace(adjacent.get(1),secondMm);
  if(proposed.findEdge(edgeId)==null)
   throw new IllegalArgumentException("Edge identity changed");
  return proposed;
 }

 /** Preview tessellation is secondary to the analytic model and always a new array. */
 /** Exact source positions in the identical triangle order as the legacy view. */
 public double[] triangleCoordinates(int cylinderSegments){
  if(kind==Kind.CYLINDER)return CylinderBRep.from(bodyId,width,height,pose()).triangleCoordinates(cylinderSegments);
  CadKernel.Box b=new CadKernel.Box(bodyId,-width/2,-depth/2,0,width,depth,height);
  return BoxBRep.from(new TransformedBody(b,pose())).triangleCoordinates();
 }
 public float[] triangles(int cylinderSegments){
  if(kind==Kind.CYLINDER)
   return CylinderBRep.from(bodyId,width,height,pose()).triangles(cylinderSegments);
  if(cylinderSegments<12||cylinderSegments>128)
   throw new IllegalArgumentException("Invalid preview resolution");
  CadKernel.Box b=new CadKernel.Box(bodyId,-width/2,-depth/2,0,width,depth,height);
  return BoxBRep.from(new TransformedBody(b,pose())).triangles();
 }
}
