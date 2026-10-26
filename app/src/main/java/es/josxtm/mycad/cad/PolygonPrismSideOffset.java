package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Transactional planar-side offset for a straight closed linked sketch prism.
 * Moves the selected side's two underlying polygon corners along the outer
 * UV normal, updating the adjacent sketch segment endpoints at the same time.
 * The complete proposed B-Rep is validated before returning any new data.
 *
 * This is a restricted polygon-profile edit, not a general freeform B-Rep
 * surface offset, Boolean operation, bevel, or constraint solver.
 */
public final class PolygonPrismSideOffset {
 private PolygonPrismSideOffset(){}
 private static final double SNAP=0.15;
 public static final class Plan {
  public final String featureId,faceId,sourceLineId;
  public final double millimeters;
  public final PolygonPrismBRep solid;
  private final Map<String,ClosedSketchExtrusion.Segment> updated;
  private final Set<String> modified;
  private Plan(LoopExtrusionFeature feature,PolygonPrismBRep.Face face,
      double mm,Map<String,ClosedSketchExtrusion.Segment> next,
      Set<String> changes,PolygonPrismBRep solid){
   featureId=feature.id;faceId=face.id;sourceLineId=face.sourceLineId;
   millimeters=mm;this.solid=solid;
   updated=Collections.unmodifiableMap(new LinkedHashMap<>(next));
   modified=Collections.unmodifiableSet(new LinkedHashSet<>(changes));
  }
  public Map<String,ClosedSketchExtrusion.Segment> segments(){return updated;}
  public Set<String> modifiedLineIds(){return modified;}
 }
 private static double[] uv(int plane,double[] world){
  if(plane==0)return new double[]{world[0],world[1]};
  if(plane==1)return new double[]{world[0],world[2]};
  return new double[]{world[1],world[2]};
 }
 private static boolean near(double x,double y,double[] p){
  return Math.hypot(x-p[0],y-p[1])<=SNAP;
 }
 private static double[] shift(double x,double y,double[] a,double[] b,
                               double dx,double dy,int[] count){
  boolean atA=near(x,y,a),atB=near(x,y,b);
  if(atA&&atB)throw new IllegalArgumentException("Segmento lateral ambiguo");
  if(atA||atB){
   count[0]++;
   return new double[]{x+dx,y+dy};
  }
  return new double[]{x,y};
 }
 public static Plan offset(LoopExtrusionFeature feature,
      Map<String,ClosedSketchExtrusion.Segment> current,
      String faceId,double millimeters){
  if(feature==null||current==null||faceId==null||
     !Double.isFinite(millimeters)||millimeters==0||
     Math.abs(millimeters)>10000)
   throw new IllegalArgumentException("Desplazamiento de cara inválido");
  PolygonPrismBRep before=feature.asBRep(current);
  PolygonPrismBRep.Face face=before.findFace(faceId);
  if(face==null||face.sourceLineId==null||face.vertexIds().size()!=4)
   throw new IllegalArgumentException("Solo se pueden desplazar caras laterales planas");
  PolygonPrismBRep.Vertex v0=before.findVertex(face.vertexIds().get(0)),
    v1=before.findVertex(face.vertexIds().get(1));
  if(v0==null||v1==null)throw new IllegalArgumentException("Cara sin vértices");
  double[] a=uv(feature.plane,v0.position()),b=uv(feature.plane,v1.position());
  double deltaU=b[0]-a[0],deltaV=b[1]-a[1];
  double length=Math.hypot(deltaU,deltaV);
  if(!Double.isFinite(length)||length<=SNAP)
   throw new IllegalArgumentException("Arista lateral degenerada");
  // Ring is canonical UV counterclockwise even for signed extrusions.
  double dx=deltaV/length*millimeters;
  double dy=-deltaU/length*millimeters;
  LinkedHashMap<String,ClosedSketchExtrusion.Segment> next=new LinkedHashMap<>();
  LinkedHashSet<String> changed=new LinkedHashSet<>();
  int[] matches={0};
  for(String id:feature.lineIds()){
   ClosedSketchExtrusion.Segment s=current.get(id);
   if(s==null||s.plane!=feature.plane)
    throw new IllegalArgumentException("Boceto original no disponible");
   double[] start=shift(s.ax,s.ay,a,b,dx,dy,matches);
   double[] end=shift(s.bx,s.by,a,b,dx,dy,matches);
   if(start[0]!=s.ax||start[1]!=s.ay||
      end[0]!=s.bx||end[1]!=s.by)changed.add(id);
   // Save/edit Android sketch coordinates as float, so rebuild from those
   // exact quantized values and never stage a mesh that JSON can't reload.
   ClosedSketchExtrusion.Segment edit=new ClosedSketchExtrusion.Segment(s.plane,
    (float)start[0],(float)start[1],(float)end[0],(float)end[1]);
   next.put(id,edit);
  }
  if(matches[0]!=4||!changed.contains(face.sourceLineId)||
     changed.size()<2||changed.size()>3)
   throw new IllegalArgumentException("Contorno mal conectado alrededor de la cara");
  PolygonPrismBRep after=feature.asBRep(next);
  if(after.findFace(faceId)==null||after.vertices().size()!=before.vertices().size()||
     after.edges().size()!=before.edges().size())
   throw new IllegalArgumentException("La edición cambia la topología");
  return new Plan(feature,face,millimeters,next,changed,after);
 }
}
