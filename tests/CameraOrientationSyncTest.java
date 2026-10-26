import es.josxtm.mycad.cad.CameraOrientationSync;

public final class CameraOrientationSyncTest {
 static void near(double actual,double expected,String label){
  if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-5)
   throw new AssertionError(label+": "+actual+" != "+expected);
 }
 static void rejects(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Bad angle accepted: "+label);
 }
 public static void main(String[] args){
  near(CameraOrientationSync.yawDegrees(0),0,"Zero yaw");
  near(CameraOrientationSync.yawDegrees(35),35,"Isometric yaw");
  near(CameraOrientationSync.yawDegrees(370),10,"Wrap positive turn");
  near(CameraOrientationSync.yawDegrees(-370),-10,"Wrap negative turn");
  near(CameraOrientationSync.yawDegrees(180),-180,"Half-turn normal form");
  near(CameraOrientationSync.yawDegrees(-180),-180,"Negative half-turn");
  near(CameraOrientationSync.yawDegrees(900),-180,"Many turns");
  near(CameraOrientationSync.pitchDegrees(25),25,"Isometric pitch");
  near(CameraOrientationSync.pitchDegrees(110),89,"Clamp top");
  near(CameraOrientationSync.pitchDegrees(-110),-89,"Clamp bottom");
  float[] scene=CameraOrientationSync.sceneRadians(395,25);
  near(scene[0],Math.toRadians(35),"Degrees to Canvas yaw");
  near(scene[1],Math.toRadians(25),"Degrees to Canvas pitch");
  float[] top=CameraOrientationSync.sceneRadians(0,90);
  near(top[1],Math.toRadians(89),"Near-pole scene protects camera");
  float[] negative=CameraOrientationSync.sceneRadians(-450,-90);
  near(negative[0],Math.toRadians(-90),"Wrapped negative heading");
  near(negative[1],Math.toRadians(-89),"Near-bottom scene");
  rejects(()->CameraOrientationSync.yawDegrees(Float.NaN),"NaN yaw");
  rejects(()->CameraOrientationSync.pitchDegrees(Float.POSITIVE_INFINITY),
          "Infinite pitch");
  rejects(()->CameraOrientationSync.sceneRadians(0,Float.NaN),"NaN scene");
  System.out.println("CameraOrientationSyncTest: OK");
 }
}
