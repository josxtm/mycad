import es.josxtm.mycad.cad.*;
import java.util.*;

public final class PrimitiveEdgeMoveTest {
 static void ok(boolean condition,String label){
  if(!condition)throw new AssertionError(label);
 }
 static void near(double value,double target,String label){
  if(!Double.isFinite(value)||
     Math.abs(value-target)>1e-7*Math.max(1,Math.abs(target)))
   throw new AssertionError(label+": "+value+" not "+target);
 }
 static void same(double[] a,double[] b,String label){
  ok(a.length==3&&b.length==3,label+" length");
  for(int j=0;j<3;j++)near(a[j],b[j],label+" axis "+j);
 }
 static void rejects(Runnable r,String label){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid: "+label);
 }
 static void stable(PrimitiveBRepBody before,PrimitiveBRepBody after){
  ok(before.faces().size()==after.faces().size(),"Faces replaced");
  ok(before.edges().size()==after.edges().size(),"Edges replaced");
  for(int i=0;i<before.faces().size();i++)
   ok(before.faces().get(i).id.equals(after.faces().get(i).id),"Face UUID not stable");
  for(int i=0;i<before.edges().size();i++)
   ok(before.edges().get(i).id.equals(after.edges().get(i).id),"Edge UUID not stable");
 }
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(48,-17,14,29,41,67);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("box-edge-id",20,10,8,pose);
  String xEdge="box-edge-id/edge/X_0";
  PrimitiveBRepBody.Edge e=box.findEdge(xEdge);
  ok(e!=null&&e.kind.equals("LINE"),"Selected X edge");
  ok(e.adjacentFaceIds().size()==2,"Two incident planar faces");
  double[] originalTop=pose.point(0,0,8);
  PrimitiveBRepBody both=box.offsetEdge(xEdge,3,2);
  PrimitiveBRepBody sequential=box.offsetFace(e.adjacentFaceIds().get(0),3)
                                  .offsetFace(e.adjacentFaceIds().get(1),2);
  near(both.width,sequential.width,"Box width");
  near(both.depth,sequential.depth,"Box depth");
  near(both.height,sequential.height,"Box height");
  same(both.pose().point(0,0,0),sequential.pose().point(0,0,0),
       "Same transformed origin as two edits");
  stable(box,both);
  near(box.width,20,"Original width");
  near(box.depth,10,"Original depth");
  near(box.height,8,"Original height");
  ok(box.findEdge(xEdge)!=null,"Original edge survives");
  // Z_MIN lies adjacent to X_0. Extending bottom must preserve original top.
  double bottomOffset=e.adjacentFaceIds().get(0).endsWith("/Z_MIN")?3:2;
  near(both.height,8+bottomOffset,"Axial dimension from edge");
  same(both.pose().point(0,0,both.height),originalTop,
       "Top face fixed after bottom face displacement");
  PrimitiveBRepBody firstOnly=box.offsetEdge(xEdge,0,1.5);
  PrimitiveBRepBody secondOnly=box.offsetEdge(xEdge,1.5,0);
  stable(box,firstOnly);stable(box,secondOnly);
  ok(firstOnly.volume()!=box.volume()&&secondOnly.volume()!=box.volume(),
     "Each adjacent face can be offset individually");
  double oldVolume=box.volume();
  rejects(()->box.offsetEdge(xEdge,-200,1),"first face collapses");
  rejects(()->box.offsetEdge(xEdge,1,-200),"second face collapses");
  near(box.volume(),oldVolume,"Failure leaves original untouched");

  PrimitiveBRepBody cyl=PrimitiveBRepBody.cylinder("cyl-edge-id",20,8,pose);
  String topId="cyl-edge-id/edge/CYLINDER_TOP";
  PrimitiveBRepBody.Edge ring=cyl.findEdge(topId);
  ok(ring!=null&&ring.kind.equals("CIRCLE"),"Circular rim present");
  ok(ring.adjacentFaceIds().size()==2,"Two incident curved/planar faces");
  PrimitiveBRepBody newTop=cyl.offsetEdge(topId,2,3);
  near(newTop.width,24,"Lateral outward changes diameter");
  near(newTop.depth,24,"Depth stays synchronized");
  near(newTop.height,11,"Top cap outward changes height");
  same(newTop.pose().point(0,0,0),pose.point(0,0,0),
       "Bottom held while changing top rim");
  stable(cyl,newTop);
  ok(newTop.findEdge(topId)!=null,"Circular rim survives edit");
  String bottomId="cyl-edge-id/edge/CYLINDER_BOTTOM";
  PrimitiveBRepBody down=cyl.offsetEdge(bottomId,1,3);
  near(down.width,22,"Radius of bottom ring");
  near(down.height,11,"Height after bottom ring");
  same(down.pose().point(0,0,down.height),pose.point(0,0,8),
       "Top held while extending bottom rim");
  stable(cyl,down);

  rejects(()->box.offsetEdge(null,1,0),"Missing edge");
  rejects(()->box.offsetEdge("other/edge/X_0",1,0),"Cross-body edge");
  rejects(()->box.offsetEdge(xEdge,0,0),"No movement");
  rejects(()->box.offsetEdge(xEdge,Double.NaN,2),"NaN");
  rejects(()->box.offsetEdge(xEdge,1,Double.POSITIVE_INFINITY),"Infinity");
  rejects(()->cyl.offsetEdge("cyl-edge-id/edge/INVALID",1,1),"Invalid circle");
  rejects(()->cyl.offsetEdge(topId,-10,0),"Collapsed cylinder radius");
  rejects(()->cyl.offsetEdge(bottomId,0,-8),"Collapsed cylinder height");
  near(cyl.width,20,"Original cylinder unchanged");
  near(cyl.height,8,"Original cylinder height unchanged");
  System.out.println("PrimitiveEdgeMoveTest: OK");
 }
}
