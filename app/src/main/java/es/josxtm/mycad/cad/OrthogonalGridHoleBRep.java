package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Oriented planar B-Rep cell complex for orthogonal prism Booleans, including
 * THROUGH-HOLES. Unlike PolygonPrismBRep, cap faces are divided at the exact
 * sketch vertex-grid lines, making every cap edge match a neighboring cap or
 * the inward-facing wall of a hole. No hidden edges or triangle stitching.
 *
 * The result has true vertices, planar quad faces, shared two-use edges and
 * opposite-oriented halfedges. A ring has Euler V-E+F=0 (genus one);
 * independent components have additive Euler and are supported.
 *
 * Deliberate limits: shared XY/XZ/YZ origin and signed extrusion height,
 * orthogonal line profiles, at most 16 solid components and 16 through-holes.
 * Does not imply general CAD 3D Booleans, curved wires, or blind holes.
 */
public final class OrthogonalGridHoleBRep {
 private static final double EPS=1e-6;
 private static final int MAX_AXES=48,MAX_PARTS=16,MAX_HOLES=16;
 public static final class Vertex {
  public final String id;
  public final double x,y,z;
  private Vertex(String id,double[] xyz){
   this.id=id;x=xyz[0];y=xyz[1];z=xyz[2];
  }
 }
 public static final class Face {
  public final String id,kind;
  public final double area;
  private final List<String> corners,uses;
  private final double[] normal;
  private Face(String id,double area,List<String> corners,List<String> uses,
               double[] normal){
   this.id=id;kind="PLANE";this.area=area;
   this.corners=Collections.unmodifiableList(new ArrayList<>(corners));
   this.uses=Collections.unmodifiableList(new ArrayList<>(uses));
   this.normal=normal.clone();
  }
  public List<String> vertexIds(){return corners;}
  public List<String> halfEdgeIds(){return uses;}
  public double[] outwardNormal(){return normal.clone();}
 }
 public static final class Edge {
  public final String id,firstVertexId,secondVertexId;
  private final List<String> faceIds,halfEdgeIds;
  private Edge(String id,String a,String b,List<String> faces,List<String> uses){
   this.id=id;firstVertexId=a;secondVertexId=b;
   faceIds=Collections.unmodifiableList(new ArrayList<>(faces));
   halfEdgeIds=Collections.unmodifiableList(new ArrayList<>(uses));
  }
  public List<String> adjacentFaceIds(){return faceIds;}
  public List<String> halfEdgeIds(){return halfEdgeIds;}
 }
 public static final class HalfEdge {
  public final String id,edgeId,faceId,fromId,toId,nextId,previousId,twinId;
  private HalfEdge(Use use,String edgeId,String twinId){
   id=use.id;this.edgeId=edgeId;faceId=use.faceId;
   fromId=use.fromId;toId=use.toId;
   nextId=use.nextId;previousId=use.previousId;
   this.twinId=twinId;
  }
 }
 private static final class Use {
  String id,faceId,fromId,toId,nextId,previousId;
  int from,to;
 }
 private static final class Quad {
  String id;
  int[] nodes;
  double area;
  Quad(String id,int[] nodes,double area){
   this.id=id;this.nodes=nodes;this.area=area;
  }
 }
 private static final class Data {
  int solids,holes;
  double area;
  double[] xs,ys;
  boolean[][] filled;
 }
 public final String id;
 public final int plane,solidCount,holeCount;
 public final double area,volume,distance;
 private final List<Vertex> vertices;
 private final List<Face> faces;
 private final List<Edge> edges;
 private final List<HalfEdge> halfEdges;
 private final float[] triangles;

