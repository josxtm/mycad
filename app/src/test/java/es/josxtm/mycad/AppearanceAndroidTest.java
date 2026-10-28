package es.josxtm.mycad;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import android.os.Looper;
import es.josxtm.mycad.ui.CadAppearance;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;
import static es.josxtm.mycad.ui.CadAppearance.Role.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class AppearanceAndroidTest {
 private ActivityController<MainActivity> controller;private MainActivity app;
 @Before public void setup(){controller=Robolectric.buildActivity(MainActivity.class);app=controller.get();app.getPreferences(0).edit().clear().commit();controller.setup();}
 @After public void teardown(){controller.pause().stop().destroy();}
 private void reopen(){controller.pause().stop().destroy();controller=Robolectric.buildActivity(MainActivity.class).setup();app=controller.get();}
 private View find(View root,String text){
  if((root instanceof TextView&&text.contentEquals(((TextView)root).getText()))||text.equals(root.getContentDescription()))return root;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View result=find(group.getChildAt(i),text);if(result!=null)return result;}}
  return null;
 }
 private AlertDialog latest(){shadowOf(Looper.getMainLooper()).idle();return ShadowAlertDialog.getLatestAlertDialog();}
 private void click(AlertDialog dialog,int which){dialog.getButton(which).performClick();shadowOf(Looper.getMainLooper()).idle();}
 private String cadContent(String text)throws Exception{
  org.json.JSONObject document=new org.json.JSONObject(text);
  // Existing advisory integration probes mint temporary UUIDs on every encode.
  // The saved file is checked byte-for-byte separately; compare all actual CAD data.
  document.remove("parametricPreview");document.remove("parametricBodyBindings");document.remove("parametricPreviewCoverageComplete");return document.toString();
 }
 @Test public void clearIsDefaultAndAllSixThemesSurviveActivityRecreation(){
  assertEquals(0,app.themeIndex);assertFalse(app.darkMode);
  CadAppearance custom=CadAppearance.preset(4).with(ACCENT,0xff8844cc).with(SELECTION,0xffb02070);
  for(int index=0;index<6;index++){
   app.selectAppearance(index,custom);reopen();assertEquals(index,app.themeIndex);
   assertArrayEquals((index==5?custom:CadAppearance.preset(index)).colors(),app.appearance.colors());
   assertArrayEquals(custom.colors(),app.customAppearance.colors());
  }
 }
 @Test public void legacyDarkAndFiveThemePreferencesMigrateWithoutChangingDesign(){
  app.getPreferences(0).edit().clear().putBoolean("dark",true).putString("design","[]").commit();reopen();assertEquals(1,app.themeIndex);
  app.getPreferences(0).edit().putInt("themeIndex",3).commit();reopen();assertEquals(3,app.themeIndex);
  app.getPreferences(0).edit().putInt("themeIndex",99).commit();reopen();assertEquals(0,app.themeIndex);
  assertEquals("[]",app.getPreferences(0).getString("design",null));
 }
 @Test public void appearanceNeverChangesCadFileHistoryGeometryCameraOrPickingGeneration()throws Exception{
  app.parts.clear();app.sketches.clear();MainActivity.Sketch sketch=app.new Sketch(2,0,0,40,30);app.sketches.add(sketch);
  MainActivity.Part part=app.new Part(false,40,30,10,20);part.y=15;part.sourceSketchId=sketch.id;app.parts.add(part);
  app.save();app.checkpoint();app.toggleMeshView();
  java.util.concurrent.CountDownLatch settled=new java.util.concurrent.CountDownLatch(1);app.meshView.queueEvent(settled::countDown);assertTrue(settled.await(5,java.util.concurrent.TimeUnit.SECONDS));
  String encoded=cadContent(app.encode()),saved=app.getPreferences(0).getString("design",null);
  float[] triangles=app.currentMesh().vertices.clone();long revision=app.meshView.sceneRevision();float distance=app.meshView.cameraDistance();
  Object topology=app.topologySnapshots;int undo=app.undo.size(),redo=app.redo.size();
  for(int index=0;index<6;index++){
   app.selectAppearance(index,CadAppearance.preset(4));assertSame(part,app.parts.get(0));assertSame(sketch,app.sketches.get(0));
   assertEquals(encoded,cadContent(app.encode()));assertEquals(saved,app.getPreferences(0).getString("design",null));
   assertArrayEquals(triangles,app.currentMesh().vertices,0);assertEquals(undo,app.undo.size());assertEquals(redo,app.redo.size());
   assertEquals(revision,app.meshView.sceneRevision());assertEquals(distance,app.meshView.cameraDistance(),0);assertSame(topology,app.topologySnapshots);
   assertSame(app.appearance,app.meshView.appearance());
  }
 }
 @Test public void previewSelectionAndCancelDoNotPersistOrApply(){
  app.chooseTheme();AlertDialog dialog=latest();View row=find(dialog.getWindow().getDecorView(),"Tema Verde Oliva");assertNotNull(row);row.performClick();
  assertEquals(0,app.themeIndex);assertFalse(app.getPreferences(0).contains("themeIndex"));
  click(dialog,-2);assertEquals(0,app.themeIndex);
  app.chooseTheme();dialog=latest();find(dialog.getWindow().getDecorView(),"Tema Azul Técnico").performClick();click(dialog,-1);assertEquals(2,app.themeIndex);reopen();assertEquals(2,app.themeIndex);
 }
 @Test public void customColorEditorValidatesPreviewsAndCommitsOnlyOnFinalApply(){
  app.chooseTheme();AlertDialog chooser=latest();find(chooser.getWindow().getDecorView(),"Elegir colores…").performClick();AlertDialog custom=latest();
  find(custom.getWindow().getDecorView(),"Color Fondo general").performClick();AlertDialog color=latest();
  EditText value=(EditText)find(color.getWindow().getDecorView(),"Color hexadecimal");value.setText("wrong");click(color,-1);assertTrue(color.isShowing());assertNotNull(value.getError());
  value.setText("#123456");click(color,-1);assertFalse(color.isShowing());
  assertEquals(0,app.themeIndex);assertFalse(app.getPreferences(0).contains("themeIndex"));
  click(custom,-1);assertEquals(0,app.themeIndex);click(chooser,-1);
  assertEquals(5,app.themeIndex);assertEquals(0xff123456,app.appearance.color(BACKGROUND));reopen();assertEquals(0xff123456,app.appearance.color(BACKGROUND));
 }
 @Test public void widgetsViewCubeAndNativeDialogsUseTheSelectedPalette(){
  app.selectAppearance(4,CadAppearance.preset(0));app.ensureMeshView();assertSame(app.appearance,app.meshView.appearance());
  Button mode=(Button)find(app.rootLayout,"NAVEGAR");assertNotNull(mode);assertEquals(app.appearance.color(PANEL),mode.getBackgroundTintList().getDefaultColor());
  assertEquals(app.appearance.ink(app.appearance.color(BACKGROUND)),app.titleView.getCurrentTextColor());
  assertEquals(app.appearance.ink(app.appearance.color(CUBE)),((TextView)((ViewGroup)app.gpuCube).getChildAt(0)).getCurrentTextColor());
  app.settings();AlertDialog dialog=latest();View decor=dialog.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));decor.layout(0,0,600,900);shadowOf(Looper.getMainLooper()).idle();
  assertNotNull(dialog.getListView().getChildAt(0));TextView row=(TextView)find(dialog.getListView().getChildAt(0),"Apariencia · Sepia / Papel Técnico");
  assertNotNull(row);assertEquals(app.appearance.ink(app.appearance.color(PANEL)),row.getCurrentTextColor());
 }
 @Test public void corruptCustomTransparencyFallsBackToAnOpaqueSafeColor(){
  app.getPreferences(0).edit().putInt("themeIndex",5).putInt("appearance.custom.BACKGROUND",0x00123456).commit();reopen();assertEquals(0xfff8fafd,app.appearance.color(BACKGROUND));
 }

 @Test public void dropdownPalettePreservesAxisDataAndSelectedItem(){
  Spinner choice=new Spinner(app);ArrayAdapter<String> values=new ArrayAdapter<>(app,android.R.layout.simple_spinner_dropdown_item,new String[]{"X","Y","Z"});choice.setAdapter(values);choice.setSelection(2);
  CadAppearance palette=CadAppearance.preset(4);AppearanceStyler.tree(choice,palette,palette.color(PANEL));
  assertEquals(2,choice.getSelectedItemPosition());assertEquals("Z",choice.getSelectedItem());
  assertEquals(3,choice.getAdapter().getCount());TextView row=(TextView)choice.getAdapter().getDropDownView(2,null,new LinearLayout(app));
  assertEquals(palette.ink(palette.color(PANEL)),row.getCurrentTextColor());assertEquals("Z",row.getText().toString());
  Object adapter=choice.getAdapter();AppearanceStyler.tree(choice,CadAppearance.preset(2),CadAppearance.preset(2).color(PANEL));assertSame(adapter,choice.getAdapter());assertEquals(2,choice.getSelectedItemPosition());
 }

 @Test public void systemBarsFollowTheRootSurfaceUnderAndroid15EdgeToEdge(){
  CadAppearance mixed=CadAppearance.preset(1).with(PANEL,0xffffffff);app.selectAppearance(5,mixed);
  assertTrue(app.appearance.dark());assertEquals(0,app.getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  app.selectAppearance(5,mixed.with(BACKGROUND,0xffffffff));assertEquals(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR,app.getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
 }
}
