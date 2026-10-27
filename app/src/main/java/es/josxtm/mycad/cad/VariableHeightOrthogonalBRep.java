package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Analytic planar quad-face / half-edge topology for a validated stepped
 * orthogonal Boolean surface. Unlike PolygonPrismBRep this shell can have
 * different top heights and a recessed pocket. Each quad is a planar face,
 * not a triangle; coplanar quad merging is deliberately deferred.
 *
 * The source prisms and their persisted feature recipes are never changed.
 * This kernel object is not yet wired into saved Android Boolean features.
 */
public final class VariableHeightOrthogonalBRep {
 public static final class Vertex {
  public final String id;
  public final double x,y,z;
  private Vertex(String id,float[] xyz){
   this.id=id;x=xyz[0];y=xyz[1];z=xyz[2];
  }
 }
 public static final class Edge {
  public final String id,firstVertexId,secondVertexId;
  public final double length;
  private final List<String> faceIds,halfEdgeIds;
  private Edge(String id,String a,String b,double length,List<String> faces,List<String> uses){
   this.id=id;firstVertexId=a;secondVertexId=b;this.length=length;
   faceIds=Collections.unmodifiableList(new ArrayList<>(faces));
   halfEdgeIds=Collections.unmodifiableList(new ArrayList<>(uses));
  }
  public List<String> adjacentFaceIds(){return faceIds;}
  public List<String> halfEdgeIds(){return halfEdgeIds;}
 }
 public static final class Face {
  public final String id,kind;
  public final double area;
  private final double[] normal;
  private final List<String> vertexIds,halfEdgeIds;
  private Face(String id,double area,double[] normal,List<String> vertices,List<String> uses){
   this.id=id;kind="PLANE";this.area=area;this.normal=normal.clone();
   vertexIds=Collections.unmodifiableList(new ArrayList<>(vertices));
   halfEdgeIds=Collections.unmodifiableList(new ArrayList<>(uses));
  }
  public double[] outwardNormal(){return normal.clone();}
  public List<String> vertexIds(){return vertexIds;}
  public List<String> halfEdgeIds(){return halfEdgeIds;}
 }
 public static final class HalfEdge {
  public final String id,edgeId,faceId,fromId,toId,nextId,previousId,twinId;
  private HalfEdge(Use use){
   id=use.id;edgeId=use.edgeId;faceId=use.faceId;
   fromId=use.from;toId=use.to;
   nextId=use.next;previousId=use.previous;twinId=use.twin;
  }
 }
 private static final class Use {
  String id,edgeId,faceId,from,to,next,previous,twin;
  Use(String id,String edge,String face,String from,String to){
   this.id=id;edgeId=edge;faceId=face;this.from=from;this.to=to;
  }
 }
 private final String id;
 private final int plane;
 private final VariableHeightOrthogonalMesh.Operation operation;
 private final double volume,surfaceArea;
 private final List<Vertex> vertices;
 private final List<Edge> edges;
 private final List<Face> faces;
 private final List<HalfEdge> halfEdges;
 private final float[] preview;
 private VariableHeightOrthogonalBRep(String id,VariableHeightOrthogonalMesh.Result mesh,
     List<Vertex> vertices,List<Edge> edges,List<Face> faces,List<HalfEdge> halfEdges,
     double surfaceArea){
  this.id=id;plane=mesh.plane;operation=mesh.operation;volume=mesh.volume;
  this.vertices=Collections.unmodifiableList(new ArrayList<>(vertices));
  this.edges=Collections.unmodifiableList(new ArrayList<>(edges));
  this.faces=Collections.unmodifiableList(new ArrayList<>(faces));
  this.halfEdges=Collections.unmodifiableList(new ArrayList<>(halfEdges));
  this.surfaceArea=surfaceArea;preview=mesh.triangles();
 }
 public String id(){return id;}
 public int plane(){return plane;}
 public VariableHeightOrthogonalMesh.Operation operation(){return operation;}
 public double volume(){return volume;}
 public double surfaceArea(){return surfaceArea;}
 public List<Vertex> vertices(){return vertices;}
 public List<Edge> edges(){return edges;}
 public List<Face> faces(){return faces;}
 public List<HalfEdge> halfEdges(){return halfEdges;}
 public float[] triangles(){return preview.clone();}
 private static String uuid(String seed){
  return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
 }
 private static String key(float[] p){
  for(float value:p)if(!Float.isFinite(value))
   throw new IllegalArgumentException("Nonfinite B-Rep vertex");
  return Integer.toHexString(Float.floatToIntBits(p[0]==0?0:p[0]))+"/"+
         Integer.toHexString(Float.floatToIntBits(p[1]==0?0:p[1]))+"/"+
         Integer.toHexString(Float.floatToIntBits(p[2]==0?0:p[2]));
 }
 private static float[] point(float[] mesh,int offset){
  return new float[]{mesh[offset],mesh[offset+1],mesh[offset+2]};
 }
 private static double[] subtract(float[] a,float[] b){
  return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};
 }
 private static double[] cross(double[] a,double[] b){
  return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],
      a[0]*b[1]-a[1]*b[0]};
 }
 private static double dot(double[] a,double[] b){
  return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
 }
 private static double length(double[] a){return Math.sqrt(dot(a,a));}
 private static Vertex addVertex(Map<String,Vertex> byKey,Map<String,Vertex> byId,
                                  float[] point,String shellId){
  String key=key(point);
  Vertex v=byKey.get(key);
  if(v==null){
   v=new Vertex(shellId+"/vertex/"+key,point);
   byKey.put(key,v);byId.put(v.id,v);
  }
  return v;
 }
 /**
  * Converts adjacent, consistently oriented triangle pairs into analytic
  * quads, then pairs their directed boundary uses into manifold half-edges.
  */
 public static VariableHeightOrthogonalBRep apply(
     PolygonPrismBRep first,PolygonPrismBRep second,
     VariableHeightOrthogonalMesh.Operation operation){
  VariableHeightOrthogonalMesh.Result result=
      VariableHeightOrthogonalMesh.apply(first,second,operation);
  float[] mesh=result.triangles();
  if(mesh.length==0||mesh.length%18!=0)
   throw new IllegalArgumentException("Stepped B-Rep requires paired quad triangles");
  String shellId=uuid(first.id()+"|"+second.id()+"|"+operation+"|"+
      Arrays.toString(mesh));
  Map<String,Vertex> byKey=new LinkedHashMap<>(),byId=new LinkedHashMap<>();
  Map<String,List<Use>> owners=new LinkedHashMap<>();
  List<Face> faces=new ArrayList<>();
  List<Use> uses=new ArrayList<>();
  double surface=0;
  for(int start=0;start<mesh.length;start+=18){
   float[] p0=point(mesh,start),p1=point(mesh,start+3),
           p2=point(mesh,start+6),q0=point(mesh,start+9),
           q2=point(mesh,start+12),p3=point(mesh,start+15);
   if(!key(p0).equals(key(q0))||!key(p2).equals(key(q2)))
    throw new IllegalArgumentException("Triangle pair is not a quad");
   double[] n=cross(subtract(p1,p0),subtract(p2,p0));
   double[] n2=cross(subtract(p2,p0),subtract(p3,p0));
   double magnitude=length(n),other=length(n2);
   if(magnitude==0||other==0||dot(n,n2)<=0)
    throw new IllegalArgumentException("Invalid oriented analytic quad");
   double[] normal={n[0]/magnitude,n[1]/magnitude,n[2]/magnitude};
   if(Math.abs(dot(normal,subtract(p3,p0)))>1e-5*Math.max(1,length(subtract(p3,p0))))
    throw new IllegalArgumentException("Nonplanar B-Rep face");
   float[][] corners={p0,p1,p2,p3};
   List<String> vertexIds=new ArrayList<>(),halfEdgeIds=new ArrayList<>();
   String faceId=shellId+"/face/"+faces.size();
   List<Use> faceUses=new ArrayList<>();
   for(float[] corner:corners)
    vertexIds.add(addVertex(byKey,byId,corner,shellId).id);
   if(new HashSet<>(vertexIds).size()!=4)
    throw new IllegalArgumentException("Collapsed B-Rep quad");
   for(int i=0;i<4;i++){
    String from=vertexIds.get(i),to=vertexIds.get((i+1)%4);
    String pair=from.compareTo(to)<0?from+"|"+to:to+"|"+from;
    String edgeId=shellId+"/edge/"+uuid(pair);
    String useId=faceId+"/halfedge/"+i;
    Use use=new Use(useId,edgeId,faceId,from,to);
    uses.add(use);faceUses.add(use);halfEdgeIds.add(useId);
    owners.computeIfAbsent(edgeId,k->new ArrayList<>()).add(use);
   }
   for(int i=0;i<4;i++){
    Use use=faceUses.get(i);
    use.next=halfEdgeIds.get((i+1)%4);
    use.previous=halfEdgeIds.get((i+3)%4);
   }
   double area=(magnitude+other)/2;
   surface+=area;
   faces.add(new Face(faceId,area,normal,vertexIds,halfEdgeIds));
  }
  List<Edge> edges=new ArrayList<>();
  for(Map.Entry<String,List<Use>> entry:owners.entrySet()){
   List<Use> pair=entry.getValue();
   if(pair.size()!=2||!pair.get(0).from.equals(pair.get(1).to)||
       !pair.get(0).to.equals(pair.get(1).from))
    throw new IllegalArgumentException("Nonmanifold or reversed analytic B-Rep edge");
   Use a=pair.get(0),b=pair.get(1);
   a.twin=b.id;b.twin=a.id;
   Vertex from=byId.get(a.from),to=byId.get(a.to);
   double dx=from.x-to.x,dy=from.y-to.y,dz=from.z-to.z;
   double len=Math.sqrt(dx*dx+dy*dy+dz*dz);
   if(!Double.isFinite(len)||len<=0)
    throw new IllegalArgumentException("Degenerate analytic B-Rep edge");
   edges.add(new Edge(entry.getKey(),a.from,a.to,len,
       Arrays.asList(a.faceId,b.faceId),Arrays.asList(a.id,b.id)));
  }
  if(byKey.size()-edges.size()+faces.size()!=2||uses.size()!=4*faces.size()||
     edges.size()*2!=uses.size())
   throw new IllegalArgumentException("Stepped B-Rep Euler / half-edge topology failed");
  List<HalfEdge> halfEdges=new ArrayList<>();
  for(Use use:uses)halfEdges.add(new HalfEdge(use));
  return new VariableHeightOrthogonalBRep(shellId,result,
      new ArrayList<>(byKey.values()),edges,faces,halfEdges,surface);
 }
}
