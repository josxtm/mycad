import es.josxtm.mycad.cad.SurfaceTapPolicy;

public final class SurfaceTapPolicyTest {
 static void check(boolean value,boolean expected,String name){
  if(value!=expected)throw new AssertionError(name+": "+value);
 }
 public static void main(String[] args){
  check(SurfaceTapPolicy.shouldPick(false,true,false),true,
    "Modern ray callback receives a short tap even without legacy callback");
  check(SurfaceTapPolicy.shouldPick(false,false,true),true,
    "Legacy tap callback remains supported");
  check(SurfaceTapPolicy.shouldPick(false,true,true),true,
    "Both callbacks may be registered");
  check(SurfaceTapPolicy.shouldPick(false,false,false),false,
    "No callbacks: do not hit test");
  for(boolean ray:new boolean[]{false,true})
   for(boolean legacy:new boolean[]{false,true})
    check(SurfaceTapPolicy.shouldPick(true,ray,legacy),false,
      "Camera movement cannot dispatch an unwanted hit");
  System.out.println("SurfaceTapPolicyTest: OK");
 }
}
