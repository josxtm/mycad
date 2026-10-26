package es.josxtm.mycad.cad;

/**
 * Shared orientation commands for the 3x3 ViewCube.
 *
 * Coordinates are yaw/pitch in radians matching Scene and mapped into the
 * degree-based MeshEngine camera by the Android UI.
 */
public final class CubeCameraPreset {
 private CubeCameraPreset(){}
 public static final class Angles {
  public final float yawRadians,pitchRadians;
  private Angles(float yaw,float pitch){
   yawRadians=yaw;pitchRadians=pitch;
  }
  public float yawDegrees(){return (float)Math.toDegrees(yawRadians);}
  public float pitchDegrees(){return (float)Math.toDegrees(pitchRadians);}
 }
 public static Angles cell(int row,int col){
  if(row<0||row>2||col<0||col>2)
   throw new IllegalArgumentException("Invalid ViewCube cell");
  if(row==0&&col==1)return new Angles(0,1.5f);
  if(row==2&&col==1)return new Angles(0,-1.5f);
  if(row==1&&col==0)return new Angles((float)(Math.PI/2),0);
  if(row==1&&col==2)return new Angles((float)(-Math.PI/2),0);
  if(row==1&&col==1)return new Angles(0,0);
  float yaw=col==0?.75f:col==2?-.75f:0;
  float pitch=row==0?.65f:-.65f;
  return new Angles(yaw,pitch);
 }
}
