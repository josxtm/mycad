package es.josxtm.mycad;

import android.graphics.*;
import es.josxtm.mycad.ui.CadAppearance;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import static org.junit.Assert.*;
import static es.josxtm.mycad.ui.CadAppearance.Role.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AppearancePreviewAndroidTest {
 @Test public void allPreviewCardsRenderTheirOwnGridModelAndSelectionColors()throws Exception{
  Bitmap sheet=Bitmap.createBitmap(480,6*180,Bitmap.Config.ARGB_8888);Canvas page=new Canvas(sheet);
  for(int i=0;i<6;i++){
   CadAppearance palette=i==5?CadAppearance.preset(0).with(MODEL,0xffcb42a1):CadAppearance.preset(i);
   AppearancePreview preview=new AppearancePreview(RuntimeEnvironment.getApplication(),palette);preview.layout(0,0,480,180);
   Bitmap sample=Bitmap.createBitmap(480,180,Bitmap.Config.ARGB_8888);preview.draw(new Canvas(sample));
   assertEquals(palette.color(MODEL),sample.getPixel(220,110));assertEquals(palette.color(CUBE),sample.getPixel(450,94));assertEquals(palette.color(SELECTION),sample.getPixel(125,90));
   page.drawBitmap(sample,0,i*180,null);
  }
  File folder=new File("build/theme-preview");assertTrue(folder.isDirectory()||folder.mkdirs());
  try(FileOutputStream out=new FileOutputStream(new File(folder,"six-themes.png"))){assertTrue(sheet.compress(Bitmap.CompressFormat.PNG,100,out));}
 }

 @Test public void fullCanvasInterfaceRendersAllThemesWithoutChangingTheDesign()throws Exception{
  Bitmap sheet=Bitmap.createBitmap(576*2,900*3,Bitmap.Config.ARGB_8888);Canvas page=new Canvas(sheet);
  for(int i=0;i<6;i++){
   org.robolectric.android.controller.ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity app=controller.get();
   try{
    app.parts.clear();app.sketches.clear();app.parts.add(app.new Part(false,40,30,10,0));
    app.selectAppearance(i,CadAppearance.preset(4).with(MODEL,0xffb73494));
    int width=android.view.View.MeasureSpec.makeMeasureSpec(576,android.view.View.MeasureSpec.EXACTLY),height=android.view.View.MeasureSpec.makeMeasureSpec(900,android.view.View.MeasureSpec.EXACTLY);
    app.rootLayout.measure(width,height);app.rootLayout.layout(0,0,576,900);
    Bitmap bitmap=Bitmap.createBitmap(576,900,Bitmap.Config.ARGB_8888);app.rootLayout.draw(new Canvas(bitmap));page.drawBitmap(bitmap,(i%2)*576,(i/2)*900,null);
   }finally{controller.pause().stop().destroy();}
  }
  File folder=new File("build/theme-preview");assertTrue(folder.isDirectory()||folder.mkdirs());
  try(FileOutputStream out=new FileOutputStream(new File(folder,"six-interfaces.png"))){assertTrue(sheet.compress(Bitmap.CompressFormat.PNG,100,out));}
 }
}
