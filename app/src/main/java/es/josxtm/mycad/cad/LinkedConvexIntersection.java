package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Persistable immutable Boolean feature linking a saved intersection to two
 * closed sketch extrusion UUIDs. Both upstream profiles must remain supported
 * convex prisms. Rebuild is read-only and can be committed transactionally.
 */
public final class LinkedConvexIntersection {
 public final String id,firstId,secondId;

 public LinkedConvexIntersection(String id,String firstId,String secondId){
  if(!uuid(id)||!uuid(firstId)||!uuid(secondId)||firstId.equals(secondId)||
     id.equals(firstId)||id.equals(secondId))
   throw new IllegalArgumentException("IDs de intersección B-Rep inválidos");
  this.id=id;this.firstId=firstId;this.secondId=secondId;
 }
 private static boolean uuid(String value){
  if(value==null)return false;
  try{return UUID.fromString(value).toString().equals(value);}
  catch(IllegalArgumentException invalid){return false;}
 }
 public boolean dependsOnFeature(String featureId){
  return firstId.equals(featureId)||secondId.equals(featureId);
 }
 public ConvexPrismBRepIntersection.Result regenerate(
    Map<String,LoopExtrusionFeature> features,
    Map<String,ClosedSketchExtrusion.Segment> lines){
  if(features==null||lines==null)
   throw new IllegalArgumentException("Faltan fuentes de la intersección");
  LoopExtrusionFeature a=features.get(firstId),b=features.get(secondId);
  if(a==null||b==null||!firstId.equals(a.id)||!secondId.equals(b.id))
   throw new IllegalArgumentException("Se eliminó una extrusión de origen");
  ConvexPrismBRepIntersection.Result out=ConvexPrismBRepIntersection.intersect(
   a.asBRep(lines),b.asBRep(lines));
  if(out.triangles().length>18000)
   throw new IllegalArgumentException("Intersección excede tamaño de malla");
  return out;
 }
}
