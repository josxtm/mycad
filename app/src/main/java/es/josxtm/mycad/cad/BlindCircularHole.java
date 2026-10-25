package es.josxtm.mycad.cad;
import java.util.*;

/**
 * Watertight polygonal mesh for a blind circular hole entering from the top (+Z)
 * of a local axis-aligned rectangular solid. Coordinates use center X/Y, base Z.
 * Blind depth must be strictly shorter than solid height.
 */
public final class BlindCircularHole {
 private BlindCircularHole(){}
 public static float[] triangles(double x,double y,double z,double width,double depth,double height,
                                 double offsetX,double offsetY,double diameter,double holeDepth){
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||
     !Double.isFinite(width)||!Double.isFinite(depth)||!Double.isFinite(height)||
     !Double.isFinite(offsetX)||!Double.isFinite(offsetY)||!Double.isFinite(diameter)||
     !Double.isFinite(holeDepth)||width<=0||depth<=0||height<=0||diameter<=0||
     width>10000||depth>10000||height>10000||holeDepth<=0||holeDepth>=height||
     Math.abs(x)>10000||Math.abs(y)>10000||Math.abs(z)>10000)
    throw new IllegalArgumentException("Invalid blind hole");
  double radius=diameter/2,cx=x+offsetX,cy=y+offsetY;
  double left=x-width/2,right=x+width/2,bottom=y-depth/2,top=y+depth/2;
  if(cx-radius<=left||cx+radius>=right||cy-radius<=bottom||cy+radius>=top)
   throw new IllegalArgumentException("Hole must fit strictly inside prism face");
  TreeSet<Double> angles=new TreeSet<>();
  for(int i=0;i<CircularThroughHole.SEGMENTS;i++)
   angles.add(2*Math.PI*i/CircularThroughHole.SEGMENTS);
  for(double[] corner:new double[][]{{left,bottom},{right,bottom},{right,top},{left,top}}){
   double a=Math.atan2(corner[1]-cy,corner[0]-cx);
   if(a<0)a+=2*Math.PI;
   angles.add(a);
  }
  double[] ordered=new double[angles.size()];int cursor=0;
  for(double angle:angles)ordered[cursor++]=angle;
  ArrayList<Float> out=new ArrayList<>(ordered.length*8*9);
  for(int i=0;i<ordered.length;i++){
   int j=(i+1)%ordered.length;
   double[] inner0={cx+radius*Math.cos(ordered[i]),cy+radius*Math.sin(ordered[i])};
   double[] inner1={cx+radius*Math.cos(ordered[j]),cy+radius*Math.sin(ordered[j])};
   double[] outer0=boundary(cx,cy,ordered[i],left,right,bottom,top);
   double[] outer1=boundary(cx,cy,ordered[j],left,right,bottom,top);
   double[] topInner0=p(inner0,z+height),topInner1=p(inner1,z+height);
   double[] topOuter0=p(outer0,z+height),topOuter1=p(outer1,z+height);
   double[] bottomOuter0=p(outer0,z),bottomOuter1=p(outer1,z);
   double[] floorInner0=p(inner0,z+height-holeDepth),floorInner1=p(inner1,z+height-holeDepth);
   // Top annulus, positive Z normal.
   tri(out,topInner0,topOuter0,topOuter1);
   tri(out,topInner0,topOuter1,topInner1);
   // Bottom rectangle: a single fan whose edges match all outside walls.
   tri(out,new double[]{cx,cy,z},bottomOuter1,bottomOuter0);
   // Exterior vertical walls.
   tri(out,bottomOuter0,bottomOuter1,topOuter1);
   tri(out,bottomOuter0,topOuter1,topOuter0);
   // Cavity wall inward, from blind floor up to the entrance.
   tri(out,floorInner0,topInner1,floorInner1);
   tri(out,floorInner0,topInner0,topInner1);
   // Blind bore floor, positive Z (floor of the empty cylindrical cavity).
   tri(out,floorInner0,floorInner1,new double[]{cx,cy,z+height-holeDepth});
  }
  float[] result=new float[out.size()];
  for(int i=0;i<result.length;i++)result[i]=out.get(i);
  return result;
 }
 private static double[] p(double[] xy,double z){return new double[]{xy[0],xy[1],z};}
 private static double[] boundary(double cx,double cy,double theta,double l,double r,double b,double t){
  double dx=Math.cos(theta),dy=Math.sin(theta),reach=Double.POSITIVE_INFINITY;
  if(dx>1e-12)reach=Math.min(reach,(r-cx)/dx);
  if(dx< -1e-12)reach=Math.min(reach,(l-cx)/dx);
  if(dy>1e-12)reach=Math.min(reach,(t-cy)/dy);
  if(dy< -1e-12)reach=Math.min(reach,(b-cy)/dy);
  return new double[]{cx+reach*dx,cy+reach*dy};
 }
 private static void tri(ArrayList<Float> result,double[] a,double[] b,double[] c){
  for(double[] v:new double[][]{a,b,c})for(double n:v)result.add((float)n);
 }
}
