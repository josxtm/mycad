package es.josxtm.mycad.cad;

/** Immutable rigid 3D transform: rotations around X, Y, Z then translation. Angles in degrees. */
public final class RigidTransform {
 public final double tx,ty,tz,rx,ry,rz;
 public RigidTransform(double tx,double ty,double tz,double rx,double ry,double rz){
  if(!Double.isFinite(tx)||!Double.isFinite(ty)||!Double.isFinite(tz)||
     !Double.isFinite(rx)||!Double.isFinite(ry)||!Double.isFinite(rz))
   throw new IllegalArgumentException("Nonfinite transform");
  this.tx=tx;this.ty=ty;this.tz=tz;this.rx=rx;this.ry=ry;this.rz=rz;
 }
 public double[] point(double x,double y,double z){
  double ax=Math.toRadians(rx),ay=Math.toRadians(ry),az=Math.toRadians(rz);
  double cx=Math.cos(ax),sx=Math.sin(ax),cy=Math.cos(ay),sy=Math.sin(ay),cz=Math.cos(az),sz=Math.sin(az);
  double y1=y*cx-z*sx,z1=y*sx+z*cx;
  double x2=x*cy+z1*sy,z2=-x*sy+z1*cy;
  return new double[]{x2*cz-y1*sz+tx,x2*sz+y1*cz+ty,z2+tz};
 }
 public double[][] vertices(CadKernel.Box box){
  if(box==null)throw new IllegalArgumentException("Box required");
  double[][] v=new double[8][];int n=0;
  for(int i=0;i<2;i++)for(int j=0;j<2;j++)for(int k=0;k<2;k++)
   v[n++]=point(box.x+i*box.width,box.y+j*box.depth,box.z+k*box.height);
  return v;
 }
 public double[] bounds(CadKernel.Box box){
  double[] out={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
  for(double[] v:vertices(box))for(int i=0;i<3;i++){out[i]=Math.min(out[i],v[i]);out[i+3]=Math.max(out[i+3],v[i]);}
  return out;
 }
}
