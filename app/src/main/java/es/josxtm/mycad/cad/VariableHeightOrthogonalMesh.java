package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Exact grid-boundary triangle mesh for same-plane orthogonal prisms with
 * unequal signed heights. This is a validated 3D surface, NOT an analytic
 * half-edge PolygonPrismBRep. It is intentionally not wired to persisted
 * Boolean features until stepped faces and their topology are supported.
 */
public final class VariableHeightOrthogonalMesh {
 public enum Operation { UNION, DIFFERENCE }
 private static final int MAX_TRIANGLES=2000;
 private VariableHeightOrthogonalMesh(){}

 public static final class Result {
  private final float[] mesh;
  public final Operation operation;
  public final int plane;
  public final double volume;
  private Result(float[] mesh,Operation operation,int plane,double volume){
   this.mesh=mesh.clone();this.operation=operation;this.plane=plane;this.volume=volume;
  }
  public float[] triangles(){return mesh.clone();}
 }

 private static float[] xyz(int plane,double u,double v,double h){
  if(plane==0)return new float[]{(float)u,(float)v,(float)h};
  if(plane==1)return new float[]{(float)u,(float)h,(float)v};
  return new float[]{(float)h,(float)u,(float)v};
 }
 private static double[] subtract(float[] a,float[] b){
  return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};
 }
 private static double[] cross(double[] a,double[] b){
  return new double[]{a[1]*b[2]-a[2]*b[1],
      a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};
 }
 private static double dot(double[] a,double[] b){
  return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
 }
 private static float[] triangle(float[] a,float[] b,float[] c){
  return new float[]{a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2]};
 }
 private static void quad(List<float[]> out,float[][] corners,double[] outward){
  double[] normal=cross(subtract(corners[1],corners[0]),
                        subtract(corners[2],corners[0]));
  double orientation=dot(normal,outward);
  if(!Double.isFinite(orientation)||orientation==0)
   throw new IllegalArgumentException("Degenerate stepped Boolean face");
  if(orientation<0){
   float[] tmp=corners[1];corners[1]=corners[3];corners[3]=tmp;
  }
  if(out.size()+2>MAX_TRIANGLES)
   throw new IllegalArgumentException("Variable-height mesh exceeds triangle limit");
  out.add(triangle(corners[0],corners[1],corners[2]));
  out.add(triangle(corners[0],corners[2],corners[3]));
 }
 private static boolean filled(boolean[][][] grid,int i,int j,int k){
  return i>=0&&j>=0&&k>=0&&i<grid.length&&
      j<grid[0].length&&k<grid[0][0].length&&grid[i][j][k];
 }

 public static Result apply(PolygonPrismBRep first,PolygonPrismBRep second,
                            Operation operation){
  if(operation==null)throw new IllegalArgumentException("Missing Boolean operation");
  VariableHeightPrismVolume.Result volumes=
      VariableHeightPrismVolume.calculate(first,second);
  List<double[]> a=OrthogonalPolygonBRepBoolean.points(first);
  List<double[]> b=OrthogonalPolygonBRepBoolean.points(second);
  double[] xs=OrthogonalPolygonBRepBoolean.coordinates(a,b,0);
  double[] ys=OrthogonalPolygonBRepBoolean.coordinates(a,b,1);
  double ah=Math.abs(first.distance()),bh=Math.abs(second.distance());
  double shorter=Math.min(ah,bh),taller=Math.max(ah,bh);
  double[] levels=shorter==taller?new double[]{0,taller}:
      new double[]{0,shorter,taller};
  int nx=xs.length-1,ny=ys.length-1,nz=levels.length-1;
  boolean[][][] cells=new boolean[nx][ny][nz];
  double expected=operation==Operation.UNION?volumes.unionVolume:volumes.differenceVolume;
  double counted=0;
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++){
   double x=(xs[i]+xs[i+1])/2,y=(ys[j]+ys[j+1])/2;
   boolean insideA=OrthogonalPolygonBRepBoolean.inside(a,x,y);
   boolean insideB=OrthogonalPolygonBRepBoolean.inside(b,x,y);
   for(int k=0;k<nz;k++){
    double middle=(levels[k]+levels[k+1])/2;
    boolean inA=insideA&&middle<ah,inB=insideB&&middle<bh;
    cells[i][j][k]=operation==Operation.UNION?(inA||inB):(inA&&!inB);
    if(cells[i][j][k])
     counted+=(xs[i+1]-xs[i])*(ys[j+1]-ys[j])*(levels[k+1]-levels[k]);
   }
  }
  if(counted<=0||Math.abs(counted-expected)>1e-5*Math.max(1,expected))
   throw new IllegalArgumentException("Variable-height Boolean volume mismatch or empty result");
  int plane=first.plane(),sign=first.distance()<0?-1:1;
  ArrayList<float[]> triangles=new ArrayList<>();
  for(int i=0;i<nx;i++)for(int j=0;j<ny;j++)for(int k=0;k<nz;k++){
   if(!cells[i][j][k])continue;
   double u0=xs[i],u1=xs[i+1],v0=ys[j],v1=ys[j+1];
   double h0=sign*levels[k],h1=sign*levels[k+1];
   float[] p000=xyz(plane,u0,v0,h0),p100=xyz(plane,u1,v0,h0);
   float[] p010=xyz(plane,u0,v1,h0),p110=xyz(plane,u1,v1,h0);
   float[] p001=xyz(plane,u0,v0,h1),p101=xyz(plane,u1,v0,h1);
   float[] p011=xyz(plane,u0,v1,h1),p111=xyz(plane,u1,v1,h1);
   float[] center=xyz(plane,(u0+u1)/2,(v0+v1)/2,(h0+h1)/2);
   if(!filled(cells,i-1,j,k)){
    float[] face=xyz(plane,u0,(v0+v1)/2,(h0+h1)/2);
    quad(triangles,new float[][]{p000,p010,p011,p001},subtract(face,center));
   }
   if(!filled(cells,i+1,j,k)){
    float[] face=xyz(plane,u1,(v0+v1)/2,(h0+h1)/2);
    quad(triangles,new float[][]{p100,p110,p111,p101},subtract(face,center));
   }
   if(!filled(cells,i,j-1,k)){
    float[] face=xyz(plane,(u0+u1)/2,v0,(h0+h1)/2);
    quad(triangles,new float[][]{p000,p100,p101,p001},subtract(face,center));
   }
   if(!filled(cells,i,j+1,k)){
    float[] face=xyz(plane,(u0+u1)/2,v1,(h0+h1)/2);
    quad(triangles,new float[][]{p010,p110,p111,p011},subtract(face,center));
   }
   if(!filled(cells,i,j,k-1)){
    float[] face=xyz(plane,(u0+u1)/2,(v0+v1)/2,h0);
    quad(triangles,new float[][]{p000,p100,p110,p010},subtract(face,center));
   }
   if(!filled(cells,i,j,k+1)){
    float[] face=xyz(plane,(u0+u1)/2,(v0+v1)/2,h1);
    quad(triangles,new float[][]{p001,p101,p111,p011},subtract(face,center));
   }
  }
  float[] mesh=new float[triangles.size()*9];
  int at=0;
  for(float[] triangle:triangles){
   System.arraycopy(triangle,0,mesh,at,9);at+=9;
  }
  TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(mesh);
  if(!health.isWatertight()||health.shells!=1||health.negativeVolumeShells!=0||
     Math.abs(health.signedVolume-expected)>1e-4*Math.max(1,expected))
   throw new IllegalArgumentException("Variable-height mesh is not a valid single closed shell");
  return new Result(mesh,operation,plane,expected);
 }
}
