package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Analytic B-Rep UNION of two axis-aligned rectangle prism profiles.
 * The input solids must start on the same XY/XZ/YZ plane, share the same
 * signed height and overlap with positive AREA. The result is a new
 * analytic, potentially CONCAVE, polygon B-Rep, never a concatenated mesh.
 *
 * Restricting inputs to two overlapping rectangles prevents unsupported
 * holes, multiple shells, stacked elevations and general 3D booleans.
 */
public final class OrthogonalPrismBRepUnion {
 private OrthogonalPrismBRepUnion(){}
 private static final double EPS=1e-6;
 private static final class Rect {
  final double left,bottom,right,top;
  Rect(double l,double b,double r,double t){
   if(!Double.isFinite(l)||!Double.isFinite(b)||!Double.isFinite(r)||!Double.isFinite(t)||
       r-l<=.15||t-b<=.15)throw new IllegalArgumentException("Perfil rectangular degenerado");
   left=l;bottom=b;right=r;top=t;
  }
  double area(){return (right-left)*(top-bottom);}
  boolean inside(double u,double v){
   return u>=left-EPS&&u<=right+EPS&&v>=bottom-EPS&&v<=top+EPS;
  }
 }
 private static final class GridEdge {
  final int from,to;
  GridEdge(int a,int b){from=a;to=b;}
 }
 public static final class Result {
  public final PolygonPrismBRep solid;
  public final double area,volume,distance;
  public final int plane;
  private Result(PolygonPrismBRep b){
   solid=b;area=b.sectionArea();volume=b.volume();
   distance=b.distance();plane=b.plane();
  }
  public float[] triangles(){return solid.triangles();}
 }
 private static double[] uv(PolygonPrismBRep.Vertex v,int plane){
  return plane==0?new double[]{v.x,v.y}:
         plane==1?new double[]{v.x,v.z}:new double[]{v.y,v.z};
 }
 private static Rect requireRectangle(PolygonPrismBRep body){
  if(body==null||body.vertices().size()!=8||body.faces().size()!=6)
   throw new IllegalArgumentException("Booleanas de unión: dos perfiles rectangulares");
  double l=Double.POSITIVE_INFINITY,b=Double.POSITIVE_INFINITY;
  double r=Double.NEGATIVE_INFINITY,t=Double.NEGATIVE_INFINITY;
  List<double[]> p=new ArrayList<>();
  for(int i=0;i<4;i++){
   double[] pos=uv(body.vertices().get(i),body.plane());
   l=Math.min(l,pos[0]);r=Math.max(r,pos[0]);
   b=Math.min(b,pos[1]);t=Math.max(t,pos[1]);
   p.add(pos);
  }
  Rect box=new Rect(l,b,r,t);
  Set<Integer> corners=new HashSet<>();
  for(double[] v:p){
   if(Math.min(Math.abs(v[0]-l),Math.abs(v[0]-r))>EPS||
      Math.min(Math.abs(v[1]-b),Math.abs(v[1]-t))>EPS)
    throw new IllegalArgumentException("Perfil no rectangular u oblicuo");
   int key=(Math.abs(v[0]-r)<=EPS?1:0)+2*(Math.abs(v[1]-t)<=EPS?1:0);
   if(!corners.add(key))
    throw new IllegalArgumentException("Vértice rectangular duplicado");
  }
  if(corners.size()!=4||Math.abs(body.sectionArea()-box.area())>
     1e-5*Math.max(1,box.area()))
   throw new IllegalArgumentException("El perfil no es un rectángulo ortogonal");
  return box;
 }
 private static double[] cutCoordinates(double a,double b,double c,double d){
  double[] values={a,b,c,d};
  Arrays.sort(values);
  ArrayList<Double> unique=new ArrayList<>();
  for(double v:values)
   if(unique.isEmpty()||Math.abs(v-unique.get(unique.size()-1))>EPS)
    unique.add(v);
  double[] out=new double[unique.size()];
  for(int i=0;i<out.length;i++)out[i]=unique.get(i);
  return out;
 }
 private static int node(int i,int j,int width){return j*width+i;}
 private static void boundary(Map<Integer,Integer> next,
                              int a,int b){
  if(next.put(a,b)!=null)
   throw new IllegalArgumentException("La unión produce ramificaciones no válidas");
 }
 private static double area2(List<double[]> points){
  double area=0;
  for(int i=0;i<points.size();i++){
   double[] a=points.get(i),b=points.get((i+1)%points.size());
   area+=a[0]*b[1]-b[0]*a[1];
  }
  return area;
 }
 private static List<double[]> outline(Rect a,Rect b,boolean subtract){
  double[] xs=cutCoordinates(a.left,a.right,b.left,b.right);
  double[] ys=cutCoordinates(a.bottom,a.top,b.bottom,b.top);
  int nx=xs.length-1,ny=ys.length-1;
  boolean[][] filled=new boolean[nx][ny];
  int cells=0;
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++){
   double u=(xs[i]+xs[i+1])/2,v=(ys[j]+ys[j+1])/2;
   filled[i][j]=subtract?(a.inside(u,v)&&!b.inside(u,v)):
                     (a.inside(u,v)||b.inside(u,v));
   if(filled[i][j])cells++;
  }
  if(cells==0)throw new IllegalArgumentException("Unión sin superficie");
  Map<Integer,Integer> successors=new LinkedHashMap<>();
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++)if(filled[i][j]){
   int w=xs.length;
   int bl=node(i,j,w),br=node(i+1,j,w),
       tr=node(i+1,j+1,w),tl=node(i,j+1,w);
   if(j==0||!filled[i][j-1])boundary(successors,bl,br);
   if(i==nx-1||!filled[i+1][j])boundary(successors,br,tr);
   if(j==ny-1||!filled[i][j+1])boundary(successors,tr,tl);
   if(i==0||!filled[i-1][j])boundary(successors,tl,bl);
  }
  if(successors.size()<4||successors.size()>32)
   throw new IllegalArgumentException("Contorno de unión inválido");
  int initial=Collections.min(successors.keySet());
  ArrayList<double[]> contour=new ArrayList<>();
  Set<Integer> visited=new HashSet<>();
  int current=initial;
  do{
   if(!visited.add(current))
    throw new IllegalArgumentException("Contorno de unión se autointersecta");
   int ix=current%xs.length,iy=current/xs.length;
   contour.add(new double[]{xs[ix],ys[iy]});
   Integer next=successors.get(current);
   if(next==null)throw new IllegalArgumentException("Contorno de unión abierto");
   current=next;
  }while(current!=initial);
  if(visited.size()!=successors.size())
   throw new IllegalArgumentException("La unión tiene varios contornos");
  // Remove intermediate collinear grid corners; existing B-Rep requires a
  // simple polygon and stable one-segment-per-edge source identities.
  for(int i=0;contour.size()>3&&i<contour.size();){
   int n=contour.size();
   double[] p=contour.get((i+n-1)%n),q=contour.get(i),
            r=contour.get((i+1)%n);
   double cross=(q[0]-p[0])*(r[1]-q[1])-(q[1]-p[1])*(r[0]-q[0]);
   if(Math.abs(cross)<=EPS){
    contour.remove(i);
    if(i>0)i--;
   }else i++;
  }
  if(contour.size()<4||contour.size()>32||area2(contour)<=EPS)
   throw new IllegalArgumentException("Unión B-Rep degenerada");
  return contour;
 }

 public static Result union(PolygonPrismBRep first,PolygonPrismBRep second){
  if(first==null||second==null||first.plane()!=second.plane())
   throw new IllegalArgumentException("La unión requiere sólidos del mismo plano");
  double h=first.distance(),other=second.distance();
  if(!Double.isFinite(h)||!Double.isFinite(other)||
      Math.abs(h-other)>EPS*Math.max(1,Math.abs(h)))
   throw new IllegalArgumentException("La unión requiere alturas firmadas iguales");
  Rect a=requireRectangle(first),b=requireRectangle(second);
  double ix=Math.min(a.right,b.right)-Math.max(a.left,b.left);
  double iy=Math.min(a.top,b.top)-Math.max(a.bottom,b.bottom);
  if(ix<=EPS||iy<=EPS)
   throw new IllegalArgumentException("La unión necesita solape con volumen");
  List<double[]> corners=outline(a,b,false);
  // Stable operand order: the same union produces equal B-Rep and preview
  // coordinates even if the user chooses the other rectangle first.
  String aId=first.id(),bId=second.id();
  if(aId.compareTo(bId)>0){String temp=aId;aId=bId;bId=temp;}
  StringBuilder seed=new StringBuilder(aId).append('|').append(bId)
   .append("|UNION|").append(first.plane()).append('|').append(h);
  for(double[] p:corners)seed.append('|').append((float)p[0]).append(',')
                              .append((float)p[1]);
  String resultId=UUID.nameUUIDFromBytes(seed.toString()
      .getBytes(StandardCharsets.UTF_8)).toString();
  ArrayList<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<corners.size();i++){
   String name=UUID.nameUUIDFromBytes((resultId+"/edge/"+i)
     .getBytes(StandardCharsets.UTF_8)).toString();
   double[] p=corners.get(i),q=corners.get((i+1)%corners.size());
   ids.add(name);
   lines.put(name,new ClosedSketchExtrusion.Segment(first.plane(),
     (float)p[0],(float)p[1],(float)q[0],(float)q[1]));
  }
  PolygonPrismBRep solid=new LoopExtrusionFeature(resultId,ids.get(0),
      first.plane(),h,ids).asBRep(lines);
  double expected=(a.area()+b.area()-ix*iy)*Math.abs(h);
  if(Math.abs(solid.volume()-expected)>1e-5*Math.max(1,expected))
   throw new IllegalArgumentException("Volumen de unión distinto del previsto");
  return new Result(solid);
 }

 /**
  * Subtract a full-height rectangular cutter from a rectangular prism.
  * If it yields a hole, more than one connected solid, or zero volume,
  * the operation is rejected: PolygonPrismBRep currently supports exactly
  * one simple outer loop with no interior rings.
  */
 public static Result difference(PolygonPrismBRep target,PolygonPrismBRep cutter){
  if(target==null||cutter==null||target.plane()!=cutter.plane())
   throw new IllegalArgumentException("Resta B-Rep: los prismas deben compartir plano");
  double height=target.distance(),other=cutter.distance();
  if(!Double.isFinite(height)||!Double.isFinite(other)||
     Math.abs(height-other)>EPS*Math.max(1,Math.abs(height)))
   throw new IllegalArgumentException("Resta B-Rep: alturas firmadas distintas");
  Rect a=requireRectangle(target),b=requireRectangle(cutter);
  double ix=Math.min(a.right,b.right)-Math.max(a.left,b.left);
  double iy=Math.min(a.top,b.top)-Math.max(a.bottom,b.bottom);
  if(ix<=EPS||iy<=EPS)
   throw new IllegalArgumentException("Resta B-Rep: el cortador no penetra el prisma");
  List<double[]> boundary=outline(a,b,true);
  StringBuilder seed=new StringBuilder(target.id()).append("|DIFFERENCE|")
   .append(cutter.id()).append('|').append(target.plane()).append('|').append(height);
  for(double[] pt:boundary)seed.append('|').append((float)pt[0])
                                .append(',').append((float)pt[1]);
  String id=UUID.nameUUIDFromBytes(seed.toString().getBytes(StandardCharsets.UTF_8))
   .toString();
  ArrayList<String> edgeIds=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> segments=new LinkedHashMap<>();
  for(int i=0;i<boundary.size();i++){
   String edgeId=UUID.nameUUIDFromBytes((id+"/cut-edge/"+i)
     .getBytes(StandardCharsets.UTF_8)).toString();
   double[] start=boundary.get(i),end=boundary.get((i+1)%boundary.size());
   edgeIds.add(edgeId);
   segments.put(edgeId,new ClosedSketchExtrusion.Segment(target.plane(),
    (float)start[0],(float)start[1],(float)end[0],(float)end[1]));
  }
  PolygonPrismBRep solid=new LoopExtrusionFeature(id,edgeIds.get(0),
    target.plane(),height,edgeIds).asBRep(segments);
  double expected=(a.area()-ix*iy)*Math.abs(height);
  if(Math.abs(solid.volume()-expected)>1e-5*Math.max(1,expected))
   throw new IllegalArgumentException("Volumen de resta B-Rep incorrecto");
  return new Result(solid);
 }
}
