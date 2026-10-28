package es.josxtm.mycad;

import android.app.*;
import android.view.*;

/** One dialog styling path; does not replace clients' existing show/click listeners. */
final class AppearanceDialogBuilder extends AlertDialog.Builder {
 private final MainActivity owner;
 AppearanceDialogBuilder(MainActivity owner){super(owner,owner.appearance.dark()?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);this.owner=owner;}
 @Override public AlertDialog create(){
  AlertDialog dialog=super.create();
  View decor=dialog.getWindow().getDecorView();
  decor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
   public void onViewAttachedToWindow(View v){AppearanceStyler.dialog(dialog,owner.appearance);}
   public void onViewDetachedFromWindow(View v){}
  });
  return dialog;
 }
 @Override public AlertDialog show(){AlertDialog dialog=create();dialog.show();AppearanceStyler.dialog(dialog,owner.appearance);return dialog;}
}
