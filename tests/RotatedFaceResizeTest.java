import es.josxtm.mycad.cad.*;
public final class RotatedFaceResizeTest{
 static void near(double a,double b,String label){if(Math.abs(a-b)>1e-8)throw new AssertionError(label+": "+a);}
 public static void main(String[] args){
  FaceResize.Dimensions x=RotatedFaceResize.move(10,20,30,100,200,300,0,0,90,BoxFaceSelection.Face.X_MAX,4);
  near(x.w,14,"width");near(x.x,100,"center X");near(x.y,202,"center Y");
  FaceResize.Dimensions z=RotatedFaceResize.move(10,20,30,0,0,0,90,0,0,BoxFaceSelection.Face.Z_MIN,4);
  near(z.h,34,"height");near(z.y,4,"rotated offset Y");near(z.z,0,"rotated offset Z");
  FaceResize.Dimensions plain=RotatedFaceResize.move(10,20,30,5,8,2,0,0,0,BoxFaceSelection.Face.Y_MIN,4);
  near(plain.y,6,"unrotated equivalent");
  boolean bad=false;
  try{RotatedFaceResize.move(10,20,30,0,0,0,0,0,90,BoxFaceSelection.Face.X_MIN,-10);}
  catch(IllegalArgumentException expected){bad=true;}
  if(!bad)throw new AssertionError("Collapsed box accepted");
  System.out.println("RotatedFaceResizeTest: OK");
 }
}
