package es.josxtm.mycad.cad;

import java.util.*;

/**
 * First non-primitive analytic B-Rep shell: a simple closed straight-line
 * sketch extruded in XY/XZ/YZ. Includes topological vertices, two oriented
 * half-edges per shared edge, and planar faces with stable source UUID slots.
 *
 * Faces are true analytic polygon loops; triangles are a separate preview.
 * Does not implement general Boolean operations, holes, splines or fillets.
 */
public final class PolygonPrismBRep {
 public static final class Vertex {
  public final String id;
  public final double x,y,z;
  private Vertex(String id,double[] p){this.id=id;x=p[0];y=p[1];z=p[2];}
  public double[] position(){return new double[]{x,y,z};}
 }
 public static final class HalfEdge {
  public final String id,edgeId,faceId,fromId,toId,nextId,previousId,twinId;
  private HalfEdge(Use u){
   id=u.id;edgeId=u.edgeId;faceId=u.faceId;
   fromId=u.from;toId=u.to;
   nextId=u.next;previousId=u.previous;twinId=u.twin;
  }
 }
 public static final class Edge {
  public final String id,kind,firstVertexId,secondVertexId;
  public final double length;
  private final List<String> faceIds,halfEdgeIds;
  private Edge(String id,String from,String to,double length,
               List<String> faces,List<String> halfEdges){
   this.id=id;kind="LINE";firstVertexId=from;secondVertexId=to;
   this.length=length;
   faceIds=Collections.unmodifiableList(new ArrayList<>(faces));
   halfEdgeIds=Collections.unmodifiableList(new ArrayList<>(halfEdges));
  }
  public List<String> adjacentFaceIds(){return faceIds;}
  public List<String> halfEdgeIds(){return halfEdgeIds;}
 }
 public static final class Face {
  public final String id,kind,sourceLineId;
  public final double area;
  private final List<String> vertexIds,halfEdgeIds;
  private final double[] normal;
  private Face(String id,String source,double area,double[] normal,
               List<String> vertices,List<String> halfEdges){
   this.id=id;kind="PLANE";sourceLineId=source;this.area=area;
   this.normal=normal.clone();
   vertexIds=Collections.unmodifiableList(new ArrayList<>(vertices));
   halfEdgeIds=Collections.unmodifiableList(new ArrayList<>(halfEdges));
  }
  public List<String> vertexIds(){return vertexIds;}
  public List<String> halfEdgeIds(){return halfEdgeIds;}
  public double[] outwardNormal(){return normal.clone();}
 }
 private static final class Corner {
  final double u,v;
  final List<Integer> edges=new ArrayList<>();
  String id;
  Corner(double u,double v){this.u=u;this.v=v;}
 }
 private static final class Line {
  final int a,b;
  final String id;
  Line(int a,int b,String id){this.a=a;this.b=b;this.id=id;}
 }
 private static final class Use {
  String id,edgeId,faceId,from,to,next,previous,twin;
  Use(String id,String edge,String face,String from,String to){
   this.id=id;edgeId=edge;faceId=face;this.from=from;this.to=to;
  }
 }
 private final String id;
 private final int plane;
 private final double distance,area,volume,surfaceArea;
 private final List<Vertex> vertices;
 private final List<Edge> edges;
 private final List<Face> faces;
 private final List<HalfEdge> halfEdges;
 private final ClosedSketchExtrusion.Result preview;

