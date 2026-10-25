import es.josxtm.mycad.cad.*;
public final class CylinderBoxBooleanTest {
 static void ok(boolean yes,String label){if(!yes)throw new AssertionError(label);}
 static void near(double actual,double expected,double tol,String label){
  if(Math.abs(actual-expected)>tol)throw new AssertionError(label+" "+actual+" vs "+expected);
 }
 static CylinderBoxBoolean.Result fit(OrientedThroughHole.Axis axis,boolean rotated,boolean through){
  final double w=40,d=30,h=25,dia=8,cut=11;
  RigidTransform host=new RigidTransform(12,20,-5,rotated?23:0,rotated?-19:0,rotated?45:0);
  double low=axis==OrientedThroughHole.Axis.X?-w/2:axis==OrientedThroughHole.Axis.Y?-d/2:0;
  double high=axis==OrientedThroughHole.Axis.X?w/2:axis==OrientedThroughHole.Axis.Y?d/2:h;
  double below=through?low-2:high-cut;
  double above=high+3;
  double[] localBase=axis==OrientedThroughHole.Axis.X?new double[]{below,2,h/2-3}:
     axis==OrientedThroughHole.Axis.Y?new double[]{-3,below,h/2+2}:
                                               new double[]{2,-3,below};
  double[] tip=axis==OrientedThroughHole.Axis.X?new double[]{above,2,h/2-3}:
     axis==OrientedThroughHole.Axis.Y?new double[]{-3,above,h/2+2}:
                                               new double[]{2,-3,above};
  // Construct cutter rigid transform orientation from its axis direction:
  // for box rotations use a consistent basis composed with host pose.
  double[] worldStart=host.point(localBase[0],localBase[1],localBase[2]);
  double[] worldEnd=host.point(tip[0],tip[1],tip[2]);
  double[] direction={worldEnd[0]-worldStart[0],worldEnd[1]-worldStart[1],worldEnd[2]-worldStart[2]};
  double length=Math.hypot(Math.hypot(direction[0],direction[1]),direction[2]);
  double nx=direction[0]/length,ny=direction[1]/length,nz=direction[2]/length;
  double ry=Math.toDegrees(Math.atan2(nx,Math.hypot(ny,nz)));
  double rx=Math.toDegrees(Math.atan2(-ny,nz));
  // use rz=0; the cylinder's round symmetry makes local spin irrelevant.
  RigidTransform cutter=new RigidTransform(worldStart[0],worldStart[1],worldStart[2],rx,ry,0);
  // For a point along +Z, Euler X then Y gives direction (sin(ry)*cos(rx), -sin(rx), cos(ry)*cos(rx)).
  // Correct the construction to match all signs / quadrant.
  double cosRx=Math.hypot(nx,nz);
  rx=Math.toDegrees(Math.atan2(-ny,cosRx));
  ry=Math.toDegrees(Math.atan2(nx,nz));
  cutter=new RigidTransform(worldStart[0],worldStart[1],worldStart[2],rx,ry,0);
  CylinderBoxBoolean.Result result=CylinderBoxBoolean.calculate(w,d,h,host,dia,length,cutter);
  return result;
 }
 public static void main(String[] args){
  for(OrientedThroughHole.Axis axis:OrientedThroughHole.Axis.values()){
   for(boolean through:new boolean[]{false,true}){
    for(boolean rotated:new boolean[]{false,true}){
     CylinderBoxBoolean.Result r=fit(axis,rotated,through);
     ok(r.cut.axis==axis,"Axis match "+axis);
     ok(through?r.cut.drillDepth==0:Math.abs(r.cut.drillDepth-11)<0.0001,"Depth");
     TriangleMeshHealth.Report cut=TriangleMeshHealth.inspect(r.cut.regenerate());
     TriangleMeshHealth.Report intersect=TriangleMeshHealth.inspect(r.intersection);
     ok(cut.isWatertight(),"Closed cut "+axis);
     ok(intersect.isWatertight(),"Closed intersection "+axis);
     double len=axis==OrientedThroughHole.Axis.X?40:axis==OrientedThroughHole.Axis.Y?30:25;
     double removed=Math.PI*8*8/4*(through?len:11);
     near(r.intersectionVolume,removed,0.01,"Analytic intersection");
     near(intersect.signedVolume,removed,7,"Mesh intersection volume");
     near(cut.signedVolume,40*30*25-removed,8,"Cut volume");
    }
   }
  }
  boolean rejected=false;
  try{CylinderBoxBoolean.calculate(20,20,20,new RigidTransform(0,0,0,0,0,0),
      6,30,new RigidTransform(0,0,0,45,0,0));}catch(IllegalArgumentException e){rejected=true;}
  ok(rejected,"Angled cutter unexpectedly allowed");
  rejected=false;
  try{CylinderBoxBoolean.calculate(20,20,20,new RigidTransform(0,0,0,0,0,0),
      30,40,new RigidTransform(0,0,-10,0,0,0));}catch(IllegalArgumentException e){rejected=true;}
  ok(rejected,"Cutter outside cross section accepted");
  System.out.println("CylinderBoxBooleanTest: OK");
 }
}
