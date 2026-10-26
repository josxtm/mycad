package es.josxtm.mycad.cad;

/**
 * Stable two-finger navigation in the world-XYZ frame.
 * Pinch zooms by the distance ratio; centroid drag pans in the camera
 * right/up plane. Pure Java for regression tests outside Android.
 */
public final class CameraGestureMath {
 private CameraGestureMath(){}
 private static void finite(double... values){
  for(double x:values)if(!Double.isFinite(x))
   throw new IllegalArgumentException("Nonfinite camera gesture");
 }
 /** Current/previous pinch spread. 1.0 means no zoom. */
 public static double zoomFactor(double previousSpan,double nextSpan){
  finite(previousSpan,nextSpan);
  if(previousSpan<=0||nextSpan<=0)
   throw new IllegalArgumentException("Invalid pinch spread");
  return Math.max(.25,Math.min(4.0,nextSpan/previousSpan));
 }
 /** Local mm per pixel at the camera focus for a 45-degree perspective. */
 public static double worldPerPixel(double distance,int height){
  finite(distance);
  if(distance<=0||height<=0)
   throw new IllegalArgumentException("Invalid camera viewport");
  return 2*distance*Math.tan(Math.toRadians(22.5))/height;
 }
 /**
  * Finger midpoint movement in screen pixels, +X right/+Y down.
  * Pan the CAMERA focus in the opposite horizontal direction and
  * upward for positive screen Y to make the object follow the fingers.
  */
 public static double[] pan(double cx,double cy,double cz,double yawDegrees,
                            double pitchDegrees,double distance,int viewportHeight,
                            double deltaXpx,double deltaYpx){
  finite(cx,cy,cz,yawDegrees,pitchDegrees,distance,deltaXpx,deltaYpx);
  double mm=worldPerPixel(distance,viewportHeight),
         yaw=Math.toRadians(yawDegrees),pitch=Math.toRadians(pitchDegrees),
         rightX=-Math.sin(yaw),rightY=Math.cos(yaw),
         upX=-Math.sin(pitch)*Math.cos(yaw),
         upY=-Math.sin(pitch)*Math.sin(yaw),upZ=Math.cos(pitch);
  double nextX=cx-rightX*deltaXpx*mm+upX*deltaYpx*mm,
         nextY=cy-rightY*deltaXpx*mm+upY*deltaYpx*mm,
         nextZ=cz+upZ*deltaYpx*mm;
  finite(nextX,nextY,nextZ);
  return new double[]{nextX,nextY,nextZ};
 }
}
