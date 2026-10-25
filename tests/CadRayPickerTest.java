import es.josxtm.mycad.cad.*;
public final class CadRayPickerTest {
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args){
  CadKernel.Box b=new PrimitiveKernel().createBox(0,0,0,2,4,6).value;
  float[] triangles=BoxSurface.triangles(new TransformedBody(b,new RigidTransform(10,20,30,0,0,90)));
  CadRayPicker.Hit hit=CadRayPicker.pick(triangles,8,21,50,0,0,-1);
  ok(hit!=null&&Math.abs(hit.z-36)<1e-6,"Top surface selection");
  ok(hit.triangle>=0&&hit.triangle<12,"Triangle identity");
  ok(CadRayPicker.pick(triangles,100,100,50,0,0,-1)==null,"Miss");
  boolean invalid=false;
  try{CadRayPicker.pick(triangles,0,0,0,0,0,0);}catch(IllegalArgumentException expected){invalid=true;}
  ok(invalid,"Reject invalid ray");
  System.out.println("CadRayPickerTest: OK");
 }
}
