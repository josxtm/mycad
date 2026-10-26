package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Resolves a picked preview triangle back to an analytic planar B-Rep face.
 * Matching uses FLOAT-rounded vertex positions, exactly as the existing
 * ClosedSketchExtrusion tessellator emits them to the GPU. No triangle index
 * is used as a persistent face ID; logical face IDs depend on source UUIDs.
 *
 * A triangle belongs to exactly one face. A cap triangle cannot belong to a
 * lateral quad (they share at most two vertices). Adjacent side quads also
 * share at most two vertices, even when coplanar.
 */
public final class PolygonPrismFacePicker {
 private PolygonPrismFacePicker(){}
 private static final class Key {
  final int x,y,z;
  Key(double[] v){
   if(v==null||v.length!=3)throw new IllegalArgumentException("Missing 3D vertex");
   x=bits(v[0]);y=bits(v[1]);z=bits(v[2]);
  }
  private static int bits(double value){
   if(!Double.isFinite(value))throw new IllegalArgumentException("Non-finite vertex");
   float f=(float)value;
   if(!Float.isFinite(f))throw new IllegalArgumentException("Vertex out of float range");
   return Float.floatToIntBits(f==0?0:f);
  }
  @Override public boolean equals(Object other){
   if(!(other instanceof Key))return false;
   Key k=(Key)other;return x==k.x&&y==k.y&&z==k.z;
  }
  @Override public int hashCode(){return (x*31+y)*31+z;}
 }
 public static PolygonPrismBRep.Face resolve(PolygonPrismBRep solid,int localTriangle){
  if(solid==null||localTriangle<0)return null;
  float[] triangles=solid.triangles();
  if(localTriangle>=triangles.length/9)return null;
  Set<Key> corners=new HashSet<>(3);
  for(int i=0;i<3;i++){
   int at=localTriangle*9+i*3;
   corners.add(new Key(new double[]{
     triangles[at],triangles[at+1],triangles[at+2]}));
  }
  if(corners.size()!=3)
   throw new IllegalArgumentException("Degenerate selected triangle");
  Map<String,Key> points=new HashMap<>();
  for(PolygonPrismBRep.Vertex v:solid.vertices())
   points.put(v.id,new Key(v.position()));
  PolygonPrismBRep.Face match=null;
  for(PolygonPrismBRep.Face face:solid.faces()){
   Set<Key> vertices=new HashSet<>();
   for(String vid:face.vertexIds()){
    Key v=points.get(vid);
    if(v==null)throw new IllegalArgumentException("Broken analytic face loop");
    vertices.add(v);
   }
   if(vertices.containsAll(corners)){
    if(match!=null)
     throw new IllegalArgumentException("Ambiguous B-Rep triangle-to-face relation");
    match=face;
   }
  }
  if(match==null)throw new IllegalArgumentException("Triangle has no analytic face");
  return match;
 }
}
