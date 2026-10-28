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
 /**
  * Use the actual 3D touch point instead of the camera's distance to the
  * scene focus. This matters when bodies sit in front of/behind the focus:
  * two equally sized parts at different depths should have different
  * model-space touch radii for the same 12dp target.
  *
  * The ray starts at the GL near plane, so its distance to the hit is a
  * perspective-depth approximation (slightly larger off axis), not an
  * exact projected curve/pixel distance.
  */
 public static double computeAtHit(PrimitiveBRepBody body,
                                   double ox,double oy,double oz,
                                   double hx,double hy,double hz,
                                   int viewportHeightPx,double displayDensity){
  if(!Double.isFinite(ox)||!Double.isFinite(oy)||!Double.isFinite(oz)||
     !Double.isFinite(hx)||!Double.isFinite(hy)||!Double.isFinite(hz))
   throw new IllegalArgumentException("Invalid surface hit");
  double depth=Math.hypot(hx-ox,Math.hypot(hy-oy,hz-oz));
  return compute(body,depth,viewportHeightPx,displayDensity);
 }
 public static double compute(PrimitiveBRepBody body,double cameraDistance,
                              int viewportHeightPx,double displayDensity){
  if(body==null)throw new IllegalArgumentException("Missing body");
  return compute(Math.min(body.width,Math.min(body.depth,body.height)),
   cameraDistance,viewportHeightPx,displayDensity);
 }
 /** Same touch policy for a regenerated non-primitive B-Rep shell. */
 public static double compute(double minDimension,double cameraDistance,
                              int viewportHeightPx,double displayDensity){
  if(!Double.isFinite(minDimension)||minDimension<=0||
     !Double.isFinite(cameraDistance)||cameraDistance<=0||
     cameraDistance>1e7||viewportHeightPx<=0||
     !Double.isFinite(displayDensity)||displayDensity<=0||displayDensity>10)
   throw new IllegalArgumentException("Invalid camera, body or screen");
  double min=minDimension;
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