 private PolygonPrismBRep(String id,int plane,double height,double area,
                         List<Vertex> vertices,List<Edge> edges,
                         List<Face> faces,List<HalfEdge> halfEdges,
                         ClosedSketchExtrusion.Result preview){
  this.id=id;this.plane=plane;this.distance=height;this.area=area;
  this.volume=preview.volume;
  double surfaces=0;
  for(Face face:faces)surfaces+=face.area;
  this.surfaceArea=surfaces;
  this.vertices=Collections.unmodifiableList(new ArrayList<>(vertices));
  this.edges=Collections.unmodifiableList(new ArrayList<>(edges));
  this.faces=Collections.unmodifiableList(new ArrayList<>(faces));
  this.halfEdges=Collections.unmodifiableList(new ArrayList<>(halfEdges));
  this.preview=preview;
 }
 public String id(){return id;}
 public int plane(){return plane;}
 public double distance(){return distance;}
 public double sectionArea(){return area;}
 public double volume(){return volume;}
 public double surfaceArea(){return surfaceArea;}
 public List<Vertex> vertices(){return vertices;}
 public List<Edge> edges(){return edges;}
 public List<Face> faces(){return faces;}
 public List<HalfEdge> halfEdges(){return halfEdges;}
 public float[] triangles(){return preview.triangles.clone();}
 public Face findFace(String key){for(Face f:faces)if(f.id.equals(key))return f;return null;}
 public Edge findEdge(String key){for(Edge e:edges)if(e.id.equals(key))return e;return null;}
 public Vertex findVertex(String key){for(Vertex v:vertices)if(v.id.equals(key))return v;return null;}
 private static int attach(List<Corner> corners,double x,double y){
  int found=-1;
  for(int i=0;i<corners.size();i++)if(Math.hypot(corners.get(i).u-x,corners.get(i).v-y)<=.15){
   if(found>=0)throw new IllegalArgumentException("Ambiguous sketch endpoints");
   found=i;
  }
  if(found>=0)return found;
  corners.add(new Corner(x,y));
  return corners.size()-1;
 }
 private static double[] xyz(int plane,Corner c,double h){
  if(plane==0)return new double[]{c.u,c.v,h};
  if(plane==1)return new double[]{c.u,h,c.v};
  return new double[]{h,c.u,c.v};
 }
 private static double[] sub(double[] a,double[] b){
  return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};
 }
 private static double[] cross(double[] a,double[] b){
  return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};
 }
 private static double norm(double[] a){return Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);}
 private static double[] unit(double[] a){
  double n=norm(a);
  if(!Double.isFinite(n)||n<=0)throw new IllegalArgumentException("Degenerate face normal");
  return new double[]{a[0]/n,a[1]/n,a[2]/n};
 }
 private static double[] faceNormal(List<Vertex> vertices,int[] loop){
  // Newell's area-weighted polygon normal works for concave and collinear
  // corner sequences. A single local cross could flip on a reflex vertex.
  double[] origin=vertices.get(loop[0]).position();
  double[] normal={0,0,0};
  for(int i=0;i<loop.length;i++){
   double[] a=sub(vertices.get(loop[i]).position(),origin);
   double[] b=sub(vertices.get(loop[(i+1)%loop.length]).position(),origin);
   double[] c=cross(a,b);
   for(int k=0;k<3;k++)normal[k]+=c[k];
  }
  return unit(normal);
 }
 private static String stableCornerId(Corner c,List<Line> lines){
  if(c.edges.size()!=2)throw new IllegalArgumentException("Open or branching polygon sketch");
  String a=lines.get(c.edges.get(0)).id,b=lines.get(c.edges.get(1)).id;
  return a.compareTo(b)<0?a+"-"+b:b+"-"+a;
 }
 private static int opposite(Line e,int p){
  if(e.a==p)return e.b;
  if(e.b==p)return e.a;
  throw new IllegalArgumentException("Broken topology");
 }
 private static String sideEdgeId(String id,String line,boolean upper){
  return id+"/edge/"+(upper?"top/":"base/")+line;
 }
 private static String edgeId(String id,int a,int b,int n,
                              List<Corner> ring,List<String> segments){
  int ia=a%n,ib=b%n;
  if(ia==ib&&a/n!=b/n)
   return id+"/edge/riser/"+ring.get(ia).id;
  if(a/n==b/n){
   int at;
   if((ia+1)%n==ib)at=ia;
   else if((ib+1)%n==ia)at=ib;
   else throw new IllegalArgumentException("Nonadjacent face vertices");
   return sideEdgeId(id,segments.get(at),a>=n);
  }
  throw new IllegalArgumentException("Invalid boundary edge");
 }
 private static void createFace(String faceId,String sourceId,int[] loop,double area,
                                List<Vertex> vertices,List<Corner> ring,
                                List<String> segmentIds,
                                List<Face> faces,List<Use> uses,
                                Map<String,List<Use>> owners,String id,int n){
  ArrayList<String> vIds=new ArrayList<>(),heIds=new ArrayList<>();
  for(int i=0;i<loop.length;i++){
   int a=loop[i],b=loop[(i+1)%loop.length];
   String edgeId=edgeId(id,a,b,n,ring,segmentIds);
   String useId=faceId+"/halfedge/"+edgeId.substring((id+"/edge/").length());
   Use u=new Use(useId,edgeId,faceId,vertices.get(a).id,vertices.get(b).id);
   uses.add(u);
   owners.computeIfAbsent(edgeId,k->new ArrayList<>()).add(u);
   vIds.add(u.from);heIds.add(useId);
  }
  for(int i=0;i<loop.length;i++){
   Use u=uses.get(uses.size()-loop.length+i);
   u.next=heIds.get((i+1)%heIds.size());
   u.previous=heIds.get((i+heIds.size()-1)%heIds.size());
  }
  faces.add(new Face(faceId,sourceId,area,faceNormal(vertices,loop),vIds,heIds));
 }
 /**
  * Build a closed half-edge shell from linked sketch edges.
  * Geometry is prevalidated with the existing robust watertight tessellator.
  * The topology exposes analytic polygon faces separately from triangles.
  */
 public static PolygonPrismBRep from(LoopExtrusionFeature feature,
      Map<String,ClosedSketchExtrusion.Segment> current){
  if(feature==null||current==null)throw new IllegalArgumentException("Missing feature or sketch");
  List<String> ids=feature.lineIds();
  int n=ids.size();
  ArrayList<ClosedSketchExtrusion.Segment> input=new ArrayList<>();
  ArrayList<Corner> corners=new ArrayList<>();
  ArrayList<Line> segments=new ArrayList<>();
  for(String lineId:ids){
   ClosedSketchExtrusion.Segment s=current.get(lineId);
   if(s==null||s.plane!=feature.plane)
    throw new IllegalArgumentException("Missing or moved sketch source");
   input.add(s);
   int a=attach(corners,s.ax,s.ay),b=attach(corners,s.bx,s.by);
   if(a==b)throw new IllegalArgumentException("Collapsed sketch edge");
   Line edge=new Line(a,b,lineId);
   segments.add(edge);
   corners.get(a).edges.add(segments.size()-1);
   corners.get(b).edges.add(segments.size()-1);
  }
  if(corners.size()!=n)throw new IllegalArgumentException("Sketch is not one simple circuit");
  for(Corner c:corners)c.id=stableCornerId(c,segments);
  // Fail before building topology if surface cannot be triangulated.
  int selected=ids.indexOf(feature.selectedLineId);
  ClosedSketchExtrusion.Result mesh=ClosedSketchExtrusion.extrude(input,selected,feature.distance);
  if(mesh.usedLines!=n||mesh.triangles.length>18000)
   throw new IllegalArgumentException("Invalid B-Rep source contour");
  int origin=0;
  for(int i=1;i<n;i++)if(corners.get(i).id.compareTo(corners.get(origin).id)<0)
   origin=i;
  ArrayList<Integer> loop=new ArrayList<>();
  Set<Integer> seen=new HashSet<>();
  int vertex=origin,line=corners.get(origin).edges.get(0);
  while(true){
   if(!seen.add(line))throw new IllegalArgumentException("Loop repeats an edge");
   loop.add(vertex);
   int next=opposite(segments.get(line),vertex);
   if(next==origin)break;
   int nextLine=corners.get(next).edges.get(0)==line?
      corners.get(next).edges.get(1):corners.get(next).edges.get(0);
   vertex=next;line=nextLine;
   if(loop.size()>n)throw new IllegalArgumentException("Broken sketch circuit");
  }
  if(loop.size()!=n||seen.size()!=n)throw new IllegalArgumentException("Disconnected sketch circuit");
  double area2=0;
  for(int i=0;i<n;i++){
   Corner a=corners.get(loop.get(i)),b=corners.get(loop.get((i+1)%n));
   area2+=a.u*b.v-b.u*a.v;
  }
  if(area2<0){
   Collections.reverse(loop);
   // Canonical corner begins every B-Rep face topology revision.
   Collections.rotate(loop,1);
  }
  if(Math.abs(Math.abs(area2)/2-mesh.area)>1e-7*Math.max(1,mesh.area))
   throw new IllegalArgumentException("Topology and preview disagree");
  ArrayList<Corner> ring=new ArrayList<>();
  ArrayList<String> ringLineIds=new ArrayList<>();
  for(int index:loop)ring.add(corners.get(index));
  for(int i=0;i<n;i++){
   int a=loop.get(i),b=loop.get((i+1)%n);
   String id=null;
   for(int candidate:corners.get(a).edges){
    Line e=segments.get(candidate);
    if((e.a==a&&e.b==b)||(e.a==b&&e.b==a)){id=e.id;break;}
   }
   if(id==null)throw new IllegalArgumentException("Face loop edge missing");
   ringLineIds.add(id);
  }
  ArrayList<Vertex> v=new ArrayList<>(2*n);
  for(int layer=0;layer<2;layer++)for(Corner c:ring){
   double[] xyz=xyz(feature.plane,c,layer==0?0:feature.distance);
   for(double value:xyz)
    if(!Double.isFinite(value)||Math.abs(value)>10000)
     throw new IllegalArgumentException("B-Rep outside world bounds");
   v.add(new Vertex(feature.id+"/vertex/"+(layer==0?"base/":"top/")+c.id,xyz));
  }
  // Positive h on XY or YZ preserves UV orientation; XZ reverses it.
  int direction=(feature.plane==1?-1:1)*(feature.distance<0?-1:1);
  int[] base=new int[n],top=new int[n];
  for(int i=0;i<n;i++){
   base[i]=direction>0?(n-1-i):i;
   top[i]=direction>0?(n+i):(2*n-1-i);
  }
  List<Face> faces=new ArrayList<>();
  List<Use> uses=new ArrayList<>();
  Map<String,List<Use>> owners=new LinkedHashMap<>();
  createFace(feature.id+"/face/base",null,base,mesh.area,v,ring,ringLineIds,
             faces,uses,owners,feature.id,n);
  createFace(feature.id+"/face/top",null,top,mesh.area,v,ring,ringLineIds,
             faces,uses,owners,feature.id,n);
  for(int i=0;i<n;i++){
   int j=(i+1)%n;
   int[] side=direction>0?
      new int[]{i,j,n+j,n+i}:new int[]{n+i,n+j,j,i};
   double sideArea=Math.hypot(ring.get(j).u-ring.get(i).u,
                              ring.get(j).v-ring.get(i).v)*Math.abs(feature.distance);
   createFace(feature.id+"/face/side/"+ringLineIds.get(i),
              ringLineIds.get(i),side,sideArea,v,ring,ringLineIds,
              faces,uses,owners,feature.id,n);
  }
  List<Edge> edges=new ArrayList<>(3*n);
  for(Map.Entry<String,List<Use>> entry:owners.entrySet()){
   List<Use> pair=entry.getValue();
   if(pair.size()!=2||!pair.get(0).from.equals(pair.get(1).to)||
      !pair.get(0).to.equals(pair.get(1).from))
    throw new IllegalArgumentException("Nonmanifold or inverted B-Rep edge");
   Use a=pair.get(0),b=pair.get(1);
   a.twin=b.id;b.twin=a.id;
   Vertex va=null,vb=null;
   for(Vertex item:v){
    if(item.id.equals(a.from))va=item;
    if(item.id.equals(a.to))vb=item;
   }
   if(va==null||vb==null)throw new IllegalArgumentException("B-Rep vertices lost");
   double length=norm(sub(va.position(),vb.position()));
   if(!Double.isFinite(length)||length<=0)
    throw new IllegalArgumentException("Zero-length topological edge");
   edges.add(new Edge(entry.getKey(),a.from,a.to,length,
     Arrays.asList(a.faceId,b.faceId),Arrays.asList(a.id,b.id)));
  }
  if(v.size()!=2*n||edges.size()!=3*n||faces.size()!=n+2||
     v.size()-edges.size()+faces.size()!=2||uses.size()!=6*n)
   throw new IllegalArgumentException("Closed-shell Euler characteristic failed");
  List<HalfEdge> halfEdges=new ArrayList<>(6*n);
  for(Use use:uses)halfEdges.add(new HalfEdge(use));
  return new PolygonPrismBRep(feature.id,feature.plane,feature.distance,
                              mesh.area,v,edges,faces,halfEdges,mesh);
 }
}
