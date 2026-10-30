package es.josxtm.mycad;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import es.josxtm.mycad.cad.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowGLSurfaceView;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Replays the shipped QA fixtures through production file/dialog/renderer APIs.
 * JVM Android coverage only: no EGL, installation or physical-device acceptance. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,shadows=Release062ValidationAndroidTest.ImmediateGlQueue.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class Release062ValidationAndroidTest {
 private ActivityController<MainActivity> controller;
 private MainActivity app;
 private static final String[] CONTOURS={"01-contorno-xy-40x30", "02-contorno-xz-40x30",
     "03-contorno-yz-40x30", "04-contorno-concavo-xy"};
 private static Path fixtures(){
  Path p=Paths.get("../docs/fixtures/android-0.6.2");
  return Files.isDirectory(p)?p:Paths.get("docs/fixtures/android-0.6.2");
 }
 @Before public void setup(){
  controller=Robolectric.buildActivity(MainActivity.class);app=controller.get();
  app.getPreferences(0).edit().clear().commit();controller.setup();
 }
 @After public void teardown(){controller.pause().stop().destroy();}
 private void awaitFile()throws Exception{
  long deadline=System.nanoTime()+10_000_000_000L;
  while(app.fileBusy&&System.nanoTime()<deadline){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
  shadowOf(Looper.getMainLooper()).idle();assertFalse("File callback did not finish",app.fileBusy);
 }
 private void open(File file)throws Exception{
  app.onActivityResult(MainActivity.OPEN_FILE,MainActivity.RESULT_OK,new Intent().setData(Uri.fromFile(file)));
  awaitFile();
 }
 private void fixture(String name)throws Exception{
  open(fixtures().resolve(name+".mycad.json").toFile());
  assertTrue(app.info.getText().toString(),app.info.getText().toString().startsWith("Archivo abierto"));
 }
 private EditText input(View v){
  if(v instanceof EditText)return (EditText)v;
  if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){
   EditText e=input(((ViewGroup)v).getChildAt(i));if(e!=null)return e;
  }
  return null;
 }
 private void extrude(int height){
  app.selectedSketch=0;app.sketchMenu();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();
  menu.getListView().performItemClick(null,8,8);
  AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
  assertNotNull(input(dialog.getWindow().getDecorView()));
  input(dialog.getWindow().getDecorView()).setText(Integer.toString(height));
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();
 }
 private PolygonPrismBRep solid(){
  LoopExtrusionFeature f=app.loopExtrusionFeatures.get(0);
  return f.asBRep(app.loopSketchSegments(f,null,0,0,0,0));
 }
 // Execute the real scene installation on this JVM thread, without an EGL driver.
 @Implements(GLSurfaceView.class)
 public static class ImmediateGlQueue extends ShadowGLSurfaceView {
  @Implementation protected void queueEvent(Runnable task){task.run();}
 }
 private void gpu(){app.meshView=new MeshEngine(app);app.meshMode=true;app.renderCurrentCadMesh();}
 @SuppressWarnings("unchecked") private List<MeshEngine.Mesh> displayed()throws Exception{
  Field f=MeshEngine.class.getDeclaredField("meshes");f.setAccessible(true);
  return new ArrayList<>((List<MeshEngine.Mesh>)f.get(app.meshView));
 }
 private void assertSceneMatchesDocument()throws Exception{
  List<MeshEngine.Mesh> expected=app.currentSceneMeshes(),actual=displayed();
  assertEquals("GPU scene must replace the prior document",expected.size(),actual.size());
  for(int i=0;i<expected.size();i++)assertArrayEquals(expected.get(i).worldCoordinates(),actual.get(i).worldCoordinates(),0);
 }
 @Test public void fixtureBytesMatchThePublishedManifest()throws Exception{
  JSONObject manifest=new JSONObject(new String(Files.readAllBytes(fixtures().resolve("MANIFEST.json")),StandardCharsets.UTF_8));
  assertEquals("1e2cafa5036e4cdc6a14b333324568f189310b27",manifest.getString("source_commit"));
  JSONArray files=manifest.getJSONArray("files");assertEquals(6,files.length());
  for(int i=0;i<files.length();i++){
   JSONObject entry=files.getJSONObject(i);byte[] bytes=Files.readAllBytes(fixtures().resolve(entry.getString("path")));
   assertEquals(entry.getInt("bytes"),bytes.length);
   StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(Locale.ROOT,"%02x",b&255));
   assertEquals(entry.getString("sha256"),hash.toString());
  }
 }
 @Test public void actualFixturesExtrudeAcrossPlanesSignsAndConcavity()throws Exception{
  for(int index=0;index<CONTOURS.length;index++)for(int sign:new int[]{-1,1}){
   fixture(CONTOURS[index]);extrude(sign*10);
   assertEquals(1,app.booleanMeshes.size());PolygonPrismBRep body=solid();
   assertEquals(index==3?0:index,body.plane());assertEquals(sign*10,body.distance(),0);
   assertEquals(index==3?8000:12000,body.volume(),1e-8);
   assertTrue(TriangleMeshHealth.inspect(body.triangles()).isWatertight());
   assertEquals(2,body.vertices().size()-body.edges().size()+body.faces().size());
   String id=body.id(),saved=app.encode();app.save();app.load();
   assertEquals(saved,app.encode());assertEquals(id,solid().id());
  }
 }
 @Test public void openFileReplacesTheVisibleGpuSceneAndItsSelection()throws Exception{
  fixture(CONTOURS[0]);extrude(10);gpu();long oldRevision=app.meshView.sceneRevision();
  app.selectedStoredMeshIndex=0;app.selectedStoredMeshObject=app.booleanMeshes.get(0);app.isolatedMeshPreview=true;
  fixture("05-resta-xy-40x30x10");
  assertTrue("Opening must invalidate old GPU geometry",app.meshView.sceneRevision()>oldRevision);
  assertSceneMatchesDocument();assertEquals(-1,app.selectedStoredMeshIndex);assertFalse(app.isolatedMeshPreview);
  assertEquals(3,app.booleanMeshes.size());
  NamedGridHoleBody hole=app.renderedPlanarBindings.get(0).identity;
  assertEquals(1,hole.shell.holeCount);assertEquals(11000,hole.shell.volume,0);
 }
 @Test public void externalRoundtripAndConfirmedNewDesignRefreshGpuWithoutToggling()throws Exception{
  fixture(CONTOURS[0]);extrude(10);gpu();String encoded=app.encode(),id=solid().id();
  File copy=File.createTempFile("qa062-", ".mycad.json",app.getCacheDir());
  try{
   app.exportFile();app.onActivityResult(MainActivity.SAVE_FILE,MainActivity.RESULT_OK,new Intent().setData(Uri.fromFile(copy)));awaitFile();
   assertEquals(encoded,new String(Files.readAllBytes(copy.toPath()),StandardCharsets.UTF_8));
   long oldRevision=app.meshView.sceneRevision();app.newDesign();
   ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
   shadowOf(Looper.getMainLooper()).idle();
   assertTrue(app.booleanMeshes.isEmpty());assertTrue(app.sketches.isEmpty());
   assertTrue("New design must invalidate the visible model",app.meshView.sceneRevision()>oldRevision);
   assertSceneMatchesDocument();for(MeshEngine.Mesh m:displayed())assertEquals(0,m.triangles());
   open(copy);assertEquals(encoded,app.encode());assertEquals(id,solid().id());assertEquals(12000,solid().volume(),0);
   assertSceneMatchesDocument();
  }finally{assertTrue(copy.delete());}
 }
 @Test public void invalidVersionAndCancelledSelectorsPreserveGeometryHistoryAndGpu()throws Exception{
  fixture(CONTOURS[0]);extrude(10);gpu();String before=app.encode();
  int revisions=app.undo.size();long gpuRevision=app.meshView.sceneRevision();
  open(fixtures().resolve("06-version-invalida-rechazo-esperado.mycad.json").toFile());
  assertTrue(app.info.getText().toString().contains("El diseño actual se conserva"));
  for(int request:new int[]{MainActivity.SAVE_FILE,MainActivity.OPEN_FILE}){
   if(request==MainActivity.SAVE_FILE)app.exportFile();
   app.onActivityResult(request,MainActivity.RESULT_CANCELED,null);
  }
  assertNull(app.pendingExport);assertEquals(before,app.encode());assertEquals(revisions,app.undo.size());
  assertEquals(gpuRevision,app.meshView.sceneRevision());assertSceneMatchesDocument();
 }
 @Test public void savedDraftSurvivesAFreshActivityWithStableBodyIdentity()throws Exception{
  fixture(CONTOURS[2]);extrude(-10);String id=solid().id(),saved=app.encode();app.save();
  controller.pause().stop().destroy();controller=Robolectric.buildActivity(MainActivity.class).setup();app=controller.get();
  assertEquals(saved,app.encode());assertEquals(id,solid().id());assertEquals(-10,solid().distance(),0);
  assertEquals(12000,solid().volume(),0);
 }
}
