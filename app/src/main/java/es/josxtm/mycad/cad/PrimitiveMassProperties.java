package es.josxtm.mycad.cad;

/**
 * Mass and centroidal inertia for homogeneous analytic primitives.
 *
 * Density: g/cm^3; dimensions: mm; mass: g; center: world mm;
 * inertia about the center of mass: g*mm^2. Rotation is the existing
 * X-then-Y-then-Z pose; translation does not affect centroidal inertia.
 *
 * Does not describe imported meshes, Boolean results or hollowed geometry.
 */
public final class PrimitiveMassProperties {
 public final String bodyId;
 public final double densityGramsPerCm3,volumeMm3,areaMm2,massGrams;
 private final double[] center;
 private final double[][] inertia;

 private PrimitiveMassProperties(PrimitiveBRepBody body,double density){
  if(body==null||!Double.isFinite(density)||density<=0||density>1e6)
   throw new IllegalArgumentException("Invalid body or material density");
  bodyId=body.bodyId;
  densityGramsPerCm3=density;
  volumeMm3=body.volume();
  areaMm2=body.surfaceArea();
  massGrams=volumeMm3*density/1000d;
  if(!Double.isFinite(massGrams)||massGrams<=0||
     !Double.isFinite(areaMm2)||areaMm2<=0)
   throw new IllegalArgumentException("Invalid primitive mass");
  center=body.pose().point(0,0,body.height/2d);
  double w=body.width,d=body.depth,h=body.height;
  double[] diagonal=new double[3];
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   diagonal[0]=massGrams*(d*d+h*h)/12d;
   diagonal[1]=massGrams*(w*w+h*h)/12d;
   diagonal[2]=massGrams*(w*w+d*d)/12d;
  }else{
   double r=w/2d;
   diagonal[0]=massGrams*(3*r*r+h*h)/12d;
   diagonal[1]=diagonal[0];
   diagonal[2]=massGrams*r*r/2d;
  }
  // Each unit local axis transformed without translating the vector.
  double[] origin=body.pose().point(0,0,0);
  double[][] localAxes={{1,0,0},{0,1,0},{0,0,1}};
  double[][] columns=new double[3][3];
  for(int axis=0;axis<3;axis++){
   double[] p=body.pose().point(localAxes[axis][0],
                               localAxes[axis][1],localAxes[axis][2]);
   for(int xyz=0;xyz<3;xyz++)columns[xyz][axis]=p[xyz]-origin[xyz];
  }
  inertia=new double[3][3];
  for(int i=0;i<3;i++)for(int j=0;j<3;j++)
   for(int k=0;k<3;k++)
    inertia[i][j]+=columns[i][k]*diagonal[k]*columns[j][k];
  for(int i=0;i<3;i++){
   if(!Double.isFinite(center[i]))throw new IllegalArgumentException("Invalid centroid");
   for(int j=0;j<3;j++)
    if(!Double.isFinite(inertia[i][j]))
     throw new IllegalArgumentException("Invalid inertia tensor");
  }
 }
 public static PrimitiveMassProperties of(PrimitiveBRepBody body,double densityGramsPerCm3){
  return new PrimitiveMassProperties(body,densityGramsPerCm3);
 }
 /** Center of mass in world XYZ millimetres. */
 public double[] centroidMm(){return center.clone();}
 /** Symmetric 3x3 mass inertia tensor about the centroid (g*mm^2). */
 public double[][] inertiaGramsMm2(){
  double[][] out=new double[3][];
  for(int i=0;i<3;i++)out[i]=inertia[i].clone();
  return out;
 }
}
