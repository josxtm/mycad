package es.josxtm.mycad;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import es.josxtm.mycad.ui.CadAppearance;
import static es.josxtm.mycad.ui.CadAppearance.Role.*;

/** Presentation-only sample. Never constructs or mutates a CAD document. */
final class AppearancePreview extends View {
 CadAppearance palette;private final Paint paint=new Paint(3);
 AppearancePreview(Context context,CadAppearance palette){super(context);this.palette=palette;setTag(AppearanceStyler.PREVIEW);setContentDescription("Vista previa de apariencia");}
 private void color(CadAppearance.Role role){paint.setColor(palette.color(role));}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);float w=getWidth(),h=getHeight();
  canvas.drawColor(palette.color(CANVAS));color(GRID);paint.setStrokeWidth(1);
  for(int i=1;i<9;i++){canvas.drawLine(i*w/9,0,i*w/9,h,paint);canvas.drawLine(0,i*h/9,w,i*h/9,paint);}
  color(PANEL);canvas.drawRect(0,0,w,h*.18f,paint);
  paint.setColor(palette.ink(palette.color(PANEL)));paint.setTextSize(h*.12f);canvas.drawText("MyCAD · mm",w*.03f,h*.14f,paint);
  color(MODEL);canvas.drawRect(w*.26f,h*.38f,w*.62f,h*.76f,paint);
  color(SELECTION);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);canvas.drawRect(w*.26f,h*.38f,w*.62f,h*.76f,paint);paint.setStyle(Paint.Style.FILL);
  color(AXIS_X);canvas.drawLine(w*.12f,h*.86f,w*.29f,h*.86f,paint);color(AXIS_Y);canvas.drawLine(w*.12f,h*.86f,w*.12f,h*.61f,paint);
  color(CUBE);canvas.drawRect(w*.76f,h*.27f,w*.95f,h*.55f,paint);paint.setColor(palette.ink(palette.color(CUBE)));paint.setTextSize(h*.1f);canvas.drawText("ISO",w*.78f,h*.45f,paint);
 }
}
