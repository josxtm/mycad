import es.josxtm.mycad.cad.*;

public final class PrimitiveMassPropertiesTest {
 static void ok(boolean value,String msg){if(!value)throw new AssertionError(msg);}
 static void near(double a,double b,String msg){
  if(!Double.isFinite(a)||Math.abs(a-b)>1e-7*Math.max(1,Math.abs(b)))
   throw new AssertionError(msg+": "+a+" vs "+b);
 }
 static void point(double[] a,double[] b,String msg){
  for(int i=0;i<3;i++)near(a[i],b[i],msg+" coordinate "+i);
 }
 static void rejects(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException good){return;}
  throw new AssertionError("Invalid accepted: "+label);
 }
 public static void main(String[] args){
  RigidTransform zero=new RigidTransform(0,0,0,0,0,0);
  PrimitiveBRepBody b=PrimitiveBRepBody.box("box",20,10,6,zero);
  PrimitiveMassProperties p=PrimitiveMassProperties.of(b,2);
  double mass=20*10*6*2d/1000;
  near(p.volumeMm3,1200,"Box volume");
  near(p.areaMm2,2d*(200+120+60),"Box surface");
  near(p.massGrams,mass,"Box density to mass");
  point(p.centroidMm(),new double[]{0,0,3},"Box center");
  double[][] i=p.inertiaGramsMm2();
  near(i[0][0],mass*(100+36)/12,"Box Ixx");
  near(i[1][1],mass*(400+36)/12,"Box Iyy");
  near(i[2][2],mass*(400+100)/12,"Box Izz");
  near(i[0][1],0,"Box Ixy");
  near(i[1][2],0,"Box Iyz");
  near(i[0][2],0,"Box Ixz");

  RigidTransform z90=new RigidTransform(15,-7,10,0,0,90);
  PrimitiveBRepBody turned=PrimitiveBRepBody.box("turned",20,10,6,z90);
  PrimitiveMassProperties rotated=PrimitiveMassProperties.of(turned,2);
  point(rotated.centroidMm(),new double[]{15,-7,13},"Translated center");
  double[][] ir=rotated.inertiaGramsMm2();
  near(ir[0][0],i[1][1],"Yaw 90 swaps Ixx");
  near(ir[1][1],i[0][0],"Yaw 90 swaps Iyy");
  near(ir[2][2],i[2][2],"Yaw 90 keeps Izz");
  near(ir[0][1],0,"Yaw 90 principal directions");

  PrimitiveBRepBody z45=PrimitiveBRepBody.box("z45",20,10,6,
    new RigidTransform(12,-1,7,0,0,45));
  double[][] iz=PrimitiveMassProperties.of(z45,2).inertiaGramsMm2();
  near(iz[0][0],(i[0][0]+i[1][1])/2,"Yaw 45 Ixx");
  near(iz[1][1],(i[0][0]+i[1][1])/2,"Yaw 45 Iyy");
  near(iz[0][1],(i[0][0]-i[1][1])/2,"Yaw 45 Ixy");
  near(iz[1][0],iz[0][1],"Tensor symmetry");
  near(iz[0][0]+iz[1][1]+iz[2][2],
       i[0][0]+i[1][1]+i[2][2],"Invariant tensor trace");

  PrimitiveBRepBody oriented=PrimitiveBRepBody.box("multi",20,10,6,
    new RigidTransform(-9,11,6,30,-40,77));
  double[][] multi=PrimitiveMassProperties.of(oriented,2).inertiaGramsMm2();
  near(multi[0][1],multi[1][0],"3-axis symmetric xy");
  near(multi[0][2],multi[2][0],"3-axis symmetric xz");
  near(multi[1][2],multi[2][1],"3-axis symmetric yz");
  near(multi[0][0]+multi[1][1]+multi[2][2],
       i[0][0]+i[1][1]+i[2][2],"3-axis invariant trace");
  double[] copy=rotated.centroidMm();copy[0]=1234567;
  ok(rotated.centroidMm()[0]!=1234567,"Center cannot be mutated");
  double[][] tensorCopy=rotated.inertiaGramsMm2();
  tensorCopy[0][0]=1234567;
  ok(rotated.inertiaGramsMm2()[0][0]!=1234567,"Inertia immutable");

  PrimitiveBRepBody cyl=PrimitiveBRepBody.cylinder("c",20,8,zero);
  PrimitiveMassProperties cp=PrimitiveMassProperties.of(cyl,7.85);
  double cmass=Math.PI*100*8*7.85/1000;
  near(cp.massGrams,cmass,"Cylinder grams");
  point(cp.centroidMm(),new double[]{0,0,4},"Cylinder centroid");
  double[][] ci=cp.inertiaGramsMm2();
  near(ci[0][0],cmass*(3*100+64)/12,"Cylinder Ixx");
  near(ci[1][1],ci[0][0],"Cylinder Iyy");
  near(ci[2][2],cmass*100/2,"Cylinder polar Izz");
  PrimitiveBRepBody taller=cyl.offsetFace("c/face/CYLINDER_TOP",2);
  PrimitiveMassProperties cp2=PrimitiveMassProperties.of(taller,7.85);
  near(cp2.volumeMm3,Math.PI*100*10,"Edited height volume");
  point(cp2.centroidMm(),new double[]{0,0,5},"Edited centroid");
  ok(cp2.massGrams>cp.massGrams,"Increasing height increases mass");
  PrimitiveMassProperties low=PrimitiveMassProperties.of(b,1);
  near(low.massGrams,p.massGrams/2,"Density scales mass");
  near(low.inertiaGramsMm2()[0][0],i[0][0]/2,"Density scales inertia");

  rejects(()->PrimitiveMassProperties.of(null,1),"Null body");
  rejects(()->PrimitiveMassProperties.of(b,0),"Zero density");
  rejects(()->PrimitiveMassProperties.of(b,-1),"Negative density");
  rejects(()->PrimitiveMassProperties.of(b,Double.NaN),"NaN density");
  rejects(()->PrimitiveMassProperties.of(b,Double.POSITIVE_INFINITY),"Infinite density");
  rejects(()->PrimitiveMassProperties.of(b,1e7),"Excessive density");
  System.out.println("PrimitiveMassPropertiesTest: OK");
 }
}
