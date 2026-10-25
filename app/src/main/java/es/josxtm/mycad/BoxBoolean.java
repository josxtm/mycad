package es.josxtm.mycad;

/** Exact volumes for two axis-aligned boxes; this does not create a B-Rep solid. */
public final class BoxBoolean {
 private BoxBoolean(){}
 public static final class Box {
  public final double x0,y0,z0,x1,y1,z1;
  public Box(double x,double y,double z,double width,double depth,double height) {
   GeometryMath.requirePositive(width);GeometryMath.requirePositive(depth);GeometryMath.requirePositive(height);
   if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Invalid position");
   x0=x-width/2;y0=y-depth/2;z0=z;x1=x+width/2;y1=y+depth/2;z1=z+height;
  }
  public double volume(){return (x1-x0)*(y1-y0)*(z1-z0);}
 }
 public static double overlap(Box a,Box b){
  double w=Math.max(0,Math.min(a.x1,b.x1)-Math.max(a.x0,b.x0));
  double d=Math.max(0,Math.min(a.y1,b.y1)-Math.max(a.y0,b.y0));
  double h=Math.max(0,Math.min(a.z1,b.z1)-Math.max(a.z0,b.z0));
  return w*d*h;
 }
 public static double unionVolume(Box a,Box b){return a.volume()+b.volume()-overlap(a,b);}
 public static double differenceVolume(Box a,Box b){return a.volume()-overlap(a,b);}
}
