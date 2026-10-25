import es.josxtm.mycad.cad.*;
public final class CurvedGeometryTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void near(double actual,double expected,double tolerance,String what){
  if(Math.abs(actual-expected)>tolerance)throw new AssertionError(what+": "+actual+" vs "+expected);
 }
 static double signedVolume(float[] t){
  double sum=0;
  for(int i=0;i<t.length;i+=9){
   double ax=t[i],ay=t[i+1],az=t[i+2],bx=t[i+3],by=t[i+4],bz=t[i+5],cx=t[i+6],cy=t[i+7],cz=t[i+8];
   sum+=(ax*(by*cz-bz*cy)+ay*(bz*cx-bx*cz)+az*(bx*cy-by*cx))/6;
  }
  return sum;
 }
 public static void main(String[] args){
  int n=64;
  float[] cylinder=CylinderSurface.triangles(10,20,new RigidTransform(15,2,-5,0,0,0),n);
  check(cylinder.length==n*4*9,"Cylinder side and caps");
  near(signedVolume(cylinder),Math.PI*25*20,4,"Cylinder volume");
  float[] tilted=CylinderSurface.triangles(10,20,new RigidTransform(15,2,-5,90,0,30),n);
  near(signedVolume(tilted),Math.PI*25*20,4,"Rotated cylinder volume");
  DrilledBoxRecipe recipe=new DrilledBoxRecipe(5,10,-3,30,20,15,4,-2,6);
  float[] hole=recipe.regenerate();
  check(hole.length%9==0&&hole.length>3000,"Closed drilled box");
  near(signedVolume(hole),recipe.nominalVolume(),4,"Through-hole mesh volume");
  DrilledBoxRecipe changed=recipe.withHole(-4,3,9);
  near(signedVolume(changed.regenerate()),changed.nominalVolume(),8,"Edited hole");
  check(changed.diameter==9&&recipe.diameter==6,"Immutable recipe");
  boolean invalid=false;try{recipe.withHole(13,0,6);}catch(IllegalArgumentException e){invalid=true;}
  check(invalid,"Hole outside box accepted");
  invalid=false;try{CylinderSurface.triangles(-2,4,new RigidTransform(0,0,0,0,0,0),n);}catch(IllegalArgumentException e){invalid=true;}
  check(invalid,"Invalid cylinder accepted");
  System.out.println("CurvedGeometryTest: OK");
 }
}