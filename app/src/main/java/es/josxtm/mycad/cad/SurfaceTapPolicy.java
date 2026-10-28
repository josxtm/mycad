package es.josxtm.mycad.cad;

/**
 * Decide whether ACTION_UP should trigger a 3D hit test.
 *
 * New Android screens subscribe to SurfaceRayListener (ray + hit); older
 * callers may still subscribe only to SurfaceTapListener. Do not require
 * the legacy listener when a ray listener is already registered.
 * Opt-in stepped B-Rep subscribers also need to receive taps independently.
 */
public final class SurfaceTapPolicy {
 private SurfaceTapPolicy(){}
 public static boolean shouldPick(boolean gestureMoved,boolean hasRayListener,
                                  boolean hasLegacyListener){
  return shouldPick(gestureMoved,hasRayListener,hasLegacyListener,false);
 }
 /** Opt-in analytic patch callback; never pick during camera movement. */
 public static boolean shouldPick(boolean gestureMoved,boolean hasRayListener,
                                  boolean hasLegacyListener,boolean hasSteppedPatchListener){
  return !gestureMoved&&(hasRayListener||hasLegacyListener||hasSteppedPatchListener);
 }
}
