package es.josxtm.mycad;

import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@LooperMode(LooperMode.Mode.PAUSED)
public class StablePreviewAndroidTest {
 private ActivityController<MainActivity> controller;private MainActivity app;
 @Before public void setup(){controller=Robolectric.buildActivity(MainActivity.class);app=controller.get();app.getPreferences(0).edit().clear().commit();controller.setup();}
 @After public void teardown(){controller.pause().stop().destroy();}
 private MainActivity.Part fixture(){
  MainActivity.Sketch source=app.new Sketch(2,0,0,40,30);app.sketches.add(source);
  MainActivity.Part part=app.new Part(false,40,30,10,20);part.y=15;part.sourceSketchId=source.id;app.parts.add(part);return part;
 }
 @Test public void completeEncodingAndSavedProjectsKeepActualOwnerIdsAcrossReload()throws Exception{
  MainActivity.Part first=fixture();MainActivity.Part second=app.new Part(false,40,30,20,20);second.y=15;second.sourceSketchId=first.sourceSketchId;app.parts.add(second);
  String before=app.encode();assertEquals(before,app.encode());JSONObject encoded=new JSONObject(before);
  JSONArray bindings=encoded.getJSONArray("parametricBodyBindings");assertEquals(first.id,bindings.getJSONObject(0).getString("bodyId"));assertEquals(second.id,bindings.getJSONObject(1).getString("bodyId"));
  app.save();app.load();assertEquals(before,app.encode());assertEquals(before,app.getPreferences(0).getString("design",null));
  assertEquals(first.id,app.parts.get(0).id);assertEquals(second.id,app.parts.get(1).id);
 }
 @Test public void changingDimensionsKeepsTheSameFeatureAndBodyOwners()throws Exception{
  MainActivity.Part part=fixture();JSONObject before=new JSONObject(app.encode());
  es.josxtm.mycad.cad.ParametricSketchDocument a=es.josxtm.mycad.cad.ParametricSketchDocument.importSnapshot(new es.josxtm.mycad.cad.PrimitiveKernel(),before.getString("parametricPreview"));
  float[] old=app.currentMesh().vertices.clone();part.h=12;
  JSONObject after=new JSONObject(app.encode());es.josxtm.mycad.cad.ParametricSketchDocument b=es.josxtm.mycad.cad.ParametricSketchDocument.importSnapshot(new es.josxtm.mycad.cad.PrimitiveKernel(),after.getString("parametricPreview"));
  assertEquals(a.extrusions().keySet(),b.extrusions().keySet());assertEquals(a.bodies().keySet(),b.bodies().keySet());assertTrue(b.bodies().containsKey(part.id));
  assertFalse(java.util.Arrays.equals(old,app.currentMesh().vertices));
 }
}
