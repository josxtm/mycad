import es.josxtm.mycad.cad.*;

public final class PrimitiveEdgeCurveTest {
 static void ok(boolean yes,String name){if(!yes)throw new AssertionError(name);}
 static void near(double a,double b,String name){
  if(!Double.isFinite(a)||Math.abs(a-b)>2e-4*Math.max(1,Math.abs(b)))
   throw new AssertionError(name+" "+a+" != "+b);
 }
 static void point(float[] coords,int i,double[] expected,String msg){
  for(int j=0;j<3;j++)near(coords[i*3+j],expected[j],msg+"/"+j);
 }
 static void rejects(Runnable op,String msg){
  try{op.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted "+msg);
 }
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(32,-18,7,23,38,-41);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("box-edge",20,10,8,pose);
  String xEdge="box-edge/edge/X_0";
  float[] line=PrimitiveEdgeCurve.points(box,xEdge,48);
  ok(line.length==6,"Segment has two endpoints");
  point(line,0,pose.point(-10,-5,0),"Rotated start");
  point(line,1,pose.point(10,-5,0),"Rotated end");
  line[0]=1234567;
  ok(PrimitiveEdgeCurve.points(box,xEdge,48)[0]!=1234567,
      "Outlines are independent arrays");
  PrimitiveBRepBody wider=box.offsetFace("box-edge/face/Y_MAX",2);
  ok(PrimitiveEdgeCurve.points(wider,xEdge,48).length==6,
     "Edge identity survives dimensional edit");

  PrimitiveBRepBody cylinder=PrimitiveBRepBody.cylinder("rim",20,8,pose);
  String top="rim/edge/CYLINDER_TOP";
  for(int segments:new int[]{12,48,96,256}){
   float[] outline=PrimitiveEdgeCurve.points(cylinder,top,segments);
   ok(outline.length==(segments+1)*3,"Periodic edge length");
   for(int j=0;j<3;j++)ok(outline[j]==outline[segments*3+j],
                             "Exact closed circle");
   point(outline,0,pose.point(10,0,8),"Top circle start");
   point(outline,segments/4,pose.point(0,10,8),"Top 90 degree vertex");
  }
  float[] bottom=PrimitiveEdgeCurve.points(cylinder,"rim/edge/CYLINDER_BOTTOM",48);
  point(bottom,0,pose.point(10,0,0),"Bottom circle start");
  PrimitiveBRepBody up=cylinder.offsetFace("rim/face/CYLINDER_TOP",2);
  point(PrimitiveEdgeCurve.points(up,top,48),0,pose.point(10,0,10),
        "Circle outline regenerates from edited model");
  rejects(()->PrimitiveEdgeCurve.points(box,"rim/edge/CYLINDER_TOP",48),
          "Cross-body edge");
  rejects(()->PrimitiveEdgeCurve.points(cylinder,null,48),"Null edge");
  rejects(()->PrimitiveEdgeCurve.points(null,top,48),"Null body");
  rejects(()->PrimitiveEdgeCurve.points(cylinder,top,8),"Too few segments");
  rejects(()->PrimitiveEdgeCurve.points(cylinder,top,257),"Too many segments");
  System.out.println("PrimitiveEdgeCurveTest: OK");
 }
}
