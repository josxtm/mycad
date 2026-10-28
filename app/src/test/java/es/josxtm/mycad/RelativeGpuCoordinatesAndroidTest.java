package es.josxtm.mycad;

import es.josxtm.mycad.cad.*;
import android.opengl.Matrix;
import android.os.Looper;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Exercises the renderer's actual scene installation, Android camera matrices
 * and callbacks without claiming EGL or physical-device validation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@LooperMode(LooperMode.Mode.PAUSED)
public class RelativeGpuCoordinatesAndroidTest {
 private MeshEngine viewer;
 private org.robolectric.android.controller.ActivityController<android.app.Activity> controller;
 @Before public void setup(){controller=Robolectric.buildActivity(android.app.Activity.class).setup();viewer=new MeshEngine(controller.get());controller.get().setContentView(viewer);viewer.layout(0,0,800,600);}
 @After public void teardown(){controller.pause().stop().destroy();}
 private Object invoke(String name,Class<?>[] types,Object... args)throws Exception{
  Method m=MeshEngine.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(viewer,args);
 }
 private Object field(String name)throws Exception{Field f=MeshEngine.class.getDeclaredField(name);f.setAccessible(true);return f.get(viewer);}
 private void install(List<MeshEngine.Mesh> meshes,boolean fit)throws Exception{
  List<double[]> world=new ArrayList<>();for(MeshEngine.Mesh mesh:meshes)world.add(mesh.worldCoordinates());
  viewer.installSceneMeshes(meshes,RelativeSceneCoordinates.from(world),fit,viewer.sceneRevision());
 }
 private MeshEngine.Mesh box(double x,double y,double z,double width){
  return MeshEngine.Mesh.analyticBox(BoxBRep.from(new TransformedBody(new CadKernel.Box("test-box",x,y,z,width,2,2),new RigidTransform(0,0,0,0,0,0))));
 }
 @Test public void accurateBoxSourceSurvivesGpuConversionAndLegacyBufferRemainsUnchanged()throws Exception{
  MeshEngine.Mesh mesh=box(9000,9000,9000,.001);float[] legacy=mesh.vertices.clone();
  install(Collections.singletonList(mesh),true);
  RelativeSceneCoordinates frame=(RelativeSceneCoordinates)field("coordinates");
  float[] local=frame.triangles(0);float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY;
  for(int i=0;i<local.length;i+=3){lo=Math.min(lo,local[i]);hi=Math.max(hi,local[i]);}
  assertEquals(.001,hi-lo,1e-8);assertArrayEquals(legacy,mesh.vertices,0);
  assertEquals(9000.0005,(Double)field("centerX"),1e-9);
 }
 @Test public void cameraAndHandleProjectionAreInvariantUnderSceneTranslation()throws Exception{
  viewer.setViewOrientation(35,25);install(Collections.singletonList(box(0,0,0,2)),true);
  float[] screen=(float[])invoke("projectPoint",new Class<?>[]{double.class,double.class,double.class},1.0,1.0,1.0);
  install(Collections.singletonList(box(9000,-8000,7000,2)),true);
  float[] shifted=(float[])invoke("projectPoint",new Class<?>[]{double.class,double.class,double.class},9001.0,-7999.0,7001.0);
  assertNotNull(screen);assertArrayEquals(screen,shifted,1e-3f);
  RelativeSceneCoordinates frame=(RelativeSceneCoordinates)field("coordinates");
  assertArrayEquals(new float[]{-1,-1,-1,1,1,1},frame.localPoints(new float[]{9000,-8000,7000,9002,-7998,7002}),0);
 }
 @Test public void rebasingAnEditedScenePreservesWorldFocusDistanceAndOrientation()throws Exception{
  install(Collections.singletonList(box(100,200,300,2)),true);viewer.setViewOrientation(70,40);viewer.zoom(2);
  double focus=(Double)field("centerX");float distance=viewer.cameraDistance();
  install(Collections.singletonList(box(110,220,330,4)),false);
  assertEquals(focus,(Double)field("centerX"),0);assertEquals(distance,viewer.cameraDistance(),0);
  assertEquals(70,(Float)field("yaw"),0);assertEquals(40,(Float)field("pitch"),0);
  assertEquals(112,((RelativeSceneCoordinates)field("coordinates")).originX,0);
 }
 @Test public void actualViewportPickPublishesWorldRayAndRejectsPriorGeneration()throws Exception{
  install(Collections.singletonList(box(9000,8000,7000,2)),true);viewer.setViewOrientation(35,25);
  float[] projection=(float[])field("projection");Matrix.perspectiveM(projection,0,45,800f/600,.1f,1000000);
  final double[][] received={null};
  viewer.setSurfaceRayListener((triangle,x,y,z,ox,oy,oz,dx,dy,dz)->received[0]=new double[]{triangle,x,y,z,ox,oy,oz,dx,dy,dz});
  invoke("pickSurfaceAt",new Class<?>[]{float.class,float.class},400f,300f);shadowOf(Looper.getMainLooper()).idle();
  assertNotNull(received[0]);assertTrue(received[0][1]>=9000&&received[0][1]<=9002);
  assertTrue(received[0][2]>=8000&&received[0][2]<=8002);assertTrue(received[0][3]>=7000&&received[0][3]<=7002);
  assertTrue(received[0][4]>9000);assertEquals(1,Math.sqrt(received[0][7]*received[0][7]+received[0][8]*received[0][8]+received[0][9]*received[0][9]),1e-6);
  received[0]=null;invoke("pickSurfaceAt",new Class<?>[]{float.class,float.class},400f,300f);
  viewer.previewMesh(box(9000,8000,7000,3));shadowOf(Looper.getMainLooper()).idle();assertNull(received[0]);
 }
 @Test public void invalidSceneIsRejectedBeforeRevisionChanges(){
  long before=viewer.sceneRevision();
  try{viewer.setMeshes(Collections.singletonList(new MeshEngine.Mesh(new float[]{Float.NaN,0,0,0,1,0,1,0,0})));fail();}
  catch(IllegalArgumentException expected){}
  assertEquals(before,viewer.sceneRevision());
 }
}
