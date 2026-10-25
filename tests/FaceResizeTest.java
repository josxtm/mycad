import es.josxtm.mycad.cad.*;
public final class FaceResizeTest {
 static void eq(double a,double b,String m){if(Math.abs(a-b)>1e-9)throw new AssertionError(m);}
 public static void main(String[] args){
  for(BoxFaceSelection.Face face:BoxFaceSelection.Face.values()){
   FaceResize.Dimensions r=FaceResize.move(10,20,30,5,8,2,face,4);
   switch(face){
    case X_MIN:eq(r.w,14,"width");eq(r.x,3,"x min");break;
    case X_MAX:eq(r.w,14,"width");eq(r.x,7,"x max");break;
    case Y_MIN:eq(r.d,24,"depth");eq(r.y,6,"y min");break;
    case Y_MAX:eq(r.d,24,"depth");eq(r.y,10,"y max");break;
    case Z_MIN:eq(r.h,34,"height");eq(r.z,-2,"z min");break;
    case Z_MAX:eq(r.h,34,"height");eq(r.z,2,"z max");break;
   }
  }
  boolean rejected=false;
  try{FaceResize.move(10,20,30,0,0,0,BoxFaceSelection.Face.X_MAX,-10);}catch(IllegalArgumentException ok){rejected=true;}
  if(!rejected)throw new AssertionError("Collapsed box accepted");
  System.out.println("FaceResizeTest: OK");
 }
}