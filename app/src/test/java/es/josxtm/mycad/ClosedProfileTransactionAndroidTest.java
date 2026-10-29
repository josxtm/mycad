package es.josxtm.mycad;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import es.josxtm.mycad.cad.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Production menu/dialog/codec transactions; not physical Android acceptance. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@LooperMode(LooperMode.Mode.PAUSED)
public class ClosedProfileTransactionAndroidTest {
 private ActivityController<MainActivity> controller;private MainActivity app;
 @Before public void setup(){controller=Robolectric.buildActivity(MainActivity.class);app=controller.get();app.getPreferences(0).edit().clear().commit();controller.setup();}
 @After public void teardown(){controller.pause().stop().destroy();}
 private void profile(int plane){
  profile(plane,new float[][]{{0,0},{40,0},{40,30},{0,30}});
 }
 private void profile(int plane,float[][] p){
  int first=app.sketches.size();
  for(int i=0;i<p.length;i++){
   MainActivity.Sketch s=app.new Sketch(1,p[i][0],p[i][1],p[(i+1)%p.length][0],p[(i+1)%p.length][1]);s.plane=plane;app.sketches.add(s);
  }
  app.selectedSketch=first;
 }
 private void open(){
  app.sketchMenu();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();menu.getListView().performItemClick(null,8,8);
 }
 private EditText input(View v){
  if(v instanceof EditText)return (EditText)v;
  if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){EditText e=input(((ViewGroup)v).getChildAt(i));if(e!=null)return e;}
  return null;
 }
 private void apply(String height){
  AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();input(d.getWindow().getDecorView()).setText(height);
  d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();
 }
 private void choose(int item){AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();d.getListView().performItemClick(null,item,item);}
 private View tool(View root,String name){
  if(name.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
  if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View found=tool(((ViewGroup)root).getChildAt(i),name);if(found!=null)return found;}
  return null;
 }
 private void press(String name){View v=tool(app.rootLayout,name);assertNotNull("Missing visible tool: "+name,v);assertTrue(v.performClick());}
 private void rejected()throws Exception{
  String json=app.encode(),saved=app.getPreferences(0).getString("design",null);int undo=app.undo.size(),redo=app.redo.size();
  apply("10");assertTrue(app.booleanMeshes.isEmpty());assertTrue(app.loopExtrusionFeatures.isEmpty());
  assertEquals(undo,app.undo.size());assertEquals(redo,app.redo.size());assertEquals(json,app.encode());assertEquals(saved,app.getPreferences(0).getString("design",null));
 }
 @Test public void changedSourceCannotPublishGeometryHistoryOrSavedJson()throws Exception{
  profile(0);open();app.sketches.get(1).x2+=2;app.save();String current=app.encode(),saved=app.getPreferences(0).getString("design",null);
  apply("10");assertTrue("Stale dialog committed a body",app.booleanMeshes.isEmpty());assertTrue(app.undo.isEmpty());
  assertEquals(current,app.encode());assertEquals(saved,app.getPreferences(0).getString("design",null));
 }
 @Test public void removedSourceCannotPublishAStaleContour()throws Exception{profile(0);open();app.sketches.remove(2);rejected();}
 @Test public void movedPlaneCannotPublishAStaleContour()throws Exception{profile(1);open();app.sketches.get(2).plane=2;rejected();}
 @Test public void duplicateLineIdCannotOverwriteTheSourceMap()throws Exception{
  profile(0);open();MainActivity.Sketch copy=app.new Sketch(1,100,0,120,0);copy.id=app.sketches.get(0).id;app.sketches.add(copy);rejected();
 }
 @Test public void invalidHeightsAndCancelPreserveTheDocument()throws Exception{
  profile(2);String before=app.encode();
  for(String value:new String[]{"0","NaN","Infinity","10001"}){open();apply(value);assertTrue(app.booleanMeshes.isEmpty());assertTrue(app.undo.isEmpty());assertEquals(before,app.encode());}
  open();ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(before,app.encode());
 }
 @Test public void reorderedListKeepsTheSelectedProfileUuid()throws Exception{
  profile(0);String selected=app.sketches.get(0).id;open();Collections.reverse(app.sketches);apply("10");
  assertEquals(1,app.booleanMeshes.size());assertEquals(selected,app.loopExtrusionFeatures.get(0).selectedLineId);
 }
 private void clear(){
  app.sketches.clear();app.parts.clear();app.booleanMeshes.clear();app.loopExtrusionFeatures.clear();app.intersectionRecipes.clear();app.unionRecipes.clear();app.holeRecipes.clear();
  app.extrusionChains.clear();app.chainFeatures.clear();app.undo.clear();app.redo.clear();
 }
 @Test public void productionMenusExtrudeEditUndoRedoAndReopenEveryPlaneWithExactHeights()throws Exception{
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   clear();profile(plane);double h=sign*10.123456789;open();apply(Double.toString(h).replace('.',','));
   assertEquals(1,app.booleanMeshes.size());LoopExtrusionFeature first=app.loopExtrusionFeatures.get(0);assertEquals(h,first.distance,0);
   assertEquals(plane,first.plane);assertEquals(1,app.undo.size());
   PolygonPrismBRep solid=first.asBRep(app.loopSketchSegments(first,null,0,0,0,0));TopologySnapshot before=TopologySnapshot.from(solid);
   assertEquals(1200*Math.abs(h),solid.volume(),1e-7);
   assertArrayEquals(solid.triangleCoordinates(),app.currentSceneMeshes().get(0).worldCoordinates(),0);
   app.booleanMeshManager();choose(0);choose(5);apply(Double.toString(h*2));
   LoopExtrusionFeature edited=app.loopExtrusionFeatures.get(0);assertEquals(first.id,edited.id);assertEquals(h*2,edited.distance,0);assertEquals(2,app.undo.size());
   TopologySnapshot after=TopologySnapshot.from(edited.asBRep(app.loopSketchSegments(edited,null,0,0,0,0)));
   for(TopologySnapshot.Kind kind:TopologySnapshot.Kind.values())assertEquals(before.ids(kind),after.ids(kind));
   app.history(true);assertEquals(h,app.loopExtrusionFeatures.get(0).distance,0);
   app.history(true);assertTrue(app.booleanMeshes.isEmpty());assertEquals(4,app.sketches.size());
   app.history(false);assertEquals(h,app.loopExtrusionFeatures.get(0).distance,0);
   app.history(false);assertEquals(h*2,app.loopExtrusionFeatures.get(0).distance,0);
   String json=app.encode();app.save();app.load();assertEquals(json,app.encode());assertEquals(first.id,app.loopExtrusionFeatures.get(0).id);
   LoopExtrusionFeature restored=app.loopExtrusionFeatures.get(0);
   assertArrayEquals(restored.asBRep(app.loopSketchSegments(restored,null,0,0,0,0)).triangleCoordinates(),app.currentSceneMeshes().get(0).worldCoordinates(),0);
  }
 }
 @Test public void extrusionToolbarUsesTheRealClosedProfileKernelForEveryPlane(){
  for(int plane=0;plane<3;plane++){
   clear();profile(plane);press("Extruir boceto");
   AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Extruir contorno de líneas",shadowOf(d).getTitle().toString());
   assertNull("Unsupported contour Push/Pull must not be offered here",d.getListView());apply("10.123456789");
   assertEquals(1,app.booleanMeshes.size());assertEquals(plane,app.loopExtrusionFeatures.get(0).plane);assertEquals(10.123456789,app.loopExtrusionFeatures.get(0).distance,0);
  }
 }
 @Test public void standardSketchExtrudeEntryAlsoAcceptsAClosedLineProfile(){
  profile(2);app.sketchMenu();choose(5);apply("-10");assertEquals(1,app.booleanMeshes.size());assertEquals(-10,app.loopExtrusionFeatures.get(0).distance,0);
 }
 @Test public void rectangleAndCircleKeepTheirExistingDragAndSliderChoices(){
  for(int type:new int[]{2,3}){
   clear();MainActivity.Sketch q=app.new Sketch(type,0,0,type==2?40:10,type==2?30:0);app.sketches.add(q);app.selectedSketch=0;
   press("Extruir boceto");AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals(2,d.getListView().getAdapter().getCount());
   choose(1);assertEquals("Extrusión · vista previa 3D",shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getTitle().toString());
   ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(app.parts.isEmpty());assertTrue(app.undo.isEmpty());
  }
 }
 @Test public void toolbarSolidsSubtractRegenerateAfterFaceEditAndReopenWithoutLosingTopology()throws Exception{
  profile(0);press("Extruir boceto");apply("10");
  profile(0,new float[][]{{10,10},{20,10},{20,20},{10,20}});press("Extruir boceto");apply("10");
  assertEquals(2,app.booleanMeshes.size());String firstId=app.loopExtrusionFeatures.get(0).id,secondId=app.loopExtrusionFeatures.get(1).id;
  press("Resultados booleanos");choose(0);choose(13);choose(1);choose(0);
  assertEquals(3,app.booleanMeshes.size());LinkedConvexIntersection cut=app.intersectionRecipes.get(2);assertTrue(cut.isGridHole());
  app.currentSceneMeshes();NamedGridHoleBody before=app.renderedPlanarBindings.get(0).identity;
  assertEquals(1,before.shell.holeCount);assertEquals(11000,before.shell.volume,0);
  LoopExtrusionFeature feature=app.loopExtrusionFeatures.get(0);PolygonPrismBRep solid=feature.asBRep(app.loopSketchSegments(feature,null,0,0,0,0));
  String face=null;for(PolygonPrismBRep.Face f:solid.faces())if(app.sketches.get(1).id.equals(f.sourceLineId))face=f.id;assertNotNull(face);
  app.showStoredMeshTouchActions(0,app.booleanMeshes.get(0),face);choose(0);choose(0);apply("5");
  assertEquals(firstId,app.loopExtrusionFeatures.get(0).id);assertEquals(secondId,app.loopExtrusionFeatures.get(1).id);assertEquals(cut.id,app.intersectionRecipes.get(2).id);
  app.currentSceneMeshes();NamedGridHoleBody edited=app.renderedPlanarBindings.get(0).identity;assertEquals(12500,edited.shell.volume,0);assertEquals(1,edited.shell.holeCount);
  assertEquals(TopologySnapshot.from(before).ids(TopologySnapshot.Kind.FACE),TopologySnapshot.from(edited).ids(TopologySnapshot.Kind.FACE));
  app.history(true);app.currentSceneMeshes();assertEquals(11000,app.renderedPlanarBindings.get(0).identity.shell.volume,0);
  app.history(false);app.currentSceneMeshes();assertEquals(12500,app.renderedPlanarBindings.get(0).identity.shell.volume,0);
  // Same-height through-hole kernels must reject a one-sided height change atomically.
  String current=app.encode();int undo=app.undo.size();press("Resultados booleanos");choose(0);choose(5);apply("12");
  assertEquals(current,app.encode());assertEquals(undo,app.undo.size());
  app.save();app.load();assertEquals(current,app.encode());app.currentSceneMeshes();
  assertEquals(cut.id,app.intersectionRecipes.get(2).id);assertEquals(12500,app.renderedPlanarBindings.get(0).identity.shell.volume,0);
 }
}
