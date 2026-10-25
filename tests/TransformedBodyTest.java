import es.josxtm.mycad.cad.*;
public final class TransformedBodyTest{
 static void near(double actual,double expected,String label){if(Math.abs(actual-expected)>1e-8)throw new AssertionError(label+": "+actual);}
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args){
  CadKernel.Box box=new PrimitiveKernel().createBox(0,0,0,2,4,6).value;
  TransformedBody solid=new TransformedBody(box,new RigidTransform(10,20,30,0,0,90));
  near(solid.volume(),48,"Volume invariant");
  double[] c=solid.center();
  near(c[0],8,"Center X");near(c[1],21,"Center Y");near(c[2],33,"Center Z");
  double[] bb=solid.worldBounds();
  near(bb[0],6,"Min X");near(bb[3],10,"Max X");near(bb[2],30,"Min Z");near(bb[5],36,"Max Z");
  check(solid.worldVertices().length==8,"Eight world vertices");
  boolean rejects=false;try{new TransformedBody(null,new RigidTransform(0,0,0,0,0,0));}catch(IllegalArgumentException expected){rejects=true;}
  check(rejects,"Reject null body");
  System.out.println("TransformedBodyTest: OK");
 }
}
