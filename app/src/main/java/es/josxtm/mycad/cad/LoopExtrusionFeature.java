package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Immutable height-editable feature for a connected line-loop extrusion.
 * Stable UUIDs connect sketch LINE entities to a regenerated watertight mesh.
 *
 * This is an intermediate feature-graph bridge, NOT general B-Rep topology.
 * Every rebuild resolves current source geometry atomically before the caller
 * replaces an existing persisted mesh or commits undo history.
 */
public final class LoopExtrusionFeature {
 public final String id,selectedLineId;
 public final int plane;
 public final double distance;
 private final List<String> sourceLineIds;

 public LoopExtrusionFeature(String id,String selectedLineId,int plane,
                             double distance,List<String> sourceLineIds){
  if(!uuid(id)||!uuid(selectedLineId)||plane<0||plane>2||
     !Double.isFinite(distance)||distance==0||Math.abs(distance)>10000||
     sourceLineIds==null||sourceLineIds.size()<3||sourceLineIds.size()>200)
   throw new IllegalArgumentException("Extrusión vinculada inválida");
  LinkedHashSet<String> unique=new LinkedHashSet<>();
  for(String line:sourceLineIds)
   if(!uuid(line)||!unique.add(line))
    throw new IllegalArgumentException("Línea de origen duplicada o inválida");
  if(!unique.contains(selectedLineId))
   throw new IllegalArgumentException("Línea seleccionada no pertenece al contorno");
  this.id=id;this.selectedLineId=selectedLineId;this.plane=plane;
  this.distance=distance;
  this.sourceLineIds=Collections.unmodifiableList(new ArrayList<>(sourceLineIds));
 }
 private static boolean uuid(String token){
  if(token==null)return false;
  try{return UUID.fromString(token).toString().equals(token);}
  catch(IllegalArgumentException error){return false;}
 }
 public List<String> lineIds(){return sourceLineIds;}
 public boolean dependsOn(String sourceId){return sourceLineIds.contains(sourceId);}
 /** Analytic planar shell with persistent edge/face/vertex IDs.
  * This is not general CAD Boolean or NURBS support. */
 public PolygonPrismBRep asBRep(
    Map<String,ClosedSketchExtrusion.Segment> current){
  return PolygonPrismBRep.from(this,current);
 }
 public LoopExtrusionFeature withDistance(double next){
  return new LoopExtrusionFeature(id,selectedLineId,plane,next,sourceLineIds);
 }
 /**
  * Resolve all stable IDs; reject missing or changed planes and malformed
  * topology. The result can be swapped as one atomic user operation.
  */
 public ClosedSketchExtrusion.Result regenerate(
       Map<String,ClosedSketchExtrusion.Segment> current){
  if(current==null)throw new IllegalArgumentException("Faltan bocetos originales");
  ArrayList<ClosedSketchExtrusion.Segment> ordered=new ArrayList<>();
  int index=-1;
  for(String sourceId:sourceLineIds){
   ClosedSketchExtrusion.Segment segment=current.get(sourceId);
   if(segment==null||segment.plane!=plane)
    throw new IllegalArgumentException("Boceto de origen inexistente o trasladado");
   if(sourceId.equals(selectedLineId))index=ordered.size();
   ordered.add(segment);
  }
  ClosedSketchExtrusion.Result result=ClosedSketchExtrusion.extrude(ordered,index,distance);
  if(result.usedLines!=sourceLineIds.size()||result.triangles.length>18000)
   throw new IllegalArgumentException("Contorno fragmentado o malla excedida");
  return result;
 }
}
