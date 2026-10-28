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
  check(SurfaceTapPolicy.shouldPick(false,false,false,true),true,
    "Stepped B-Rep callback alone receives short taps");
  check(SurfaceTapPolicy.shouldPick(false,false,false,false),false,
    "No callback in the new API does not hit test");
  for(boolean ray:new boolean[]{false,true})
   for(boolean legacy:new boolean[]{false,true})
    for(boolean stepped:new boolean[]{false,true}){
     check(SurfaceTapPolicy.shouldPick(false,ray,legacy,stepped),
       ray||legacy||stepped,"Every callback combination on short tap");
     check(SurfaceTapPolicy.shouldPick(true,ray,legacy,stepped),false,
       "Orbit and drag suppress all callback combinations");
    }
  System.out.println("SurfaceTapPolicyTest: OK");
 }
}
