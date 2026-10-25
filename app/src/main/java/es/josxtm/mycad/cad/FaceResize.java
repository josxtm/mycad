package es.josxtm.mycad.cad;
/** Resize an axis-aligned prism by moving one face outward (negative values move inward). */
public final class FaceResize {
 private FaceResize(){}
 public static final class Dimensions {
  public final double w,d,h,x,y,z;
  Dimensions(double w,double d,double h,double x,double y,double z){this.w=w;this.d=d;this.h=h;this.x=x;this.y=y;this.z=z;}
 }
 public static Dimensions move(double w,double d,double h,double x,double y,double z,BoxFaceSelection.Face face,double mm){
  if(face==null||!Double.isFinite(mm)||!Double.isFinite(w)||!Double.isFinite(d)||!Double.isFinite(h)||
     !Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||w<=0||d<=0||h<=0)
   throw new IllegalArgumentException("Invalid face resize");
  switch(face){
   case X_MIN: w+=mm;x-=mm/2;break;
   case X_MAX: w+=mm;x+=mm/2;break;
   case Y_MIN: d+=mm;y-=mm/2;break;
   case Y_MAX: d+=mm;y+=mm/2;break;
   case Z_MIN: h+=mm;z-=mm;break;
   case Z_MAX: h+=mm;break;
  }
  if(w<=0||d<=0||h<=0||w>10000||d>10000||h>10000||
    Math.abs(x)>10000||Math.abs(y)>10000||Math.abs(z)>10000)
   throw new IllegalArgumentException("Resize outside supported limits");
  return new Dimensions(w,d,h,x,y,z);
 }
}