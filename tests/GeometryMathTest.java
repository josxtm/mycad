import es.josxtm.mycad.GeometryMath;
public class GeometryMathTest {
 static void near(double got,double expected){
  if(Math.abs(got-expected)>1e-8)throw new AssertionError("Expected "+expected+" got "+got);
 }
 static void invalid(Runnable r){
  try{r.run();throw new AssertionError("Expected invalid input");}
  catch(IllegalArgumentException expected){}
 }
 public static void main(String[] args){
  near(GeometryMath.rectangleVolume(20,10,5),1000);
  near(GeometryMath.rectangleVolume(20,10,8),1600);
  near(GeometryMath.cylinderVolume(2,10),10*Math.PI);
  for(int plane=0;plane<3;plane++){
   double[] p=GeometryMath.sketchToWorld(plane,12,7);
   near(p[0],plane==2?0:12);
   near(p[1],plane==0?7:plane==2?12:0);
   near(p[2],plane==0?0:7);
  }
  invalid(()->GeometryMath.rectangleVolume(-1,2,3));
  invalid(()->GeometryMath.rectangleVolume(Double.NaN,2,3));
  invalid(()->GeometryMath.sketchToWorld(3,0,0));
  System.out.println("GeometryMath: 3 planes, volumes and invalid-input checks OK");
 }
}
