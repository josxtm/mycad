package es.josxtm.mycad.cad;

/** Six logical faces in the deterministic BoxSurface tessellation. */
public final class BoxFaceSelection {
 private BoxFaceSelection(){}
 public enum Face { Z_MIN, Z_MAX, Y_MIN, Y_MAX, X_MIN, X_MAX }
 public static Face faceForTriangle(int triangle){
  if(triangle<0||triangle>=BoxSurface.triangleCount())throw new IllegalArgumentException("Triangle out of range");
  return Face.values()[triangle/2];
 }
 public static int firstTriangle(Face face){
  if(face==null)throw new IllegalArgumentException("Face required");
  return face.ordinal()*2;
 }
 public static float[] faceTriangles(TransformedBody body,Face face){
  float[] all=BoxSurface.triangles(body);
  float[] selected=new float[18];
  System.arraycopy(all,firstTriangle(face)*9,selected,0,18);
  return selected;
 }
}
