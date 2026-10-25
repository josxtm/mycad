package es.josxtm.mycad.cad;
import java.util.*;
/** Watertight polygonal approximation of a vertical circular hole through an axis-aligned box. */
public final class CircularThroughHole {
 private CircularThroughHole(){}
 public static final int SEGMENTS=64;
 public static float[] triangles(double x,double y,double z,double w,double d,double h,
                                 double offsetX,double offsetY,double diameter){
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(w)||
     !Double.isFinite(d)||!Double.isFinite(h)||!Double.isFinite(offsetX)||
     !Double.isFinite(offsetY)||!Double.isFinite(diameter)||
     w<=0||d<=0||h<=0||diameter<=0||w>10000||d>10000||h>10000||
     Math.abs(x)>10000||Math.abs(y)>10000||Math.abs(z)>10000)
    throw new IllegalArgumentException("Invalid dimensions");
  double r=diameter/2,cx=x+offsetX,cy=y+offsetY;
  double left=x-w/2,right=x+w/2,bottom=y-d/2,top=y+d/2;
  if(cx-r<=left||cx+r>=right||cy-r<=bottom||cy+r>=top)
   throw new IllegalArgumentException("Hole must be strictly inside the box");
  TreeSet<Double> angles=new TreeSet<>();
  for(int i=0;i<SEGMENTS;i++)angles.add(2*Math.PI*i/SEGMENTS);
  double[][] corners={{left,bottom},{right,bottom},{right,top},{left,top}};
  for(double[] v:corners){double theta=Math.atan2(v[1]-cy,v[0]-cx);if(theta<0)theta+=2*Math.PI;angles.add(theta);}
  double[] sorted=new double[angles.size()];int p=0;for(double theta:angles)sorted[p++]=theta;
  ArrayList<Float> out=new ArrayList<>(sorted.length*8*9);
  for(int i=0;i<sorted.length;i++){
   int j=(i+1)%sorted.length;
   double angle=sorted[i],next=sorted[j];
   double[] in0={cx+r*Math.cos(angle),cy+r*Math.sin(angle)};
   double[] in1={cx+r*Math.cos(next),cy+r*Math.sin(next)};
   double[] out0=boundary(cx,cy,angle,left,right,bottom,top);
   double[] out1=boundary(cx,cy,next,left,right,bottom,top);
   double[] ib0=point(in0,z),ib1=point(in1,z),it0=point(in0,z+h),it1=point(in1,z+h);
   double[] ob0=point(out0,z),ob1=point(out1,z),ot0=point(out0,z+h),ot1=point(out1,z+h);
   // Top annulus (upward); bottom annulus (downward).
   tri(out,it0,ot0,ot1);tri(out,it0,ot1,it1);
   tri(out,ib0,ob1,ob0);tri(out,ib0,ib1,ob1);
   // Outside walls (outward) and bore wall (towards cylinder axis).
   tri(out,ob0,ob1,ot1);tri(out,ob0,ot1,ot0);
   tri(out,ib0,it1,ib1);tri(out,ib0,it0,it1);
  }
  float[] result=new float[out.size()];for(int i=0;i<result.length;i++)result[i]=out.get(i);
  return result;
 }
 private static double[] point(double[] xy,double z){return new double[]{xy[0],xy[1],z};}
 private static double[] boundary(double cx,double cy,double angle,double l,double r,double b,double t){
  double dx=Math.cos(angle),dy=Math.sin(angle);
  double distance=Double.POSITIVE_INFINITY;
  if(dx>1e-12)distance=Math.min(distance,(r-cx)/dx);
  if(dx< -1e-12)distance=Math.min(distance,(l-cx)/dx);
  if(dy>1e-12)distance=Math.min(distance,(t-cy)/dy);
  if(dy< -1e-12)distance=Math.min(distance,(b-cy)/dy);
  return new double[]{cx+distance*dx,cy+distance*dy};
 }
 private static void tri(ArrayList<Float> out,double[] a,double[] b,double[] c){
  for(double[] v:new double[][]{a,b,c})for(double coord:v)out.add((float)coord);
 }
}
