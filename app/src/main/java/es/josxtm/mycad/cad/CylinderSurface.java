package es.josxtm.mycad.cad;
/** Closed cylinder surface, centered on local XY and starting at local Z=0. */
public final class CylinderSurface {
 private CylinderSurface(){}
 public static int triangleCount(int segments){if(segments<12||segments>128)throw new IllegalArgumentException("Invalid segments");return segments*4;}
 public static float[] triangles(double diameter,double height,RigidTransform pose,int segments){
  triangleCount(segments);
  if(pose==null||!Double.isFinite(diameter)||!Double.isFinite(height)||diameter<=0||height<=0||
    diameter>10000||height>10000)throw new IllegalArgumentException("Invalid cylinder");
  double radius=diameter/2;
  float[] output=new float[triangleCount(segments)*9];int at=0;
  for(int i=0;i<segments;i++){
   double angle=2*Math.PI*i/segments,next=2*Math.PI*(i+1)/segments;
   double[] b0=pose.point(radius*Math.cos(angle),radius*Math.sin(angle),0);
   double[] b1=pose.point(radius*Math.cos(next),radius*Math.sin(next),0);
   double[] t0=pose.point(radius*Math.cos(angle),radius*Math.sin(angle),height);
   double[] t1=pose.point(radius*Math.cos(next),radius*Math.sin(next),height);
   double[] bottom=pose.point(0,0,0),top=pose.point(0,0,height);
   at=tri(output,at,b0,b1,t1);at=tri(output,at,b0,t1,t0);
   at=tri(output,at,bottom,b1,b0);at=tri(output,at,top,t0,t1);
  }
  return output;
 }
 private static int tri(float[] out,int at,double[] a,double[] b,double[] c){
  for(double[] v:new double[][]{a,b,c})for(double p:v){
   if(!Double.isFinite(p)||Math.abs(p)>Float.MAX_VALUE)throw new IllegalArgumentException("Invalid coordinate");
   out[at++]=(float)p;
  }
  return at;
 }
}
