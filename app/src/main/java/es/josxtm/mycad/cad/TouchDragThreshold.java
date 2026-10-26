package es.josxtm.mycad.cad;

/** Separate an intentional camera orbit from small finger jitter on tap. */
public final class TouchDragThreshold {
 private TouchDragThreshold(){}
 public static boolean moved(boolean previous,double downX,double downY,
                             double x,double y,double thresholdPx){
  if(!Double.isFinite(downX)||!Double.isFinite(downY)||
     !Double.isFinite(x)||!Double.isFinite(y)||
     !Double.isFinite(thresholdPx)||thresholdPx<=0)
   throw new IllegalArgumentException("Invalid touch coordinates or threshold");
  return previous||Math.hypot(x-downX,y-downY)>thresholdPx;
 }
}
