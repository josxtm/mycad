import es.josxtm.mycad.cad.CubeCameraPreset;
public final class CubeCameraPresetTest {
 static void near(double a,double b,String label){
  if(!Double.isFinite(a)||Math.abs(a-b)>1e-5)
   throw new AssertionError(label+" "+a+" != "+b);
 }
 static void bad(Runnable fn,String label){
  try{fn.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid ViewCube "+label);
 }
 public static void main(String[] args){
  CubeCameraPreset.Angles top=CubeCameraPreset.cell(0,1);
  near(top.yawRadians,0,"Top yaw");
  near(top.pitchRadians,1.5,"Top pitch");
  near(top.pitchDegrees(),Math.toDegrees(1.5),"Top degrees");
  near(CubeCameraPreset.cell(2,1).pitchRadians,-1.5,"Bottom");
  near(CubeCameraPreset.cell(1,0).yawDegrees(),90,"Left orientation");
  near(CubeCameraPreset.cell(1,2).yawDegrees(),-90,"Right orientation");
  near(CubeCameraPreset.cell(1,1).yawDegrees(),0,"Front yaw");
  near(CubeCameraPreset.cell(1,1).pitchDegrees(),0,"Front pitch");
  near(CubeCameraPreset.cell(0,0).yawRadians,.75,"Upper left diagonal");
  near(CubeCameraPreset.cell(0,0).pitchRadians,.65,"Upper left pitch");
  near(CubeCameraPreset.cell(0,2).yawRadians,-.75,"Upper right");
  near(CubeCameraPreset.cell(2,0).pitchRadians,-.65,"Lower left");
  near(CubeCameraPreset.cell(2,2).pitchRadians,-.65,"Lower right");
  for(int row=0;row<3;row++)for(int col=0;col<3;col++){
   CubeCameraPreset.Angles c=CubeCameraPreset.cell(row,col);
   near(c.yawDegrees(),Math.toDegrees(c.yawRadians),"Yaw unit sync");
   near(c.pitchDegrees(),Math.toDegrees(c.pitchRadians),"Pitch unit sync");
  }
  bad(()->CubeCameraPreset.cell(-1,0),"Negative row");
  bad(()->CubeCameraPreset.cell(3,0),"Row high");
  bad(()->CubeCameraPreset.cell(0,-1),"Negative column");
  bad(()->CubeCameraPreset.cell(0,3),"Column high");
  System.out.println("CubeCameraPresetTest: OK");
 }
}
