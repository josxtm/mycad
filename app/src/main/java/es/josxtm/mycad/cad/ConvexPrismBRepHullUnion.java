package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Analytic union of two convex prism sections with the same signed height.
 * Reconstructs a single convex B-Rep only when the convex hull has exactly
 * the area of the true union. Concave unions, disconnected solids, holes,
 * different heights and different work planes are deliberately rejected.
 *
 * Unlike the orthogonal Boolean, this also supports slanted polygon edges
 * and two prisms sharing a full boundary edge without volume overlap.
 */
public final class ConvexPrismBRepHullUnion {
 private static final double EPS=1e-8;
 private static final double HEIGHT_EPS=1e-9;
 private static final int MAX_VERTICES=100;
 private ConvexPrismBRepHullUnion(){}

 public static final class Result {
  public final PolygonPrismBRep solid;
  public final double area,volume,distance;
  public final int plane;
  private Result(PolygonPrismBRep solid){
   this.solid=solid;area=solid.sectionArea();volume=solid.volume();
   distance=solid.distance();plane=solid.plane();
  }
  public float[] triangles(){return solid.triangles();}
 }
 private static double[] uv(PolygonPrismBRep.Vertex v,int plane){
  if(plane==0)return new double[]{v.x,v.y};
  if(plane==1)return new double[]{v.x,v.z};
  return new double[]{v.y,v.z};
 }
 private static double cross(double[] a,double[] b,double[] c){
  return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
 }
 private static double area(List<double[]> p){
  double sum=0;
  for(int i=0;i<p.size();i++){
   double[] a=p.get(i),b=p.get((i+1)%p.size());
   sum+=a[0]*b[1]-a[1]*b[0];
  }
  return sum/2;
 }
 private static List<double[]> profile(PolygonPrismBRep body){
  int n=body.vertices().size()/2;
  if(n<3||n>MAX_VERTICES||body.vertices().size()!=2*n)
   throw new IllegalArgumentException("Unión convexa: contorno fuera de límites");
  ArrayList<double[]> p=new ArrayList<>();
  for(int i=0;i<n;i++)p.add(uv(body.vertices().get(i),body.plane()));
  if(area(p)<0)Collections.reverse(p);
  if(area(p)<=EPS)throw new IllegalArgumentException("Unión convexa: área nula");
  for(int i=0;i<n;i++){
   double[] a=p.get(i),b=p.get((i+1)%n),c=p.get((i+2)%n);
   double ab=Math.hypot(b[0]-a[0],b[1]-a[1]);
   double bc=Math.hypot(c[0]-b[0],c[1]-b[1]);
   if(ab<=.15||cross(a,b,c)<-EPS*Math.max(1,ab*bc))
    throw new IllegalArgumentException("Unión convexa: perfil no convexo");
  }
  return p;
 }
 private static List<double[]> hull(List<double[]> a,List<double[]> b){
  ArrayList<double[]> sorted=new ArrayList<>();
  sorted.addAll(a);sorted.addAll(b);
  sorted.sort((x,y)->{
   int c=Double.compare(x[0],y[0]);
   return c!=0?c:Double.compare(x[1],y[1]);
  });
  ArrayList<double[]> unique=new ArrayList<>();
  for(double[] p:sorted){
   if(unique.isEmpty()||Math.hypot(p[0]-unique.get(unique.size()-1)[0],
       p[1]-unique.get(unique.size()-1)[1])>EPS)unique.add(p);
  }
  if(unique.size()<3)throw new IllegalArgumentException("Unión convexa degenerada");
  ArrayList<double[]> lower=new ArrayList<>(),upper=new ArrayList<>();
  for(double[] p:unique){
   while(lower.size()>=2&&cross(lower.get(lower.size()-2),
       lower.get(lower.size()-1),p)<=EPS)lower.remove(lower.size()-1);
   lower.add(p);
  }
  for(int i=unique.size()-1;i>=0;i--){
   double[] p=unique.get(i);
   while(upper.size()>=2&&cross(upper.get(upper.size()-2),
       upper.get(upper.size()-1),p)<=EPS)upper.remove(upper.size()-1);
   upper.add(p);
  }
  lower.remove(lower.size()-1);upper.remove(upper.size()-1);
  lower.addAll(upper);
  if(lower.size()<3||lower.size()>MAX_VERTICES||area(lower)<=EPS)
   throw new IllegalArgumentException("Unión convexa degenerada");
  return lower;
 }

 public static Result union(PolygonPrismBRep first,PolygonPrismBRep second){
  if(first==null||second==null||first.plane()!=second.plane())
   throw new IllegalArgumentException("Unión convexa: planos incompatibles");
  double h=first.distance(),other=second.distance();
  if(!Double.isFinite(h)||!Double.isFinite(other)||h*other<=0||
     Math.abs(h-other)>HEIGHT_EPS*Math.max(1,Math.max(Math.abs(h),Math.abs(other))))
   throw new IllegalArgumentException("Unión convexa: alturas firmadas diferentes");
  List<double[]> a=profile(first),b=profile(second);
  List<double[]> boundary=hull(a,b);
  double common=0;
  try{
   common=ConvexPrismBRepIntersection.intersect(first,second).area;
  }catch(IllegalArgumentException noOverlap){
   // The only admissible zero-overlap case is full-edge contact.
   // The hull-area equality below excludes separated or point-only bodies.
   if(!"Los sólidos no comparten volumen".equals(noOverlap.getMessage()))
    throw noOverlap;
  }
  double expected=area(a)+area(b)-common;
  double hullArea=area(boundary);
  if(Math.abs(hullArea-expected)>1e-5*Math.max(1,expected))
   throw new IllegalArgumentException("Unión convexa: resultado cóncavo o desconectado");
  String idA=first.id(),idB=second.id();
  if(idA.compareTo(idB)>0){String t=idA;idA=idB;idB=t;}
  StringBuilder seed=new StringBuilder(idA).append('|').append(idB)
      .append("|CONVEX_UNION|").append(first.plane()).append('|').append(h);
  for(double[] p:boundary)seed.append('|').append((float)p[0]).append(',').append((float)p[1]);
  String bodyId=UUID.nameUUIDFromBytes(seed.toString().getBytes(StandardCharsets.UTF_8))
      .toString();
  ArrayList<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<boundary.size();i++){
   double[] p=boundary.get(i),q=boundary.get((i+1)%boundary.size());
   String line=UUID.nameUUIDFromBytes((bodyId+"/line/"+i)
       .getBytes(StandardCharsets.UTF_8)).toString();
   ids.add(line);
   lines.put(line,new ClosedSketchExtrusion.Segment(first.plane(),
       (float)p[0],(float)p[1],(float)q[0],(float)q[1]));
  }
  PolygonPrismBRep solid=new LoopExtrusionFeature(bodyId,ids.get(0),
      first.plane(),h,ids).asBRep(lines);
  if(Math.abs(solid.sectionArea()-expected)>1e-5*Math.max(1,expected)||
     Math.abs(solid.volume()-expected*Math.abs(h))>
       1e-5*Math.max(1,expected*Math.abs(h)))
   throw new IllegalArgumentException("Unión convexa: área o volumen inconsistente");
  return new Result(solid);
 }
}
