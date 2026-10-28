package es.josxtm.mycad.cad;

/**
 * Geometric ray selection of a stepped shell's nearest visible analytic patch.
 * Ray coordinates use the same world space as the shell's preview triangles.
 * This is a kernel helper, not yet connected to Android screen-to-ray input.
 */
public final class SteppedPatchRayPicker {
 private SteppedPatchRayPicker(){}

 public static final class Hit {
  public final int triangleIndex;
  public final double distance;
  public final SteppedCoplanarPatchTopology.PatchFace patch;
  private final double[] point;
  private Hit(int triangleIndex,double distance,
      SteppedCoplanarPatchTopology.PatchFace patch,double[] point){
   this.triangleIndex=triangleIndex;this.distance=distance;
   this.patch=patch;this.point=point.clone();
  }
  /** Defensive copy of the world-space intersection. */
  public double[] point(){return point.clone();}
 }

 private static double[] vector(double[] value,String label){
  if(value==null||value.length!=3)
   throw new IllegalArgumentException("Expected 3D "+label);
  for(double component:value)
   if(!Double.isFinite(component))
    throw new IllegalArgumentException("Nonfinite "+label);
  return value.clone();
 }
 private static double[] cross(double[] a,double[] b){
  return new double[]{a[1]*b[2]-a[2]*b[1],
      a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};
 }
 private static double dot(double[] a,double[] b){
  return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
 }

 /**
  * Returns the nearest forward intersection, or null for a miss.
  * Direction need not be normalized; distance is in world-space units.
  * Both sides of a face are selectable. Exact shared-edge ties resolve
  * deterministically to the first triangle in preview order.
  */
 public static Hit pick(VariableHeightOrthogonalBRep shell,
     SteppedCoplanarPatchTopology.Topology topology,
     double[] rayOrigin,double[] rayDirection){
  if(shell==null||topology==null)
   throw new IllegalArgumentException("Missing shell or patch topology");
  if(!shell.id().equals(topology.shellId))
   throw new IllegalArgumentException("Patch topology belongs to another shell");
  double[] origin=vector(rayOrigin,"ray origin");
  double[] direction=vector(rayDirection,"ray direction");
  double norm=Math.hypot(Math.hypot(direction[0],direction[1]),direction[2]);
  if(!(norm>0)||!Double.isFinite(norm))
   throw new IllegalArgumentException("Zero ray direction");
  for(int axis=0;axis<3;axis++)direction[axis]/=norm;
  float[] triangles=shell.triangles();
  if(triangles.length!=(long)shell.faces().size()*18)
   throw new IllegalArgumentException("Preview/source face mapping changed");

  int best=-1;
  double bestDistance=Double.POSITIVE_INFINITY;
  for(int triangle=0;triangle<triangles.length/9;triangle++){
   int at=triangle*9;
   double[] a={triangles[at],triangles[at+1],triangles[at+2]};
   double[] e1={triangles[at+3]-a[0],triangles[at+4]-a[1],
       triangles[at+5]-a[2]};
   double[] e2={triangles[at+6]-a[0],triangles[at+7]-a[1],
       triangles[at+8]-a[2]};
   double[] p=cross(direction,e2);
   double determinant=dot(e1,p);
   double[] areaVector=cross(e1,e2);
   double areaTwice=Math.sqrt(dot(areaVector,areaVector));
   if(!(areaTwice>0)||!Double.isFinite(areaTwice))
    throw new IllegalArgumentException("Degenerate preview triangle");
   if(Math.abs(determinant)<=1e-12*areaTwice)continue;
   double inverse=1/determinant;
   double[] fromVertex={origin[0]-a[0],origin[1]-a[1],origin[2]-a[2]};
   double u=dot(fromVertex,p)*inverse;
   if(u< -1e-9||u>1+1e-9)continue;
   double[] q=cross(fromVertex,e1);
   double v=dot(direction,q)*inverse;
   if(v< -1e-9||u+v>1+1e-9)continue;
   double distance=dot(e2,q)*inverse;
   if(!Double.isFinite(distance)||distance<0)continue;
   if(distance<bestDistance-1e-9*Math.max(1,bestDistance==Double.POSITIVE_INFINITY?1:bestDistance)){
    best=triangle;bestDistance=distance;
   }
  }
  if(best<0)return null;
  SteppedCoplanarPatchTopology.PatchFace patch=
      SteppedPatchTrianglePicker.resolve(shell,topology,best);
  return new Hit(best,bestDistance,patch,
      new double[]{origin[0]+direction[0]*bestDistance,
          origin[1]+direction[1]*bestDistance,
          origin[2]+direction[2]*bestDistance});
 }
}
