import es.josxtm.mycad.cad.*;
import java.util.*;

public final class CylinderBRepTest {
 static void check(boolean condition,String description){
  if(!condition)throw new AssertionError(description);
 }
 static void near(double actual,double expected,String description){
  if(!Double.isFinite(actual)||
    Math.abs(actual-expected)>1e-7*Math.max(1,Math.abs(expected)))
   throw new AssertionError(description+": "+actual+" != "+expected);
 }
 static void same(double[] x,double[] y,String label){
  check(x.length==3&&y.length==3,label+" length");
  for(int i=0;i<3;i++)near(x[i],y[i],label+"/"+i);
 }
 static void rejects(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid "+label);
 }
 static double dot(double[] a,double[] b){
  return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
 }
 static double[] diff(double[] a,double[] b){
  return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};
 }
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(17,-9,24,37,21,-64);
  CylinderBRep c=CylinderBRep.from("cyl-uuid",20,10,pose);
  near(c.volume(),1000*Math.PI,"Analytic volume");
  near(c.surfaceArea(),400*Math.PI,"Analytic surface area");
  check(c.faces().size()==3&&c.edges().size()==2,"3 faces, 2 circular edges");
  Set<String> faceIds=new HashSet<>(),edgeIds=new HashSet<>();
  for(CylinderBRep.Face f:c.faces()){
   check(faceIds.add(f.id),"Duplicate face ID");
   check(f.id.equals("cyl-uuid/face/"+f.kind.name()),"Logical face key");
   check(c.findFace(f.id)==f,"Face lookup");
   check(f.edgeIds().size()==(f.kind==CylinderBRep.Kind.CYLINDER_SIDE?2:1),
         "Face boundary loops");
   for(String id:f.edgeIds())check(c.findEdge(id)!=null,"Missing boundary edge");
   near(dot(f.outwardNormalAt(Math.PI/3),f.outwardNormalAt(Math.PI/3)),1,
        "Unit outward normal");
  }
  for(CylinderBRep.CircularEdge e:c.edges()){
   check(edgeIds.add(e.id),"Duplicate circle ID");
   check(e.id.equals("cyl-uuid/edge/"+e.cap.name()),"Stable edge key");
   check(e.adjacentFaceIds().size()==2,"Circle must join two faces");
   near(e.circumference,20*Math.PI,"Analytic circumference");
   near(e.radius,10,"Analytic radius");
   same(e.pointAt(0),pose.point(10,0,e.cap==CylinderBRep.Kind.CYLINDER_TOP?10:0),
        "Circle position");
   same(e.pointAt(2*Math.PI),e.pointAt(0),"Periodic closed edge");
   for(String id:e.adjacentFaceIds())check(c.findFace(id)!=null,
                                          "Orphan face adjacency");
  }
  for(int i=0;i<12;i++){
   double angle=i*2*Math.PI/12;
   double[] normal=c.faces().get(0).outwardNormalAt(angle);
   double[] radial=diff(c.edges().get(0).pointAt(angle),c.edges().get(0).center());
   near(dot(normal,radial)/10,1,"Rotated radial normal");
  }
  double[] axis=diff(pose.point(0,0,1),pose.point(0,0,0));
  near(dot(c.faces().get(2).outwardNormalAt(0),axis),1,"Top outward cap");
  near(dot(c.faces().get(1).outwardNormalAt(0),axis),-1,"Bottom outward cap");
  check(c.triangles(12).length==12*4*9,"Low resolution tessellation");
  check(c.triangles(48).length==48*4*9,"High resolution tessellation");
  float[] triangles=c.triangles(12);triangles[0]=123456;
  check(c.triangles(12)[0]!=123456,"Leaked mutable preview");

  CylinderBRep wider=c.offsetFace(CylinderBRep.Kind.CYLINDER_SIDE,2);
  near(wider.diameter(),24,"Offset side diameter");
  near(wider.height(),10,"Offset side fixed height");
  same(wider.pose().point(0,0,0),pose.point(0,0,0),"Side fixed origin");
  CylinderBRep higher=c.offsetFace(CylinderBRep.Kind.CYLINDER_TOP,3);
  near(higher.height(),13,"Top face offset");
  same(higher.pose().point(0,0,0),pose.point(0,0,0),"Bottom fixed on top move");
  CylinderBRep lower=c.offsetFace(CylinderBRep.Kind.CYLINDER_BOTTOM,3);
  near(lower.height(),13,"Bottom face offset");
  same(lower.pose().point(0,0,lower.height()),pose.point(0,0,10),
       "Opposite top remains fixed");

  for(CylinderBRep moved:new CylinderBRep[]{wider,higher,lower}){
   for(int i=0;i<3;i++)check(moved.faces().get(i).id.equals(c.faces().get(i).id),
                              "Face identity changed");
   for(int i=0;i<2;i++)check(moved.edges().get(i).id.equals(c.edges().get(i).id),
                              "Edge identity changed");
  }
  near(c.diameter(),20,"Original immutable diameter");
  near(c.height(),10,"Original immutable height");
  rejects(()->c.offsetFace(CylinderBRep.Kind.CYLINDER_SIDE,-10),"collapsed radius");
  rejects(()->c.offsetFace(CylinderBRep.Kind.CYLINDER_TOP,-10),"collapsed height");
  rejects(()->c.offsetFace(CylinderBRep.Kind.CYLINDER_BOTTOM,-11),"bottom collapse");
  rejects(()->c.offsetFace(null,1),"missing face");
  rejects(()->c.offsetFace(CylinderBRep.Kind.CYLINDER_SIDE,Double.NaN),"NaN");
  rejects(()->CylinderBRep.from("x",0,10,pose),"diameter zero");
  rejects(()->CylinderBRep.from("x",10,Double.POSITIVE_INFINITY,pose),"infinite height");
  rejects(()->CylinderBRep.from("x",10,10,null),"missing pose");
  rejects(()->CylinderBRep.from("",10,10,pose),"missing identity");
  rejects(()->c.triangles(5),"invalid triangulation resolution");
  rejects(()->c.edges().get(0).pointAt(Double.NaN),"invalid angle");
  try{c.faces().clear();throw new AssertionError("Mutable face list");}
  catch(UnsupportedOperationException expected){}
  try{c.edges().get(0).adjacentFaceIds().clear();
      throw new AssertionError("Mutable face adjacency");}
  catch(UnsupportedOperationException expected){}
  System.out.println("CylinderBRepTest: OK");
 }
}
