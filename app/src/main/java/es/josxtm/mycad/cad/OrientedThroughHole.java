package es.josxtm.mycad.cad;

/**
 * Circular through-hole in a rectangular body's local coordinates.
 * Axis X: offsets lie in Y/Z. Axis Y: offsets lie in Z/X.
 * Axis Z: offsets lie in X/Y. Rigid pose maps the result to world space.
 * Result is a closed polygonal mesh; it is not B-Rep.
 */
public final class OrientedThroughHole {
 private OrientedThroughHole(){}
 public enum Axis { X, Y, Z }

 public static float[] triangles(double width,double depth,double height,
                                 double offsetU,double offsetV,double diameter,
                                 Axis axis,RigidTransform pose){
  if(axis==null||pose==null)throw new IllegalArgumentException("Axis and pose required");
  // Permutations (u,v,axis) -> (x,y,z) are cyclic, keeping triangle winding.
  final double faceWidth=axis==Axis.Z?width:axis==Axis.X?depth:height;
  final double faceDepth=axis==Axis.Z?depth:axis==Axis.X?height:width;
  final double length=axis==Axis.Z?height:axis==Axis.X?width:depth;
  float[] local=CircularThroughHole.triangles(0,0,0,faceWidth,faceDepth,length,
                                              offsetU,offsetV,diameter);
  float[] result=new float[local.length];
  for(int i=0;i<local.length;i+=3){
   double u=local[i],v=local[i+1],longitudinal=local[i+2];
   double x=axis==Axis.Z?u:axis==Axis.X?longitudinal:v;
   double y=axis==Axis.Z?v:axis==Axis.X?u:longitudinal;
   double z=axis==Axis.Z?longitudinal:axis==Axis.X?v:u;
   // X and Y were synthesized with a local box starting at longitudinal=0,
   // and must be centered around the physical X/Y axis of the host prism.
   if(axis==Axis.X)x-=width/2;
   if(axis==Axis.Y)y-=depth/2;
   double[] transformed=pose.point(x,y,z);
   for(int j=0;j<3;j++){
    if(!Double.isFinite(transformed[j])||Math.abs(transformed[j])>Float.MAX_VALUE)
     throw new IllegalArgumentException("Invalid world coordinate");
    result[i+j]=(float)transformed[j];
   }
  }
  return result;
 }
}
