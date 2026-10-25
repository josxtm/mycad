import es.josxtm.mycad.cad.*;
public final class RigidTransformTest {
 static void near(double x,double y,String m){if(Math.abs(x-y)>1e-8)throw new AssertionError(m+" "+x);}
 static void check(boolean x,String m){if(!x)throw new AssertionError(m);}
 public static void main(String[] args){
  RigidTransform t=new RigidTransform(10,20,30,0,0,90);
  double[] p=t.point(2,3,4);near(p[0],7,"X");near(p[1],22,"Y");near(p[2],34,"Z");
  CadKernel.Box b=new PrimitiveKernel().createBox(0,0,0,2,4,6).value;
  double[] a=t.bounds(b);near(a[0],6,"Min X");near(a[3],10,"Max X");near(a[1],20,"Min Y");near(a[4],22,"Max Y");
  check(t.vertices(b).length==8,"Vertices");
  double[] q=new RigidTransform(0,0,0,90,0,0).point(0,1,0);
  near(q[1],0,"Rotate X Y");near(q[2],1,"Rotate X Z");
  q=new RigidTransform(0,0,0,0,90,0).point(1,0,0);
  near(q[0],0,"Rotate Y X");near(q[2],-1,"Rotate Y Z");
  boolean bad=false;try{new RigidTransform(Double.NaN,0,0,0,0,0);}catch(IllegalArgumentException ok){bad=true;}check(bad,"Reject NaN");
  System.out.println("RigidTransformTest: OK");
 }
}
