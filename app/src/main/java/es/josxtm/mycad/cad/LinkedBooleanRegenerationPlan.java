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
  private final Map<String,String> sources;
  private Plan(Map<String,float[]> meshes,int triangleCount,
               Map<String,String> sourceSignatures){
   staged=Collections.unmodifiableMap(new LinkedHashMap<>(meshes));
   triangles=triangleCount;
   sources=Collections.unmodifiableMap(new TreeMap<>(sourceSignatures));
  }
  public int size(){return staged.size();}
  public int triangleCount(){return triangles;}
  public Set<String> featureIds(){return staged.keySet();}
  /**
   * True only when every referenced extrusion and sketch segment still has
   * exactly the geometry used for this batch. Unrelated edits are ignored.
   * Check immediately before publishing meshes or committing undo history.
   */
  public boolean isCurrent(Map<String,LoopExtrusionFeature> features,
                           Map<String,ClosedSketchExtrusion.Segment> lines){
   try{return sources.equals(captureSources(sources.keySet(),features,lines));}
   catch(IllegalArgumentException stale){return false;}
  }
  public void requireCurrent(Map<String,LoopExtrusionFeature> features,
                             Map<String,ClosedSketchExtrusion.Segment> lines){
   if(!isCurrent(features,lines))
    throw new IllegalArgumentException("La geometría de origen cambió tras el preflight");
  }
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
  // A positive total volume can hide an inside-out disconnected shell.
  // Saved Boolean multi-shell results represent separate solid components;
  // internal cavity shells are not emitted by these prism operations.
  if(!health.isWatertight()||health.negativeVolumeShells!=0||
     !Double.isFinite(health.signedVolume)||health.signedVolume<=0)
   throw new IllegalArgumentException("Booleana B-Rep no es un sólido cerrado y orientado");
 }

 /**
  * Lossless, stable source snapshot. A hash alone would introduce collision
  * risk into the commit guard; double bit patterns retain sub-mm edits.
  * Segment order and direction are part of the saved parametric topology.
  */
 private static Map<String,String> captureSources(
     Collection<String> sourceIds,Map<String,LoopExtrusionFeature> features,
     Map<String,ClosedSketchExtrusion.Segment> lines){
  if(sourceIds==null||features==null||lines==null)
   throw new IllegalArgumentException("Faltan fuentes de booleana");
  TreeMap<String,String> signatures=new TreeMap<>();
  for(String id:sourceIds){
   LoopExtrusionFeature feature=features.get(id);
   if(feature==null||!id.equals(feature.id))
    throw new IllegalArgumentException("Extrusión de origen eliminada o sustituida");
   StringBuilder value=new StringBuilder();
   value.append(feature.plane).append('|')
       .append(Long.toHexString(Double.doubleToLongBits(feature.distance)))
       .append('|').append(feature.selectedLineId);
   for(String lineId:feature.lineIds()){
    ClosedSketchExtrusion.Segment segment=lines.get(lineId);
    if(segment==null||segment.plane!=feature.plane)
     throw new IllegalArgumentException("Línea de origen eliminada o trasladada");
    value.append('|').append(lineId).append(':').append(segment.plane)
       .append(':').append(Long.toHexString(Double.doubleToLongBits(segment.ax)))
       .append(':').append(Long.toHexString(Double.doubleToLongBits(segment.ay)))
       .append(':').append(Long.toHexString(Double.doubleToLongBits(segment.bx)))
       .append(':').append(Long.toHexString(Double.doubleToLongBits(segment.by)));
   }
   signatures.put(id,value.toString());
  }
  return signatures;
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
  TreeSet<String> referenced=new TreeSet<>();
  for(LinkedConvexIntersection feature:ordered.values()){
   referenced.add(feature.firstId);
   referenced.add(feature.secondId);
  }
  Map<String,String> sourceSnapshot=captureSources(referenced,features,lines);
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
  // Detect edits during a long batch, before the caller can publish anything.
  if(!sourceSnapshot.equals(captureSources(referenced,features,lines)))
   throw new IllegalArgumentException("Las fuentes cambiaron durante el preflight");
  return new Plan(staged,totalFloats/9,sourceSnapshot);
 }
}
