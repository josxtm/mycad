package es.josxtm.mycad.cad;
import java.util.*;

/**
 * Analytic box boundary representation: 8 vertices, 12 shared edges and
 * 6 oriented planar faces. Stable logical names use the persistent body UUID.
 * It is NOT a general B-Rep kernel: no curved faces or arbitrary Booleans.
 */
public final class BoxBRep {
 // Binary-order vertices from RigidTransform.vertices().
 private static final int[][] FACE_LOOPS={
  {0,2,6,4},{1,5,7,3},{0,4,5,1},
  {2,3,7,6},{0,1,3,2},{4,6,7,5}
 };
 // Edge ordering matches PrimitiveEdges X_0..3, Y_0..3, Z_0..3.
 private static final int[][] EDGE_POINTS={
  {0,4},{2,6},{1,5},{3,7},
  {0,2},{4,6},{1,3},{5,7},
  {0,1},{4,5},{2,3},{6,7}
 };
 public static final class Vertex {
  public final String id;
  public final double x,y,z;
  private Vertex(String id,double[] a){this.id=id;x=a[0];y=a[1];z=a[2];}
  public double[] position(){return new double[]{x,y,z};}
 }
 public static final class Edge {
  public final String id,startId,endId;
  public final double length;
  private final List<String> adjacent;
  private Edge(String id,Vertex a,Vertex b,List<String> adjacent){
   this.id=id;startId=a.id;endId=b.id;
   length=norm(diff(b.position(),a.position()));
   this.adjacent=Collections.unmodifiableList(new ArrayList<>(adjacent));
  }
  public List<String> adjacentFaceIds(){return adjacent;}
 }
 public static final class Face {
  public final String id;
  public final BoxFaceSelection.Face slot;
  public final double area;
  private final int[] loop;
  private final List<String> edgeIds;
  private final double[] normal;
  private Face(String id,BoxFaceSelection.Face slot,int[] loop,List<String> edgeIds,
               double[] normal,double area){
   this.id=id;this.slot=slot;this.loop=loop.clone();
   this.edgeIds=Collections.unmodifiableList(new ArrayList<>(edgeIds));
   this.normal=normal.clone();this.area=area;
  }
  public int[] vertexIndices(){return loop.clone();}
  public List<String> edgeIds(){return edgeIds;}
  public double[] outwardNormal(){return normal.clone();}
 }
 private final String bodyId;
 private final List<Vertex> vertices;
 private final List<Edge> edges;
 private final List<Face> faces;
 private final double volume,area;
 private BoxBRep(String id,List<Vertex> v,List<Edge> e,List<Face> f,double volume,double area){
  bodyId=id;vertices=Collections.unmodifiableList(v);
  edges=Collections.unmodifiableList(e);faces=Collections.unmodifiableList(f);
  this.volume=volume;this.area=area;
 }
 public String bodyId(){return bodyId;}
 public List<Vertex> vertices(){return vertices;}
 public List<Edge> edges(){return edges;}
 public List<Face> faces(){return faces;}
 public double volume(){return volume;}
 public double surfaceArea(){return area;}
 public Face findFace(String id){
  for(Face f:faces)if(f.id.equals(id))return f;return null;
 }
 public Edge findEdge(String id){
  for(Edge e:edges)if(e.id.equals(id))return e;return null;
 }
 private static double[] diff(double[] a,double[] b){
  return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};
 }
 private static double dot(double[] a,double[] b){
  return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
 }
 private static double norm(double[] a){return Math.sqrt(dot(a,a));}
 private static double[] cross(double[] a,double[] b){
  return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};
 }
 private static String edgeId(String body,int i){
  return body+"/edge/"+(i<4?"X_":i<8?"Y_":"Z_")+(i%4);
 }
 private static int edgeIndex(int a,int b){
  for(int i=0;i<12;i++)if((EDGE_POINTS[i][0]==a&&EDGE_POINTS[i][1]==b)||
                           (EDGE_POINTS[i][0]==b&&EDGE_POINTS[i][1]==a))return i;
  throw new IllegalArgumentException("Missing edge");
 }
 public static BoxBRep from(TransformedBody body){
  if(body==null||body.local==null||body.transform==null)
   throw new IllegalArgumentException("Missing body");
  CadKernel.Box b=body.local;
  if(b.id==null||b.id.trim().isEmpty()||
     !Double.isFinite(b.x)||!Double.isFinite(b.y)||!Double.isFinite(b.z)||
     !Double.isFinite(b.width)||!Double.isFinite(b.depth)||!Double.isFinite(b.height)||
     b.width<=0||b.depth<=0||b.height<=0)
   throw new IllegalArgumentException("Invalid body");
  double expected=b.width*b.depth*b.height;
  if(!Double.isFinite(expected))throw new IllegalArgumentException("Invalid volume");
  double[][] p=body.worldVertices();
  double[] center={0,0,0};
  List<Vertex> v=new ArrayList<>(8);
  for(int i=0;i<8;i++){
   for(int j=0;j<3;j++){
    if(!Double.isFinite(p[i][j]))throw new IllegalArgumentException("Nonfinite vertex");
    center[j]+=p[i][j]/8;
   }
   v.add(new Vertex(b.id+"/vertex/"+i,p[i]));
  }
  List<List<String>> neighbors=new ArrayList<>();
  List<List<Integer>> directions=new ArrayList<>();
  for(int i=0;i<12;i++){
   neighbors.add(new ArrayList<>());directions.add(new ArrayList<>());
  }
  List<Face> f=new ArrayList<>(6);
  double area=0,signedVolume=0;
  BoxFaceSelection.Face[] slots=BoxFaceSelection.Face.values();
  for(int i=0;i<6;i++){
   int[] loop=FACE_LOOPS[i];
   String id=b.id+"/face/"+slots[i].name();
   List<String> edgeIds=new ArrayList<>(4);
   for(int k=0;k<4;k++){
    int a=loop[k],c=loop[(k+1)%4],ei=edgeIndex(a,c);
    neighbors.get(ei).add(id);
    directions.get(ei).add(EDGE_POINTS[ei][0]==a?1:-1);
    edgeIds.add(edgeId(b.id,ei));
   }
   double[] a=p[loop[0]],c=p[loop[1]],d=p[loop[2]],e=p[loop[3]];
   double[] normal=cross(diff(c,a),diff(e,a));
   double faceArea=norm(normal);
   if(!Double.isFinite(faceArea)||faceArea<=0)
    throw new IllegalArgumentException("Degenerate face");
   double[] mid={(a[0]+c[0]+d[0]+e[0])/4,
                 (a[1]+c[1]+d[1]+e[1])/4,
                 (a[2]+c[2]+d[2]+e[2])/4};
   if(dot(normal,diff(mid,center))<=0)
    throw new IllegalArgumentException("Inward face");
   double[] p0=diff(a,center),p1=diff(c,center),
    p2=diff(d,center),p3=diff(e,center);
   signedVolume+=(dot(p0,cross(p1,p2))+dot(p0,cross(p2,p3)))/6;
   for(int j=0;j<3;j++)normal[j]/=faceArea;
   f.add(new Face(id,slots[i],loop,edgeIds,normal,faceArea));
   area+=faceArea;
  }
  List<Edge> ed=new ArrayList<>(12);
  for(int i=0;i<12;i++){
   if(neighbors.get(i).size()!=2||
      directions.get(i).get(0).equals(directions.get(i).get(1)))
    throw new IllegalArgumentException("Not a closed oriented manifold");
   ed.add(new Edge(edgeId(b.id,i),
    v.get(EDGE_POINTS[i][0]),v.get(EDGE_POINTS[i][1]),neighbors.get(i)));
  }
  if(v.size()-ed.size()+f.size()!=2||!Double.isFinite(signedVolume)||
     Math.abs(signedVolume-expected)>1e-7*Math.max(1,expected))
   throw new IllegalArgumentException("Invalid closed-shell volume");
  return new BoxBRep(b.id,v,ed,f,signedVolume,area);
 }
 /** Render mesh is generated separately from analytic topology. */
 public float[] triangles(){
  float[] out=new float[108];int offset=0;
  for(Face face:faces){
   int[] c=face.loop;
   for(int i:new int[]{c[0],c[1],c[2],c[0],c[2],c[3]}){
    Vertex p=vertices.get(i);
    for(double axis:new double[]{p.x,p.y,p.z}){
     if(!Double.isFinite(axis)||Math.abs(axis)>Float.MAX_VALUE)
      throw new IllegalArgumentException("Coordinate outside preview range");
     out[offset++]=(float)axis;
    }
   }
  }
  return out;
 }
}
