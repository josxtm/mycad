package es.josxtm.mycad.cad;

/**
 * Converts a touch target in screen pixels to an approximate model-space edge
 * hit radius. This is a selection tolerance only: geometry remains exact.
 *
 * At a perspective FOV of 45 degrees, the world span at the camera focus is
 * 2 * distance * tan(22.5 degrees). Clamp by primitive size to avoid
 * accidentally selecting a distant edge when the camera is far away.
 */
public final class EdgeScreenTolerance {
 private EdgeScreenTolerance(){}
 public static double compute(PrimitiveBRepBody body,double cameraDistance,
                              int viewportHeightPx,double displayDensity){
  if(body==null||!Double.isFinite(cameraDistance)||cameraDistance<=0||
     cameraDistance>1e7||viewportHeightPx<=0||
     !Double.isFinite(displayDensity)||displayDensity<=0||displayDensity>10)
   throw new IllegalArgumentException("Invalid camera, body or screen");
  double min=Math.min(body.width,Math.min(body.depth,body.height));
  double worldPerPixel=2*cameraDistance*Math.tan(Math.toRadians(22.5))/
                       viewportHeightPx;
  // 12 dp gives a finger-friendly target; avoid arbitrarily huge targets.
  double touchPx=12*displayDensity;
  double desired=worldPerPixel*touchPx;
  double lower=Math.max(0.02,Math.min(0.25,min*.004));
  double upper=Math.max(lower,Math.min(8.0,min*.12));
  if(!Double.isFinite(desired))throw new IllegalArgumentException("Invalid projection");
  return Math.max(lower,Math.min(upper,desired));
 }
}
