package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Read-only, all-or-nothing preflight for regenerating saved Boolean features.
 * Callers may replace their displayed meshes only AFTER build() succeeds.
 * No source sketch, extrusion, history entry or mesh is changed here.
 */
public final class LinkedBooleanRegenerationPlan {
 private static final int MAX_FEATURES=64;
 private static final int MAX_FLOATS=250000;
 private LinkedBooleanRegenerationPlan(){}

 public static final class Plan {
  private final Map<String,float[]> staged;
  private final int triangles;
  private Plan(Map<String,float[]> meshes,int triangleCount){
   staged=Collections.unmodifiableMap(new LinkedHashMap<>(meshes));
   triangles=triangleCount;
  }
  public int size(){return staged.size();}
  public int triangleCount(){return triangles;}
  public Set<String> featureIds(){return staged.keySet();}
  /** Always return a copy: editing a mesh cannot corrupt the preflight. */
  public float[] triangles(String featureId){
   float[] mesh=staged.get(featureId);
   if(mesh==null)throw new IllegalArgumentException("Booleana no incluida en el plan");
   return mesh.clone();
  }
 }

 /**
  * Validate the tessellation of a candidate analytic Boolean before publication.
  * Multi-shell solids are allowed, but every shell must be a closed oriented
  * manifold with nonzero volume. Rejecting invalid results keeps the previous
  * displayed meshes and saved parametric history intact.
  */
 public static void requireClosedMesh(float[] mesh){
  if(mesh==null||mesh.length==0||mesh.length%9!=0||mesh.length>18000)
   throw new IllegalArgumentException("Malla de booleana inválida o demasiado grande");
  TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(mesh);
  if(!health.isWatertight()||!Double.isFinite(health.signedVolume)||
     health.signedVolume<=0)
   throw new IllegalArgumentException("Booleana B-Rep no es un sólido cerrado y orientado");
 }

 public static Plan build(Collection<LinkedConvexIntersection> booleans,
                          Map<String,LoopExtrusionFeature> features,
                          Map<String,ClosedSketchExtrusion.Segment> lines){
  if(booleans==null||features==null||lines==null||
     booleans.size()>MAX_FEATURES)
   throw new IllegalArgumentException("Dependencias booleanas no válidas");
  // Stable UUID ordering independent of the incoming collection order.
  TreeMap<String,LinkedConvexIntersection> ordered=new TreeMap<>();
  for(LinkedConvexIntersection feature:booleans){
   if(feature==null||ordered.putIfAbsent(feature.id,feature)!=null)
    throw new IllegalArgumentException("Booleana duplicada o nula");
   if(features.containsKey(feature.id))
    throw new IllegalArgumentException("ID de booleana colisiona con extrusión");
  }
  LinkedHashMap<String,float[]> staged=new LinkedHashMap<>();
  int totalFloats=0;
  for(LinkedConvexIntersection feature:ordered.values()){
   float[] mesh=feature.regenerateTriangles(features,lines);
   requireClosedMesh(mesh);
   if(totalFloats>MAX_FLOATS-mesh.length)
    throw new IllegalArgumentException("Límite de mallas booleanas excedido");
   totalFloats+=mesh.length;
   staged.put(feature.id,mesh.clone());
  }
  return new Plan(staged,totalFloats/9);
 }
}
