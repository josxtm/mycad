package es.josxtm.mycad;
import java.util.ArrayList;
import java.util.TreeSet;

/** Closed, consistently partitioned triangle surface for boolean combinations of two axis-aligned boxes. */
public final class BoxBooleanMesh {
 private BoxBooleanMesh(){}
 public enum Operation { UNION, INTERSECTION, DIFFERENCE }
 private static double[] cuts(double a,double b,double c,double d){
  TreeSet<Double> s=new TreeSet<>();
  s.add(a);s.add(b);s.add(c);s.add(d);
  double[] result=new double[s.size()];int k=0;for(Double value:s)result[k++]=value;return result;
 }
 private static boolean inside(BoxBoolean.Box b,double x,double y,double z){
  return x>b.x0&&x<b.x1&&y>b.y0&&y<b.y1&&z>b.z0&&z<b.z1;
 }
 private static void tri(ArrayList<Float> v,double[] a,double[] b,double[] c){
  for(double p:a)v.add((float)p);for(double p:b)v.add((float)p);for(double p:c)v.add((float)p);
 }
 private static void face(ArrayList<Float> v,double[] a,double[] b,double[] c,double[] d){
  tri(v,a,b,c);tri(v,a,c,d);
 }
 public static float[] build(BoxBoolean.Box a,BoxBoolean.Box b,Operation operation){
  double[] xs=cuts(a.x0,a.x1,b.x0,b.x1),ys=cuts(a.y0,a.y1,b.y0,b.y1),zs=cuts(a.z0,a.z1,b.z0,b.z1);
  int nx=xs.length-1,ny=ys.length-1,nz=zs.length-1;
  boolean[][][] active=new boolean[nx][ny][nz];
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++)for(int k=0;k<nz;k++){
   double x=(xs[i]+xs[i+1])/2,y=(ys[j]+ys[j+1])/2,z=(zs[k]+zs[k+1])/2;
   boolean inA=inside(a,x,y,z),inB=inside(b,x,y,z);
   active[i][j][k]=operation==Operation.UNION?(inA||inB):operation==Operation.INTERSECTION?(inA&&inB):(inA&&!inB);
  }
  ArrayList<Float> out=new ArrayList<>();
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++)for(int k=0;k<nz;k++){
   if(!active[i][j][k])continue;
   double x=xs[i],X=xs[i+1],y=ys[j],Y=ys[j+1],z=zs[k],Z=zs[k+1];
   if(i==0||!active[i-1][j][k])face(out,new double[]{x,y,z},new double[]{x,y,Z},new double[]{x,Y,Z},new double[]{x,Y,z});
   if(i==nx-1||!active[i+1][j][k])face(out,new double[]{X,y,z},new double[]{X,Y,z},new double[]{X,Y,Z},new double[]{X,y,Z});
   if(j==0||!active[i][j-1][k])face(out,new double[]{x,y,z},new double[]{X,y,z},new double[]{X,y,Z},new double[]{x,y,Z});
   if(j==ny-1||!active[i][j+1][k])face(out,new double[]{x,Y,z},new double[]{x,Y,Z},new double[]{X,Y,Z},new double[]{X,Y,z});
   if(k==0||!active[i][j][k-1])face(out,new double[]{x,y,z},new double[]{x,Y,z},new double[]{X,Y,z},new double[]{X,y,z});
   if(k==nz-1||!active[i][j][k+1])face(out,new double[]{x,y,Z},new double[]{X,y,Z},new double[]{X,Y,Z},new double[]{x,Y,Z});
  }
  float[] vertices=new float[out.size()];for(int i=0;i<vertices.length;i++)vertices[i]=out.get(i);
  return vertices;
 }
}
