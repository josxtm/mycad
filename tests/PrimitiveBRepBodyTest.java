import es.josxtm.mycad.cad.*;
import java.util.*;

public final class PrimitiveBRepBodyTest {
 static void yes(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static void near(double a,double b,String msg){
  if(!Double.isFinite(a)||Math.abs(a-b)>1e-7*Math.max(1,Math.abs(b)))
   throw new AssertionError(msg+": "+a+" not "+b);
 }
 static void xyz(double[] a,double[] b,String msg){
  yes(a.length==3&&b.length==3,msg+" dimensions");
  for(int i=0;i<3;i++)near(a[i],b[i],msg+"/"+i);
 }
 static void reject(Runnable r,String msg){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted "+msg);
 }
 static void topology(PrimitiveBRepBody body,int faces,int edges){
  yes(body.faces().size()==faces&&body.edges().size()==edges,"Counts");
  HashSet<String> ff=new HashSet<>(),ee=new HashSet<>();
  for(PrimitiveBRepBody.Face f:body.faces()){
   yes(ff.add(f.id),"Duplicate face ID");
   yes(body.findFace(f.id)==f,"Unknown face");
   yes(f.area>0,"Invalid face area");
   double[] n=f.outwardNormalAt(.5);
   near(n[0]*n[0]+n[1]*n[1]+n[2]*n[2],1,"Unit normal");
   for(String edge:f.edgeIds())yes(body.findEdge(edge)!=null,"Missing edge");
  }
  for(PrimitiveBRepBody.Edge e:body.edges()){
   yes(ee.add(e.id),"Duplicate edge");
   yes(e.length>0,"Invalid length");
   yes(e.adjacentFaceIds().size()==2,"Expected two faces per boundary");
   for(String f:e.adjacentFaceIds())yes(body.findFace(f)!=null,"Orphan face");
  }
 }
 static void sameKeys(PrimitiveBRepBody a,PrimitiveBRepBody b){
  yes(a.faces().size()==b.faces().size(),"Face count changed");
  yes(a.edges().size()==b.edges().size(),"Edge count changed");
  for(int i=0;i<a.faces().size();i++)
   yes(a.faces().get(i).id.equals(b.faces().get(i).id),"Face name changed");
  for(int i=0;i<a.edges().size();i++)
   yes(a.edges().get(i).id.equals(b.edges().get(i).id),"Edge name changed");
 }
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(20,-7,18,30,40,55);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("box-persistent",20,10,8,pose);
  yes(box.kind==PrimitiveBRepBody.Kind.BOX,"Box kind");
  topology(box,6,12);
  near(box.volume(),1600,"Box volume");
  near(box.surfaceArea(),880,"Box surface");
  yes(box.triangles(48).length==108,"Box tessellation");
  String bx="box-persistent/face/X_MIN";
  PrimitiveBRepBody x=box.offsetFace(bx,3);
  sameKeys(box,x);
  near(x.width,23,"Width offset");
  xyz(x.pose().point(x.width/2,0,4),pose.point(10,0,4),
      "Opposite X face fixed after rotation");
  PrimitiveBRepBody lower=box.offsetFace("box-persistent/face/Z_MIN",3);
  xyz(lower.pose().point(0,0,lower.height),
      pose.point(0,0,8),"Top fixed when extending bottom");
  yes(!box.findFace(bx).edgeIds().isEmpty(),"Box face edges");
  near(box.width,20,"Box immutable");

  PrimitiveBRepBody cyl=PrimitiveBRepBody.cylinder("cyl-persistent",20,8,pose);
  yes(cyl.kind==PrimitiveBRepBody.Kind.CYLINDER,"Cylinder kind");
  topology(cyl,3,2);
  near(cyl.volume(),800*Math.PI,"Cylinder volume");
  near(cyl.surfaceArea(),360*Math.PI,"Cylinder area");
  yes(cyl.triangles(48).length==48*4*9,"Cylinder triangles");
  PrimitiveBRepBody radial=cyl.offsetFace("cyl-persistent/face/CYLINDER_SIDE",2);
  near(radial.width,24,"Cylinder width changes");
  near(radial.depth,24,"Cylinder depth synchronized");
  sameKeys(cyl,radial);
  PrimitiveBRepBody bottom=cyl.offsetFace("cyl-persistent/face/CYLINDER_BOTTOM",2);
  near(bottom.height,10,"Cylinder bottom offset");
  xyz(bottom.pose().point(0,0,10),pose.point(0,0,8),"Rotated cylinder top fixed");
  sameKeys(cyl,bottom);
  PrimitiveBRepBody top=cyl.offsetFace("cyl-persistent/face/CYLINDER_TOP",2);
  xyz(top.pose().point(0,0,0),pose.point(0,0,0),"Cylinder base fixed");
  near(top.height,10,"Cylinder top grows");

  float[] preview=box.triangles(48);preview[0]=99999;
  yes(box.triangles(48)[0]!=99999,"Mutated box preview leaked");
  float[] cylPreview=cyl.triangles(48);cylPreview[0]=99999;
  yes(cyl.triangles(48)[0]!=99999,"Mutated cylinder preview leaked");
  reject(()->cyl.offsetFace(bx,1),"other body face ID");
  reject(()->box.offsetFace("cyl-persistent/face/CYLINDER_SIDE",1),"other kind face ID");
  reject(()->box.offsetFace(bx,0),"zero motion");
  reject(()->box.offsetFace(bx,Double.POSITIVE_INFINITY),"infinite motion");
  reject(()->box.offsetFace(bx,-20),"collapsed box");
  reject(()->cyl.offsetFace("cyl-persistent/face/CYLINDER_SIDE",-10),"collapsed cylinder");
  reject(()->PrimitiveBRepBody.box("",20,10,8,pose),"empty ID");
  reject(()->PrimitiveBRepBody.box("x",-20,10,8,pose),"bad dimensions");
  reject(()->PrimitiveBRepBody.cylinder("x",20000,8,pose),"oversized cylinder");
  reject(()->box.triangles(8),"invalid tessellation resolution");
  try{cyl.faces().clear();throw new AssertionError("Mutable face list");}
  catch(UnsupportedOperationException expected){}
  try{box.edges().get(0).adjacentFaceIds().clear();
      throw new AssertionError("Mutable adjacency");}
  catch(UnsupportedOperationException expected){}
  System.out.println("PrimitiveBRepBodyTest: OK");
 }
}
