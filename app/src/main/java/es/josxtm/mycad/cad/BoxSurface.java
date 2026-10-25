package es.josxtm.mycad.cad;

/** Indexed watertight triangle surface for a transformed rectangular solid. */
public final class BoxSurface {
 private BoxSurface(){}
 // Eight corners in TransformedBody.worldVertices order (x, y, z binary indices).
 private static final int[] TRIANGLES={
  0,6,4, 0,2,6, // Z minimum
  1,7,3, 1,5,7, // Z maximum
  0,5,1, 0,4,5, // Y minimum
  2,7,6, 2,3,7, // Y maximum
  0,3,2, 0,1,3, // X minimum
  4,7,5, 4,6,7  // X maximum
 };
 public static float[] triangles(TransformedBody body){
  if(body==null)throw new IllegalArgumentException("Body required");
  double[][] corners=body.worldVertices();
  float[] output=new float[TRIANGLES.length*3];
  for(int i=0;i<TRIANGLES.length;i++){
   double[] point=corners[TRIANGLES[i]];
   for(int j=0;j<3;j++){
    double value=point[j];
    if(!Double.isFinite(value)||Math.abs(value)>Float.MAX_VALUE)
     throw new IllegalArgumentException("Invalid transformed coordinate");
    output[i*3+j]=(float)value;
   }
  }
  return output;
 }
 public static int triangleCount(){return TRIANGLES.length/3;}
}
