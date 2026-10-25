import es.josxtm.mycad.cad.*;
public final class SketchEditValidatorTest{
 static void test(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++){
   test(SketchEditValidator.canUpdateRectangle(plane,20,10,0,0,new double[]{5,10,-2}),"Linked planes");
   test(!SketchEditValidator.canUpdateRectangle(plane,0,0,0,10,new double[]{5}),"Zero width");
   test(!SketchEditValidator.canUpdateRectangle(plane,0,0,20,10,new double[]{5,0}),"Invalid dependent height");
   test(SketchEditValidator.canUpdateRectangle(plane,0,0,20,10,new double[]{}),"Empty dependency sketch");
  }
  test(!SketchEditValidator.canUpdateRectangle(3,0,0,10,10,new double[]{2}),"Invalid plane");
  test(!SketchEditValidator.canUpdateRectangle(0,0,0,Double.NaN,10,new double[]{2}),"NaN");
  System.out.println("SketchEditValidatorTest: OK");
 }
}
