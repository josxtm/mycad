import es.josxtm.mycad.cad.DragPrecision;

public final class DragPrecisionTest {
 static void near(double a,double b,String label){
  if(!Double.isFinite(a)||Math.abs(a-b)>1e-10)
   throw new AssertionError(label+": "+a+" != "+b);
 }
 static void rejects(Runnable r,String label){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid value accepted: "+label);
 }
 public static void main(String[] args){
  near(DragPrecision.quantize(1.234,8,0),1.234,"free gesture");
  near(DragPrecision.quantize(-1.234,8,0),-1.234,"free negative");
  near(DragPrecision.quantize(2.24,8,.1),2.2,"snap tenth");
  near(DragPrecision.quantize(2.25,8,.1),2.3,"snap positive halfway");
  near(DragPrecision.quantize(-2.25,8,.1),-2.3,"snap negative halfway");
  near(DragPrecision.quantize(2.23,8,.5),2,"snap half mm");
  near(DragPrecision.quantize(2.27,8,.5),2.5,"snap half mm upward");
  near(DragPrecision.quantize(-2.27,8,.5),-2.5,"snap half mm negative");
  near(DragPrecision.quantize(3.51,8,1),4,"snap whole mm");
  near(DragPrecision.quantize(-3.51,8,1),-4,"snap whole negative");
  near(DragPrecision.quantize(0,8,.5),0,"neutral");
  near(DragPrecision.quantize(-.1,8,1),0,"negative near neutral");
  near(DragPrecision.quantize(1000,7.3,1),7.3,"clamp AFTER snap");
  near(DragPrecision.quantize(-1000,7.3,1),-7.3,"negative clamp");
  near(DragPrecision.quantize(1000,7.3,0),7.3,"free clamp");
  near(DragPrecision.quantize(2.5,.4,1),.4,"small bound stays reachable");
  near(DragPrecision.quantize(.2,.4,1),0,"inside small bound snaps to zero");
  near(DragPrecision.quantize(-.45,.5,.1),-.5,"quantization at bound");
  rejects(()->DragPrecision.quantize(Double.NaN,1,.1),"NaN input");
  rejects(()->DragPrecision.quantize(1,Double.NaN,.1),"NaN limit");
  rejects(()->DragPrecision.quantize(1,1,Double.POSITIVE_INFINITY),"Infinite step");
  rejects(()->DragPrecision.quantize(1,0,.1),"Zero limit");
  rejects(()->DragPrecision.quantize(1,1,-.1),"Negative step");
  rejects(()->DragPrecision.quantize(1,1,100),"Step > 10");
  System.out.println("DragPrecisionTest: OK");
 }
}
