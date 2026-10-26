import es.josxtm.mycad.cad.*;

public final class PrimitiveEdgeHandleTest {
 static void ok(boolean good,String msg){if(!good)throw new AssertionError(msg);}
 static void near(double x,double y,String msg){
  if(!Double.isFinite(x)||Math.abs(x-y)>1e-7*Math.max(1,Math.abs(y)))
   throw new AssertionError(msg+" "+x+" != "+y);
 }
 static void point(double[] actual,double[] expected,String name){
  ok(actual.length==3&&expected.length==3,name+" dims");
  for(int i=0;i<3;i++)near(actual[i],expected[i],name+" axis "+i);
 }
 static void rejects(Runnable r,String name){
  try{r.run();}catch(IllegalArgumentException yes){return;}
  throw new AssertionError("Accepted "+name);
 }
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(35,-21,13,15,30,48);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("B",20,10,8,pose);
  for(PrimitiveBRepBody.Edge e:box.edges())for(int axis=0;axis<2;axis++){
   PrimitiveEdgeHandle handle=PrimitiveEdgeHandle.from(box,e.id,axis);
   ok(handle.edgeId.equals(e.id)&&handle.bodyId.equals("B"),"Persistent ref");
   ok(handle.incidentIndex==axis,"Axis index");
   ok(handle.faceId.equals(e.adjacentFaceIds().get(axis)),"Adjacent face");
   point(handle.normal(),box.findFace(handle.faceId).outwardNormalAt(0),
         "Rotated box normal");
   ok(handle.gpu().length==6,"GPU handle");
   PrimitiveMovePreview preview=PrimitiveMovePreview.forEdge(box,e.id);
   PrimitiveBRepBody moved=preview.propose(axis==0?.5:0,axis==1?.5:0);
   ok(moved.findEdge(e.id)!=null,"Stable edge after move");
  }
  PrimitiveEdgeHandle x=PrimitiveEdgeHandle.from(box,"B/edge/X_0",0);
  point(x.anchor(),pose.point(0,-5,0),"Box X_0 midpoint");
  PrimitiveBRepBody cylinder=PrimitiveBRepBody.cylinder("C",20,8,pose);
  for(PrimitiveBRepBody.Edge edge:cylinder.edges()){
   for(int index=0;index<2;index++){
    PrimitiveEdgeHandle handle=PrimitiveEdgeHandle.from(cylinder,edge.id,index);
    double z=edge.id.endsWith("TOP")?8:0;
    point(handle.anchor(),pose.point(10,0,z),"Circular rim angle zero");
    point(handle.normal(),cylinder.findFace(handle.faceId).outwardNormalAt(0),
          "Circular incident surface normal");
    PrimitiveBRepBody edited=PrimitiveMovePreview.forEdge(cylinder,edge.id)
      .propose(index==0?1:0,index==1?1:0);
    ok(edited.findEdge(edge.id)!=null,"Ring survives move");
   }
  }
  // The visual handle follows the actual touched position, not a fixed
  // midpoint or the cylinder's local +X rim point.
  double[] touchedLine=pose.point(7,-5,0);
  AnalyticEdgePicker.Hit boxHit=AnalyticEdgePicker.nearestEdgeOnFace(
      box,"B/face/Z_MIN",touchedLine[0],touchedLine[1],touchedLine[2],.001);
  ok(boxHit!=null&&boxHit.edgeId.equals("B/edge/X_0"),"Box hit edge");
  int bottomIndex=box.findEdge(boxHit.edgeId).adjacentFaceIds()
      .indexOf("B/face/Z_MIN");
  PrimitiveEdgeHandle touchedBox=PrimitiveEdgeHandle.fromHit(box,boxHit,bottomIndex);
  point(touchedBox.anchor(),touchedLine,"Off-centre touched line");
  point(touchedBox.normal(),box.findFace("B/face/Z_MIN").outwardNormalAt(0),
        "Touched box normal");
  PrimitiveBRepBody identical=PrimitiveBRepBody.box("B",20,10,8,pose);
  point(PrimitiveEdgeHandle.fromHit(identical,boxHit,bottomIndex).anchor(),
        touchedLine,"Reconstructed identical box accepted");
  PrimitiveBRepBody edited=box.offsetFace("B/face/Z_MAX",1);
  rejects(()->PrimitiveEdgeHandle.fromHit(edited,boxHit,bottomIndex),
      "Hit invalidated by opposite face edit");
  PrimitiveBRepBody shifted=PrimitiveBRepBody.box("B",20,10,8,
      new RigidTransform(36,-21,13,15,30,48));
  rejects(()->PrimitiveEdgeHandle.fromHit(shifted,boxHit,bottomIndex),
      "Hit invalidated by translation");
  rejects(()->PrimitiveEdgeHandle.fromHit(cylinder,boxHit,0),
      "Hit from a different body");

  double[] touchedCircle=pose.point(0,10,8);
  AnalyticEdgePicker.Hit rimHit=AnalyticEdgePicker.nearestEdgeOnFace(
      cylinder,"C/face/CYLINDER_SIDE",
      touchedCircle[0],touchedCircle[1],touchedCircle[2],.001);
  ok(rimHit!=null&&rimHit.edgeId.endsWith("CYLINDER_TOP"),"Touched circular rim");
  PrimitiveEdgeHandle touchedRim=PrimitiveEdgeHandle.fromHit(cylinder,rimHit,0);
  point(touchedRim.anchor(),touchedCircle,"Rim anchor follows touch angle");
  point(touchedRim.normal(),
      cylinder.findFace("C/face/CYLINDER_SIDE").outwardNormalAt(Math.PI/2),
      "Curved side normal follows touch angle");
  PrimitiveEdgeHandle topRim=PrimitiveEdgeHandle.fromHit(cylinder,rimHit,1);
  point(topRim.normal(),
      cylinder.findFace("C/face/CYLINDER_TOP").outwardNormalAt(0),
      "Top cap keeps its axial normal");
  rejects(()->PrimitiveEdgeHandle.fromHit(
      cylinder.offsetFace("C/face/CYLINDER_BOTTOM",1),rimHit,0),
      "Rim hit invalidated by opposite cap edit");
  rejects(()->PrimitiveEdgeHandle.fromHit(box,null,0),"Null hit");

  double[] defensive=x.anchor();defensive[0]=1e10;
  ok(x.anchor()[0]!=1e10,"Anchor defensive");
  float[] buf=x.gpu();buf[0]=1e10f;
  ok(x.gpu()[0]!=1e10f,"GPU defensive");
  rejects(()->PrimitiveEdgeHandle.from(box,null,0),"Null ID");
  rejects(()->PrimitiveEdgeHandle.from(box,"C/edge/CYLINDER_TOP",0),"Different body");
  rejects(()->PrimitiveEdgeHandle.from(box,"B/edge/X_0",2),"Invalid axis");
  rejects(()->PrimitiveEdgeHandle.from(null,"B/edge/X_0",0),"Null primitive");
  System.out.println("PrimitiveEdgeHandleTest: OK");
 }
}
