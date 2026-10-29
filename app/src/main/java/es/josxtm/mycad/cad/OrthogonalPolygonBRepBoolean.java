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
 private static final double HEIGHT_EPS=1e-9;
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
 static List<double[]> points(PolygonPrismBRep body){
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
 static double[] coordinates(List<double[]> a,List<double[]> b,int dimension){
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
 static boolean inside(List<double[]> poly,double x,double y){
  boolean result=false;
  for(int i=0,j=poly.size()-1;i<poly.size();j=i++){
   double[] a=poly.get(i),b=poly.get(j);
   if((a[1]>y)!=(b[1]>y)&&
       x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])
    result=!result;
  }
  return result;
 }
 static boolean retain(boolean a,boolean b,Operation operation){
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
  if(polygon.isEmpty())return 0;
  // Anchor the shoelace terms locally: world-coordinate products near
  // 10,000 mm lose precision for sub-millimetre Boolean boundaries.
  double ox=polygon.get(0)[0],oy=polygon.get(0)[1];
  double sum=0,correction=0;
  for(int i=0;i<polygon.size();i++){
   double[] a=polygon.get(i),b=polygon.get((i+1)%polygon.size());
   double ax=a[0]-ox,ay=a[1]-oy,bx=b[0]-ox,by=b[1]-oy;
   double term=ax*by-bx*ay;
   // Neumaier compensation also preserves small contributions in concave rings.
   double next=sum+term;
   if(Math.abs(sum)>=Math.abs(term))correction+=(sum-next)+term;
   else correction+=(term-next)+sum;
   sum=next;
  }
  return sum+correction;
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
     Math.abs(h-other)>HEIGHT_EPS*Math.max(1,Math.max(Math.abs(h),Math.abs(other))))
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


 /**
  * Multiple analytic disconnected solids in ONE saved GPU result mesh.
  * Every component is an independent closed B-Rep shell, so Euler = 2
  * separately; no holes / inner loops / vertex-only contacts are accepted.
  */
 public static final class MultiResult {
  private final List<PolygonPrismBRep> shells;
  private final float[] tessellation;
  public final Operation operation;
  public final int plane,shellCount;
  public final double area,volume,distance;
  private MultiResult(List<PolygonPrismBRep> bodies,Operation operation,
                      double distance,int plane){
   if(bodies.isEmpty()||bodies.size()>16)
    throw new IllegalArgumentException("Número de cuerpos B-Rep no admitido");
   this.shells=Collections.unmodifiableList(new ArrayList<>(bodies));
   this.operation=operation;this.distance=distance;this.plane=plane;
   shellCount=bodies.size();
   double a=0,v=0;
   int len=0;
   for(PolygonPrismBRep body:bodies){
    if(body.plane()!=plane||body.distance()!=distance)
     throw new IllegalArgumentException("Cascarones B-Rep incompatibles");
    a+=body.sectionArea();v+=body.volume();
    len+=body.triangles().length;
   }
   if(len>18000)throw new IllegalArgumentException("Malla multisólido demasiado grande");
   area=a;volume=v;
   tessellation=new float[len];
   int at=0;
   for(PolygonPrismBRep body:bodies){
    float[] triangle=body.triangles();
    System.arraycopy(triangle,0,tessellation,at,triangle.length);
    at+=triangle.length;
   }
  }
  public List<PolygonPrismBRep> solids(){return shells;}
  public float[] triangles(){return tessellation.clone();}
 }

 /** Enumerate every directed boundary contour, deterministically sorted
  * by minimal grid vertex index; counterclockwise loops are solids,
  * clockwise loops are HOLES and not yet representable. */
 private static List<List<double[]>> extractMultiple(double[] xs,double[] ys,
                                                      boolean[][] cells){
  int nx=xs.length-1,ny=ys.length-1,width=xs.length;
  Map<Integer,Integer> edges=new TreeMap<>();
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++)if(cells[i][j]){
   int bl=node(i,j,width),br=node(i+1,j,width),
       tr=node(i+1,j+1,width),tl=node(i,j+1,width);
   if(j==0||!cells[i][j-1])add(edges,bl,br);
   if(i==nx-1||!cells[i+1][j])add(edges,br,tr);
   if(j==ny-1||!cells[i][j+1])add(edges,tr,tl);
   if(i==0||!cells[i-1][j])add(edges,tl,bl);
  }
  if(edges.size()<4||edges.size()>2500)
   throw new IllegalArgumentException("El resultado B-Rep no tiene contornos válidos");
  Set<Integer> visited=new HashSet<>();
  ArrayList<List<double[]>> contours=new ArrayList<>();
  for(Integer begin:edges.keySet()){
   if(visited.contains(begin))continue;
   ArrayList<double[]> ring=new ArrayList<>();
   int current=begin;
   do{
    if(!visited.add(current))
     throw new IllegalArgumentException("Ciclo B-Rep ambiguo");
    ring.add(new double[]{xs[current%width],ys[current/width]});
    Integer next=edges.get(current);
    if(next==null)throw new IllegalArgumentException("Contorno B-Rep abierto");
    current=next;
   }while(current!=begin);
   // Internal cavity rims are clockwise, distinct from outer shells.
   for(int i=0;ring.size()>3&&i<ring.size();){
    int n=ring.size();
    double[] a=ring.get((i+n-1)%n),b=ring.get(i),
             c=ring.get((i+1)%n);
    double u=b[0]-a[0],v=b[1]-a[1],w=c[0]-b[0],z=c[1]-b[1];
    if(Math.abs(u*z-v*w)<=EPS&&u*w+v*z>=0){
     ring.remove(i);if(i>0)i--;
    }else i++;
   }
   if(ring.size()<4||ring.size()>100)
    throw new IllegalArgumentException("Cascarón B-Rep degenerado");
   if(twiceArea(ring)<=EPS)
    throw new IllegalArgumentException("Agujeros interiores B-Rep aún no admitidos");
   contours.add(ring);
   if(contours.size()>16)
    throw new IllegalArgumentException("Máximo 16 componentes B-Rep");
  }
  if(visited.size()!=edges.size())
   throw new IllegalArgumentException("Bordes sin componente B-Rep");
  return contours;
 }

 private static PolygonPrismBRep makeShell(
     PolygonPrismBRep first,PolygonPrismBRep second,
     Operation operation,double height,List<double[]> contour){
  String firstId=first.id(),secondId=second.id();
  if(operation!=Operation.DIFFERENCE&&firstId.compareTo(secondId)>0){
   String t=firstId;firstId=secondId;secondId=t;
  }
  StringBuilder seed=new StringBuilder(firstId).append('|').append(secondId)
    .append('|').append(operation).append('|').append(height)
    .append('|').append(first.plane());
  for(double[] corner:contour)seed.append('|')
     .append((float)corner[0]).append(',').append((float)corner[1]);
  String id=UUID.nameUUIDFromBytes(seed.toString()
    .getBytes(StandardCharsets.UTF_8)).toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<contour.size();i++){
   double[] a=contour.get(i),b=contour.get((i+1)%contour.size());
   String lineId=UUID.nameUUIDFromBytes((id+"/line/"+i)
     .getBytes(StandardCharsets.UTF_8)).toString();
   ids.add(lineId);
   lines.put(lineId,new ClosedSketchExtrusion.Segment(first.plane(),
       (float)a[0],(float)a[1],(float)b[0],(float)b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),
          first.plane(),height,ids).asBRep(lines);
 }

 /**
  * Extended operation supporting multiple DISJOINT solid shells. It does
  * not change the original single-shell apply() result or its persisted
  * coordinates, preserving existing JSON history compatibility.
  */
 public static MultiResult applyMulti(PolygonPrismBRep first,PolygonPrismBRep second,
                                     Operation operation){
  if(first==null||second==null||operation==null||
      first.plane()!=second.plane())
   throw new IllegalArgumentException("Booleanas múltiples: plano u operación incompatibles");
  double height=first.distance(),other=second.distance();
  if(!Double.isFinite(height)||!Double.isFinite(other)||
      Math.abs(height-other)>HEIGHT_EPS*Math.max(1,Math.max(Math.abs(height),Math.abs(other))))
   throw new IllegalArgumentException("Booleanas múltiples: alturas distintas");
  List<double[]> a=points(first),b=points(second);
  double[] xs=coordinates(a,b,0),ys=coordinates(a,b,1);
  boolean[][] cells=new boolean[xs.length-1][ys.length-1];
  double area=0;
  for(int i=0;i<xs.length-1;i++)for(int j=0;j<ys.length-1;j++){
   double x=(xs[i]+xs[i+1])/2,y=(ys[j]+ys[j+1])/2;
   cells[i][j]=retain(inside(a,x,y),inside(b,x,y),operation);
   if(cells[i][j])area+=(xs[i+1]-xs[i])*(ys[j+1]-ys[j]);
  }
  if(area<=EPS)throw new IllegalArgumentException("Sin volumen de booleana multisólido");
  List<List<double[]>> contour=extractMultiple(xs,ys,cells);
  if(contour.size()==1){
   Result old=apply(first,second,operation);
   return new MultiResult(Collections.singletonList(old.solid),
     operation,height,first.plane());
  }
  List<PolygonPrismBRep> shells=new ArrayList<>();
  for(List<double[]> loop:contour)
   shells.add(makeShell(first,second,operation,height,loop));
  MultiResult result=new MultiResult(shells,operation,height,first.plane());
  if(Math.abs(result.area-area)>1e-5*Math.max(1,area)||
      Math.abs(result.volume-area*Math.abs(height))>
        1e-5*Math.max(1,area*Math.abs(height)))
   throw new IllegalArgumentException("Conservación de área/volumen multisólido fallida");
  return result;
 }
}
