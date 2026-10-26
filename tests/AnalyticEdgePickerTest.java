import es.josxtm.mycad.cad.*;

public final class AnalyticEdgePickerTest {
 static void yes(boolean b,String label){if(!b)throw new AssertionError(label);}
 static void near(double a,double b,String label){
  if(Math.abs(a-b)>1e-7*Math.max(1,Math.abs(b)))throw new AssertionError(label+": "+a+" != "+b);
 }
 static void reject(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid "+label);
 }
 static void same(double[] a,double[] b,String label){
  yes(a.length==3&&b.length==3,label+" length");
  for(int k=0;k<3;k++)near(a[k],b[k],label+"/"+k);
 }
 static AnalyticEdgePicker.Hit find(PrimitiveBRepBody body,double[] xyz,double tol){
  return AnalyticEdgePicker.nearestEdge(body,xyz[0],xyz[1],xyz[2],tol);
 }
 public static void main(String[] args){
  RigidTransform rotated=new RigidTransform(40,-30,15,35,21,70);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("persistent-box",20,10,8,rotated);
  double[] atX=rotated.point(0,-5,0);
  AnalyticEdgePicker.Hit edge=find(box,atX,0.0001);
  yes(edge!=null&&edge.edgeId.equals("persistent-box/edge/X_0"),"Rotated box X edge");
  yes(edge.kind.equals("LINE")&&edge.bodyId.equals("persistent-box"),"Line topology");
  near(edge.distance,0,"Exact box hit");
  same(edge.closestPoint(),atX,"Exact line closest");
  double[] nearX=rotated.point(0,-4.8,0);
  edge=find(box,nearX,.25);
  yes(edge!=null&&edge.edgeId.equals("persistent-box/edge/X_0"),"Nearest rotated line");
  near(edge.distance,.2,"Line nearest distance");
  yes(find(box,nearX,.1)==null,"Respect caller tolerance");
  yes(find(box,rotated.point(0,0,0),.1)==null,"Interior not an edge");
  yes(find(box,rotated.point(0,-5,-5),.5)==null,"Outside finite segment");
  AnalyticEdgePicker.Hit corner=find(box,rotated.point(-10,-5,0),.1);
  yes(corner!=null&&corner.distance==0,"Vertex belongs to a logical edge");
  near(AnalyticEdgePicker.suggestedTolerance(box),.28,"Box auto tolerance");
  double[] returned=edge.closestPoint();returned[0]=1e12;
  yes(edge.closestPoint()[0]!=1e12,"Closest point is immutable");

  PrimitiveBRepBody cylinder=PrimitiveBRepBody.cylinder("persistent-cylinder",20,8,rotated);
  for(int i=0;i<16;i++){
   double a=i*Math.PI/8;
   double[] pt=rotated.point(10*Math.cos(a),10*Math.sin(a),8);
   AnalyticEdgePicker.Hit top=find(cylinder,pt,.001);
   yes(top!=null&&top.edgeId.equals("persistent-cylinder/edge/CYLINDER_TOP"),
       "Top circular rim");
   yes("CIRCLE".equals(top.kind),"Circular edge kind");
   near(top.distance,0,"Rim exact distance");
  }
  double[] closeTop=rotated.point(9.9,0,8.12);
  AnalyticEdgePicker.Hit top=find(cylinder,closeTop,.2);
  yes(top!=null&&top.edgeId.endsWith("/CYLINDER_TOP"),"Near top rim");
  near(top.distance,Math.hypot(.1,.12),"Analytic rim proximity");
  yes(find(cylinder,closeTop,.1)==null,"Rim threshold");
  yes(find(cylinder,rotated.point(10,0,4),.3)==null,"Middle lateral not rim");
  yes(find(cylinder,rotated.point(0,0,8),1)==null,"Cap interior not rim");
  AnalyticEdgePicker.Hit bottom=find(cylinder,rotated.point(-10,0,0),.001);
  yes(bottom!=null&&bottom.edgeId.endsWith("/CYLINDER_BOTTOM"),"Bottom rim");
  PrimitiveBRepBody taller=cylinder.offsetFace("persistent-cylinder/face/CYLINDER_TOP",2);
  yes(find(taller,rotated.point(-10,0,10),.001).edgeId.equals(
      "persistent-cylinder/edge/CYLINDER_TOP"),"Top ID survives parametric resize");
  PrimitiveBRepBody wider=box.offsetFace("persistent-box/face/X_MAX",2);
  yes(find(wider,rotated.point(0,-5,0),.001).edgeId.equals(
      "persistent-box/edge/X_0"),"Box edge ID survives resize");
  reject(()->AnalyticEdgePicker.nearestEdge(null,0,0,0,1),"Null body");
  reject(()->AnalyticEdgePicker.nearestEdge(box,Double.NaN,0,0,1),"NaN coordinate");
  reject(()->AnalyticEdgePicker.nearestEdge(box,0,0,0,-1),"Negative tolerance");
  reject(()->AnalyticEdgePicker.nearestEdge(box,0,0,0,Double.POSITIVE_INFINITY),
          "Infinite tolerance");
  System.out.println("AnalyticEdgePickerTest: OK");
 }
}
