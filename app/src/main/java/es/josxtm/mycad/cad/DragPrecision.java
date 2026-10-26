package es.josxtm.mycad.cad;

/**
 * Precision of constrained face/edge gesture movements in millimetres.
 * The preview, the on-screen measurement and the confirmed edit must all
 * use this same offset. Snap is symmetric for positive/negative movement.
 */
public final class DragPrecision {
 private DragPrecision(){}

 public static double quantize(double inputMm,double limitMm,double stepMm){
  if(!Double.isFinite(inputMm)||!Double.isFinite(limitMm)||limitMm<=0||
      !Double.isFinite(stepMm)||stepMm<0||stepMm>10)
   throw new IllegalArgumentException("Invalid gesture precision");
  double limited=Math.max(-limitMm,Math.min(limitMm,inputMm));
  // Keep the exact, safe end-stop reachable, even if it is not a grid tick.
  if(Math.abs(inputMm)>=limitMm)return Math.copySign(limitMm,inputMm);
  if(stepMm==0)return limited==0?0:limited;
  double snapped=Math.copySign(Math.floor(Math.abs(limited)/stepMm+0.5)*stepMm,
                               limited);
  double output=Math.max(-limitMm,Math.min(limitMm,snapped));
  return output==0?0:output;
 }
}
