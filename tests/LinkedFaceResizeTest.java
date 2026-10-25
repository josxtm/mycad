import es.josxtm.mycad.cad.*;
public final class LinkedFaceResizeTest{
 static void eq(double a,double b){if(Math.abs(a-b)>1e-9)throw new AssertionError(a+" vs "+b);}
 public static void main(String[] args){
  for(int p=0;p<3;p++){
   double[] a=LinkedFaceResize.resize(p,2,3,12,23,BoxFaceSelection.Face.X_MAX,5);
   eq(a[0],2);eq(a[2],17);
   a=LinkedFaceResize.resize(p,12,23,2,3,BoxFaceSelection.Face.Y_MIN,2);
   eq(a[1],23);eq(a[3],1);
   boolean rejected=false;try{LinkedFaceResize.resize(p,2,3,12,23,BoxFaceSelection.Face.Z_MAX,4);}catch(IllegalArgumentException ok){rejected=true;}
   if(!rejected)throw new AssertionError("Extrusion axis should not resize profile");
  }
  System.out.println("LinkedFaceResizeTest: OK");
 }
}