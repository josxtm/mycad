import es.josxtm.mycad.cad.*;
import java.util.*;

public final class AnalyticSceneMeshTest {
 static void ok(boolean good,String name){if(!good)throw new AssertionError(name);}
 static void near(float a,float b,String name){
  if(!Float.isFinite(a)||Math.abs(a-b)>1e-4*Math.max(1,Math.abs(b)))
   throw new AssertionError(name+" "+a+" != "+b);
 }
 static void reject(Runnable run,String label){
  try{run.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid scene accepted: "+label);
 }
 static void check(float[] mesh,int at,float[] reference,String name){
  for(int k=0;k<reference.length;k++)near(mesh[at+k],reference[k],name+"["+k+"]");
 }
 public static void main(String[] args){
  RigidTransform boxPose=new RigidTransform(9,-18,4,30,20,61);
  RigidTransform cylinderPose=new RigidTransform(-17,6,21,42,16,-29);
  PrimitiveBRepBody box=PrimitiveBRepBody.box("box-uuid",20,10,8,boxPose);
  PrimitiveBRepBody cylinder=PrimitiveBRepBody.cylinder("cyl-uuid",12,6,cylinderPose);
  float[] prefix={-2,0,0, 1,0,0, -1,1,0};
  float[] suffix={1,1,1, 2,1,1, 1,2,1};
  float[] expectedBox=box.triangles(48);
  float[] expectedCyl=cylinder.triangles(48);
  float[] scene=AnalyticSceneMesh.compose(
    Arrays.asList(prefix),Arrays.asList(box,cylinder),Arrays.asList(suffix),48);
  ok(scene.length==prefix.length+expectedBox.length+expectedCyl.length+suffix.length,
     "All segments in one contiguous mesh");
  check(scene,0,prefix,"Imported mesh first");
  check(scene,prefix.length,expectedBox,"Rotated B-Rep box pose");
  check(scene,prefix.length+expectedBox.length,expectedCyl,"XYZ cylinder pose");
  check(scene,scene.length-suffix.length,suffix,"Feature mesh last");
  near(scene[prefix.length],expectedBox[0],"First rotated primitive");
  float[] second=AnalyticSceneMesh.compose(
    Arrays.asList(prefix),Arrays.asList(box,cylinder),Arrays.asList(suffix),48);
  ok(Arrays.equals(scene,second),"Deterministic output");
  scene[0]=987654;
  ok(AnalyticSceneMesh.compose(Arrays.asList(prefix),Arrays.asList(box,cylinder),
     Arrays.asList(suffix),48)[0]!=987654,"Returned array is independent");
  ok(prefix[0]==-2,"Caller prefix unchanged");

  PrimitiveBRepBody taller=cylinder.offsetFace("cyl-uuid/face/CYLINDER_TOP",2);
  float[] edited=AnalyticSceneMesh.compose(Collections.<float[]>emptyList(),
    Arrays.asList(box,taller),Collections.<float[]>emptyList(),48);
  check(edited,expectedBox.length,taller.triangles(48),"Preview uses updated body");
  check(edited,0,expectedBox,"Unaffected body unchanged");
  ok(AnalyticSceneMesh.compose(Collections.<float[]>emptyList(),
       Collections.<PrimitiveBRepBody>emptyList(),
       Collections.<float[]>emptyList(),48).length==0,"Empty scene allowed");
  reject(()->AnalyticSceneMesh.compose(null,Arrays.asList(box),
         Collections.<float[]>emptyList(),48),"Missing prefix");
  reject(()->AnalyticSceneMesh.compose(Collections.<float[]>emptyList(),
         Arrays.asList(box,box),Collections.<float[]>emptyList(),48),"Duplicate stable IDs");
  reject(()->AnalyticSceneMesh.compose(Collections.<float[]>emptyList(),
         Arrays.asList(box,(PrimitiveBRepBody)null),
         Collections.<float[]>emptyList(),48),"Null body");
  reject(()->AnalyticSceneMesh.compose(Arrays.asList(new float[8]),
         Arrays.asList(box),Collections.<float[]>emptyList(),48),"Incomplete triangle");
  reject(()->AnalyticSceneMesh.compose(Arrays.asList(
         new float[]{0,0,0,1,0,0,0,Float.NaN,0}),
         Arrays.asList(box),Collections.<float[]>emptyList(),48),"NaN coordinates");
  reject(()->AnalyticSceneMesh.compose(Collections.<float[]>emptyList(),
         Arrays.asList(box),Collections.<float[]>emptyList(),8),"Invalid cylinder resolution");
  reject(()->AnalyticSceneMesh.compose(Collections.<float[]>emptyList(),
         Arrays.asList(box),Collections.<float[]>emptyList(),129),"High tessellation");
  System.out.println("AnalyticSceneMeshTest: OK");
 }
}
