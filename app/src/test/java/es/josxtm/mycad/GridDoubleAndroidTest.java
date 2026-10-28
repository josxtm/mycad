package es.josxtm.mycad;

import es.josxtm.mycad.cad.*;
import android.opengl.Matrix;
import android.os.Looper;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Actual Activity regeneration/codec and GPU camera bridge, without EGL/device claims. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@LooperMode(LooperMode.Mode.PAUSED)
public class GridDoubleAndroidTest {
 private ActivityController<MainActivity> controller;private MainActivity app;
 @Before public void setup(){controller=Robolectric.buildActivity(MainActivity.class);app=controller.get();app.getPreferences(0).edit().clear().commit();controller.setup();}
 @After public void teardown(){controller.pause().stop().destroy();}
 private void clear(){
  app.parts.clear();app.sketches.clear();app.booleanMeshes.clear();app.unionRecipes.clear();app.holeRecipes.clear();
  app.loopExtrusionFeatures.clear();app.intersectionRecipes.clear();app.extrusionChains.clear();app.chainFeatures.clear();
  app.undo.clear();app.redo.clear();
 }
 private LoopExtrusionFeature rectangle(int plane,double h,float x,float y,float size){
  float[][] p={{x,y},{x+size,y},{x+size,y+size},{x,y+size}};List<String> ids=new ArrayList<>();
  for(int i=0;i<4;i++){
   MainActivity.Sketch line=app.new Sketch(1,p[i][0],p[i][1],p[(i+1)%4][0],p[(i+1)%4][1]);line.plane=plane;app.sketches.add(line);ids.add(line.id);
  }
  LoopExtrusionFeature f=new LoopExtrusionFeature(UUID.randomUUID().toString(),ids.get(0),plane,h,ids);
  app.booleanMeshes.add(new MeshEngine.Mesh(f.regenerate(app.loopSketchSegments(f,null,0,0,0,0)).triangles));
  app.loopExtrusionFeatures.add(f);app.intersectionRecipes.add(null);app.unionRecipes.add(null);app.holeRecipes.add(null);return f;
 }
 private void fixture(int plane,double h){
  clear();LoopExtrusionFeature a=rectangle(plane,h,9000,8000,8),b=rectangle(plane,h,9002,8002,4);
  LinkedConvexIntersection recipe=new LinkedConvexIntersection(UUID.randomUUID().toString(),a.id,b.id,LinkedConvexIntersection.Operation.DIFFERENCE_WITH_HOLES);
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>(app.loopSketchSegments(a,null,0,0,0,0));lines.putAll(app.loopSketchSegments(b,null,0,0,0,0));
  app.booleanMeshes.add(new MeshEngine.Mesh(recipe.regenerateTriangles(app.intersectionSourceMap(null),lines)));
  app.intersectionRecipes.add(recipe);app.loopExtrusionFeatures.add(null);app.unionRecipes.add(null);app.holeRecipes.add(null);
 }
 private Object field(String name)throws Exception{Field f=MeshEngine.class.getDeclaredField(name);f.setAccessible(true);return f.get(app.meshView);}
 private void awaitSceneCommands()throws Exception{
  CountDownLatch complete=new CountDownLatch(1);app.meshView.queueEvent(complete::countDown);
  assertTrue("Renderer scene commands did not finish",complete.await(5,TimeUnit.SECONDS));
  shadowOf(Looper.getMainLooper()).idle();
 }
 @Test public void sceneIsolationSaveAndReloadRetainExactSignedHeightAndFaceOwners()throws Exception{
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   double h=sign*4.123456789;fixture(plane,h);float[] legacy=app.booleanMeshes.get(2).vertices.clone();
   List<MeshEngine.Mesh> scene=app.currentSceneMeshes();MainActivity.PlanarStoredBinding binding=app.renderedPlanarBindings.get(0);
   assertArrayEquals(binding.identity.shell.triangleCoordinates(),scene.get(2).worldCoordinates(),0);
   String recipe=binding.recipe.id;String face=binding.view.faceAtTriangle(0).id;
   app.showMeshOnly(app.booleanMeshes.get(2));awaitSceneCommands();RelativeSceneCoordinates frame=(RelativeSceneCoordinates)field("coordinates");
   int normal=plane==0?2:plane==1?1:0;double[] bounds=frame.worldBounds();
   assertEquals(Math.min(0,h),bounds[normal],0);assertEquals(Math.max(0,h),bounds[normal+3],0);
   assertArrayEquals(RelativeMeshCoordinates.relative(binding.view.worldCoordinates(),frame.originX,frame.originY,frame.originZ),frame.triangles(0),0);
   assertTrue(app.selectPlanarFace(app.renderedPlanarBindings.get(0),0));assertEquals(face,app.activePlanarFaceId);
   String json=app.encode();app.save();app.load();assertEquals(json,app.encode());
   scene=app.currentSceneMeshes();binding=app.renderedPlanarBindings.get(0);
   assertEquals(recipe,binding.recipe.id);assertEquals(face,binding.view.faceAtTriangle(0).id);
   assertArrayEquals(binding.identity.shell.triangleCoordinates(),scene.get(2).worldCoordinates(),0);
   assertArrayEquals(legacy,app.booleanMeshes.get(2).vertices,0);assertTrue(app.undo.isEmpty());
  }
 }
 @Test public void isolatedViewportRayStillSelectsThePersistentGridFace()throws Exception{
  double h=4.123456789;fixture(0,h);app.showMeshOnly(app.booleanMeshes.get(2));
  app.meshView.layout(0,0,800,600);app.meshView.setViewOrientation(35,65);
  awaitSceneCommands();
  Matrix.perspectiveM((float[])field("projection"),0,45,800f/600,.1f,1000000);
  float[] point=app.meshView.projectPoint(9001.0,8001.0,h);assertNotNull(point);
  app.meshView.pickSurfaceAt(point[0],point[1]);shadowOf(Looper.getMainLooper()).idle();
  assertNotNull(app.activePlanarBinding);assertNotNull(app.activePlanarFaceId);
  assertEquals(app.intersectionRecipes.get(2).id,app.activePlanarBinding.recipe.id);
  assertNotNull(app.activePlanarBinding.view.findFace(app.activePlanarFaceId));
  assertTrue(app.info.getText().toString().contains("1 agujero"));
 }
}
