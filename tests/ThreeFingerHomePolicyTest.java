import es.josxtm.mycad.cad.ThreeFingerHomePolicy;

public final class ThreeFingerHomePolicyTest {
 static void eq(boolean actual,boolean expected,String msg){
  if(actual!=expected)throw new AssertionError(msg);
 }
 public static void main(String[] args){
  eq(ThreeFingerHomePolicy.canStart(1000,1200),true,"Rapid three fingers");
  eq(ThreeFingerHomePolicy.canStart(1000,1250),true,"Start inclusive");
  eq(ThreeFingerHomePolicy.canStart(1000,1251),false,"Late third finger");
  eq(ThreeFingerHomePolicy.canStart(1000,999),false,"Bad event order");
  eq(ThreeFingerHomePolicy.shouldReset(true,1000,1450,2,12),true,
     "Tap returns to home view");
  eq(ThreeFingerHomePolicy.shouldReset(true,1000,1500,12,12),true,
     "Boundary accepted");
  eq(ThreeFingerHomePolicy.shouldReset(true,1000,1501,0,12),false,
     "Long hold not a tap");
  eq(ThreeFingerHomePolicy.shouldReset(true,1000,1400,13,12),false,
     "Moving three fingers must not reset");
  eq(ThreeFingerHomePolicy.shouldReset(false,1000,1200,0,12),false,
     "Four-finger or late-arrival disqualified");
  eq(ThreeFingerHomePolicy.shouldReset(true,1000,900,0,12),false,
     "Nonmonotonic event time");
  eq(ThreeFingerHomePolicy.shouldReset(true,1000,1400,Double.NaN,12),false,
     "NaN movement");
  eq(ThreeFingerHomePolicy.shouldReset(true,1000,1400,0,0),false,
     "Invalid touch slop");
  eq(ThreeFingerHomePolicy.shouldReset(true,1000,1400,0,24),true,
     "Dpi-adjusted threshold");
  System.out.println("ThreeFingerHomePolicyTest: OK");
 }
}
