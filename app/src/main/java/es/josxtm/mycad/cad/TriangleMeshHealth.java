package es.josxtm.mycad.cad;
import java.util.*;

/** Checks whether an indexed-by-coordinate triangle soup is a closed oriented 2-manifold. */
public final class TriangleMeshHealth {
 private TriangleMeshHealth(){}
 public static final class Report {
  public final int triangles,boundaryEdges,nonManifoldEdges,nonManifoldVertices,reversedEdges,degenerateTriangles;
  /** Edge-connected face components, including incomplete or invalid ones. */
  public final int shells,zeroVolumeShells;
  public final double signedVolume;
  Report(int t,int b,int n,int nv,int r,int d,int shells,int zeroVolumeShells,double v){
   triangles=t;boundaryEdges=b;nonManifoldEdges=n;nonManifoldVertices=nv;reversedEdges=r;degenerateTriangles=d;
   this.shells=shells;this.zeroVolumeShells=zeroVolumeShells;signedVolume=v;
  }
  public boolean isWatertight(){
   return triangles>0&&boundaryEdges==0&&nonManifoldEdges==0&&nonManifoldVertices==0&&reversedEdges==0&&degenerateTriangles==0&&zeroVolumeShells==0;
  }
 }
 private static final class Vertex implements Comparable<Vertex>{
  final int x,y,z;
  Vertex(float[] v,int p){
   if(!Float.isFinite(v[p])||!Float.isFinite(v[p+1])||!Float.isFinite(v[p+2]))
    throw new IllegalArgumentException("Nonfinite mesh coordinate");
   x=Float.floatToIntBits(v[p]==0?0:v[p]);
   y=Float.floatToIntBits(v[p+1]==0?0:v[p+1]);
   z=Float.floatToIntBits(v[p+2]==0?0:v[p+2]);
  }
  public int compareTo(Vertex o){
   int c=Integer.compare(x,o.x);if(c!=0)return c;
   c=Integer.compare(y,o.y);return c!=0?c:Integer.compare(z,o.z);
  }
  public boolean equals(Object o){
   if(!(o instanceof Vertex))return false;
   Vertex v=(Vertex)o;return x==v.x&&y==v.y&&z==v.z;
  }
  public int hashCode(){return Objects.hash(x,y,z);}
 }
 private static final class Edge {
  final Vertex a,b;
  Edge(Vertex a,Vertex b){if(a.compareTo(b)<0){this.a=a;this.b=b;}else{this.a=b;this.b=a;}}
  public boolean equals(Object o){
   if(!(o instanceof Edge))return false;
   Edge e=(Edge)o;return a.equals(e.a)&&b.equals(e.b);
  }
  public int hashCode(){return a.hashCode()*31+b.hashCode();}
 }
 private static final class Count {int uses,orientation,firstA,firstB,firstTriangle;}
 /** Each triangle corner has a separate node. Joining corners only across shared
  * edges exposes disconnected face fans at a vertex (pinched/non-manifold shells). */
 private static int root(int[] parent,int i){
  while(parent[i]!=i){parent[i]=parent[parent[i]];i=parent[i];}
  return i;
 }
 private static void join(int[] parent,int a,int b){
  int ra=root(parent,a),rb=root(parent,b);
  if(ra!=rb)parent[rb]=ra;
 }
 public static Report inspect(float[] mesh){
  if(mesh==null||mesh.length%9!=0)throw new IllegalArgumentException("Triangle xyz triplets required");
  Map<Edge,Count> edges=new HashMap<>();
  int[] parent=new int[mesh.length/3];
  for(int j=0;j<parent.length;j++)parent[j]=j;
  // Independently track face-connected shells. A vertex contact alone must
  // not combine the volume origins of two disconnected shells.
  int[] faceParent=new int[mesh.length/9];
  for(int j=0;j<faceParent.length;j++)faceParent[j]=j;
  int degenerates=0;
  for(int i=0;i<mesh.length;i+=9){
   Vertex[] v={new Vertex(mesh,i),new Vertex(mesh,i+3),new Vertex(mesh,i+6)};
   double ax=mesh[i],ay=mesh[i+1],az=mesh[i+2];
   double bx=mesh[i+3],by=mesh[i+4],bz=mesh[i+5];
   double cx=mesh[i+6],cy=mesh[i+7],cz=mesh[i+8];
   double nx=(by-ay)*(cz-az)-(bz-az)*(cy-ay);
   double ny=(bz-az)*(cx-ax)-(bx-ax)*(cz-az);
   double nz=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);
   // A fixed world-scale area threshold rejects valid sub-millimetre CAD
   // solids (e.g. a 10 µm tetrahedron). For finite float XYZ inputs the
   // double-precision cross product is sufficient to distinguish exactly
   // collinear triangles without introducing an arbitrary minimum area.
   if(v[0].equals(v[1])||v[1].equals(v[2])||v[2].equals(v[0])||
      (nx==0.0&&ny==0.0&&nz==0.0))degenerates++;
   for(int k=0;k<3;k++){
    Vertex from=v[k],to=v[(k+1)%3];
    Edge e=new Edge(from,to);Count count=edges.get(e);
    int cornerA=i/3+(from.compareTo(to)<0?k:(k+1)%3);
    int cornerB=i/3+(from.compareTo(to)<0?(k+1)%3:k);
    if(count==null){
     count=new Count();count.firstA=cornerA;count.firstB=cornerB;
     count.firstTriangle=i/9;edges.put(e,count);
    }else{
     join(faceParent,count.firstTriangle,i/9);
     if(count.uses==1){
      join(parent,count.firstA,cornerA);
      join(parent,count.firstB,cornerB);
     }
    }
    count.uses++;count.orientation+=from.compareTo(to)<0?1:-1;
   }
  }
  int boundary=0,nonManifold=0,reversed=0;
  for(Count e:edges.values()){
   if(e.uses==1)boundary++;
   else if(e.uses!=2)nonManifold++;
   else if(e.orientation!=0)reversed++;
  }
  // Edge-manifold tests alone miss two closed shells touching at one vertex.
  // A valid closed 2-manifold has exactly one connected face fan per vertex.
  Map<Vertex,Integer> firstFan=new HashMap<>();
  Set<Vertex> splitVertices=new HashSet<>();
  for(Map.Entry<Edge,Count> entry:edges.entrySet()){
   Count count=entry.getValue();
   if(count.uses!=2)continue;
   Edge edge=entry.getKey();
   int a=root(parent,count.firstA),b=root(parent,count.firstB);
   Integer previousA=firstFan.putIfAbsent(edge.a,a);
   Integer previousB=firstFan.putIfAbsent(edge.b,b);
   if(previousA!=null&&previousA!=a)splitVertices.add(edge.a);
   if(previousB!=null&&previousB!=b)splitVertices.add(edge.b);
  }
  // Compute each edge-connected shell around its own local origin. Using a
  // single anchor for a multi-body scene reintroduces catastrophic cancellation
  // when a tiny shell is millions of millimetres from the first shell.
  // Each accumulator stores [anchor XYZ, Neumaier sum, correction].
  Map<Integer,double[]> shellVolumes=new HashMap<>();
  for(int i=0;i<mesh.length;i+=9){
   int shell=root(faceParent,i/9);
   double[] acc=shellVolumes.get(shell);
   if(acc==null){
    acc=new double[]{mesh[i],mesh[i+1],mesh[i+2],0,0};
    shellVolumes.put(shell,acc);
   }
   double ax=mesh[i]-acc[0],ay=mesh[i+1]-acc[1],az=mesh[i+2]-acc[2];
   double bx=mesh[i+3]-acc[0],by=mesh[i+4]-acc[1],bz=mesh[i+5]-acc[2];
   double cx=mesh[i+6]-acc[0],cy=mesh[i+7]-acc[1],cz=mesh[i+8]-acc[2];
   double term=(ax*(by*cz-bz*cy)+ay*(bz*cx-bx*cz)+az*(bx*cy-by*cx))/6.0;
   double next=acc[3]+term;
   if(Math.abs(acc[3])>=Math.abs(term))acc[4]+=(acc[3]-next)+term;
   else acc[4]+=(term-next)+acc[3];
   acc[3]=next;
  }
  double volume=0,volumeCorrection=0;
  int zeroVolumeShells=0;
  for(double[] acc:shellVolumes.values()){
   double term=acc[3]+acc[4];
   // A two-sided coplanar sheet can have paired, correctly oriented edges
   // yet enclose no solid. Check each shell independently: opposite-volume
   // shells must not hide a collapsed component through global cancellation.
   if(term==0.0)zeroVolumeShells++;
   double next=volume+term;
   if(Math.abs(volume)>=Math.abs(term))volumeCorrection+=(volume-next)+term;
   else volumeCorrection+=(term-next)+volume;
   volume=next;
  }
  return new Report(mesh.length/9,boundary,nonManifold,splitVertices.size(),reversed,degenerates,
    shellVolumes.size(),zeroVolumeShells,volume+volumeCorrection);
 }
}
