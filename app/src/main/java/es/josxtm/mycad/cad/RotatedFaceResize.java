package es.josxtm.mycad.cad;
/** Moves a cuboid face in its local coordinate frame, retaining the opposite face in world space. */
public final class RotatedFaceResize {
 private RotatedFaceResize(){}
 public static FaceResize.Dimensions move(double w,double d,double h,double x,double y,double z,
   double rx,double ry,double rz,BoxFaceSelection.Face face,double millimeters){
  FaceResize.Dimensions local=FaceResize.move(w,d,h,0,0,0,face,millimeters);
  double[] offset=new RigidTransform(0,0,0,rx,ry,rz).point(local.x,local.y,local.z);
  double nx=x+offset[0],ny=y+offset[1],nz=z+offset[2];
  if(!Double.isFinite(nx)||!Double.isFinite(ny)||!Double.isFinite(nz)||
    Math.abs(nx)>10000||Math.abs(ny)>10000||Math.abs(nz)>10000)
   throw new IllegalArgumentException("Invalid world position");
  return new FaceResize.Dimensions(local.w,local.d,local.h,nx,ny,nz);
 }
}
