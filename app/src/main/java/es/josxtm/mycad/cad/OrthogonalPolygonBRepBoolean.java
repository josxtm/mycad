package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Planar B-Rep Boolean for TWO simple orthogonal polygon prism profiles,
 * including concave L/U/cross contours, extruded with identical signed
 * height from the same XY/XZ/YZ work plane.
 *
 * Performs exact cell membership on the vertex-coordinate arrangement,
 * extracts a single counterclockwise boundary, removes collinear points
 * and rebuilds an analytic half-edge B-Rep instead of stitching triangles.
 *
 * A hole, a disconnected region or touching-only disconnected components
 * cannot be represented by PolygonPrismBRep and are explicitly rejected.
 */
public final class OrthogonalPolygonBRepBoolean {
 public enum Operation { UNION, DIFFERENCE, INTERSECTION }
 private static final double EPS=1e-6;
 private static final int MAX_AXES=48;
 private OrthogonalPolygonBRepBoolean(){}

 public static final class Result {
  public final PolygonPrismBRep solid;
  public final Operation operation;
  public final double area,volume,distance;
  public final int plane;
  Result(PolygonPrismBRep body,Operation op){
   solid=body;operation=op;area=body.sectionArea();
   volume=body.volume();distance=body.distance();plane=body.plane();
  }
  public float[] triangles(){return solid.triangles();}
 }
 private static double[] uv(PolygonPrismBRep.Vertex v,int plane){
  return plane==0?new double[]{v.x,v.y}:
         plane==1?new double[]{v.x,v.z}:new double[]{v.y,v.z};
 }
 private static List<double[]> points(PolygonPrismBRep body){
  int n=body.vertices().size()/2;
  if(n<4||body.vertices().size()!=2*n||n>MAX_AXES*2)
   throw new IllegalArgumentException("Perfil ortogonal fuera de límites");
  List<double[]> p=new ArrayList<>(n);
  for(int i=0;i<n;i++)p.add(uv(body.vertices().get(i),body.plane()));
  for(int i=0;i<n;i++){
   double[] a=p.get(i),b=p.get((i+1)%n);
   boolean vertical=Math.abs(a[0]-b[0])<=EPS;
   boolean horizontal=Math.abs(a[1]-b[1])<=EPS;
   if(vertical==horizontal||Math.hypot(a[0]-b[0],a[1]-b[1])<=.15)
    throw new IllegalArgumentException("Booleanas ortogonales: solo líneas horizontales y verticales");
  }
  return p;
 }
 private static double[] coordinates(List<double[]> a,List<double[]> b,int dimension){
  ArrayList<Double> all=new ArrayList<>();
  for(double[] point:a)all.add(point[dimension]);
  for(double[] point:b)all.add(point[dimension]);
  Collections.sort(all);
  ArrayList<Double> unique=new ArrayList<>();
  for(double value:all){
   if(unique.isEmpty()||value-unique.get(unique.size()-1)>EPS)
    unique.add(value);
   else if(Math.abs(unique.get(unique.size()-1)-value)>1e-12)
    throw new IllegalArgumentException("Coordenadas ortogonales demasiado próximas");
  }
  if(unique.size()<2||unique.size()>MAX_AXES)
   throw new IllegalArgumentException("Demasiados ejes de corte B-Rep");
  double[] out=new double[unique.size()];
  for(int i=0;i<out.length;i++)out[i]=unique.get(i);
  return out;
 }
 private static boolean inside(List<double[]> poly,double x,double y){
  boolean result=false;
  for(int i=0,j=poly.size()-1;i<poly.size();j=i++){
   double[] a=poly.get(i),b=poly.get(j);
   if((a[1]>y)!=(b[1]>y)&&
       x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])
    result=!result;
  }
  return result;
 }
 private static boolean retain(boolean a,boolean b,Operation operation){
  switch(operation){
   case UNION:return a||b;
   case DIFFERENCE:return a&&!b;
   case INTERSECTION:return a&&b;
   default:throw new IllegalArgumentException("Operación B-Rep desconocida");
  }
 }
 private static int node(int i,int j,int width){return j*width+i;}
 private static void add(Map<Integer,Integer> edges,int from,int to){
  if(edges.put(from,to)!=null)
   throw new IllegalArgumentException("La booleana tiene contacto en un vértice o una bifurcación");
 }
 private static double twiceArea(List<double[]> polygon){
  double acc=0;
  for(int i=0;i<polygon.size();i++){
   double[] a=polygon.get(i),b=polygon.get((i+1)%polygon.size());
   acc+=a[0]*b[1]-b[0]*a[1];
  }
  return acc;
 }
 private static List<double[]> extract(double[] xs,double[] ys,boolean[][] cells){
  int nx=xs.length-1,ny=ys.length-1,width=xs.length;
  Map<Integer,Integer> edges=new LinkedHashMap<>();
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++)if(cells[i][j]){
   int bl=node(i,j,width),br=node(i+1,j,width),
       tr=node(i+1,j+1,width),tl=node(i,j+1,width);
   if(j==0||!cells[i][j-1])add(edges,bl,br);
   if(i==nx-1||!cells[i+1][j])add(edges,br,tr);
   if(j==ny-1||!cells[i][j+1])add(edges,tr,tl);
   if(i==0||!cells[i-1][j])add(edges,tl,bl);
  }
  if(edges.size()<4||edges.size()>2500)
   throw new IllegalArgumentException("La booleana no produce un contorno simple");
  int begin=Collections.min(edges.keySet()),current=begin;
  Set<Integer> visited=new HashSet<>();
  List<double[]> out=new ArrayList<>();
  do{
   if(!visited.add(current))
    throw new IllegalArgumentException("La booleana genera ciclos inválidos");
   out.add(new double[]{xs[current%width],ys[current/width]});
   Integer next=edges.get(current);
   if(next==null)throw new IllegalArgumentException("Contorno B-Rep abierto");
   current=next;
  }while(current!=begin);
  // One exterior contour, no interior holes and no disconnected shells.
  if(visited.size()!=edges.size())
   throw new IllegalArgumentException("Agujeros interiores o múltiples cuerpos no admitidos");
  for(int i=0;out.size()>3&&i<out.size();){
   int n=out.size();
   double[] p=out.get((i+n-1)%n),q=out.get(i),r=out.get((i+1)%n);
   double ux=q[0]-p[0],uy=q[1]-p[1],
          vx=r[0]-q[0],vy=r[1]-q[1];
   double cross=ux*vy-uy*vx;
   if(Math.abs(cross)<=EPS&&ux*vx+uy*vy>=0){
    out.remove(i);if(i>0)i--;
   }else i++;
  }
  if(out.size()<4||out.size()>100||twiceArea(out)<=EPS)
   throw new IllegalArgumentException("Contorno B-Rep degenerado o no conexo");
  return out;
 }
 public static Result apply(PolygonPrismBRep first,PolygonPrismBRep second,
                             Operation operation){
  if(first==null||second==null||operation==null||
     first.plane()!=second.plane())
   throw new IllegalArgumentException("Booleanas ortogonales: plano u operación incompatibles");
  double h=first.distance(),other=second.distance();
  if(!Double.isFinite(h)||!Double.isFinite(other)||
     Math.abs(h-other)>EPS*Math.max(1,Math.abs(h)))
   throw new IllegalArgumentException("Booleanas ortogonales: alturas firmadas distintas");
  List<double[]> a=points(first),b=points(second);
  double[] xs=coordinates(a,b,0),ys=coordinates(a,b,1);
  if((xs.length-1)*(ys.length-1)>MAX_AXES*MAX_AXES)
   throw new IllegalArgumentException("Booleana excede la cuadrícula admitida");
  boolean[][] cells=new boolean[xs.length-1][ys.length-1];
  double area=0;
  for(int i=0;i<xs.length-1;i++)for(int j=0;j<ys.length-1;j++){
   double x=(xs[i]+xs[i+1])/2,y=(ys[j]+ys[j+1])/2;
   cells[i][j]=retain(inside(a,x,y),inside(b,x,y),operation);
   if(cells[i][j])area+=(xs[i+1]-xs[i])*(ys[j+1]-ys[j]);
  }
  if(area<=EPS)
   throw new IllegalArgumentException("Sin volumen en el resultado B-Rep");
  List<double[]> boundary=extract(xs,ys,cells);
  String fId=first.id(),sId=second.id();
  if(operation!=Operation.DIFFERENCE&&fId.compareTo(sId)>0){
   String t=fId;fId=sId;sId=t;
  }
  StringBuilder seed=new StringBuilder(fId).append('|').append(sId)
    .append('|').append(operation).append('|').append(h).append('|').append(first.plane());
  for(double[] corner:boundary)
   seed.append('|').append((float)corner[0]).append(',').append((float)corner[1]);
  String bodyId=UUID.nameUUIDFromBytes(seed.toString().getBytes(StandardCharsets.UTF_8))
    .toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lineMap=new LinkedHashMap<>();
  for(int i=0;i<boundary.size();i++){
   double[] start=boundary.get(i),end=boundary.get((i+1)%boundary.size());
   String id=UUID.nameUUIDFromBytes((bodyId+"/line/"+i)
     .getBytes(StandardCharsets.UTF_8)).toString();
   ids.add(id);
   lineMap.put(id,new ClosedSketchExtrusion.Segment(first.plane(),
     (float)start[0],(float)start[1],(float)end[0],(float)end[1]));
  }
  PolygonPrismBRep result=new LoopExtrusionFeature(bodyId,ids.get(0),
     first.plane(),h,ids).asBRep(lineMap);
  if(Math.abs(result.sectionArea()-area)>1e-5*Math.max(1,area))
   throw new IllegalArgumentException("La topología B-Rep no conserva el área");
  if(Math.abs(result.volume()-area*Math.abs(h))>1e-5*Math.max(1,area*Math.abs(h)))
   throw new IllegalArgumentException("Volumen B-Rep incompatible con sección");
  return new Result(result,operation);
 }
}
