import es.josxtm.mycad.cad.CameraRefitPolicy;
public final class CameraRefitPolicyTest {
 static void yes(boolean actual,boolean expected,String msg){
  if(actual!=expected)throw new AssertionError(msg);
 }
 public static void main(String[] args){
  yes(CameraRefitPolicy.shouldFit(false,false,true),true,
      "First nonempty geometry needs camera fit");
  yes(CameraRefitPolicy.shouldFit(false,true,true),true,
      "Explicit fit on first geometry");
  yes(CameraRefitPolicy.shouldFit(true,false,true),false,
      "Preserve user's camera during CAD edit");
  yes(CameraRefitPolicy.shouldFit(true,true,true),true,
      "Open another isolated mesh forces fit");
  yes(CameraRefitPolicy.shouldFit(false,false,false),false,
      "Empty first scene needs no fit");
  yes(CameraRefitPolicy.shouldFit(true,true,false),false,
      "Empty model cannot be fitted");
  yes(CameraRefitPolicy.rememberFitted(true),true,
      "Nonempty scenes retain initialized camera");
  yes(CameraRefitPolicy.rememberFitted(false),false,
      "Empty scene rearms first fit for next created solid");
  System.out.println("CameraRefitPolicyTest: OK");
 }
}
