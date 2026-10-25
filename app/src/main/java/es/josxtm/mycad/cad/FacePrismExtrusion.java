package es.josxtm.mycad.cad;
/** Adds a separate face-attached cuboid (not a Boolean union). Geometry in mm. */
public final class FacePrismExtrusion {
 private FacePrismExtrusion(){}
 public static final class Result {
  public final double w,d,h,x,y,z;
  Result(double w,double d,double h,double x,double y,double z){this.w=w;this.d=d;this.h=h;this.x=x;this.y=y;this.z=z;}
 }
 public static Result create(double w,double d,double h,double x,double y,double z,double rx,double ry,double rz,
                             BoxFaceSelection.Face face,double distance){
  if(face==null||!Double.isFinite(distance)||distance<=0||distance>10000||
    !Double.isFinite(w)||!Double.isFinite(d)||!Double.isFinite(h)||w<=0||d<=0||h<=0)
   throw new IllegalArgumentException("Invalid face extrusion");
  double nw=w,nd=d,nh=h,lx=0,ly=0,lz=0;
  switch(face){
   case X_MIN:nw=distance;lx=-(w+distance)/2;break;
   case X_MAX:nw=distance;lx=(w+distance)/2;break;
   case Y_MIN:nd=distance;ly=-(d+distance)/2;break;
   case Y_MAX:nd=distance;ly=(d+distance)/2;break;
   case Z_MIN:nh=distance;lz=-distance;break;
   case Z_MAX:nh=distance;lz=h;break;
  }
  double[] t=new RigidTransform(0,0,0,rx,ry,rz).point(lx,ly,lz);
  double nx=x+t[0],ny=y+t[1],nz=z+t[2];
  if(!Double.isFinite(nx)||!Double.isFinite(ny)||!Double.isFinite(nz)||Math.abs(nx)>10000||Math.abs(ny)>10000||Math.abs(nz)>10000)
   throw new IllegalArgumentException("Extrusion beyond world limits");
  return new Result(nw,nd,nh,nx,ny,nz);
 }
}