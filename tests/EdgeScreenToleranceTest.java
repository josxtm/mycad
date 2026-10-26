import es.josxtm.mycad.cad.*;

public final class EdgeScreenToleranceTest {
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);}
 static void near(double a,double b,String label){
  if(Math.abs(a-b)>1e-9*Math.max(1,Math.abs(b)))
   throw new AssertionError(label+": "+a+" != "+b);
 }
 static void rejects(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid "+label);
 }
 public static void main(String[] args){
  PrimitiveBRepBody box=PrimitiveBRepBody.box("tolerance",100,70,50,
    new RigidTransform(0,0,0,30,-18,53));
  double close=EdgeScreenTolerance.compute(box,20,800,2);
  double medium=EdgeScreenTolerance.compute(box,100,800,2);
  double far=EdgeScreenTolerance.compute(box,400,800,2);
  ok(close>0&&medium>close&&far>medium,
     "Zoom out increases allowable real distance to a screen-space edge");
  near(medium,2*100*Math.tan(Math.toRadians(22.5))/800*24,
       "Pixel scale matched camera perspective");
  near(EdgeScreenTolerance.compute(box,100,1600,2),medium/2,
       "Double resolution halves model-space tolerance");
  near(EdgeScreenTolerance.compute(box,100,800,4),2*medium,
       "Higher device density enlarges touch target");
  double max=EdgeScreenTolerance.compute(box,1e7,100,3);
  near(max,6,"Safe maximum is bounded by smallest dimension");
  double min=EdgeScreenTolerance.compute(box,.0001,10000,1);
  near(min,.2,"Minimum depends on smallest dimension");
  PrimitiveBRepBody tiny=PrimitiveBRepBody.cylinder("tiny",.4,.3,
    new RigidTransform(0,0,0,0,0,0));
  double tinyLimit=EdgeScreenTolerance.compute(tiny,1e6,100,3);
  near(tinyLimit,.036,"Tiny cylinder's edge tolerance bounded by geometry");
  ok(tinyLimit<max,"Small parts do not select distant edges");
  // Perspective touch tolerance follows the actual hit depth, not the
  // distance to the scene's focus (which can be far from the touched body).
  double hitNear=EdgeScreenTolerance.computeAtHit(
      box,0,0,0,0,0,40,800,2);
  double hitFar=EdgeScreenTolerance.computeAtHit(
      box,0,0,0,0,0,160,800,2);
  ok(hitNear<hitFar,"Further body has larger mm target at fixed pixel size");
  near(hitNear,EdgeScreenTolerance.compute(box,40,800,2),
       "Hit depth equals reference model-space perspective");
  near(hitFar,EdgeScreenTolerance.compute(box,160,800,2),
       "Distant hit depth uses actual touched geometry");
  near(EdgeScreenTolerance.computeAtHit(box,10,-3,8,10,-3,48,800,2),
       hitNear,"Translation of ray origin and hit does not change tolerance");
  near(EdgeScreenTolerance.computeAtHit(box,0,0,0,30,40,0,800,2),
       EdgeScreenTolerance.compute(box,50,800,2),
       "Slanted ray uses physical near-plane to hit distance");
  rejects(()->EdgeScreenTolerance.computeAtHit(
      box,0,0,0,0,0,0,800,2),"Coincident origin and hit");
  rejects(()->EdgeScreenTolerance.computeAtHit(
      box,Double.NaN,0,0,0,0,10,800,2),"NaN ray origin");
  rejects(()->EdgeScreenTolerance.computeAtHit(
      box,0,0,0,Double.POSITIVE_INFINITY,0,10,800,2),"Infinite hit");
  rejects(()->EdgeScreenTolerance.computeAtHit(
      box,0,0,0,0,0,10,0,2),"Zero viewport at hit");
  rejects(()->EdgeScreenTolerance.compute(null,100,100,2),"No body");
  rejects(()->EdgeScreenTolerance.compute(box,Double.NaN,100,2),"NaN camera");
  rejects(()->EdgeScreenTolerance.compute(box,0,100,2),"Zero camera distance");
  rejects(()->EdgeScreenTolerance.compute(box,100,0,2),"Zero viewport");
  rejects(()->EdgeScreenTolerance.compute(box,100,100,Double.POSITIVE_INFINITY),
          "Infinite density");
  rejects(()->EdgeScreenTolerance.compute(box,100,100,-1),"Negative density");
  rejects(()->EdgeScreenTolerance.compute(box,1e8,100,1),"Unrealistic camera");
  System.out.println("EdgeScreenToleranceTest: OK");
 }
}
