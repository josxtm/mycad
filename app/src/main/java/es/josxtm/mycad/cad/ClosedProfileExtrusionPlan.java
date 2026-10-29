package es.josxtm.mycad.cad;

import java.util.*;

/** Immutable source selection for a deferred extrusion dialog. No document mutation.
 * The existing tessellator and analytic B-Rep remain the geometry authorities. */
public final class ClosedProfileExtrusionPlan {
 public static final class Prepared {
  public final LoopExtrusionFeature feature;
  public final PolygonPrismBRep solid;
  private Prepared(LoopExtrusionFeature feature,PolygonPrismBRep solid){this.feature=feature;this.solid=solid;}
 }
 private final Map<String,ClosedSketchExtrusion.Segment> source;
 private final String selected;
 private ClosedProfileExtrusionPlan(Map<String,ClosedSketchExtrusion.Segment> lines,String selected){
  if(lines==null||lines.isEmpty()||lines.size()>200||!uuid(selected)||!lines.containsKey(selected))
   throw new IllegalArgumentException("Selecciona un contorno de hasta 200 líneas");
  ClosedSketchExtrusion.Segment anchor=lines.get(selected);
  if(anchor==null)throw new IllegalArgumentException("Línea seleccionada inexistente");
  LinkedHashMap<String,ClosedSketchExtrusion.Segment> copy=new LinkedHashMap<>();
  for(Map.Entry<String,ClosedSketchExtrusion.Segment> entry:lines.entrySet()){
   ClosedSketchExtrusion.Segment segment=entry.getValue();
   if(!uuid(entry.getKey())||segment==null||segment.plane!=anchor.plane)
    throw new IllegalArgumentException("Línea de origen inválida o de otro plano");
   copy.put(entry.getKey(),segment); // Segment fields are immutable doubles.
  }
  source=Collections.unmodifiableMap(copy);this.selected=selected;
 }
 public static ClosedProfileExtrusionPlan capture(Map<String,ClosedSketchExtrusion.Segment> lines,String selected){
  return new ClosedProfileExtrusionPlan(lines,selected);
 }
 private static boolean uuid(String id){
  try{return id!=null&&UUID.fromString(id).toString().equals(id);}catch(IllegalArgumentException invalid){return false;}
 }
 private static boolean same(ClosedSketchExtrusion.Segment a,ClosedSketchExtrusion.Segment b){
  return b!=null&&a.plane==b.plane&&a.ax==b.ax&&a.ay==b.ay&&a.bx==b.bx&&a.by==b.by;
 }
 /** Reordering the editor's list is harmless; changing any selected-plane source is not. */
 public boolean isCurrent(Map<String,ClosedSketchExtrusion.Segment> current){
  if(current==null||!source.keySet().equals(current.keySet()))return false;
  for(Map.Entry<String,ClosedSketchExtrusion.Segment> entry:source.entrySet())
   if(!same(entry.getValue(),current.get(entry.getKey())))return false;
  return true;
 }
 public Prepared prepare(String featureId,double height,Map<String,ClosedSketchExtrusion.Segment> current){
  if(!isCurrent(current))throw new IllegalArgumentException("El contorno cambió; vuelve a seleccionarlo");
  if(!uuid(featureId)||source.containsKey(featureId))throw new IllegalArgumentException("UUID de sólido inválido o duplicado");
  ArrayList<String> ids=new ArrayList<>(source.keySet());
  ArrayList<ClosedSketchExtrusion.Segment> lines=new ArrayList<>(source.values());
  ClosedSketchExtrusion.Result mesh=ClosedSketchExtrusion.extrude(lines,ids.indexOf(selected),height);
  ArrayList<String> used=new ArrayList<>();LinkedHashMap<String,ClosedSketchExtrusion.Segment> profile=new LinkedHashMap<>();
  for(int index:mesh.sourceIndices){String id=ids.get(index);used.add(id);profile.put(id,source.get(id));}
  LoopExtrusionFeature feature=new LoopExtrusionFeature(featureId,selected,mesh.plane,height,used);
  PolygonPrismBRep solid=feature.asBRep(profile);
  if(solid.triangles().length>18000||Math.abs(solid.volume()-mesh.volume)>1e-5*Math.max(1,mesh.volume))
   throw new IllegalArgumentException("La malla y el sólido analítico discrepan");
  if(!isCurrent(current))throw new IllegalArgumentException("El contorno cambió durante la extrusión");
  return new Prepared(feature,solid);
 }
}
