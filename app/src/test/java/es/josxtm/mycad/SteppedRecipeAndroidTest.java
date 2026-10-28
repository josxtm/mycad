package es.josxtm.mycad;

import android.app.AlertDialog;
import android.os.Looper;
import java.util.*;
import es.josxtm.mycad.cad.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;

/** Real Activity codec, recipe menus, render ownership and callback delivery.
 * JVM Android tests do not substitute for EGL/device acceptance. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class SteppedRecipeAndroidTest {
 private ActivityController<MainActivity> controller;
 private MainActivity app;
 @Before public void setup(){
  controller=Robolectric.buildActivity(MainActivity.class).setup();app=controller.get();
  app.parts.clear();app.sketches.clear();app.booleanMeshes.clear();app.unionRecipes.clear();
  app.holeRecipes.clear();app.loopExtrusionFeatures.clear();app.intersectionRecipes.clear();
  app.extrusionChains.clear();app.chainFeatures.clear();app.undo.clear();app.redo.clear();
 }
 @After public void teardown(){controller.pause().stop().destroy();}
 private LoopExtrusionFeature operand(float x1,float y1,float x2,float y2,double h){
  float[][] p={{x1,y1},{x2,y1},{x2,y2},{x1,y2}};
  List<String> ids=new ArrayList<>();
  for(int i=0;i<4;i++){
   MainActivity.Sketch line=app.new Sketch(1,p[i][0],p[i][1],p[(i+1)%4][0],p[(i+1)%4][1]);
   app.sketches.add(line);ids.add(line.id);
  }
  LoopExtrusionFeature feature=new LoopExtrusionFeature(UUID.randomUUID().toString(),ids.get(0),0,h,ids);
  app.booleanMeshes.add(new MeshEngine.Mesh(feature.asBRep(app.loopSketchSegments(feature,null,0,0,0,0)).triangles()));
  app.loopExtrusionFeatures.add(feature);app.intersectionRecipes.add(null);
  app.unionRecipes.add(null);app.holeRecipes.add(null);return feature;
 }
 private LinkedConvexIntersection recipe(LinkedConvexIntersection.Operation operation){
  LoopExtrusionFeature a=operand(0,0,4,4,6),b=operand(1,1,3,3,3);
  LinkedConvexIntersection recipe=new LinkedConvexIntersection(UUID.randomUUID().toString(),a.id,b.id,operation);
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>(app.loopSketchSegments(a,null,0,0,0,0));
  lines.putAll(app.loopSketchSegments(b,null,0,0,0,0));
  app.booleanMeshes.add(new MeshEngine.Mesh(recipe.regenerateTriangles(app.intersectionSourceMap(null),lines)));
  app.intersectionRecipes.add(recipe);app.loopExtrusionFeatures.add(null);
  app.unionRecipes.add(null);app.holeRecipes.add(null);return recipe;
 }
 private void choose(int index){
  AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
  dialog.getListView().performItemClick(null,index,index);shadowOf(Looper.getMainLooper()).idle();
 }
 private double[] projection(){return new double[]{.2,0,0,0,0,.2,0,0,0,0,-.1,0,0,0,0,1};}
 private ViewportMeshRayPicker.Hit visible(List<MeshEngine.Mesh> meshes,double u,double v){
  return visible(meshes,u,v,false);
 }
 private ViewportMeshRayPicker.Hit visible(List<MeshEngine.Mesh> meshes,double u,double v,boolean fromBelow){
  List<ViewportMeshRayPicker.Candidate> candidates=new ArrayList<>();
  for(MeshEngine.Mesh mesh:meshes)candidates.add(new ViewportMeshRayPicker.Candidate(mesh.rayIndex(),mesh.triangles()));
  double[] matrix=projection();if(fromBelow)matrix[10]=.1;
  return ViewportMeshRayPicker.pick(candidates,matrix,100,100,50+u*10,50-v*10);
 }
 private SteppedVisiblePatchBridge.Hit patch(List<MeshEngine.Mesh> meshes,ViewportMeshRayPicker.Hit visible){
  List<float[]> triangles=new ArrayList<>();List<SteppedScenePatchRegistry.Binding> bindings=new ArrayList<>();
  for(int i=0;i<meshes.size();i++){
   triangles.add(meshes.get(i).vertices);
   if(meshes.get(i).steppedShell!=null)bindings.add(new SteppedScenePatchRegistry.Binding(i,meshes.get(i).steppedShell));
  }
  return SteppedScenePatchRegistry.register(triangles,bindings).resolve(visible);
 }
 @Test public void unequalHeightRecipeMenuIsUndoableAndReopensWithTopology() throws Exception {
  operand(0,0,4,4,6);operand(2,0,6,4,3);
  app.showStoredMeshTouchActions(0,app.booleanMeshes.get(0),null);
  choose(13);choose(0);choose(0);
  assertEquals(3,app.booleanMeshes.size());
  LinkedConvexIntersection saved=app.intersectionRecipes.get(2);assertTrue(saved.isStepped());
  assertEquals(1,app.undo.size());String json=app.encode();
  MainActivity.ParsedDesign reopened=app.decode(json);
  assertEquals(saved.id,reopened.intersections.get(2).id);
  assertEquals(saved.operation,reopened.intersections.get(2).operation);
  assertArrayEquals(app.booleanMeshes.get(2).vertices,reopened.meshes.get(2).vertices,0);
  app.save();app.load();
  assertNotNull(app.currentSceneMeshes().get(2).steppedShell);
 }
 @Test public void renderedFragmentsMatchExportAndOldMeshesStillOcclude(){
  recipe(LinkedConvexIntersection.Operation.UNION_STEPPED);
  app.importedMesh=new MeshEngine.Mesh(new float[]{0,0,8,4,0,8,0,4,8});
  List<MeshEngine.Mesh> meshes=app.currentSceneMeshes();
  ArrayList<float[]> arrays=new ArrayList<>();for(MeshEngine.Mesh mesh:meshes)arrays.add(mesh.vertices);
  assertArrayEquals(app.currentMesh().vertices,AnalyticSceneMesh.compose(arrays,Collections.emptyList(),Collections.emptyList(),48),0);
  assertEquals(3,app.renderedSteppedBindings.get(0).index+1);
  assertEquals(1+app.booleanMeshes.get(0).triangles()+app.booleanMeshes.get(1).triangles(),app.renderedSteppedBindings.get(0).firstTriangle);
  ViewportMeshRayPicker.Hit front=visible(meshes,1,1);assertNotNull(front);
  assertNull(patch(meshes,front));
 }
 @Test public void isolatedPocketTapSelectsAnalyticPatchThroughProductionListeners(){
  LinkedConvexIntersection owner=recipe(LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED);
  app.showMeshOnly(app.booleanMeshes.get(2));
  List<MeshEngine.Mesh> displayed=Collections.singletonList(app.renderedSteppedBindings.get(0).rendered);
  ViewportMeshRayPicker.Hit front=visible(displayed,1.7,1.9,true);
  SteppedVisiblePatchBridge.Hit hit=patch(displayed,front);assertNotNull(hit);
  app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),front,hit);
  assertEquals(owner.id,app.activeSteppedRecipeId);assertSame(hit,app.activeSteppedPatch);
  assertTrue(app.info.getText().toString().contains("área"));assertEquals(2,app.selectedStoredMeshIndex);
  assertEquals(3,hit.point()[2],1e-6);
 }
 @Test public void sceneReplacementAndPreviewRejectQueuedCallbacks(){
  recipe(LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED);app.showMeshOnly(app.booleanMeshes.get(2));
  MeshEngine.Mesh rendered=app.renderedSteppedBindings.get(0).rendered;
  List<MeshEngine.Mesh> displayed=Collections.singletonList(rendered);
  ViewportMeshRayPicker.Hit front=visible(displayed,1.7,1.9);
  SteppedVisiblePatchBridge.Hit hit=patch(displayed,front);long old=app.meshView.sceneRevision();
  app.meshView.previewMesh(rendered);app.meshView.dispatchSurfaceHit(old,front,hit);
  assertNull(app.activeSteppedPatch);
  old=app.meshView.sceneRevision();app.meshView.updateMeshesPreservingCamera(displayed);
  app.meshView.dispatchSurfaceHit(old,front,hit);assertNull(app.activeSteppedPatch);
 }
 @Test public void changedStoredMeshAndTamperedGeometryCannotReuseTopology(){
  recipe(LinkedConvexIntersection.Operation.UNION_STEPPED);
  app.showMeshOnly(app.booleanMeshes.get(2));
  MainActivity.SteppedStoredBinding binding=app.renderedSteppedBindings.get(0);
  List<MeshEngine.Mesh> single=Collections.singletonList(binding.rendered);
  ViewportMeshRayPicker.Hit front=visible(single,.7,.9);
  SteppedVisiblePatchBridge.Hit hit=patch(single,front);
  assertTrue(app.selectSteppedPatch(hit));
  app.booleanMeshes.set(2,new MeshEngine.Mesh(binding.stored.vertices.clone()));
  assertFalse(app.selectSteppedPatch(hit));
  app.booleanMeshes.get(2).vertices[0]+=1;
  try{app.currentSceneMeshes();fail("Stale mesh accepted");}catch(IllegalArgumentException expected){}
 }
}
