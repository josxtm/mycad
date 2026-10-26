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
  for(double rx:new double[]{0,23,-76,180})for(double ry:new double[]{-47,0,38})
   for(double rz:new double[]{0,45,130}){
    RigidTransform pose=new RigidTransform(17,-32,8,rx,ry,rz);
    double[] world=pose.point(3,-7,11);
    double[] local=pose.inversePoint(world[0],world[1],world[2]);
    near(local[0],3,"Inverse X");near(local[1],-7,"Inverse Y");near(local[2],11,"Inverse Z");
   }
  RigidTransform far=new RigidTransform(1e12,-2e12,3e12,23,41,-37);
  RigidTransform rotation=new RigidTransform(0,0,0,23,41,-37);
  double[] direction=rotation.point(0.2,-0.7,0.4);
  double[] back=far.inverseDirection(direction[0],direction[1],direction[2]);
  near(back[0],0.2,"Inverse direction X at large translation");
  near(back[1],-0.7,"Inverse direction Y at large translation");
  near(back[2],0.4,"Inverse direction Z at large translation");
  boolean badDirection=false;
  try{far.inverseDirection(Double.NaN,0,0);}catch(IllegalArgumentException ok){badDirection=true;}
  check(badDirection,"Reject nonfinite direction");
  boolean bad=false;try{new RigidTransform(Double.NaN,0,0,0,0,0);}catch(IllegalArgumentException ok){bad=true;}check(bad,"Reject NaN");
  System.out.println("RigidTransformTest: OK");
 }
}
