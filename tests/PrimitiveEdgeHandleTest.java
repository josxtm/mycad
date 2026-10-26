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
