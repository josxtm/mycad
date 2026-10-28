package es.josxtm.mycad;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
  return polygonOperand(new float[][]{{x1,y1},{x2,y1},{x2,y2},{x1,y2}},h);
 }
 private LoopExtrusionFeature polygonOperand(float[][] p,double h){
  List<String> ids=new ArrayList<>();
  for(int i=0;i<p.length;i++){
   MainActivity.Sketch line=app.new Sketch(1,p[i][0],p[i][1],p[(i+1)%p.length][0],p[(i+1)%p.length][1]);
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
 private SteppedVisiblePatchBridge.Hit selectPocket(){
  app.showMeshOnly(app.booleanMeshes.get(2));
  List<MeshEngine.Mesh> single=Collections.singletonList(app.renderedSteppedBindings.get(0).rendered);
  ViewportMeshRayPicker.Hit visible=visible(single,1.7,1.9,true);
  SteppedVisiblePatchBridge.Hit hit=patch(single,visible);
  app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),visible,hit);return hit;
 }
 private EditText heightInput(View view){
  if(view instanceof EditText)return (EditText)view;
  if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
   EditText found=heightInput(((ViewGroup)view).getChildAt(i));if(found!=null)return found;
  }
  return null;
 }
 private void applyHeight(double value){
  AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
  heightInput(dialog.getWindow().getDecorView()).setText(String.valueOf(value));
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();
 }
 @Test public void secondTapEditsSourceHeightAndRegeneratesWithUndoAndReload() throws Exception {
  LinkedConvexIntersection owner=recipe(LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED);
  SteppedVisiblePatchBridge.Hit hit=selectPocket();
  String shell=hit.shellId;float[] before=app.booleanMeshes.get(2).vertices.clone();
  assertTrue(app.selectSteppedPatch(hit)); // repeated tap opens parameter menu
  choose(0);applyHeight(7);
  assertEquals(7,app.loopExtrusionFeatures.get(0).distance,0);
  assertEquals(owner.id,app.intersectionRecipes.get(2).id);assertEquals(1,app.undo.size());
  assertFalse(Arrays.equals(before,app.booleanMeshes.get(2).vertices));assertNull(app.activeSteppedPatch);
  VariableHeightOrthogonalBRep rebuilt=app.currentSceneMeshes().get(2).steppedShell;
  assertEquals(100,rebuilt.volume(),1e-6);assertNotEquals(shell,rebuilt.id());
  MainActivity.ParsedDesign reopened=app.decode(app.encode());
  assertEquals(owner.id,reopened.intersections.get(2).id);
  assertArrayEquals(rebuilt.triangles(),reopened.meshes.get(2).vertices,0);
  app.history(true);assertEquals(6,app.loopExtrusionFeatures.get(0).distance,0);
  assertArrayEquals(before,app.booleanMeshes.get(2).vertices,0);
  app.history(false);assertEquals(7,app.loopExtrusionFeatures.get(0).distance,0);
 }
 @Test public void staleResultRejectsAnAlreadyOpenHeightDialog(){
  LinkedConvexIntersection owner=recipe(LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED);
  selectPocket();app.editSelectedCadFace();choose(0);
  float[] before=app.booleanMeshes.get(2).vertices.clone();
  app.intersectionRecipes.set(2,new LinkedConvexIntersection(owner.id,owner.firstId,owner.secondId,owner.operation));
  applyHeight(8);
  assertEquals(6,app.loopExtrusionFeatures.get(0).distance,0);assertTrue(app.undo.isEmpty());
  assertArrayEquals(before,app.booleanMeshes.get(2).vertices,0);
 }
 @Test public void invalidDependentTopologyLeavesEveryMeshAndHeightUntouched(){
  recipe(LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED);
  selectPocket();app.editSelectedSteppedFace();choose(0);
  List<float[]> before=new ArrayList<>();for(MeshEngine.Mesh mesh:app.booleanMeshes)before.add(mesh.vertices.clone());
  applyHeight(2); // a through-hole would need a different topological route
  assertEquals(6,app.loopExtrusionFeatures.get(0).distance,0);assertTrue(app.undo.isEmpty());
  for(int i=0;i<before.size();i++)assertArrayEquals(before.get(i),app.booleanMeshes.get(i).vertices,0);
 }
 @Test public void boundaryTapSelectsRealEdgeAndInspectorShowsItsLength(){
  recipe(LinkedConvexIntersection.Operation.UNION_STEPPED);app.showMeshOnly(app.booleanMeshes.get(2));
  List<MeshEngine.Mesh> single=Collections.singletonList(app.renderedSteppedBindings.get(0).rendered);
  ViewportMeshRayPicker.Hit front=visible(single,.02,1.7);
  SteppedVisiblePatchBridge.Hit hit=patch(single,front);
  app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),front,hit);
  assertNotNull(app.activeSteppedEdge);assertEquals(hit.patch.index,app.activeSteppedEdge.patchIndex);
  app.inspectSelectedEdges();AlertDialog inspector=ShadowAlertDialog.getLatestAlertDialog();
  assertEquals("Arista del resultado B-Rep",shadowOf(inspector).getTitle().toString());
  assertTrue(shadowOf(inspector).getMessage().toString().contains(app.activeSteppedEdge.edgeId));
  app.clearStoredMeshSelection();assertNull(app.activeSteppedEdge);
 }
 @Test public void movedResultCannotInspectPreviouslySelectedBoundary(){
  LinkedConvexIntersection owner=recipe(LinkedConvexIntersection.Operation.UNION_STEPPED);
  app.showMeshOnly(app.booleanMeshes.get(2));
  List<MeshEngine.Mesh> single=Collections.singletonList(app.renderedSteppedBindings.get(0).rendered);
  ViewportMeshRayPicker.Hit front=visible(single,.02,1.7);
  app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),front,patch(single,front));
  assertNotNull(app.activeSteppedEdge);
  app.intersectionRecipes.set(2,new LinkedConvexIntersection(owner.id,owner.firstId,owner.secondId,owner.operation));
  assertNull(app.selectedSteppedBinding());app.inspectSelectedEdges();
  assertTrue(org.robolectric.shadows.ShadowToast.getTextOfLatestToast().contains("actual"));
 }
 @Test public void persistentEntityNamesSurviveActualSaveAndReload() throws Exception {
  LinkedConvexIntersection owner=recipe(LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED);
  selectPocket();MainActivity.SteppedStoredBinding before=app.selectedSteppedBinding();
  String patchId=before.identity.patchId(app.activeSteppedPatch.patch);
  app.inspectSelectedFaces();
  assertTrue(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage().toString().contains(patchId));
  app.save();app.load();selectPocket();MainActivity.SteppedStoredBinding after=app.selectedSteppedBinding();
  assertEquals(owner.id,after.identity.bodyId);assertEquals(patchId,after.identity.patchId(app.activeSteppedPatch.patch));
  for(VariableHeightOrthogonalBRep.Vertex vertex:before.identity.shell.vertices())
   assertNotNull(after.identity.findVertex(before.identity.vertexId(vertex.id)));
  for(VariableHeightOrthogonalBRep.Edge edge:before.identity.shell.edges())
   assertNotNull(after.identity.findEdge(before.identity.edgeId(edge.id)));
 }
 @Test public void heightEditKeepsNamesButInvalidatesOldGeometryHits(){
  recipe(LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED);
  SteppedVisiblePatchBridge.Hit oldHit=selectPocket();NamedSteppedBody before=app.selectedSteppedBinding().identity;
  app.editSelectedSteppedFace();choose(0);applyHeight(7);
  selectPocket();NamedSteppedBody after=app.selectedSteppedBinding().identity;
  assertEquals(before.bodyId,after.bodyId);assertNotEquals(before.shell.id(),after.shell.id());
  for(VariableHeightOrthogonalBRep.Face face:before.shell.faces())
   assertNotNull(after.findFace(before.faceId(face.id)));
  assertFalse(app.selectSteppedPatch(oldHit));
 }
 private LinkedConvexIntersection gridRecipe(){
  LoopExtrusionFeature a=operand(0,0,8,8,6),b=operand(2,2,6,6,6);
  LinkedConvexIntersection recipe=new LinkedConvexIntersection(UUID.randomUUID().toString(),a.id,b.id,
   LinkedConvexIntersection.Operation.DIFFERENCE_WITH_HOLES);
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>(app.loopSketchSegments(a,null,0,0,0,0));
  lines.putAll(app.loopSketchSegments(b,null,0,0,0,0));
  app.booleanMeshes.add(new MeshEngine.Mesh(recipe.regenerateTriangles(app.intersectionSourceMap(null),lines)));
  app.intersectionRecipes.add(recipe);app.loopExtrusionFeatures.add(null);app.unionRecipes.add(null);app.holeRecipes.add(null);
  return recipe;
 }
 @Test public void throughHoleFaceIsSelectedAndInspectedWithPersistentId(){
  LinkedConvexIntersection owner=gridRecipe();app.showMeshOnly(app.booleanMeshes.get(2));
  assertEquals(1,app.renderedPlanarBindings.size());
  List<MeshEngine.Mesh> single=Collections.singletonList(app.booleanMeshes.get(2));
  assertNull(visible(single,3,3)); // ray passes through the actual hole
  ViewportMeshRayPicker.Hit front=visible(single,1,1.5);assertNotNull(front);
  app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),front,null);
  assertNotNull(app.activePlanarBinding);assertEquals(owner.id,app.activePlanarBinding.identity.bodyId);
  String faceId=app.activePlanarFaceId;app.inspectSelectedFaces();
  assertTrue(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage().toString().contains(faceId));
 }
 @Test public void holeTopologyAndNamesSurviveJsonAndRegenerateWithoutChangingOldFormats() throws Exception {
  LinkedConvexIntersection owner=gridRecipe();app.currentSceneMeshes();
  NamedGridHoleBody before=app.renderedPlanarBindings.get(0).identity;
  app.save();app.load();app.currentSceneMeshes();NamedGridHoleBody after=app.renderedPlanarBindings.get(0).identity;
  assertEquals(owner.id,after.bodyId);assertEquals(1,after.shell.holeCount);
  for(OrthogonalGridHoleBRep.Face face:before.shell.faces())assertNotNull(after.findFace(before.faceId(face.id)));
  assertArrayEquals(before.shell.triangles(),after.shell.triangles(),0);
 }
 @Test public void gridInspectorAndSelectionRejectReplacedAndTamperedMeshes(){
  gridRecipe();app.showMeshOnly(app.booleanMeshes.get(2));MainActivity.PlanarStoredBinding old=app.renderedPlanarBindings.get(0);
  assertTrue(app.selectPlanarFace(old,0));
  app.booleanMeshes.set(2,new MeshEngine.Mesh(old.stored.vertices.clone()));
  assertFalse(app.selectPlanarFace(old,0));app.inspectSelectedPlanarFace();
  assertTrue(org.robolectric.shadows.ShadowToast.getTextOfLatestToast().contains("cambiado"));
  app.booleanMeshes.get(2).vertices[0]+=1;
  try{app.currentSceneMeshes();fail("Stale grid geometry accepted");}catch(IllegalArgumentException expected){}
 }
 @Test public void gridRegistrationPreservesGlobalOffsetsExportAndImportedOcclusion(){
  gridRecipe();app.importedMesh=new MeshEngine.Mesh(new float[]{0,0,8,4,0,8,0,4,8});
  app.toggleMeshView();List<MeshEngine.Mesh> meshes=app.currentSceneMeshes();
  MainActivity.PlanarStoredBinding grid=app.renderedPlanarBindings.get(0);
  assertEquals(1+app.booleanMeshes.get(0).triangles()+app.booleanMeshes.get(1).triangles(),grid.firstTriangle);
  List<float[]> arrays=new ArrayList<>();for(MeshEngine.Mesh mesh:meshes)arrays.add(mesh.vertices);
  assertArrayEquals(app.currentMesh().vertices,AnalyticSceneMesh.compose(arrays,Collections.emptyList(),Collections.emptyList(),48),0);
  ViewportMeshRayPicker.Hit front=visible(meshes,.2,.4);assertEquals(0,front.triangle);
  app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),front,null);
  assertNull(app.activePlanarBinding);assertTrue(app.info.getText().toString().contains("importada"));
 }
 @Test public void topologyLedgerTracksAnActualHoleOpeningAndUndo(){
  LinkedConvexIntersection owner=gridRecipe();app.toggleMeshView();
  TopologySnapshot before=app.topologySnapshots.get(owner.id);assertNotNull(before);
  LoopExtrusionFeature cutter=app.loopExtrusionFeatures.get(1);
  PolygonPrismBRep polygon=cutter.asBRep(app.loopSketchSegments(cutter,null,0,0,0,0));
  PolygonPrismBRep.Face right=null;
  for(PolygonPrismBRep.Face face:polygon.faces())if(face.outwardNormal()[0]>.9)right=face;
  assertNotNull(right);app.offsetPolygonSideFace(1,app.booleanMeshes.get(1),right.id);applyHeight(4);
  assertEquals(0,app.renderedPlanarBindings.get(0).identity.shell.holeCount);
  TopologyReconciliation delta=app.topologyChanges.get(owner.id);assertNotNull(delta);
  assertFalse(delta.change(TopologySnapshot.Kind.FACE).removed.isEmpty());
  assertFalse(delta.change(TopologySnapshot.Kind.FACE).created.isEmpty());
  assertTrue(app.topologyChangeSummary(owner.id).contains("eliminadas"));
  app.currentSceneMeshes();assertSame(delta,app.topologyChanges.get(owner.id));
  app.history(true);assertEquals(1,app.renderedPlanarBindings.get(0).identity.shell.holeCount);
  assertEquals(before.geometryVersion,app.topologySnapshots.get(owner.id).geometryVersion);
 }
 @Test public void topologyRegistryIncludesAnalyticCylindersAndDropsDeletedBodies(){
  gridRecipe();MainActivity.Part box=app.new Part(false,10,12,20,0),cylinder=app.new Part(true,12,12,20,0);
  app.parts.add(box);app.parts.add(cylinder);app.toggleMeshView();
  assertEquals(8,app.topologySnapshots.get(box.id).ids(TopologySnapshot.Kind.VERTEX).size());
  assertEquals(0,app.topologySnapshots.get(cylinder.id).ids(TopologySnapshot.Kind.VERTEX).size());
  assertEquals(2,app.topologySnapshots.get(cylinder.id).ids(TopologySnapshot.Kind.EDGE).size());
  assertNotNull(app.topologySnapshots.get(app.loopExtrusionFeatures.get(0).id));
  app.parts.remove(cylinder);app.renderCurrentCadMesh();assertFalse(app.topologySnapshots.containsKey(cylinder.id));
  try{app.topologySnapshots.clear();fail("Mutable index");}catch(UnsupportedOperationException expected){}
 }
 @Test public void invalidGeometryDoesNotPublishAPartialTopologyRegistry(){
  gridRecipe();app.toggleMeshView();Map<String,TopologySnapshot> before=app.topologySnapshots;
  app.booleanMeshes.get(2).vertices[0]+=1;
  try{app.currentSceneMeshes();fail("Stale mesh accepted");}catch(IllegalArgumentException expected){}
  assertSame(before,app.topologySnapshots);
 }
 private LinkedConvexIntersection convexRecipe(){
  LoopExtrusionFeature a=operand(0,0,6,6,6),b=polygonOperand(new float[][]{{-1,1},{7,1},{3,7}},4);
  LinkedConvexIntersection owner=new LinkedConvexIntersection(UUID.randomUUID().toString(),a.id,b.id);
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>(app.loopSketchSegments(a,null,0,0,0,0));
  lines.putAll(app.loopSketchSegments(b,null,0,0,0,0));
  app.booleanMeshes.add(new MeshEngine.Mesh(owner.regenerateTriangles(app.intersectionSourceMap(null),lines)));
  app.intersectionRecipes.add(owner);app.loopExtrusionFeatures.add(null);app.unionRecipes.add(null);app.holeRecipes.add(null);
  return owner;
 }
 private void selectConvexCap(){
  app.showMeshOnly(app.booleanMeshes.get(2));
  ViewportMeshRayPicker.Hit hit=visible(Collections.singletonList(app.booleanMeshes.get(2)),3,2);
  assertNotNull(hit);app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),hit,null);
  assertNotNull(app.activePlanarBinding);
 }
 @Test public void angledIntersectionSelectsWholeCapAndReopensPersistentNames() throws Exception {
  LinkedConvexIntersection owner=convexRecipe();selectConvexCap();
  String face=app.activePlanarFaceId;MainActivity.PlanarStoredBinding binding=app.activePlanarBinding;
  assertNotNull(binding.convexIdentity);assertEquals(owner.id,binding.view.bodyId);
  assertEquals(4,binding.view.trianglesForFace(face,0).length);
  app.inspectSelectedFaces();assertTrue(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage().toString().contains(face));
  app.save();app.load();selectConvexCap();assertEquals(face,app.activePlanarFaceId);
 }
 @Test public void editingAnIntersectionSourceRetainsCapIdentityAndRejectsOldVersion(){
  convexRecipe();selectConvexCap();String faceId=app.activePlanarFaceId;
  TopologySnapshot before=app.activePlanarBinding.view.topology;
  TopologySnapshot.Reference old=before.reference(TopologySnapshot.Kind.FACE,faceId);
  LoopExtrusionFeature source=app.loopExtrusionFeatures.get(0);
  PolygonPrismBRep polygon=source.asBRep(app.loopSketchSegments(source,null,0,0,0,0));
  PolygonPrismBRep.Face right=null;for(PolygonPrismBRep.Face face:polygon.faces())if(face.outwardNormal()[0]>.9)right=face;
  assertNotNull(right);app.offsetPolygonSideFace(0,app.booleanMeshes.get(0),right.id);applyHeight(.5);
  selectConvexCap();TopologySnapshot after=app.activePlanarBinding.view.topology;
  assertEquals(faceId,app.activePlanarFaceId);assertNotNull(after.resolvePersistent(old));assertNull(after.resolveExact(old));
 }
 @Test public void transientPreviewCannotUseStoredPlanarSelectionBindings(){
  convexRecipe();selectConvexCap();MainActivity.PlanarStoredBinding before=app.activePlanarBinding;
  app.meshView.previewMesh(new MeshEngine.Mesh(new float[]{0,0,8,4,0,8,0,4,8}));
  assertFalse(app.currentPlanarBinding(before));assertFalse(app.selectPlanarFace(before,0));
  app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),visible(Collections.singletonList(app.booleanMeshes.get(2)),3,2),null);
  assertNull(app.activePlanarBinding);
 }
 @Test public void hiddenViewportDoesNotApplyAQueuedSurfaceSelection(){
  convexRecipe();selectConvexCap();
  ViewportMeshRayPicker.Hit hit=visible(Collections.singletonList(app.booleanMeshes.get(2)),3,2);
  app.toggleMeshView();assertFalse(app.meshMode);
  app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),hit,null);
  assertNull(app.activePlanarBinding);assertEquals(-1,app.selectedStoredMeshIndex);
 }

 private LinkedConvexIntersection planarRecipe(LinkedConvexIntersection.Operation operation,boolean split){
  LoopExtrusionFeature a=operand(0,0,10,10,4),b=split?operand(3,3,7,12,4):operand(4,2,12,6,4);
  return installPlanarRecipe(a,b,operation);
 }
 private LinkedConvexIntersection installPlanarRecipe(LoopExtrusionFeature a,LoopExtrusionFeature b,LinkedConvexIntersection.Operation operation){
  LinkedConvexIntersection owner=new LinkedConvexIntersection(UUID.randomUUID().toString(),a.id,b.id,operation);
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>(app.loopSketchSegments(a,null,0,0,0,0));
  lines.putAll(app.loopSketchSegments(b,null,0,0,0,0));
  app.booleanMeshes.add(new MeshEngine.Mesh(owner.regenerateTriangles(app.intersectionSourceMap(null),lines)));
  app.intersectionRecipes.add(owner);app.loopExtrusionFeatures.add(null);app.unionRecipes.add(null);app.holeRecipes.add(null);return owner;
 }
 private void selectPlanarCap(double x,double y){
  app.showMeshOnly(app.booleanMeshes.get(2));
  ViewportMeshRayPicker.Hit hit=visible(Collections.singletonList(app.booleanMeshes.get(2)),x,y);
  assertNotNull(hit);app.meshView.dispatchSurfaceHit(app.meshView.sceneRevision(),hit,null);
  assertNotNull(app.activePlanarBinding);assertNotNull(app.activePlanarBinding.planarIdentity);
 }
 @Test public void allRemainingPlanarRecipesSelectAndReopenWithoutChangingSavedTriangles(){
  for(LinkedConvexIntersection.Operation operation:LinkedConvexIntersection.Operation.values())if(NamedPlanarBoolean.supports(operation)){
   app.clearStoredMeshSelection();app.parts.clear();app.sketches.clear();app.booleanMeshes.clear();
   app.unionRecipes.clear();app.holeRecipes.clear();app.loopExtrusionFeatures.clear();app.intersectionRecipes.clear();
   LinkedConvexIntersection owner=planarRecipe(operation,false);
   boolean intersection=operation.name().startsWith("INTERSECTION");selectPlanarCap(intersection?5:1,intersection?3:1);
   String face=app.activePlanarFaceId;TopologySnapshot before=app.activePlanarBinding.view.topology;
   float[] saved=app.booleanMeshes.get(2).vertices.clone();
   app.inspectSelectedFaces();assertTrue(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage().toString().contains(face));
   app.save();app.load();selectPlanarCap(intersection?5:1,intersection?3:1);
   assertEquals(owner.id,app.activePlanarBinding.view.bodyId);assertEquals(face,app.activePlanarFaceId);
   assertEquals(before.geometryVersion,app.activePlanarBinding.view.topology.geometryVersion);
   assertArrayEquals(saved,app.booleanMeshes.get(2).vertices,0);
  }
 }
 @Test public void nonOrthogonalAnalyticUnionHasRecipeOwnedFaces(){
  LoopExtrusionFeature upper=polygonOperand(new float[][]{{0,0},{4,0},{2,2}},4);
  LoopExtrusionFeature lower=polygonOperand(new float[][]{{0,0},{2,-2},{4,0}},4);
  LinkedConvexIntersection owner=installPlanarRecipe(upper,lower,LinkedConvexIntersection.Operation.UNION_ANALYTIC);
  selectPlanarCap(2,.5);assertEquals(owner.id,app.activePlanarBinding.view.topology.bodyId);
  assertEquals(8,app.activePlanarBinding.view.vertexCount);assertEquals(6,app.activePlanarBinding.view.faceCount);
 }
 @Test public void sourceSideEditSplitsComponentsRetiresCapsAndUndoRestoresThem(){
  LinkedConvexIntersection owner=planarRecipe(LinkedConvexIntersection.Operation.DIFFERENCE_MULTISHELL,true);
  selectPlanarCap(1,1);TopologySnapshot before=app.activePlanarBinding.view.topology;String cap=app.activePlanarFaceId;
  LoopExtrusionFeature cutter=app.loopExtrusionFeatures.get(1);
  PolygonPrismBRep body=cutter.asBRep(app.loopSketchSegments(cutter,null,0,0,0,0));
  PolygonPrismBRep.Face bottom=null;for(PolygonPrismBRep.Face face:body.faces())if(face.outwardNormal()[1]<-.9)bottom=face;
  assertNotNull(bottom);app.offsetPolygonSideFace(1,app.booleanMeshes.get(1),bottom.id);applyHeight(5);
  selectPlanarCap(1,1);assertEquals(2,app.activePlanarBinding.view.solidCount);
  assertNull(app.activePlanarBinding.view.topology.resolvePersistent(before.reference(TopologySnapshot.Kind.FACE,cap)));
  assertTrue(app.topologyChanges.get(owner.id).change(TopologySnapshot.Kind.FACE).removed.contains(cap));
  app.history(true);selectPlanarCap(1,1);assertEquals(cap,app.activePlanarFaceId);assertEquals(1,app.activePlanarBinding.view.solidCount);
 }
 @Test public void planarBooleanTamperingDoesNotPublishGeometryOrAcceptAStaleBinding(){
  planarRecipe(LinkedConvexIntersection.Operation.DIFFERENCE_ANALYTIC,false);selectPlanarCap(1,1);
  MainActivity.PlanarStoredBinding binding=app.activePlanarBinding;Map<String,TopologySnapshot> snapshots=app.topologySnapshots;
  app.booleanMeshes.get(2).vertices[0]++;
  assertFalse(app.currentPlanarBinding(binding));assertFalse(app.selectPlanarFace(binding,0));
  try{app.currentSceneMeshes();fail("Tampered Boolean accepted");}catch(IllegalArgumentException expected){}
  assertSame(snapshots,app.topologySnapshots);
 }

 @Test public void rebasedViewportRayStillResolvesTheActualSteppedPatch()throws Exception{
  recipe(LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED);app.toggleMeshView();
  List<MeshEngine.Mesh> meshes=app.currentSceneMeshes();List<double[]> world=new ArrayList<>();
  for(MeshEngine.Mesh mesh:meshes)world.add(mesh.worldCoordinates());
  app.meshView.layout(0,0,800,600);
  app.meshView.installSceneMeshes(meshes,RelativeSceneCoordinates.from(world),true,app.meshView.sceneRevision());
  app.meshView.setViewOrientation(35,25);
  java.lang.reflect.Field projection=MeshEngine.class.getDeclaredField("projection");projection.setAccessible(true);
  android.opengl.Matrix.perspectiveM((float[])projection.get(app.meshView),0,45,800f/600,.1f,1000000);
  float[] pixel=app.meshView.projectPoint(2.0,2.0,3.0);assertNotNull(pixel);
  // Isolate the Boolean so source operands cannot occlude the result.
  app.showMeshOnly(app.booleanMeshes.get(2));meshes=Collections.singletonList(app.booleanMeshes.get(2));
  MeshEngine.Mesh rendered=app.renderedSteppedBindings.get(0).rendered;
  app.meshView.installSceneMeshes(Collections.singletonList(rendered),RelativeSceneCoordinates.from(Collections.singletonList(rendered.worldCoordinates())),true,app.meshView.sceneRevision());
  app.meshView.pickSurfaceAt(pixel[0],pixel[1]);shadowOf(Looper.getMainLooper()).idle();
  assertNotNull(app.activeSteppedPatch);assertEquals(app.intersectionRecipes.get(2).id,app.activeSteppedRecipeId);
 }

 @Test public void cylinderSceneAndTransientPreviewRetainTheSameAnalyticDoubleCoordinates(){
  MainActivity.Part part=app.new Part(true,.015f,.015f,2,9000);part.y=8000;part.z=7000;part.rotationX=20;part.rotationY=30;part.rotationZ=40;app.parts.add(part);
  PrimitiveBRepBody body=app.analyticBody(part);double[] expected=body.triangleCoordinates(48);
  MeshEngine.Mesh preview=app.experimentalCadMeshPreview(0,body);
  assertArrayEquals(expected,preview.worldCoordinates(),0);
  List<MeshEngine.Mesh> scene=app.currentSceneMeshes();assertEquals(2,scene.size());
  assertArrayEquals(expected,scene.get(0).worldCoordinates(),0);
  assertArrayEquals(body.triangles(48),scene.get(0).vertices,0);
 }
}
