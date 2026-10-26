package es.josxtm.mycad.cad;

/**
 * Decide whether ACTION_UP should trigger a 3D hit test.
 *
 * New Android screens subscribe to SurfaceRayListener (ray + hit); older
 * callers may still subscribe only to SurfaceTapListener. Do not require
 * the legacy listener when a ray listener is already registered.
 */
public final class SurfaceTapPolicy {
 private SurfaceTapPolicy(){}
 public static boolean shouldPick(boolean gestureMoved,boolean hasRayListener,
                                  boolean hasLegacyListener){
  return !gestureMoved&&(hasRayListener||hasLegacyListener);
 }
}
