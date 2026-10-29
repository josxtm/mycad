package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Exact planar half-space clipping of two CONVEX polygonal prism sections
 * extruded from the same work plane and the same origin plane. Produces a new
 * analytic half-edge B-Rep for the common volume and its watertight preview.
 *
 * Convex/convex only: Sutherland-Hodgman cannot represent disconnected
 * intersections or holes in concave profiles. Such operands are rejected.
 * Other-plane intersections, coplanar touch without volume and general
 * freeform/curved Boolean operations are deliberately unsupported.
 */
public final class ConvexPrismBRepIntersection {
 private static final double EPS=1e-8;
 private static final int MAX_CORNERS=200;
 private ConvexPrismBRepIntersection(){}

 public static final class Result {
  public final PolygonPrismBRep solid;
  public final double area,volume,distance;
  public final int plane;
  private Result(PolygonPrismBRep solid){
   this.solid=solid;
   area=solid.sectionArea();volume=solid.volume();
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
 private static double sqdist(double[] a,double[] b){
  double dx=a[0]-b[0],dy=a[1]-b[1];return dx*dx+dy*dy;
 }
 private static double twiceArea(List<double[]> p){
  if(p.isEmpty())return 0;
  // Local coordinates avoid cancellation for small sections far from origin.
  double ox=p.get(0)[0],oy=p.get(0)[1];
  double sum=0,correction=0;
  for(int i=0;i<p.size();i++){
   double[] a=p.get(i),b=p.get((i+1)%p.size());
   double ax=a[0]-ox,ay=a[1]-oy,bx=b[0]-ox,by=b[1]-oy;
   double term=ax*by-bx*ay;
   double next=sum+term;
   // Neumaier compensation retains small contributions in long contours.
   if(Math.abs(sum)>=Math.abs(term))correction+=(sum-next)+term;
   else correction+=(term-next)+sum;
   sum=next;
  }
  return sum+correction;
 }
 private static List<double[]> polygon(PolygonPrismBRep body){
  if(body==null)throw new IllegalArgumentException("Falta sólido B-Rep");
  int n=body.vertices().size()/2;
  if(n<3||n>MAX_CORNERS||body.vertices().size()!=2*n)
   throw new IllegalArgumentException("Contorno B-Rep fuera de límites");
  ArrayList<double[]> p=new ArrayList<>(n);
  for(int i=0;i<n;i++)p.add(uv(body.vertices().get(i),body.plane()));
  if(twiceArea(p)<0)Collections.reverse(p);
  double epsArea=EPS*Math.max(1,body.sectionArea());
  for(int i=0;i<n;i++){
   double[] a=p.get(i),b=p.get((i+1)%n),c=p.get((i+2)%n);
   if(sqdist(a,b)<=.15*.15)
    throw new IllegalArgumentException("Arista B-Rep demasiado corta");
   if(cross(a,b,c)<-epsArea)
    throw new IllegalArgumentException("Booleanas B-Rep: solo perfiles convexos");
  }
  if(twiceArea(p)<=epsArea)
   throw new IllegalArgumentException("Contorno sin superficie");
  return p;
 }
 private static boolean inside(double[] p,double[] a,double[] b){
  return cross(a,b,p)>=-EPS;
 }
 private static double[] intersectLine(double[] start,double[] end,
                                       double[] a,double[] b){
  double s=cross(a,b,start),e=cross(a,b,end);
  double den=s-e;
  if(Math.abs(den)<1e-14)
   throw new IllegalArgumentException("Corte B-Rep numéricamente degenerado");
  double t=s/den;
  if(t< -1e-7||t>1+1e-7)
   throw new IllegalArgumentException("Intersección fuera de segmento");
  t=Math.max(0,Math.min(1,t));
  return new double[]{start[0]+t*(end[0]-start[0]),
                      start[1]+t*(end[1]-start[1])};
 }
 private static void push(List<double[]> out,double[] v){
  if(out.isEmpty()||sqdist(out.get(out.size()-1),v)>1e-16)
   out.add(v);
 }
 private static List<double[]> simplify(List<double[]> input){
  ArrayList<double[]> p=new ArrayList<>();
  for(double[] v:input)push(p,v);
  if(p.size()>1&&sqdist(p.get(0),p.get(p.size()-1))<=1e-16)
   p.remove(p.size()-1);
  boolean changed=true;
  while(changed&&p.size()>2){
   changed=false;
   for(int i=0;i<p.size();i++){
    int n=p.size();
    double[] a=p.get((i+n-1)%n),b=p.get(i),c=p.get((i+1)%n);
    double u=b[0]-a[0],v=b[1]-a[1],w=c[0]-b[0],z=c[1]-b[1];
    double turn=u*z-v*w;
    if(sqdist(a,b)<=1e-12||sqdist(b,c)<=1e-12||
       (Math.abs(turn)<=EPS*Math.max(1,Math.hypot(u,v)*Math.hypot(w,z))&&
        u*w+v*z>=0)){
     p.remove(i);changed=true;break;
    }
   }
  }
  if(p.size()>MAX_CORNERS)
   throw new IllegalArgumentException("Resultado B-Rep demasiado complejo");
  return p;
 }
 private static List<double[]> clip(List<double[]> subject,List<double[]> cutter){
  List<double[]> input=subject;
  for(int i=0;i<cutter.size();i++){
   double[] a=cutter.get(i),b=cutter.get((i+1)%cutter.size());
   if(input.isEmpty())break;
   ArrayList<double[]> output=new ArrayList<>();
   double[] previous=input.get(input.size()-1);
   boolean wasInside=inside(previous,a,b);
   for(double[] current:input){
    boolean nowInside=inside(current,a,b);
    if(wasInside!=nowInside)push(output,intersectLine(previous,current,a,b));
    if(nowInside)push(output,current);
    previous=current;wasInside=nowInside;
   }
   input=simplify(output);
  }
  return simplify(input);
 }

 /**
  * The result is a SNAPSHOT Boolean body; no new source sketches are appended
  * to the Android project. A new deterministic UUID is derived from both
  * source B-Rep IDs and clipped coordinates solely for analytic topology.
  */
 public static Result intersect(PolygonPrismBRep a,PolygonPrismBRep b){
  if(a==null||b==null||a.plane()!=b.plane())
   throw new IllegalArgumentException("Ambos contornos deben compartir plano XY/XZ/YZ");
  if(a.distance()*b.distance()<=0)
   throw new IllegalArgumentException("Extrusiones hacia lados opuestos no solapan volumen");
  List<double[]> subject=polygon(a),cutter=polygon(b);
  List<double[]> overlap=clip(subject,cutter);
  if(overlap.size()<3||Math.abs(twiceArea(overlap))<=EPS)
   throw new IllegalArgumentException("Los sólidos no comparten volumen");
  if(twiceArea(overlap)<0)Collections.reverse(overlap);
  // Clipping can start at a different vertex when operands are swapped.
  // Normalize the CCW loop before deriving topology IDs and tessellation.
  int firstCorner=0;
  for(int i=1;i<overlap.size();i++){
   double[] p=overlap.get(i),q=overlap.get(firstCorner);
   int cmp=Double.compare(p[0],q[0]);
   if(cmp<0||(cmp==0&&Double.compare(p[1],q[1])<0))firstCorner=i;
  }
  Collections.rotate(overlap,-firstCorner);
  double distance=Math.copySign(Math.min(Math.abs(a.distance()),Math.abs(b.distance())),
                               a.distance());
  // A intersection B and B intersection A describe the same feature.
  String idA=a.id(),idB=b.id();
  if(idA.compareTo(idB)>0){String tmp=idA;idA=idB;idB=tmp;}
  StringBuilder seed=new StringBuilder(idA).append('|').append(idB)
   .append('|').append(a.plane()).append('|').append(distance);
  for(double[] p:overlap)seed.append('|').append((float)p[0]).append(',')
                                .append((float)p[1]);
  byte[] bytes=seed.toString().getBytes(StandardCharsets.UTF_8);
  String resultId=UUID.nameUUIDFromBytes(bytes).toString();
  ArrayList<String> sources=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<overlap.size();i++){
   double[] p=overlap.get(i),q=overlap.get((i+1)%overlap.size());
   String line=UUID.nameUUIDFromBytes((resultId+"/line/"+i)
       .getBytes(StandardCharsets.UTF_8)).toString();
   sources.add(line);
   lines.put(line,new ClosedSketchExtrusion.Segment(a.plane(),
       (float)p[0],(float)p[1],(float)q[0],(float)q[1]));
  }
  LoopExtrusionFeature linked=new LoopExtrusionFeature(resultId,sources.get(0),
       a.plane(),distance,sources);
  PolygonPrismBRep solid=linked.asBRep(lines);
  double expected=Math.abs(twiceArea(overlap))/2*Math.abs(distance);
  if(Math.abs(solid.volume()-expected)>1e-5*Math.max(1,expected))
   throw new IllegalArgumentException("Volumen de la intersección incorrecto");
  return new Result(solid);
 }
}
