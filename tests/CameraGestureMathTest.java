import es.josxtm.mycad.cad.CameraGestureMath;
public final class CameraGestureMathTest {
 static void near(double a,double b,String msg){
  if(!Double.isFinite(a)||Math.abs(a-b)>1e-8*Math.max(1,Math.abs(b)))
   throw new AssertionError(msg+": "+a+" != "+b);
 }
 static void rejects(Runnable op,String name){
  try{op.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid camera gesture accepted: "+name);
 }
 public static void main(String[] args){
  near(CameraGestureMath.zoomFactor(100,100),1,"Static pinch");
  near(CameraGestureMath.zoomFactor(100,200),2,"Open fingers zoom in");
  near(CameraGestureMath.zoomFactor(200,100),.5,"Close fingers zoom out");
  near(CameraGestureMath.zoomFactor(1,1000),4,"Pinch max clamp");
  near(CameraGestureMath.zoomFactor(1000,1),.25,"Pinch min clamp");
  near(CameraGestureMath.worldPerPixel(100,800),
    200*Math.tan(Math.toRadians(22.5))/800,"Perspective scale");
  near(CameraGestureMath.worldPerPixel(100,1600),
    CameraGestureMath.worldPerPixel(100,800)/2,"Viewport pixel density");
  double scale=CameraGestureMath.worldPerPixel(100,800);
  double[] pan=CameraGestureMath.pan(0,0,0,0,0,100,800,100,50);
  near(pan[0],0,"Yaw zero pan X");
  near(pan[1],-100*scale,"Yaw zero moves opposite camera-right");
  near(pan[2],50*scale,"Finger down moves focus up");
  pan=CameraGestureMath.pan(11,-21,7,90,0,100,800,100,0);
  near(pan[0],11+100*scale,"Yaw 90 right basis");
  near(pan[1],-21,"Yaw 90 no Y shift");
  near(pan[2],7,"Horizontal pan preserves height");
  pan=CameraGestureMath.pan(0,0,0,0,45,100,800,0,60);
  near(pan[0],-Math.sin(Math.toRadians(45))*60*scale,
       "Pitch-aware screen-up projects into X");
  near(pan[1],0,"Pitch pan Y remains zero");
  near(pan[2],Math.cos(Math.toRadians(45))*60*scale,
       "Pitch-aware up projects into Z");
  double[] inverse=CameraGestureMath.pan(pan[0],pan[1],pan[2],0,45,100,800,0,-60);
  for(int i=0;i<3;i++)near(inverse[i],0,"Pan reversible");
  rejects(()->CameraGestureMath.zoomFactor(0,10),"Zero spread");
  rejects(()->CameraGestureMath.zoomFactor(10,Double.NaN),"NaN spread");
  rejects(()->CameraGestureMath.worldPerPixel(0,800),"Zero camera distance");
  rejects(()->CameraGestureMath.worldPerPixel(100,0),"Zero viewport height");
  rejects(()->CameraGestureMath.pan(0,0,0,0,0,100,800,Double.NaN,0),
    "NaN touch movement");
  rejects(()->CameraGestureMath.pan(0,0,0,0,0,100,-1,3,1),
    "Negative viewport");
  System.out.println("CameraGestureMathTest: OK");
 }
}
