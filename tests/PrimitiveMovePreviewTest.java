import es.josxtm.mycad.cad.*;

public final class PrimitiveMovePreviewTest {
 static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static void near(double x,double y,String msg){
  if(!Double.isFinite(x)||Math.abs(x-y)>1e-7*Math.max(1,Math.abs(y)))
   throw new AssertionError(msg+": "+x+" != "+y);
 }
 static void rejects(Runnable r,String name){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid preview accepted: "+name);
 }
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(9,-22,5,20,45,-15);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("persistent",20,10,8,pose);
  PrimitiveMovePreview face=PrimitiveMovePreview.forFace(box,"persistent/face/X_MAX");
  ok(face.target==PrimitiveMovePreview.Target.FACE,"Face target");
  ok(face.matches(box),"Baseline matches itself");
  ok(face.propose(0,0)==box,"Neutral preview is original");
  near(face.propose(2,0).width,22,"Face proposal 2");
  near(face.propose(3,0).width,23,"Face proposal 3 uses original");
  near(box.width,20,"Preview never mutated source");
  ok(face.matches(box),"Preview evaluation did not stale base");
  ok(!face.matches(face.propose(2,0)),"Edited geometry is stale");
  ok(!face.matches(PrimitiveBRepBody.box("other",20,10,8,pose)),
     "Other body ID rejected");
  ok(!face.matches(PrimitiveBRepBody.box("persistent",20,10,8,
      new RigidTransform(9,-22,5,20,45,-14))),"Rotation change rejected");

  PrimitiveMovePreview edge=PrimitiveMovePreview.forEdge(box,"persistent/edge/X_0");
  ok(edge.target==PrimitiveMovePreview.Target.EDGE,"Edge target");
  ok(edge.propose(0,0)==box,"Neutral edge preview");
  PrimitiveBRepBody changed=edge.propose(2,1);
  near(changed.width,20,"X edge keeps its X-length");
  ok(changed.volume()!=box.volume(),"Nontrivial edge edit");
  ok(changed.findEdge("persistent/edge/X_0")!=null,"Stable topology");
  ok(edge.matches(box),"Edge preview did not mutate the original");
  rejects(()->edge.propose(-100,3),"Invalid first offset");
  rejects(()->edge.propose(2,-100),"Invalid second offset");
  near(edge.propose(0,0).volume(),box.volume(),"Invalid trial leaves base intact");

  PrimitiveBRepBody cyl=PrimitiveBRepBody.cylinder("cyl",20,8,pose);
  PrimitiveMovePreview rim=PrimitiveMovePreview.forEdge(cyl,"cyl/edge/CYLINDER_TOP");
  near(rim.propose(2,3).width,24,"Cylinder radial preview");
  near(rim.propose(2,3).height,11,"Cylinder axial preview");
  ok(rim.matches(cyl),"Cylinder preview immutable");
  PrimitiveMovePreview cap=PrimitiveMovePreview.forFace(cyl,"cyl/face/CYLINDER_TOP");
  near(cap.propose(-2,0).height,6,"Cap height preview");
  rejects(()->cap.propose(2,1),"Face rejects secondary offset");
  rejects(()->cap.propose(Double.NaN,0),"Nonfinite displacement");
  rejects(()->rim.propose(0,Double.POSITIVE_INFINITY),"Infinite displacement");
  rejects(()->PrimitiveMovePreview.forFace(box,"wrong/face/X_MAX"),"Cross-body face");
  rejects(()->PrimitiveMovePreview.forEdge(box,"wrong/edge/X_0"),"Cross-body edge");
  rejects(()->PrimitiveMovePreview.forEdge(null,"x"),"Null body");
  System.out.println("PrimitiveMovePreviewTest: OK");
 }
}