 private OrthogonalGridHoleBRep(String id,int plane,double height,Data data){
  this.id=id;this.plane=plane;distance=height;
  solidCount=data.solids;holeCount=data.holes;area=data.area;
  volume=area*Math.abs(height);
  int nx=data.xs.length-1,ny=data.ys.length-1;
  LinkedHashMap<Integer,Integer> nodeIndices=new LinkedHashMap<>();
  ArrayList<Vertex> v=new ArrayList<>();
  ArrayList<Quad> quads=new ArrayList<>();
  final boolean flipBase=height<0;
  final boolean flipAxes=plane==1;
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++){
   if(!data.filled[i][j])continue;
   int a=nodeIndex(nodeIndices,v,id,plane,data,height,0,i,j),
       b=nodeIndex(nodeIndices,v,id,plane,data,height,0,i+1,j),
       c=nodeIndex(nodeIndices,v,id,plane,data,height,0,i+1,j+1),
       d=nodeIndex(nodeIndices,v,id,plane,data,height,0,i,j+1),
       A=nodeIndex(nodeIndices,v,id,plane,data,height,1,i,j),
       B=nodeIndex(nodeIndices,v,id,plane,data,height,1,i+1,j),
       C=nodeIndex(nodeIndices,v,id,plane,data,height,1,i+1,j+1),
       D=nodeIndex(nodeIndices,v,id,plane,data,height,1,i,j+1);
   double cap=(data.xs[i+1]-data.xs[i])*(data.ys[j+1]-data.ys[j]);
   addQuad(quads,id,"bottom/"+i+"/"+j,new int[]{a,b,c,d},cap,flipBase!=flipAxes);
   addQuad(quads,id,"top/"+i+"/"+j,new int[]{A,B,C,D},cap,!flipBase!=flipAxes);
   // The material is always LEFT of a CCW boundary segment. The default
   // side quad is (base from,base to,tip to,tip from) for positive height.
   if(j==0||!data.filled[i][j-1])
    side(quads,id,"south/"+i+"/"+j,a,b,B,A,data.xs[i+1]-data.xs[i],height,flipAxes);
   if(i==nx-1||!data.filled[i+1][j])
    side(quads,id,"east/"+i+"/"+j,b,c,C,B,data.ys[j+1]-data.ys[j],height,flipAxes);
   if(j==ny-1||!data.filled[i][j+1])
    side(quads,id,"north/"+i+"/"+j,c,d,D,C,data.xs[i+1]-data.xs[i],height,flipAxes);
   if(i==0||!data.filled[i-1][j])
    side(quads,id,"west/"+i+"/"+j,d,a,A,D,data.ys[j+1]-data.ys[j],height,flipAxes);
  }
  if(quads.size()>950||v.size()>1400)
   throw new IllegalArgumentException("B-Rep con agujeros supera el límite de caras");
  Map<String,List<Use>> edgeUses=new LinkedHashMap<>();
  ArrayList<Face> faceList=new ArrayList<>();
  ArrayList<Float> soup=new ArrayList<>();
  for(Quad q:quads){
   String faceId=q.id;
   List<String> ids=new ArrayList<>();
   List<String> useIds=new ArrayList<>();
   for(int k=0;k<4;k++){
    int from=q.nodes[k],to=q.nodes[(k+1)%4];
    String key=Math.min(from,to)+":"+Math.max(from,to);
    Use use=new Use();
    use.id=stable(faceId+"/half/"+k);use.faceId=faceId;
    use.from=from;use.to=to;
    use.fromId=v.get(from).id;use.toId=v.get(to).id;
    use.nextId=stable(faceId+"/half/"+((k+1)%4));
    use.previousId=stable(faceId+"/half/"+((k+3)%4));
    edgeUses.computeIfAbsent(key,unused->new ArrayList<>()).add(use);
    ids.add(use.fromId);useIds.add(use.id);
   }
   double[] p=coords(v.get(q.nodes[0])),
            r=coords(v.get(q.nodes[1])),t=coords(v.get(q.nodes[2]));
   double[] normal=normal(p,r,t);
   faceList.add(new Face(faceId,q.area,ids,useIds,normal));
   for(int[] tri:new int[][]{{0,1,2},{0,2,3}})
    for(int index:tri){
     Vertex corner=v.get(q.nodes[index]);
     soup.add((float)corner.x);soup.add((float)corner.y);soup.add((float)corner.z);
    }
  }
  ArrayList<Edge> es=new ArrayList<>();
  ArrayList<HalfEdge> hs=new ArrayList<>();
  for(Map.Entry<String,List<Use>> e:edgeUses.entrySet()){
   List<Use> pair=e.getValue();
   if(pair.size()!=2||pair.get(0).from!=pair.get(1).to||
      pair.get(0).to!=pair.get(1).from)
    throw new IllegalArgumentException("B-Rep no manifold: arista sin semiarista gemela");
   Use a=pair.get(0),b=pair.get(1);
   String eid=stable(id+"/edge/"+e.getKey());
   es.add(new Edge(eid,a.fromId,a.toId,
       Arrays.asList(a.faceId,b.faceId),Arrays.asList(a.id,b.id)));
   hs.add(new HalfEdge(a,eid,b.id));hs.add(new HalfEdge(b,eid,a.id));
  }
  float[] mesh=new float[soup.size()];
  for(int i=0;i<mesh.length;i++)mesh[i]=soup.get(i);
  if(mesh.length==0||mesh.length>18000)
   throw new IllegalArgumentException("B-Rep con agujeros: malla demasiado grande");
  TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(mesh);
  if(!health.isWatertight()||health.shells!=solidCount||
     Math.abs(health.signedVolume-volume)>1e-4*Math.max(1,volume))
   throw new IllegalArgumentException("B-Rep con agujeros no genera una malla cerrada y orientada");
  if(v.size()-es.size()+faceList.size()!=2*solidCount-2*holeCount)
   throw new IllegalArgumentException("Característica de Euler B-Rep incorrecta");
  vertices=Collections.unmodifiableList(v);
  edges=Collections.unmodifiableList(es);
  faces=Collections.unmodifiableList(faceList);
  halfEdges=Collections.unmodifiableList(hs);
  triangles=mesh;
 }
 private static String stable(String x){
  return UUID.nameUUIDFromBytes(x.getBytes(StandardCharsets.UTF_8)).toString();
 }
 private static double[] coords(Vertex a){return new double[]{a.x,a.y,a.z};}
 private static double[] normal(double[] a,double[] b,double[] c){
  double ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],
         vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
  double[] n={uy*vz-uz*vy,uz*vx-ux*vz,ux*vy-uy*vx};
  double len=Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
  if(len<=EPS)throw new IllegalArgumentException("Cara B-Rep degenerada");
  return new double[]{n[0]/len,n[1]/len,n[2]/len};
 }
 private static void addQuad(List<Quad> quads,String seed,String label,
                              int[] nodes,double area,boolean forward){
  if(area<=EPS)throw new IllegalArgumentException("Cara B-Rep demasiado pequeña");
  if(!forward)nodes=new int[]{nodes[0],nodes[3],nodes[2],nodes[1]};
  quads.add(new Quad(stable(seed+"/face/"+label),nodes,area));
 }
 private static void side(List<Quad> quads,String seed,String label,
     int a,int b,int B,int A,double width,double height,boolean flipAxes){
  addQuad(quads,seed,label,new int[]{a,b,B,A},
          width*Math.abs(height),(height>0)!=flipAxes);
 }
 private static int nodeIndex(Map<Integer,Integer> index,List<Vertex> v,
    String seed,int plane,Data data,double height,int layer,int i,int j){
  int key=(layer*data.ys.length+j)*data.xs.length+i;
  Integer found=index.get(key);
  if(found!=null)return found;
  double u=data.xs[i],w=data.ys[j];
  double h=layer==0?0:height;
  double[] p=plane==0?new double[]{u,w,h}:
             plane==1?new double[]{u,h,w}:new double[]{h,u,w};
  int created=v.size();
  v.add(new Vertex(stable(seed+"/vertex/"+layer+"/"+i+"/"+j),p));
  index.put(key,created);
  return created;
 }
 private static int componentCount(boolean[][] cells,boolean material){
  int nx=cells.length,ny=cells[0].length,parts=0;
  boolean[][] visited=new boolean[nx][ny];
  int[] dx={-1,1,0,0},dy={0,0,-1,1};
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++){
   if(cells[i][j]!=material||visited[i][j])continue;
   parts++;ArrayDeque<int[]> queue=new ArrayDeque<>();
   queue.add(new int[]{i,j});visited[i][j]=true;
   boolean boundary=false;
   while(!queue.isEmpty()){
    int[] pos=queue.removeFirst(),x=pos[0],y=pos[1];
    if(x==0||y==0||x==nx-1||y==ny-1)boundary=true;
    for(int k=0;k<4;k++){
     int a=x+dx[k],b=y+dy[k];
     if(a<0||b<0||a>=nx||b>=ny||visited[a][b]||
        cells[a][b]!=material)continue;
     visited[a][b]=true;queue.add(new int[]{a,b});
    }
   }
   // Empty regions touching the arrangement perimeter are exterior,
   // even when the occupied material spans the whole bounding rectangle.
   if(!material&&boundary)parts--;
  }
  return parts;
 }
 public static OrthogonalGridHoleBRep apply(
    PolygonPrismBRep first,PolygonPrismBRep second,
    OrthogonalPolygonBRepBoolean.Operation operation){
  if(first==null||second==null||operation==null||
     first.plane()!=second.plane())
   throw new IllegalArgumentException("B-Rep con agujeros requiere dos prismas coplanares");
  double h=first.distance();
  if(!Double.isFinite(h)||Math.abs(h-second.distance())>EPS*Math.max(1,Math.abs(h)))
   throw new IllegalArgumentException("B-Rep con agujeros requiere alturas firmadas iguales");
  List<double[]> a=OrthogonalPolygonBRepBoolean.points(first),
                 b=OrthogonalPolygonBRepBoolean.points(second);
  double[] xs=OrthogonalPolygonBRepBoolean.coordinates(a,b,0),
           ys=OrthogonalPolygonBRepBoolean.coordinates(a,b,1);
  Data data=new Data();data.xs=xs;data.ys=ys;
  data.filled=new boolean[xs.length-1][ys.length-1];
  for(int i=0;i<xs.length-1;i++)for(int j=0;j<ys.length-1;j++){
   double x=(xs[i]+xs[i+1])/2,y=(ys[j]+ys[j+1])/2;
   boolean one=OrthogonalPolygonBRepBoolean.inside(a,x,y),
           two=OrthogonalPolygonBRepBoolean.inside(b,x,y);
   boolean fill=OrthogonalPolygonBRepBoolean.retain(one,two,operation);
   data.filled[i][j]=fill;
   if(fill)data.area+=(xs[i+1]-xs[i])*(ys[j+1]-ys[j]);
  }
  if(data.area<=EPS)throw new IllegalArgumentException("La booleana no tiene volumen");
  data.solids=componentCount(data.filled,true);
  data.holes=componentCount(data.filled,false);
  if(data.solids<1||data.solids>MAX_PARTS||data.holes>MAX_HOLES)
   throw new IllegalArgumentException("Se excede el máximo de agujeros o sólidos");
  String left=first.id(),right=second.id();
  if(operation!=OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE&&
     left.compareTo(right)>0){String temp=left;left=right;right=temp;}
  String seed=left+"|"+right+"|GRID_HOLES|"+operation+"|"+h+"|"+first.plane();
  for(double[] corner:a)seed+="|a"+(float)corner[0]+","+(float)corner[1];
  for(double[] corner:b)seed+="|b"+(float)corner[0]+","+(float)corner[1];
  return new OrthogonalGridHoleBRep(stable(seed),first.plane(),h,data);
 }
 public List<Vertex> vertices(){return vertices;}
 public List<Face> faces(){return faces;}
 public List<Edge> edges(){return edges;}
 public List<HalfEdge> halfEdges(){return halfEdges;}
 public float[] triangles(){return triangles.clone();}
}
