package es.josxtm.mycad.cad;

/**
 * Camera yaw/pitch conversions shared by the GPU viewport and the Canvas
 * ViewCube. MeshEngine works in degrees; the Canvas scene uses radians.
 *
 * Normalize yaw before float trigonometry to avoid lost precision after
 * many touch rotations. Neither the focus nor the zoom changes.
 */
public final class CameraOrientationSync {
 private CameraOrientationSync(){}

 public static float yawDegrees(float degrees){
  if(!Float.isFinite(degrees))
   throw new IllegalArgumentException("Nonfinite yaw");
  double normalized=(double)degrees%360.0;
  if(normalized>=180)normalized-=360;
  if(normalized< -180)normalized+=360;
  return (float)normalized;
 }

 public static float pitchDegrees(float degrees){
  if(!Float.isFinite(degrees))
   throw new IllegalArgumentException("Nonfinite pitch");
  return Math.max(-89f,Math.min(89f,degrees));
 }

 /** Array [yaw radians, pitch radians] for the existing Canvas Scene. */
 public static float[] sceneRadians(float yawDeg,float pitchDeg){
  return new float[]{
   (float)Math.toRadians(yawDegrees(yawDeg)),
   (float)Math.toRadians(pitchDegrees(pitchDeg))
  };
 }
}
