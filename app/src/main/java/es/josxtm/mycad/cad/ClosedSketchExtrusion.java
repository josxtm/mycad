package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Extrudes one connected closed polyline of straight sketch segments into
 * an oriented watertight triangle mesh. Concave profiles are supported.
 *
 * No holes, self-intersections, arcs or B-Rep history. Geometry is snapshot
 * mesh output and never silently changes when its source sketch is edited.
 */
public final class ClosedSketchExtrusion {
 private ClosedSketchExtrusion(){}
 private static final double SNAP_MM=0.15;
 private static final int MAX_LINES=200;
 public static final class Segment {
  public final int plane;
  public final double ax,ay,bx,by;
  public Segment(int plane,double ax,double ay,double bx,double by){
   if(plane<0||plane>2||!ok(ax)||!ok(ay)||!ok(bx)||!ok(by)||
      Math.max(Math.max(Math.abs(ax),Math.abs(ay)),Math.max(Math.abs(bx),Math.abs(by)))>10000||
      Math.hypot(bx-ax,by-ay)<=SNAP_MM)
    throw new IllegalArgumentException("Segmento de boceto inválido");
   this.plane=plane;this.ax=ax;this.ay=ay;this.bx=bx;this.by=by;
  }
 }
 public static final class Result {
  public final float[] triangles;
  public final int plane,usedLines;
  public final double area,volume;
  private Result(float[] t,int plane,int used,double area,double volume){
   this.triangles=t.clone();this.plane=plane;usedLines=used;
   this.area=area;this.volume=volume;
  }
 }
 private static final class Vertex {
  final double u,v;
  final ArrayList<Integer> incident=new ArrayList<>();
  Vertex(double u,double v){this.u=u;this.v=v;}
 }
 private static final class Edge {
  final int a,b;
  Edge(int a,int b){this.a=a;this.b=b;}
  int opposite(int vertex){
   if(vertex==a)return b;
   if(vertex==b)return a;
   throw new IllegalArgumentException("Segmentos sin continuidad");
  }
 }
 private static boolean ok(double v){return Double.isFinite(v);}
 private static double cross(Vertex a,Vertex b,Vertex c){
  return (b.u-a.u)*(c.v-a.v)-(b.v-a.v)*(c.u-a.u);
 }
 private static int attach(ArrayList<Vertex> points,double u,double v){
  int match=-1;
  for(int i=0;i<points.size();i++){
   Vertex p=points.get(i);
   if(Math.hypot(p.u-u,p.v-v)<=SNAP_MM){
    if(match!=-1)throw new IllegalArgumentException("Nudos de perfil ambiguos");
    match=i;
   }
  }
  if(match>=0)return match;
  points.add(new Vertex(u,v));return points.size()-1;
 }
 private static boolean between(Vertex a,Vertex b,Vertex p,double eps){
  return Math.abs(cross(a,b,p))<=eps&&
    p.u>=Math.min(a.u,b.u)-1e-8&&p.u<=Math.max(a.u,b.u)+1e-8&&
    p.v>=Math.min(a.v,b.v)-1e-8&&p.v<=Math.max(a.v,b.v)+1e-8;
 }
 private static int sign(double n,double eps){return n>eps?1:n< -eps?-1:0;}
 private static boolean intersects(Vertex a,Vertex b,Vertex c,Vertex d,double eps){
  double x=cross(a,b,c),y=cross(a,b,d),z=cross(c,d,a),w=cross(c,d,b);
  int sx=sign(x,eps),sy=sign(y,eps),sz=sign(z,eps),sw=sign(w,eps);
  return (sx*sy<0&&sz*sw<0)||
   (sx==0&&between(a,b,c,eps))||
   (sy==0&&between(a,b,d,eps))||
   (sz==0&&between(c,d,a,eps))||
   (sw==0&&between(c,d,b,eps));
 }
 private static boolean withinTriangle(Vertex p,Vertex a,Vertex b,Vertex c,double eps){
  return cross(a,b,p)>=-eps&&cross(b,c,p)>=-eps&&cross(c,a,p)>=-eps;
 }

 /**
  * Return true for an interior or boundary point, without modifying the
  * selected loop. Rejecting external geometry inside a selected profile
  * prevents silently filling what may be an intended hole.
  */
 private static boolean insideOrBoundary(Vertex p,List<Integer> loop,
                                         List<Vertex> points,double eps){
  boolean inside=false;
  for(int i=0;i<loop.size();i++){
   Vertex a=points.get(loop.get(i)),
          b=points.get(loop.get((i+1)%loop.size()));
   if(between(a,b,p,eps))return true;
   if((a.v>p.v)!=(b.v>p.v)){
    double intersection=a.u+(p.v-a.v)*(b.u-a.u)/(b.v-a.v);
    if(p.u<intersection)inside=!inside;
   }
  }
  return inside;
 }

