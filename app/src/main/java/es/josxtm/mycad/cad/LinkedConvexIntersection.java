package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Persistable UUID feature joining two closed sketch extrusions through
 * either a convex intersection or an orthogonal rectangle union.
 * Historical JSON without an operation field remains INTERSECTION.
 * Rebuild is read-only and can be committed transactionally.
 */
public final class LinkedConvexIntersection {
 public enum Operation { INTERSECTION, UNION_ORTHOGONAL, DIFFERENCE_ORTHOGONAL, UNION_POLYGONAL, DIFFERENCE_POLYGONAL, INTERSECTION_POLYGONAL, UNION_MULTISHELL, DIFFERENCE_MULTISHELL, INTERSECTION_MULTISHELL, UNION_WITH_HOLES, DIFFERENCE_WITH_HOLES, INTERSECTION_WITH_HOLES, UNION_ANALYTIC, INTERSECTION_ANALYTIC, DIFFERENCE_ANALYTIC, UNION_STEPPED, DIFFERENCE_STEPPED }
 public final String id,firstId,secondId;
 public final Operation operation;

 public LinkedConvexIntersection(String id,String firstId,String secondId){
  this(id,firstId,secondId,Operation.INTERSECTION);
 }
 public LinkedConvexIntersection(String id,String firstId,String secondId,Operation operation){
  if(!uuid(id)||!uuid(firstId)||!uuid(secondId)||firstId.equals(secondId)||
     id.equals(firstId)||id.equals(secondId)||operation==null)
   throw new IllegalArgumentException("IDs u operación de booleana B-Rep inválidos");
  this.id=id;this.firstId=firstId;this.secondId=secondId;
  this.operation=operation;
 }
 private static boolean uuid(String value){
  if(value==null)return false;
  try{return UUID.fromString(value).toString().equals(value);}
  catch(IllegalArgumentException invalid){return false;}
 }
 public boolean dependsOnFeature(String featureId){
  return firstId.equals(featureId)||secondId.equals(featureId);
 }
 public boolean isStepped(){
  return operation==Operation.UNION_STEPPED||operation==Operation.DIFFERENCE_STEPPED;
 }
 public boolean isGridHole(){
  return operation==Operation.UNION_WITH_HOLES||operation==Operation.DIFFERENCE_WITH_HOLES||
   operation==Operation.INTERSECTION_WITH_HOLES;
 }
 public NamedGridHoleBody regenerateNamedGridHole(Map<String,LoopExtrusionFeature> features,
      Map<String,ClosedSketchExtrusion.Segment> lines){
  if(!isGridHole()||features==null||lines==null)throw new IllegalArgumentException("Faltan fuentes B-Rep con agujeros");
  LoopExtrusionFeature a=features.get(firstId),b=features.get(secondId);
  if(a==null||b==null||!firstId.equals(a.id)||!secondId.equals(b.id))
   throw new IllegalArgumentException("Falta un operando B-Rep");
  return NamedGridHoleBody.apply(id,a.asBRep(lines),b.asBRep(lines),
   operation==Operation.UNION_WITH_HOLES?OrthogonalPolygonBRepBoolean.Operation.UNION:
   operation==Operation.DIFFERENCE_WITH_HOLES?OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE:
    OrthogonalPolygonBRepBoolean.Operation.INTERSECTION);
 }
 /** Rebuild topology from the same persisted sources as the preview. */
 public VariableHeightOrthogonalBRep regenerateStepped(
    Map<String,LoopExtrusionFeature> features,
    Map<String,ClosedSketchExtrusion.Segment> lines){
  if(!isStepped()||features==null||lines==null)
   throw new IllegalArgumentException("Faltan fuentes de la booleana escalonada");
  LoopExtrusionFeature a=features.get(firstId),b=features.get(secondId);
  if(a==null||b==null||!firstId.equals(a.id)||!secondId.equals(b.id))
   throw new IllegalArgumentException("Se eliminó una extrusión de origen");
  VariableHeightOrthogonalBRep shell=VariableHeightOrthogonalBRep.apply(
   a.asBRep(lines),b.asBRep(lines),operation==Operation.UNION_STEPPED?
    VariableHeightOrthogonalMesh.Operation.UNION:
    VariableHeightOrthogonalMesh.Operation.DIFFERENCE);
  if(shell.triangles().length>18000)
   throw new IllegalArgumentException("Booleana escalonada demasiado grande");
  return shell;
 }
 /** Source-derived names are rebuilt lazily for the Android topology inspector. */
 public NamedSteppedBody regenerateNamedStepped(
    Map<String,LoopExtrusionFeature> features,
    Map<String,ClosedSketchExtrusion.Segment> lines){
  if(!isStepped()||features==null||lines==null)
   throw new IllegalArgumentException("Faltan fuentes de la booleana escalonada");
  LoopExtrusionFeature a=features.get(firstId),b=features.get(secondId);
  if(a==null||b==null||!firstId.equals(a.id)||!secondId.equals(b.id))
   throw new IllegalArgumentException("Se eliminó una extrusión de origen");
  NamedSteppedBody named=NamedSteppedBody.apply(id,a.asBRep(lines),b.asBRep(lines),
   operation==Operation.UNION_STEPPED?VariableHeightOrthogonalMesh.Operation.UNION:
    VariableHeightOrthogonalMesh.Operation.DIFFERENCE);
  if(named.shell.triangles().length>18000)
   throw new IllegalArgumentException("Booleana escalonada demasiado grande");
  return named;
 }
 public ConvexPrismBRepIntersection.Result regenerate(
    Map<String,LoopExtrusionFeature> features,
    Map<String,ClosedSketchExtrusion.Segment> lines){
  if(operation!=Operation.INTERSECTION)
   throw new IllegalArgumentException("Usa regenerateTriangles para la unión B-Rep");
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
 /**
  * Shared non-mutating tessellation for both parametric Boolean operations.
  * Caller prevalidates EVERY dependent feature before modifying any sketch,
  * undo stack, saved JSON or mesh list.
  */
 public float[] regenerateTriangles(
    Map<String,LoopExtrusionFeature> features,
    Map<String,ClosedSketchExtrusion.Segment> lines){
  if(isStepped())return regenerateStepped(features,lines).triangles();
  if(operation==Operation.INTERSECTION)return regenerate(features,lines).triangles();
  if(features==null||lines==null)
   throw new IllegalArgumentException("Faltan operandos de unión B-Rep");
  LoopExtrusionFeature a=features.get(firstId),b=features.get(secondId);
  if(a==null||b==null||!firstId.equals(a.id)||!secondId.equals(b.id))
   throw new IllegalArgumentException("Se eliminó un sólido de la unión");
  // New saved operations reuse the unified analytic B-Rep kernel.
  // Existing operation names retain their original reconstruction routes.
  if(operation==Operation.UNION_ANALYTIC||
     operation==Operation.INTERSECTION_ANALYTIC||
     operation==Operation.DIFFERENCE_ANALYTIC){
   PrismBRepBoolean.Operation op=
       operation==Operation.UNION_ANALYTIC?PrismBRepBoolean.Operation.UNION:
       operation==Operation.INTERSECTION_ANALYTIC?PrismBRepBoolean.Operation.INTERSECTION:
           PrismBRepBoolean.Operation.DIFFERENCE;
   float[] mesh=PrismBRepBoolean.apply(
       a.asBRep(lines),b.asBRep(lines),op).triangles();
   if(mesh.length>18000)
    throw new IllegalArgumentException("Booleana analítica demasiado grande");
   return mesh;
  }
  if(operation==Operation.UNION_WITH_HOLES||
     operation==Operation.DIFFERENCE_WITH_HOLES||
     operation==Operation.INTERSECTION_WITH_HOLES){
   OrthogonalPolygonBRepBoolean.Operation op=
     operation==Operation.UNION_WITH_HOLES?OrthogonalPolygonBRepBoolean.Operation.UNION:
     operation==Operation.DIFFERENCE_WITH_HOLES?OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE:
      OrthogonalPolygonBRepBoolean.Operation.INTERSECTION;
   return OrthogonalGridHoleBRep.apply(
     a.asBRep(lines),b.asBRep(lines),op).triangles();
  }
  if(operation==Operation.UNION_MULTISHELL||
     operation==Operation.DIFFERENCE_MULTISHELL||
     operation==Operation.INTERSECTION_MULTISHELL){
   OrthogonalPolygonBRepBoolean.Operation op=
     operation==Operation.UNION_MULTISHELL?OrthogonalPolygonBRepBoolean.Operation.UNION:
     operation==Operation.DIFFERENCE_MULTISHELL?OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE:
       OrthogonalPolygonBRepBoolean.Operation.INTERSECTION;
   return OrthogonalPolygonBRepBoolean.applyMulti(
     a.asBRep(lines),b.asBRep(lines),op).triangles();
  }
  if(operation==Operation.UNION_POLYGONAL||
     operation==Operation.DIFFERENCE_POLYGONAL||
     operation==Operation.INTERSECTION_POLYGONAL){
   OrthogonalPolygonBRepBoolean.Operation op=
     operation==Operation.UNION_POLYGONAL?OrthogonalPolygonBRepBoolean.Operation.UNION:
     operation==Operation.DIFFERENCE_POLYGONAL?OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE:
       OrthogonalPolygonBRepBoolean.Operation.INTERSECTION;
   float[] mesh=OrthogonalPolygonBRepBoolean.apply(
     a.asBRep(lines),b.asBRep(lines),op).triangles();
   if(mesh.length>18000)throw new IllegalArgumentException("Booleana poligonal demasiado grande");
   return mesh;
  }
  float[] out=(operation==Operation.DIFFERENCE_ORTHOGONAL?
   OrthogonalPrismBRepUnion.difference(a.asBRep(lines),b.asBRep(lines)):
   OrthogonalPrismBRepUnion.union(a.asBRep(lines),b.asBRep(lines)))
   .triangles();
  if(out.length>18000)throw new IllegalArgumentException("Unión B-Rep demasiado grande");
  return out;
 }
}
