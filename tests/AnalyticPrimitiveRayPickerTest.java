import es.josxtm.mycad.cad.*;

public final class AnalyticPrimitiveRayPickerTest {
 static void yes(boolean v,String msg){if(!v)throw new AssertionError(msg);}
 static void near(double a,double b,String msg){
  if(!Double.isFinite(a)||Math.abs(a-b)>1e-6*Math.max(1,Math.abs(b)))
   throw new AssertionError(msg+": "+a+" != "+b);
 }
 static void point(AnalyticPrimitiveRayPicker.Hit hit,double[] pos,String msg){
  yes(hit!=null,msg+" missing");
  near(hit.x,pos[0],msg+" X");near(hit.y,pos[1],msg+" Y");near(hit.z,pos[2],msg+" Z");
 }
 static void bad(Runnable op,String reason){
  try{op.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Bad ray accepted: "+reason);
 }
 static AnalyticPrimitiveRayPicker.Hit ray(PrimitiveBRepBody body,double[] o,double[] d){
  return AnalyticPrimitiveRayPicker.pick(body,o[0],o[1],o[2],d[0],d[1],d[2]);
 }
 static double[] minus(double[] a,double[] b){
  return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};
 }
 public static void main(String[] args){
  RigidTransform identity=new RigidTransform(0,0,0,0,0,0);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("B",20,10,8,identity);
  AnalyticPrimitiveRayPicker.Hit x=ray(box,new double[]{-30,0,4},new double[]{2,0,0});
  yes(x!=null&&x.faceId.equals("B/face/X_MIN"),"Exact box X face ID");
  near(x.distance,20,"World distance with scaled ray");
  point(x,new double[]{-10,0,4},"Box X hit");
  yes(ray(box,new double[]{-30,8,4},new double[]{1,0,0})==null,
      "Miss outside bounded face");
  yes(ray(box,new double[]{-30,0,12},new double[]{1,0,0})==null,
      "Miss above box");
  yes(ray(box,new double[]{0,0,4},new double[]{0,0,1}).faceId.equals("B/face/Z_MAX"),
      "Ray starts inside box and exits top");
  yes(ray(box,new double[]{-20,0,4},new double[]{-1,0,0})==null,
      "Box behind view rejected");

  PrimitiveBRepBody cyl=PrimitiveBRepBody.cylinder("C",20,8,identity);
  AnalyticPrimitiveRayPicker.Hit side=ray(cyl,new double[]{-40,0,4},new double[]{3,0,0});
  yes(side!=null&&side.faceId.equals("C/face/CYLINDER_SIDE"),"Exact curved side");
  near(side.distance,30,"Circular side distance");
  point(side,new double[]{-10,0,4},"Cylinder side hit");
  AnalyticPrimitiveRayPicker.Hit cap=ray(cyl,new double[]{0,0,30},new double[]{0,0,-2});
  yes(cap!=null&&cap.faceId.equals("C/face/CYLINDER_TOP"),"Top circle cap");
  near(cap.distance,22,"Cap hit distance");
  yes(ray(cyl,new double[]{11,0,30},new double[]{0,0,-1})==null,
      "Cap disk radius enforced");
  yes(ray(cyl,new double[]{-40,0,12},new double[]{1,0,0})==null,
      "Lateral height enforced");
  yes(ray(cyl,new double[]{0,0,4},new double[]{0,0,-1}).faceId.equals(
      "C/face/CYLINDER_BOTTOM"),"Interior exit via bottom");
  yes(ray(cyl,new double[]{-40,10,4},new double[]{1,0,0})!=null,
      "Tangent cylinder is a valid mathematical hit");
  yes(ray(cyl,new double[]{-40,10.00001,4},new double[]{1,0,0})==null,
      "Near-tangent ray outside the cylinder must not be selected");
  near(side.nx,-1,"Unrotated cylinder side outward normal X");
  near(side.ny,0,"Unrotated cylinder side outward normal Y");
  near(cap.nz,1,"Top cap outward normal");
  double[] normalCopy=side.normal();normalCopy[0]=123;
  near(side.normal()[0],-1,"Hit normal is a defensive copy");

  RigidTransform pose=new RigidTransform(23,-11,31,27,39,56);
  PrimitiveBRepBody rotatedBox=PrimitiveBRepBody.box("RB",20,10,8,pose);
  double[] boxPoint=pose.point(-10,0,4);
  double[] boxEye=pose.point(-30,0,4);
  double[] boxDir=minus(boxPoint,boxEye);
  AnalyticPrimitiveRayPicker.Hit rotatedHit=ray(rotatedBox,boxEye,boxDir);
  yes(rotatedHit!=null&&rotatedHit.faceId.equals("RB/face/X_MIN"),"Rotated box face");
  near(rotatedHit.distance,20,"Rotated box distance");
  point(rotatedHit,boxPoint,"Rotated box exact point");
  PrimitiveBRepBody rotatedCyl=PrimitiveBRepBody.cylinder("RC",20,8,pose);
  double[] cylPoint=pose.point(0,10,3);
  double[] cylEye=pose.point(0,32,3);
  AnalyticPrimitiveRayPicker.Hit rounded=ray(rotatedCyl,cylEye,minus(cylPoint,cylEye));
  yes(rounded!=null&&rounded.faceId.equals("RC/face/CYLINDER_SIDE"),
      "Rotated circle picked without tessellation");
  point(rounded,cylPoint,"Rotated cylinder circle");
  double[] expectedNormal=rotatedCyl.findFace(rounded.faceId)
   .outwardNormalAt(Math.PI/2);
  near(rounded.nx,expectedNormal[0],"Rotated curved normal X");
  near(rounded.ny,expectedNormal[1],"Rotated curved normal Y");
  near(rounded.nz,expectedNormal[2],"Rotated curved normal Z");
  PrimitiveBRepBody updated=rotatedCyl.offsetFace("RC/face/CYLINDER_SIDE",2);
  double[] futurePoint=pose.point(0,12,3);
  AnalyticPrimitiveRayPicker.Hit afterEdit=ray(updated,cylEye,minus(futurePoint,cylEye));
  yes(afterEdit!=null&&afterEdit.faceId.equals(rounded.faceId),
      "Persistent face IDs after editing");
  point(afterEdit,futurePoint,"Updated radius picked");

  // Ray directions remain stable near the allowed 10,000 mm primitive offset.
  // The standalone transform test also covers much larger translations.
  RigidTransform farPose=new RigidTransform(9000,-9000,8000,23,41,-37);
  RigidTransform farRotation=new RigidTransform(0,0,0,23,41,-37);
  PrimitiveBRepBody farBox=PrimitiveBRepBody.box("FAR",20,10,8,farPose);
  double[] farEye=farPose.point(-30,0,4);
  double[] farDirection=farRotation.point(1,0,0);
  AnalyticPrimitiveRayPicker.Hit farHit=ray(farBox,farEye,farDirection);
  yes(farHit!=null&&farHit.faceId.equals("FAR/face/X_MIN"),
      "Large-coordinate rotated box face remains selectable");
  yes(Math.abs(farHit.distance-20)<0.01,"Large-coordinate ray distance");
  double[] farExpected=farPose.point(-10,0,4);
  yes(Math.abs(farHit.x-farExpected[0])<0.01&&
      Math.abs(farHit.y-farExpected[1])<0.01&&
      Math.abs(farHit.z-farExpected[2])<0.01,
      "Large-coordinate ray world-space hit point");

  bad(()->AnalyticPrimitiveRayPicker.pick(null,0,0,0,1,0,0),"Null solid");
  bad(()->AnalyticPrimitiveRayPicker.pick(box,0,0,0,0,0,0),"Null direction");
  bad(()->AnalyticPrimitiveRayPicker.pick(box,0,0,Double.NaN,1,0,0),"NaN coordinate");
  bad(()->AnalyticPrimitiveRayPicker.pick(box,0,0,0,Double.POSITIVE_INFINITY,0,0),
      "Infinite direction");
  System.out.println("AnalyticPrimitiveRayPickerTest: OK");
 }
}
