import es.josxtm.mycad.cad.*;
public final class BoxFaceSelectionTest{
 static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);}
 public static void main(String[] args){
  CadKernel.Box box=new PrimitiveKernel().createBox(0,0,0,2,4,6).value;
  TransformedBody body=new TransformedBody(box,new RigidTransform(10,20,30,0,0,90));
  for(BoxFaceSelection.Face face:BoxFaceSelection.Face.values()){
   int first=BoxFaceSelection.firstTriangle(face);
   check(BoxFaceSelection.faceForTriangle(first)==face,"First triangle");
   check(BoxFaceSelection.faceForTriangle(first+1)==face,"Second triangle");
   check(BoxFaceSelection.faceTriangles(body,face).length==18,"Six vertices per face");
  }
  boolean rejected=false;
  try{BoxFaceSelection.faceForTriangle(12);}catch(IllegalArgumentException ok){rejected=true;}
  check(rejected,"Reject non-face triangle");
  System.out.println("BoxFaceSelectionTest: OK");
 }
}
