package es.josxtm.mycad;

/** Pure Java geometric validation utilities. Coordinates and lengths in millimetres. */
public final class GeometryMath {
 private GeometryMath(){}
 public static double rectangleVolume(double width,double depth,double height){
  requirePositive(width);requirePositive(depth);requirePositive(height);
  return width*depth*height;
 }
 public static double cylinderVolume(double diameter,double height){
  requirePositive(diameter);requirePositive(height);
  double radius=diameter/2.0;
  return Math.PI*radius*radius*height;
 }
 public static void requirePositive(double value){
  if(!Double.isFinite(value)||value<=0||value>10000)throw new IllegalArgumentException("Invalid length in millimetres");
 }
 /** Local sketch x/y mapped into world XYZ for XY, XZ or YZ. */
 public static double[] sketchToWorld(int plane,double a,double b){
  if(!Double.isFinite(a)||!Double.isFinite(b))throw new IllegalArgumentException("Invalid coordinate");
  switch(plane){
   case 0:return new double[]{a,b,0};
   case 1:return new double[]{a,0,b};
   case 2:return new double[]{0,a,b};
   default:throw new IllegalArgumentException("Invalid plane");
  }
 }
}
