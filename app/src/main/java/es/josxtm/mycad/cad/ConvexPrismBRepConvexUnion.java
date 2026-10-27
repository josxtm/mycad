package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Analytic B-Rep union of two convex polygon prisms whose union remains convex.
 * Both prisms must start on the same work plane and have IDENTICAL signed
 * extrusion heights. A partially overlapping pair can be represented by one
 * prism exactly when its section union equals the convex hull of both sections.
 *
 * Nonconvex unions, disconnected bodies, contact without shared volume,
 * different heights and curved/freeform operands are deliberately rejected.
 * No source sketches or operands are mutated.
 */
public final class ConvexPrismBRepConvexUnion {
 private static final double AREA_EPS=1e-6;
 private static final int MAX_CORNERS=200;
 private ConvexPrismBRepConvexUnion(){}

 private static double[] uv(PolygonPrismBRep.Vertex v,int plane){
  if(plane==0)return new double[]{v.x,v.y};
  if(plane==1)return new double[]{v.x,v.z};
  return new double[]{v.y,v.z};
 }
 private static double cross(double[] a,double[] b,double[] c){
  return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
 }
 private static double area(List<double[]> ring){
  double twice=0;
  for(int i=0;i<ring.size();i++){
   double[] a=ring.get(i),b=ring.get((i+1)%ring.size());
   twice+=a[0]*b[1]-b[0]*a[1];
  }
  return Math.abs(twice)*.5;
 }
 private static List<double[]> hull(PolygonPrismBRep a,PolygonPrismBRep b){
  ArrayList<double[]> points=new ArrayList<>();
  for(PolygonPrismBRep body:new PolygonPrismBRep[]{a,b}){
   int n=body.vertices().size()/2;
   for(int i=0;i<n;i++)points.add(uv(body.vertices().get(i),body.plane()));
  }
  points.sort((p,q)->{
   int x=Double.compare(p[0],q[0]);
   return x!=0?x:Double.compare(p[1],q[1]);
  });
  ArrayList<double[]> unique=new ArrayList<>();
  for(double[] p:points)
   if(unique.isEmpty()||p[0]!=unique.get(unique.size()-1)[0]||
       p[1]!=unique.get(unique.size()-1)[1])unique.add(p);
  if(unique.size()<3)throw new IllegalArgumentException("Unión convexa degenerada");
  ArrayList<double[]> lower=new ArrayList<>(),upper=new ArrayList<>();
  for(double[] p:unique){
   while(lower.size()>1&&cross(lower.get(lower.size()-2),
        lower.get(lower.size()-1),p)<=0)lower.remove(lower.size()-1);
   lower.add(p);
  }
  for(int i=unique.size()-1;i>=0;i--){
   double[] p=unique.get(i);
   while(upper.size()>1&&cross(upper.get(upper.size()-2),
        upper.get(upper.size()-1),p)<=0)upper.remove(upper.size()-1);
   upper.add(p);
  }
  lower.remove(lower.size()-1);
  upper.remove(upper.size()-1);
  lower.addAll(upper);
  if(lower.size()<3||lower.size()>MAX_CORNERS)
   throw new IllegalArgumentException("Contorno de unión fuera de límites");
  return lower;
 }

 public static PolygonPrismBRep union(PolygonPrismBRep a,PolygonPrismBRep b){
  if(a==null||b==null||a.plane()!=b.plane())
   throw new IllegalArgumentException("Unión convexa: ambos sólidos deben compartir plano");
  if(Double.compare(a.distance(),b.distance())!=0||a.distance()==0)
   throw new IllegalArgumentException("Unión convexa: se requieren alturas firmadas idénticas");
  // Intersection validates convexity and rejects disjoint/contact-only sections.
  ConvexPrismBRepIntersection.Result common=ConvexPrismBRepIntersection.intersect(a,b);
  double expected=a.sectionArea()+b.sectionArea()-common.area;
  if(!Double.isFinite(expected)||expected<=0)
   throw new IllegalArgumentException("Área de unión inválida");
  double tolerance=AREA_EPS*Math.max(1,expected);
  if(Math.abs(common.area-a.sectionArea())<=tolerance&&
     Math.abs(common.area-b.sectionArea())<=tolerance)
   return a.id().compareTo(b.id())<=0?a:b;
  if(Math.abs(common.area-a.sectionArea())<=tolerance)return b;
  if(Math.abs(common.area-b.sectionArea())<=tolerance)return a;

  List<double[]> ring=hull(a,b);
  double hullArea=area(ring);
  if(Math.abs(hullArea-expected)>tolerance)
   throw new IllegalArgumentException("Unión no convexa: requiere reconstruir caras y aristas");
  String first=a.id().compareTo(b.id())<=0?a.id():b.id();
  String second=a.id().compareTo(b.id())<=0?b.id():a.id();
  StringBuilder seed=new StringBuilder("convex-union|").append(first).append('|')
    .append(second).append('|').append(a.plane()).append('|').append(a.distance());
  for(double[] p:ring)seed.append('|').append(Float.toString((float)p[0]))
                           .append(',').append(Float.toString((float)p[1]));
  String resultId=UUID.nameUUIDFromBytes(seed.toString()
      .getBytes(StandardCharsets.UTF_8)).toString();
  ArrayList<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<ring.size();i++){
   double[] p=ring.get(i),q=ring.get((i+1)%ring.size());
   String edgeId=UUID.nameUUIDFromBytes((resultId+"/edge/"+i)
       .getBytes(StandardCharsets.UTF_8)).toString();
   ids.add(edgeId);
   lines.put(edgeId,new ClosedSketchExtrusion.Segment(a.plane(),
       (float)p[0],(float)p[1],(float)q[0],(float)q[1]));
  }
  PolygonPrismBRep solid=new LoopExtrusionFeature(resultId,ids.get(0),
      a.plane(),a.distance(),ids).asBRep(lines);
  if(Math.abs(solid.sectionArea()-expected)>tolerance||
     Math.abs(solid.volume()-expected*Math.abs(a.distance()))>
       1e-5*Math.max(1,expected*Math.abs(a.distance())))
   throw new IllegalArgumentException("Volumen de unión B-Rep inconsistente");
  return solid;
 }
}
