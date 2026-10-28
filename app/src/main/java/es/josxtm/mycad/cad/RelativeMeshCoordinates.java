package es.josxtm.mycad.cad;

/**
 * Converts double-precision B-Rep world coordinates into local float positions
 * relative to a double-precision camera/mesh origin. Never changes the source.
 * The renderer must apply the same origin when constructing its view matrix.
 */
public final class RelativeMeshCoordinates {
 private RelativeMeshCoordinates(){}
 public static float[] relative(double[] xyz,double originX,double originY,double originZ){
  if(xyz==null||xyz.length%9!=0||!Double.isFinite(originX)||
     !Double.isFinite(originY)||!Double.isFinite(originZ))
   throw new IllegalArgumentException("Invalid coordinates or origin");
  float[] out=new float[xyz.length];
  for(int i=0;i<xyz.length;i++){
   double origin=i%3==0?originX:i%3==1?originY:originZ;
   if(!Double.isFinite(xyz[i]))throw new IllegalArgumentException("Nonfinite B-Rep coordinate");
   double relative=xyz[i]-origin;
   if(!Double.isFinite(relative)||Math.abs(relative)>Float.MAX_VALUE)
    throw new IllegalArgumentException("Relative coordinate exceeds float range");
   out[i]=(float)relative;
  }
  return out;
 }
}
