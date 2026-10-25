import es.josxtm.mycad.cad.*;

/** Regression: volume and sealing for X/Y/Z bores in rotated and unrotated cuboids. */
public final class OrientedThroughHoleTest {
 static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
 static void near(double got,double expected,double tolerance,String label){
  if(Math.abs(got-expected)>tolerance)throw new AssertionError(label+": "+got+" != "+expected);
 }
 static double signedVolume(float[] triangles){
  double sum=0;
  for(int i=0;i<triangles.length;i+=9){
   double ax=triangles[i],ay=triangles[i+1],az=triangles[i+2];
   double bx=triangles[i+3],by=triangles[i+4],bz=triangles[i+5];
   double cx=triangles[i+6],cy=triangles[i+7],cz=triangles[i+8];
   sum+=(ax*(by*cz-bz*cy)+ay*(bz*cx-bx*cz)+az*(bx*cy-by*cx))/6;
  }
  return sum;
 }
 public static void main(String[] args){
  double w=40,d=30,h=20,dia=8;
  double rootVolume=w*d*h;
  for(OrientedThroughHole.Axis axis:OrientedThroughHole.Axis.values()){
   double length=axis==OrientedThroughHole.Axis.X?w:axis==OrientedThroughHole.Axis.Y?d:h;
   double volume=rootVolume-Math.PI*dia*dia/4*length;
   for(int rotation=0;rotation<2;rotation++){
    double rx=rotation==0?0:30,ry=rotation==0?0:-45,rz=rotation==0?0:60;
    DrilledBoxRecipe recipe=new DrilledBoxRecipe(9,15,-7,w,d,h,2,-3,dia,axis,rx,ry,rz);
    float[] mesh=recipe.regenerate();
    check(mesh.length>3000&&mesh.length%9==0,"Closed faces "+axis);
    near(signedVolume(mesh),volume,12,"Oriented bore "+axis+" rotation "+rotation);
    if(rotation==0){
     double minZ=Double.POSITIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;
     for(int v=2;v<mesh.length;v+=3){minZ=Math.min(minZ,mesh[v]);maxZ=Math.max(maxZ,mesh[v]);}
     near(minZ,-7,0.0002,"Body bottom bounds "+axis);
     near(maxZ,-7+h,0.0002,"Body top bounds "+axis);
    }
    near(recipe.nominalVolume(),volume,0.0001,"Nominal volume "+axis);
    DrilledBoxRecipe edited=recipe.withHole(0,0,6);
    near(signedVolume(edited.regenerate()),rootVolume-Math.PI*9*length,10,"Changed cutter "+axis);
    check(edited.axis==axis&&edited.rotationX==rx&&edited.rotationY==ry&&edited.rotationZ==rz,
      "Preserve local axis / solid pose");
   }
  }
  DrilledBoxRecipe legacy=new DrilledBoxRecipe(9,15,-7,w,d,h,2,-3,dia);
  check(legacy.axis==OrientedThroughHole.Axis.Z,"Legacy Z-axis defaults");
  near(signedVolume(legacy.regenerate()),legacy.nominalVolume(),12,"Legacy bore");
  boolean invalid=false;
  try{new DrilledBoxRecipe(0,0,0,40,30,20,18,0,8,OrientedThroughHole.Axis.Z,0,0,0);}
  catch(IllegalArgumentException ok){invalid=true;}
  check(invalid,"Bore outside local face accepted");
  invalid=false;
  try{new DrilledBoxRecipe(0,0,0,40,30,20,0,0,35,OrientedThroughHole.Axis.X,0,0,0);}
  catch(IllegalArgumentException ok){invalid=true;}
  check(invalid,"Oversized bore in rotated axis accepted");
  System.out.println("OrientedThroughHoleTest: OK");
 }
}
