import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.FloatBuffer;

public final class RelativeSceneCoordinatesTest {
 private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 private static void near(double a,double b,double tolerance){check(Math.abs(a-b)<=tolerance,a+" != "+b);}
 private static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Invalid frame accepted");}
 private static double[] identity(){double[] m=new double[16];for(int i=0;i<16;i+=5)m[i]=1;return m;}
 public static void main(String[] args){
  for(double far:new double[]{-1e9,0,1e9}){
   double[] world={far,far,far,far+.01,far,far,far,far+.01,far};double[] original=world.clone();
   RelativeSceneCoordinates frame=RelativeSceneCoordinates.from(Arrays.asList(new double[0],world));
   float[] local=frame.triangles(1);near(local[3]-local[0],.01,2e-7);
   check(Arrays.equals(world,original),"Source changed");world[0]++;check(Arrays.equals(local,frame.triangles(1)),"Retained caller array");
   local[0]+=10;check(!Arrays.equals(local,frame.triangles(1)),"Leaked local array");
   check(frame.rayIndex(1)==frame.rayIndex(1),"Ray index rebuilt per tap");
   List<ViewportMeshRayPicker.Candidate> candidates=Arrays.asList(new ViewportMeshRayPicker.Candidate(frame.rayIndex(0),0),new ViewportMeshRayPicker.Candidate(frame.rayIndex(1),1));
   ViewportMeshRayPicker.Hit localHit=ViewportMeshRayPicker.pick(candidates,identity(),100,100,49.8,50.2);
   check(localHit!=null&&localHit.triangle==0,"Lost visible detail");
   ViewportMeshRayPicker.Hit hit=frame.worldHit(localHit);near(hit.x,far+.001,3e-7);near(hit.y,far+.001,3e-7);near(hit.z,far,1e-7);
   near(hit.dx,localHit.dx,0);near(hit.distance,localHit.distance,0);near(hit.oz,localHit.oz+frame.originZ,0);
   FloatVertexBufferCache cache=new FloatVertexBufferCache();Object owner=new Object();
   FloatBuffer buffer=frame.buffer(1,cache,owner);frame.buffer(1,cache,owner);check(cache.entries()==1&&cache.residentBytes()==36,"GPU cache churn");
   near(buffer.get(3)-buffer.get(0),.01,2e-7);cache.clear();check(cache.entries()==0,"Cache not invalidated");
   float[] focus=frame.localPoint(frame.originX,frame.originY,frame.originZ);near(focus[0],0,0);near(focus[2],0,0);
  }

  // Selected analytic edges must be rebased in double before float GPU output.
  double far=1e9;
  double[] small={far,far,far,far+.01,far,far,far,far+.01,far};
  RelativeSceneCoordinates overlay=RelativeSceneCoordinates.from(Arrays.asList(small));
  double[] edge={far,far,far,far+.01,far,far},unchanged=edge.clone();
  float[] exact=overlay.localPoints(edge);
  near(exact[3]-exact[0],.01,2e-7);
  check(Arrays.equals(edge,unchanged),"Precise overlay source changed");
  float[] legacy=overlay.localPoints(new float[]{(float)far,(float)far,(float)far,
                                               (float)(far+.01),(float)far,(float)far});
  check(legacy[3]==legacy[0],"Fixture must expose legacy world-float collapse");
  reject(()->overlay.localPoints((double[])null));
  reject(()->overlay.localPoints(new double[]{0,0}));
  reject(()->overlay.localPoints(new double[]{Double.NaN,0,0}));
  reject(()->overlay.localPoints(new double[]{1e300,0,0}));
  // A translated ray never changes front/back occlusion or global triangle IDs.
  double[] back={9,9,20,11,9,20,9,11,20},front={9,9,19,11,9,19,9,11,19};
  RelativeSceneCoordinates scene=RelativeSceneCoordinates.from(Arrays.asList(back,front));
  ViewportMeshRayPicker.Hit frontHit=ViewportMeshRayPicker.pick(Arrays.asList(new ViewportMeshRayPicker.Candidate(scene.rayIndex(0),1),new ViewportMeshRayPicker.Candidate(scene.rayIndex(1),1)),identity(),100,100,40,60);
  check(frontHit!=null&&scene.worldHit(frontHit).triangle==1,"Occlusion changed");
  check(scene.worldHit(null)==null,"Miss translated into hit");
  reject(()->RelativeSceneCoordinates.from(null));reject(()->RelativeSceneCoordinates.from(Arrays.asList(new double[3])));
  reject(()->RelativeSceneCoordinates.from(Arrays.asList(new double[]{Double.NaN,0,0,0,0,0,0,0,0})));
  reject(()->RelativeSceneCoordinates.from(Arrays.asList(new double[]{Double.MAX_VALUE,0,0,-Double.MAX_VALUE,0,0,0,0,0})));
  check(RelativeSceneCoordinates.from(Collections.emptyList()).originX==0,"Empty scene origin");
  System.out.println("RelativeSceneCoordinatesTest: OK");
 }
}
