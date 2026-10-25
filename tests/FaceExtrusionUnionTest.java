import es.josxtm.mycad.*;
import es.josxtm.mycad.cad.*;
public final class FaceExtrusionUnionTest{
 static void near(double actual,double expected,String label){if(Math.abs(actual-expected)>1e-6)throw new AssertionError(label+" "+actual);}
 public static void main(String[] args){
  double w=10,d=20,h=30,x=4,y=6,z=2;
  for(BoxFaceSelection.Face face:BoxFaceSelection.Face.values()){
   FacePrismExtrusion.Result f=FacePrismExtrusion.create(w,d,h,x,y,z,0,0,0,face,5);
   BoxBoolean.Box original=new BoxBoolean.Box(x-w/2,y-d/2,z,w,d,h);
   BoxBoolean.Box added=new BoxBoolean.Box(f.x-f.w/2,f.y-f.d/2,f.z,f.w,f.d,f.h);
   float[] mesh=BoxBooleanMesh.build(original,added,BoxBooleanMesh.Operation.UNION);
   if(mesh.length==0||mesh.length%9!=0)throw new AssertionError("Missing union mesh "+face);
   // The adjacent extrusion touches only the selected face, so volume is additive.
   double expected=w*d*h+f.w*f.d*f.h;
   near(BoxBoolean.volumeUnion(original,added),expected,face.toString());
  }
  System.out.println("FaceExtrusionUnionTest: OK");
 }
}