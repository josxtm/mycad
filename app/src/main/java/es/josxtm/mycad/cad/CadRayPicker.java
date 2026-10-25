package es.josxtm.mycad.cad;
/** Ray/triangle intersections for transformed CAD preview meshes. */
public final class CadRayPicker {
 private CadRayPicker(){}
 public static final class Hit {
  public final int triangle;public final double distance,x,y,z;
  Hit(int triangle,double distance,double x,double y,double z){this.triangle=triangle;this.distance=distance;this.x=x;this.y=y;this.z=z;}
 }
 public static Hit pick(float[] triangles,double ox,double oy,double oz,double dx,double dy,double dz){
  if(triangles==null||triangles.length%9!=0||!Double.isFinite(ox)||!Double.isFinite(oy)||!Double.isFinite(oz)
   ||!Double.isFinite(dx)||!Double.isFinite(dy)||!Double.isFinite(dz)||dx*dx+dy*dy+dz*dz<1e-20)
   throw new IllegalArgumentException("Invalid ray or triangles");
  Hit best=null;
  for(int i=0;i<triangles.length;i+=9){
   double ax=triangles[i],ay=triangles[i+1],az=triangles[i+2];
   double e1x=triangles[i+3]-ax,e1y=triangles[i+4]-ay,e1z=triangles[i+5]-az;
   double e2x=triangles[i+6]-ax,e2y=triangles[i+7]-ay,e2z=triangles[i+8]-az;
   double px=dy*e2z-dz*e2y,py=dz*e2x-dx*e2z,pz=dx*e2y-dy*e2x;
   double det=e1x*px+e1y*py+e1z*pz;
   if(Math.abs(det)<1e-12)continue;
   double inv=1/det,tx=ox-ax,ty=oy-ay,tz=oz-az;
   double u=(tx*px+ty*py+tz*pz)*inv;
   if(u<0||u>1)continue;
   double qx=ty*e1z-tz*e1y,qy=tz*e1x-tx*e1z,qz=tx*e1y-ty*e1x;
   double v=(dx*qx+dy*qy+dz*qz)*inv;
   if(v<0||u+v>1)continue;
   double t=(e2x*qx+e2y*qy+e2z*qz)*inv;
   if(t<=1e-9||best!=null&&t>=best.distance)continue;
   best=new Hit(i/9,t,ox+t*dx,oy+t*dy,oz+t*dz);
  }
  return best;
 }
}
