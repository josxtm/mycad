import es.josxtm.mycad.cad.*;
/** Regression suite for circular blind holes in rotated cuboids. */
public final class BlindCircularHoleTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void close(double actual,double expected,double tol,String message){
  if(Math.abs(actual-expected)>tol)throw new AssertionError(message+": "+actual+" expected "+expected);
 }
 public static void main(String[] args){
  final double w=40,d=30,h=25,diameter=8,blindDepth=11;
  final double solidVolume=w*d*h;
  for(OrientedThroughHole.Axis axis:OrientedThroughHole.Axis.values()){
   double fullLength=axis==OrientedThroughHole.Axis.X?w:
                     axis==OrientedThroughHole.Axis.Y?d:h;
   for(int rotated=0;rotated<2;rotated++){
    double rx=rotated==0?0:17,ry=rotated==0?0:-35,rz=rotated==0?0:62;
    DrilledBoxRecipe recipe=new DrilledBoxRecipe(13,7,-6,w,d,h,2,-3,diameter,
       axis,rx,ry,rz,blindDepth);
    TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(recipe.regenerate());
    check(report.isWatertight(),"Open blind "+axis+" rotated="+rotated+" boundary="+report.boundaryEdges+
      " bad="+report.reversedEdges+" degenerate="+report.degenerateTriangles);
    close(report.signedVolume,solidVolume-Math.PI*diameter*diameter/4*blindDepth,8,
      "Blind cavity volume "+axis+" rotated="+rotated);
    close(recipe.nominalVolume(),solidVolume-Math.PI*diameter*diameter/4*blindDepth,1e-7,
      "Analytical blind volume");
    DrilledBoxRecipe shorter=recipe.withDepth(6);
    check(recipe.drillDepth==11&&shorter.drillDepth==6,"Immutable edit");
    check(shorter.axis==axis&&shorter.rotationX==rx,"Preserved rotation/axis");
    close(TriangleMeshHealth.inspect(shorter.regenerate()).signedVolume,
      solidVolume-Math.PI*diameter*diameter/4*6,8,"Shorter bore");
    DrilledBoxRecipe thru=recipe.withDepth(0);
    TriangleMeshHealth.Report full=TriangleMeshHealth.inspect(thru.regenerate());
    check(full.isWatertight(),"Regression: through hole shell "+axis);
    close(full.signedVolume,solidVolume-Math.PI*diameter*diameter/4*fullLength,8,"Through volume");
   }
  }
  DrilledBoxRecipe historical=new DrilledBoxRecipe(0,0,0,40,30,25,0,0,6);
  check(historical.drillDepth==0&&historical.axis==OrientedThroughHole.Axis.Z,"Historical constructor");
  double[] invalid={-1,25,Double.NaN,Double.POSITIVE_INFINITY};
  for(double depth:invalid){
   boolean rejected=false;
   try{historical.withDepth(depth);}catch(IllegalArgumentException expected){rejected=true;}
   check(rejected,"Invalid bore depth accepted: "+depth);
  }
  boolean rejected=false;
  try{new DrilledBoxRecipe(0,0,0,40,30,25,0,0,8,OrientedThroughHole.Axis.X,0,0,0,40);}
  catch(IllegalArgumentException expected){rejected=true;}
  check(rejected,"Blind length equal to X-body width accepted");
  System.out.println("BlindCircularHoleTest: OK");
 }
}
