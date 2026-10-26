package es.josxtm.mycad.cad;

/**
 * Three-finger tap is a navigation command, not CAD selection or a pinch.
 * Require the 3rd finger to arrive quickly and the whole gesture to finish
 * without a significant movement, otherwise do not reset the camera.
 */
public final class ThreeFingerHomePolicy {
 private ThreeFingerHomePolicy(){}
 public static boolean shouldReset(boolean threeFingersQualified,
   long firstTouchTimeMs,long firstLiftTimeMs,double maxTravelPx,
   double touchSlopPx){
  if(!threeFingersQualified||firstTouchTimeMs<0||
     firstLiftTimeMs<firstTouchTimeMs||
     !Double.isFinite(maxTravelPx)||maxTravelPx<0||
     !Double.isFinite(touchSlopPx)||touchSlopPx<=0)
   return false;
  return firstLiftTimeMs-firstTouchTimeMs<=500&&maxTravelPx<=touchSlopPx;
 }
 /** Avoid mistaking a slowly added 3rd finger during pinch for a home tap. */
 public static boolean canStart(long firstTouchTimeMs,long thirdDownTimeMs){
  return firstTouchTimeMs>=0&&thirdDownTimeMs>=firstTouchTimeMs&&
   thirdDownTimeMs-firstTouchTimeMs<=250;
 }
}
