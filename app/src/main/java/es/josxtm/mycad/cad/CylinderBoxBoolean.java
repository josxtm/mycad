package es.josxtm.mycad.cad;

/** Restricted Boolean between a rigid rectangular prism and an existing cylindrical cutter.
 * The cutter must be coaxial with one local axis, completely fit inside its cross-section,
 * and enter from the positive local face. Otherwise the operation is rejected.
 * It returns both a regenerable cut recipe and a closed intersection mesh.
 */
public final class CylinderBoxBoolean {
 private CylinderBoxBoolean(){}
 private static final double TOL=0.0001;
 public static final class Result {
  public final DrilledBoxRecipe cut;
  public final float[] intersection;
  public final double intersectionVolume;
  Result(DrilledBoxRecipe cut,float[] intersection,double volume){
   this.cut=cut;this.intersection=intersection;this.intersectionVolume=volume;
  }
 }
 public static Result calculate(double w,double d,double h,RigidTransform boxPose,
                                double diameter,double cylinderHeight,RigidTransform cylinderPose){
  if(boxPose==null||cylinderPose==null||
     !Double.isFinite(w)||!Double.isFinite(d)||!Double.isFinite(h)||
     !Double.isFinite(diameter)||!Double.isFinite(cylinderHeight)||
     w<=0||d<=0||h<=0||diameter<=0||cylinderHeight<=0||
     w>10000||d>10000||h>10000||diameter>10000||cylinderHeight>10000)
   throw new IllegalArgumentException("Invalid solids");
  double[] base=cylinderPose.point(0,0,0),end=cylinderPose.point(0,0,cylinderHeight);
  double[] localBase=boxPose.inversePoint(base[0],base[1],base[2]);
  double[] localEnd=boxPose.inversePoint(end[0],end[1],end[2]);
  double[] delta={localEnd[0]-localBase[0],localEnd[1]-localBase[1],localEnd[2]-localBase[2]};
  int axis=0;
  for(int i=1;i<3;i++)if(Math.abs(delta[i])>Math.abs(delta[axis]))axis=i;
  if(Math.abs(Math.abs(delta[axis])-cylinderHeight)>TOL||
     Math.abs(delta[(axis+1)%3])>TOL||Math.abs(delta[(axis+2)%3])>TOL)
   throw new IllegalArgumentException("Cylinder must be aligned with a local box axis");
  double lo=axis==0?-w/2:axis==1?-d/2:0;
  double hi=axis==0?w/2:axis==1?d/2:h;
  double segmentLo=Math.min(localBase[axis],localEnd[axis]);
  double segmentHi=Math.max(localBase[axis],localEnd[axis]);
  if(segmentHi<hi-TOL||segmentLo>=hi-TOL)
   throw new IllegalArgumentException("Cutter must enter from the positive local face");
  double insideLo=Math.max(lo,segmentLo),insideHi=hi;
  if(insideHi-insideLo<=TOL)throw new IllegalArgumentException("No material cut");
  double depth=segmentLo<=lo+TOL?0:hi-segmentLo;
  double u=axis==0?localBase[1]:axis==1?localBase[2]-h/2:localBase[0];
  double v=axis==0?localBase[2]-h/2:axis==1?localBase[0]:localBase[1];
  // Both cylinder endpoints must have the same cross-sectional center.
  double uEnd=axis==0?localEnd[1]:axis==1?localEnd[2]-h/2:localEnd[0];
  double vEnd=axis==0?localEnd[2]-h/2:axis==1?localEnd[0]:localEnd[1];
  if(Math.abs(u-uEnd)>TOL||Math.abs(v-vEnd)>TOL)
   throw new IllegalArgumentException("Offset cylinder axis");
  OrientedThroughHole.Axis choice=axis==0?OrientedThroughHole.Axis.X:
                                   axis==1?OrientedThroughHole.Axis.Y:OrientedThroughHole.Axis.Z;
  DrilledBoxRecipe cut=new DrilledBoxRecipe(boxPose.tx,boxPose.ty,boxPose.tz,w,d,h,u,v,diameter,
      choice,boxPose.rx,boxPose.ry,boxPose.rz,depth);
  double overlap=insideHi-insideLo;
  float[] localMesh=CylinderSurface.triangles(diameter,overlap,
      new RigidTransform(0,0,0,0,0,0),64);
  float[] surface=new float[localMesh.length];
  for(int i=0;i<localMesh.length;i+=3){
   double a=localMesh[i],b=localMesh[i+1],t=localMesh[i+2]+insideLo;
   double x=axis==0?t:axis==1?b:a;
   double y=axis==0?a:axis==1?t:b;
   double z=axis==0?b+h/2:axis==1?a+h/2:t;
   double[] world=boxPose.point(x,y,z);
   surface[i]=(float)world[0];surface[i+1]=(float)world[1];surface[i+2]=(float)world[2];
  }
  return new Result(cut,surface,Math.PI*diameter*diameter/4*overlap);
 }
}
