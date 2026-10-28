import es.josxtm.mycad.cad.*;
import java.util.*;

/** Regression for Android touch-to-mesh ray bridge and analytic ray parity. */
public final class ViewportMeshRayPickerTest {
 private static void near(double actual,double expected){
  if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-6)
   throw new AssertionError("Expected "+expected+" got "+actual);
 }
 private static void reject(Runnable r){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid viewport accepted");
 }
 private static double[] identity(){
  return new double[]{1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
 }
 private static ViewportMeshRayPicker.Candidate triangle(float z){
  float[] vertices={-1,-1,z, 1,-1,z, 0,1,z};
  return new ViewportMeshRayPicker.Candidate(new MeshRayIndex(vertices),1);
 }
 private static ViewportMeshRayPicker.Hit pick(
     List<ViewportMeshRayPicker.Candidate> scene,double x,double y){
  return ViewportMeshRayPicker.pick(scene,identity(),200,100,x,y);
 }
 public static void main(String[] args){
  ViewportMeshRayPicker.Candidate far=triangle(2);
  ViewportMeshRayPicker.Candidate front=triangle(-.5f);
  ViewportMeshRayPicker.Candidate back=triangle(.5f);
  List<ViewportMeshRayPicker.Candidate> scene=Arrays.asList(far,front,back);
  ViewportMeshRayPicker.Hit hit=pick(scene,100,50);
  if(hit==null||hit.triangle!=1)throw new AssertionError("Wrong global triangle");
  near(hit.z,-.5);near(hit.distance,.5);
  near(hit.oz,-1);near(hit.dz,1);
  near(hit.ox,0);near(hit.oy,0);
  SteppedViewportRayPicker.Ray analytic=
    SteppedViewportRayPicker.ray(identity(),200,100,100,50);
  near(hit.ox,analytic.origin()[0]);
  near(hit.dz,analytic.direction()[2]);
  if(pick(Arrays.asList(far),100,50)!=null)
   throw new AssertionError("Far-clipped triangle was selectable");
  if(pick(Arrays.asList(triangle(-2)),100,50)!=null)
   throw new AssertionError("Near-clipped triangle was selectable");
  if(pick(Arrays.asList(front,front),100,50).triangle!=0)
   throw new AssertionError("Tie did not retain first triangle");
  if(pick(Arrays.asList(front,back),190,5)!=null)
   throw new AssertionError("Empty viewport region hit a triangle");
  if(pick(Collections.emptyList(),100,50)!=null)
   throw new AssertionError("Empty scene selected a face");
  double[] vp=identity(),before=vp.clone();
  pick(Arrays.asList(front,back),100,50);
  if(!Arrays.equals(vp,before))throw new AssertionError("Camera changed");
  reject(()->pick(null,100,50));
  reject(()->pick(Arrays.asList(front),-1,50));
  reject(()->new ViewportMeshRayPicker.Candidate(null,1));
  reject(()->new ViewportMeshRayPicker.Candidate(new MeshRayIndex(new float[0]),-1));
  System.out.println("ViewportMeshRayPickerTest: OK");
 }
}
