import es.josxtm.mycad.cad.*;
public final class FacePrismExtrusionTest {
 static void eq(double a,double b,String name){if(Math.abs(a-b)>1e-8)throw new AssertionError(name+": "+a);}
 public static void main(String[] args){
  for(BoxFaceSelection.Face face:BoxFaceSelection.Face.values()){
   FacePrismExtrusion.Result r=FacePrismExtrusion.create(10,20,30,0,0,0,0,0,0,face,4);
   if(face==BoxFaceSelection.Face.X_MAX){eq(r.w,4,"width");eq(r.x,7,"X+");}
   if(face==BoxFaceSelection.Face.X_MIN)eq(r.x,-7,"X-");
   if(face==BoxFaceSelection.Face.Y_MAX)eq(r.y,12,"Y+");
   if(face==BoxFaceSelection.Face.Y_MIN)eq(r.y,-12,"Y-");
   if(face==BoxFaceSelection.Face.Z_MAX)eq(r.z,30,"Z+");
   if(face==BoxFaceSelection.Face.Z_MIN)eq(r.z,-4,"Z-");
  }
  FacePrismExtrusion.Result rotated=FacePrismExtrusion.create(10,20,30,0,0,0,0,0,90,BoxFaceSelection.Face.X_MAX,4);
  eq(rotated.x,0,"Rotated X");eq(rotated.y,7,"Rotated Y");
  boolean bad=false;try{FacePrismExtrusion.create(10,20,30,0,0,0,0,0,0,BoxFaceSelection.Face.Z_MIN,-1);}catch(IllegalArgumentException ok){bad=true;}
  if(!bad)throw new AssertionError("Negative extrusion accepted");
  System.out.println("FacePrismExtrusionTest: OK");
 }
}