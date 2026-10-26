import es.josxtm.mycad.cad.*;
public final class PrimitiveFaceHandleTest {
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);}
 static void near(double a,double b,String label){
  if(!Double.isFinite(a)||Math.abs(a-b)>1e-7*Math.max(1,Math.abs(b)))
   throw new AssertionError(label+" "+a+" != "+b);
 }
 static void vector(double[] a,double[] b,String label){
  ok(a.length==3&&b.length==3,label+" dimensions");
  for(int i=0;i<3;i++)near(a[i],b[i],label+"/"+i);
 }
 static void rejects(Runnable f,String msg){
  try{f.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid "+msg);
 }
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(25,-18,11,33,-42,16);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("box",20,10,8,pose);
  String[] names={"X_MIN","X_MAX","Y_MIN","Y_MAX","Z_MIN","Z_MAX"};
  double[][] points={{-10,0,4},{10,0,4},{0,-5,4},{0,5,4},{0,0,0},{0,0,8}};
  for(int i=0;i<names.length;i++){
   String id="box/face/"+names[i];
   PrimitiveFaceHandle handle=PrimitiveFaceHandle.from(box,id);
   vector(handle.anchor(),pose.point(points[i][0],points[i][1],points[i][2]),
          "Rotated box "+names[i]);
   vector(handle.normal(),box.findFace(id).outwardNormalAt(0),"Normal "+names[i]);
   ok(handle.bodyId.equals("box")&&handle.faceId.equals(id),"Stable face ref");
   ok(handle.gpu().length==6,"GPU handle layout");
  }
  PrimitiveBRepBody cyl=PrimitiveBRepBody.cylinder("cyl",20,8,pose);
  PrimitiveFaceHandle side=PrimitiveFaceHandle.from(cyl,"cyl/face/CYLINDER_SIDE");
  vector(side.anchor(),pose.point(10,0,4),"Rotated cylinder lateral");
  vector(side.normal(),cyl.findFace(side.faceId).outwardNormalAt(0),"Side normal");
  vector(PrimitiveFaceHandle.from(cyl,"cyl/face/CYLINDER_TOP").anchor(),
         pose.point(0,0,8),"Top cap center");
  vector(PrimitiveFaceHandle.from(cyl,"cyl/face/CYLINDER_BOTTOM").anchor(),
         pose.point(0,0,0),"Bottom cap center");
  PrimitiveFaceHandle extended=PrimitiveFaceHandle.from(
    cyl.offsetFace("cyl/face/CYLINDER_TOP",2),"cyl/face/CYLINDER_TOP");
  vector(extended.anchor(),pose.point(0,0,10),"Regenerated top");
  double[] copied=side.anchor();copied[0]=1234567;
  ok(side.anchor()[0]!=1234567,"Anchor defensive copy");
  float[] gpu=side.gpu();gpu[0]=1234567;
  ok(side.gpu()[0]!=1234567,"GPU buffer new on read");
  rejects(()->PrimitiveFaceHandle.from(null,"x"),"Null solid");
  rejects(()->PrimitiveFaceHandle.from(box,"cyl/face/CYLINDER_SIDE"),"Other body");
  rejects(()->PrimitiveFaceHandle.from(box,null),"Null face");
  System.out.println("PrimitiveFaceHandleTest: OK");
 }
}
