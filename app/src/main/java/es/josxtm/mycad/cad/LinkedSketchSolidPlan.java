package es.josxtm.mycad.cad;
import java.util.*;

/**
 * Android-independent mapping from rectangle/circle sketches to their linked
 * extruded primitive. Builds ALL dependents before the UI mutates a sketch.
 *
 * Coordinates follow MyCAD legacy: XY / XZ / YZ sketch planes, positive
 * extrusion height, circles represented by diameter and centre point.
 * This is a constrained parametric bridge, not a B-Rep sketch solver.
 */
public final class LinkedSketchSolidPlan {
 private LinkedSketchSolidPlan(){}
 public static final class Body {
  public final boolean circle;
  public final float w,d,h,x,y,z,rx,ry,rz;
  private Body(boolean c,double w,double d,double h,double x,double y,double z,
               double rx,double ry,double rz){
   this.circle=c;this.w=(float)w;this.d=(float)d;this.h=(float)h;
   this.x=(float)x;this.y=(float)y;this.z=(float)z;
   this.rx=(float)rx;this.ry=(float)ry;this.rz=(float)rz;
  }
  public double volume(){return circle?Math.PI*w*w*h/4:w*d*h;}
 }
 private static void limit(double value){
  if(!Double.isFinite(value)||Math.abs(value)>10000)
   throw new IllegalArgumentException("Out-of-range coordinate or dimension");
 }
 public static Body build(int type,int plane,double x1,double y1,double x2,double y2,double height){
  if(type!=2&&type!=3)throw new IllegalArgumentException("Only closed rectangle/circle profiles");
  if(plane<0||plane>2)throw new IllegalArgumentException("Unsupported work plane");
  limit(x1);limit(y1);limit(x2);limit(y2);limit(height);
  if(height<=0)throw new IllegalArgumentException("Height must be positive");
  final boolean circle=type==3;
  double width=circle?2*Math.hypot(x2-x1,y2-y1):Math.abs(x2-x1);
  double depth=circle?width:Math.abs(y2-y1);
  if(!Double.isFinite(width)||!Double.isFinite(depth)||width<=0.0001||
     depth<=0.0001||width>10000||depth>10000)
   throw new IllegalArgumentException("Invalid sketch profile");
  double cx=circle?x1:(x1+x2)/2,cy=circle?y1:(y1+y2)/2;
  double x=cx,y=cy,z=0,rx=0,ry=0,rz=0;
  if(plane==1){x=cx;y=height/2;z=cy-height/2;rx=-90;}
  if(plane==2){x=height/2;y=cx;z=cy-height/2;rx=90;rz=90;}
  limit(x);limit(y);limit(z);
  return new Body(circle,width,depth,height,x,y,z,rx,ry,rz);
 }
 /**
  * Preflight all downstream bodies. If any is invalid, no body is returned
  * and the caller must leave the original sketch and bodies untouched.
  */
 public static List<Body> plan(int type,int plane,double x1,double y1,double x2,double y2,
                              double[] linkedHeights){
  if(linkedHeights==null||linkedHeights.length>500)throw new IllegalArgumentException("Invalid dependencies");
  List<Body> staged=new ArrayList<>();
  if(linkedHeights.length==0)build(type,plane,x1,y1,x2,y2,1);
  for(double h:linkedHeights)staged.add(build(type,plane,x1,y1,x2,y2,h));
  return Collections.unmodifiableList(staged);
 }
}
