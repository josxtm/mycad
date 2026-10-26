package es.josxtm.mycad.cad;

/**
 * Keep the user's pan/zoom/orientation while a saved CAD edit regenerates
 * the same scene. Fit explicitly on first nonempty scene and on actions that
 * ask to open/frame a different mesh. An empty scene re-arms the first fit.
 */
public final class CameraRefitPolicy {
 private CameraRefitPolicy(){}
 public static boolean shouldFit(boolean alreadyFitted,boolean forceFit,
                                 boolean hasTriangles){
  return hasTriangles&&(forceFit||!alreadyFitted);
 }
 public static boolean rememberFitted(boolean hasTriangles){
  return hasTriangles;
 }
}
