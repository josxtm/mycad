package es.josxtm.mycad.cad;
import java.util.*;

/** Checks whether an indexed-by-coordinate triangle soup is a closed oriented 2-manifold. */
public final class TriangleMeshHealth {
 private TriangleMeshHealth(){}
 public static final class Report {
  public final int triangles,boundaryEdges,nonManifoldEdges,reversedEdges,degenerateTriangles;
  public final double signedVolume;
  Report(int t,int b,int n,int r,int d,double v){
   triangles=t;boundaryEdges=b;nonManifoldEdges=n;reversedEdges=r;degenerateTriangles=d;signedVolume=v;
  }
  public boolean isWatertight(){
   return triangles>0&&boundaryEdges==0&&nonManifoldEdges==0&&reversedEdges==0&&degenerateTriangles==0;
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
 private static final class Count {int uses;int orientation;}
 public static Report inspect(float[] mesh){
  if(mesh==null||mesh.length%9!=0)throw new IllegalArgumentException("Triangle xyz triplets required");
  Map<Edge,Count> edges=new HashMap<>();
  int degenerates=0;double volume=0;
  for(int i=0;i<mesh.length;i+=9){
   Vertex[] v={new Vertex(mesh,i),new Vertex(mesh,i+3),new Vertex(mesh,i+6)};
   double ax=mesh[i],ay=mesh[i+1],az=mesh[i+2];
   double bx=mesh[i+3],by=mesh[i+4],bz=mesh[i+5];
   double cx=mesh[i+6],cy=mesh[i+7],cz=mesh[i+8];
   double nx=(by-ay)*(cz-az)-(bz-az)*(cy-ay);
   double ny=(bz-az)*(cx-ax)-(bx-ax)*(cz-az);
   double nz=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);
   if(v[0].equals(v[1])||v[1].equals(v[2])||v[2].equals(v[0])||nx*nx+ny*ny+nz*nz<1e-16)degenerates++;
   volume+=(ax*(by*cz-bz*cy)+ay*(bz*cx-bx*cz)+az*(bx*cy-by*cx))/6;
   for(int k=0;k<3;k++){
    Vertex from=v[k],to=v[(k+1)%3];
    Edge e=new Edge(from,to);Count count=edges.get(e);
    if(count==null){count=new Count();edges.put(e,count);}
    count.uses++;count.orientation+=from.compareTo(to)<0?1:-1;
   }
  }
  int boundary=0,nonManifold=0,reversed=0;
  for(Count e:edges.values()){
   if(e.uses==1)boundary++;
   else if(e.uses!=2)nonManifold++;
   else if(e.orientation!=0)reversed++;
  }
  return new Report(mesh.length/9,boundary,nonManifold,reversed,degenerates,volume);
 }
}