 /** Map (u,v,height) consistently with the app's XY/XZ/YZ work planes. */
 private static double[] world(int plane,Vertex p,double height){
  if(plane==0)return new double[]{p.u,p.v,height};
  if(plane==1)return new double[]{p.u,height,p.v};
  return new double[]{height,p.u,p.v};
 }
 private static void triangle(ArrayList<Float> result,int plane,
                              Vertex a,double ah,Vertex b,double bh,Vertex c,double ch){
  double[][] v={world(plane,a,ah),world(plane,b,bh),world(plane,c,ch)};
  // (u,v,h)->(x,h,v) reverses handedness for plane XZ.
  int[] order=plane==1?new int[]{0,2,1}:new int[]{0,1,2};
  for(int n:order)for(double coord:v[n]){
   if(!ok(coord)||Math.abs(coord)>10000)
    throw new IllegalArgumentException("Extrusión fuera del área de trabajo");
   result.add((float)coord);
  }
 }
 /**
  * All line segments in the selected work plane, and index of the selected
  * line within that list. Only its connected component is extruded.
  */
 public static Result extrude(List<Segment> lines,int selected,double height){
  if(lines==null||lines.isEmpty()||lines.size()>MAX_LINES||
     selected<0||selected>=lines.size()||lines.get(selected)==null||
     !ok(height)||height==0||Math.abs(height)>10000)
   throw new IllegalArgumentException("Perfil o altura de extrusión inválidos");
  int plane=lines.get(selected).plane;
  ArrayList<Vertex> points=new ArrayList<>();
  ArrayList<Edge> edges=new ArrayList<>();
  for(Segment s:lines){
   if(s==null||s.plane!=plane)throw new IllegalArgumentException("Plano de boceto inconsistente");
   int a=attach(points,s.ax,s.ay),b=attach(points,s.bx,s.by);
   if(a==b)throw new IllegalArgumentException("Segmento colapsado al unir extremos");
   int id=edges.size();edges.add(new Edge(a,b));
   points.get(a).incident.add(id);points.get(b).incident.add(id);
  }
  Set<Integer> component=new HashSet<>();
  ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(selected);
  while(!queue.isEmpty()){
   int id=queue.removeFirst();
   if(!component.add(id))continue;
   Edge e=edges.get(id);
   for(int endpoint:new int[]{e.a,e.b})
    for(int neighbor:points.get(endpoint).incident)if(!component.contains(neighbor))
     queue.add(neighbor);
  }
  if(component.size()<3)throw new IllegalArgumentException("Contorno sin cierre: hacen falta 3 líneas");
  for(int id:component){
   Edge edge=edges.get(id);
   if(points.get(edge.a).incident.size()!=2||
      points.get(edge.b).incident.size()!=2)
    throw new IllegalArgumentException("Contorno abierto o con ramificaciones");
  }
  ArrayList<Integer> contour=new ArrayList<>();
  Set<Integer> visited=new HashSet<>();
  int start=edges.get(selected).a,vertex=start,current=selected;
  do{
   if(!visited.add(current))throw new IllegalArgumentException("Contorno repetido");
   contour.add(vertex);
   int next=edges.get(current).opposite(vertex);
   if(next==start){vertex=start;break;}
   ArrayList<Integer> linked=points.get(next).incident;
   int follow=linked.get(0)==current?linked.get(1):linked.get(0);
   vertex=next;current=follow;
  }while(contour.size()<=component.size());
  if(vertex!=start||visited.size()!=component.size()||contour.size()<3)
   throw new IllegalArgumentException("Contorno no cerrado");
  double area2=0;
  double minU=Double.POSITIVE_INFINITY,minV=Double.POSITIVE_INFINITY;
  double maxU=Double.NEGATIVE_INFINITY,maxV=Double.NEGATIVE_INFINITY;
  for(int i=0;i<contour.size();i++){
   Vertex a=points.get(contour.get(i)),b=points.get(contour.get((i+1)%contour.size()));
   area2+=a.u*b.v-b.u*a.v;
   minU=Math.min(minU,a.u);minV=Math.min(minV,a.v);
   maxU=Math.max(maxU,a.u);maxV=Math.max(maxV,a.v);
  }
  double extent=Math.max(maxU-minU,maxV-minV);
  double eps=1e-12*extent*extent;
  if(Math.abs(area2)<=eps)
   throw new IllegalArgumentException("Contorno sin superficie");
  if(area2<0){Collections.reverse(contour);area2= -area2;}
  int n=contour.size();
  for(int i=0;i<n;i++)for(int j=i+1;j<n;j++){
   if(j==i+1||(i==0&&j==n-1))continue;
   Vertex a=points.get(contour.get(i)),b=points.get(contour.get((i+1)%n));
   Vertex c=points.get(contour.get(j)),d=points.get(contour.get((j+1)%n));
   if(intersects(a,b,c,d,eps))
    throw new IllegalArgumentException("El contorno se cruza o se toca a sí mismo");
  }
  // A second disconnected loop inside this boundary normally represents a
  // hole. It is NOT a separate filled extrusion: reject instead of creating
  // incorrect solid material. An unrelated exterior loop remains selectable.
  for(int i=0;i<edges.size();i++){
   if(component.contains(i))continue;
   Edge other=edges.get(i);
   Vertex a=points.get(other.a),b=points.get(other.b);
   if(insideOrBoundary(a,contour,points,eps)||
      insideOrBoundary(b,contour,points,eps))
    throw new IllegalArgumentException("El perfil contiene líneas internas: agujeros no soportados");
   for(int k=0;k<n;k++){
    Vertex c=points.get(contour.get(k)),d=points.get(contour.get((k+1)%n));
    if(intersects(a,b,c,d,eps))
     throw new IllegalArgumentException("Otra línea atraviesa el contorno seleccionado");
   }
  }
  // Ear clipping handles concavity; point-on-edge counts as inside so a cap
  // never skips a boundary vertex and creates a non-manifold T junction.
  ArrayList<Integer> remaining=new ArrayList<>(contour);
  ArrayList<int[]> cap=new ArrayList<>();
  while(remaining.size()>3){
   boolean cut=false;
   for(int i=0;i<remaining.size();i++){
    int nRem=remaining.size();
    int ai=remaining.get((i+nRem-1)%nRem),bi=remaining.get(i),
        ci=remaining.get((i+1)%nRem);
    Vertex a=points.get(ai),b=points.get(bi),c=points.get(ci);
    if(cross(a,b,c)<=eps)continue;
    boolean contains=false;
    for(int other:remaining){
     if(other!=ai&&other!=bi&&other!=ci&&
        withinTriangle(points.get(other),a,b,c,eps)){contains=true;break;}
    }
    if(contains)continue;
    cap.add(new int[]{ai,bi,ci});
    remaining.remove(i);cut=true;break;
   }
   if(!cut)throw new IllegalArgumentException("No se puede triangular el perfil");
  }
  if(cross(points.get(remaining.get(0)),points.get(remaining.get(1)),
           points.get(remaining.get(2)))<=eps)
   throw new IllegalArgumentException("Última cara degenerada");
  cap.add(new int[]{remaining.get(0),remaining.get(1),remaining.get(2)});
  ArrayList<Float> geometry=new ArrayList<>((4*n-4)*9);
  for(int[] t:cap){
   Vertex a=points.get(t[0]),b=points.get(t[1]),c=points.get(t[2]);
   triangle(geometry,plane,a,height,b,height,c,height);
   triangle(geometry,plane,c,0,b,0,a,0);
  }
  for(int i=0;i<n;i++){
   Vertex a=points.get(contour.get(i)),b=points.get(contour.get((i+1)%n));
   triangle(geometry,plane,a,0,b,0,b,height);
   triangle(geometry,plane,a,0,b,height,a,height);
  }
  float[] mesh=new float[geometry.size()];
  for(int i=0;i<mesh.length;i++)mesh[i]=geometry.get(i);
  // The same caps and walls can be used in both normal directions, but
  // the outward winding must reverse when the extrusion goes backwards.
  if(height<0)for(int i=0;i<mesh.length;i+=9){
   for(int k=0;k<3;k++){
    float tmp=mesh[i+3+k];
    mesh[i+3+k]=mesh[i+6+k];
    mesh[i+6+k]=tmp;
   }
  }
  TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(mesh);
  if(!health.isWatertight()||health.signedVolume<=0)
   throw new IllegalArgumentException("La extrusión no produce una malla cerrada válida");
  return new Result(mesh,plane,component.size(),area2/2,area2*Math.abs(height)/2);
 }
}
