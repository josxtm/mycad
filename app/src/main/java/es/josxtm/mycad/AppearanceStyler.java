package es.josxtm.mycad;

import android.app.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import es.josxtm.mycad.ui.CadAppearance;
import static es.josxtm.mycad.ui.CadAppearance.Role.*;

final class AppearanceStyler {
 static final String PREVIEW="mycad.appearance.preview";
 static void tree(View root,CadAppearance palette,int surface){
  if(PREVIEW.equals(root.getTag()))return;
  int accent=palette.color(ACCENT);
  if(root instanceof TextView){
   TextView text=(TextView)root;text.setTextColor(palette.ink(surface));text.setHintTextColor(palette.ink(surface));
   text.setHighlightColor((palette.color(SELECTION)&0xffffff)|0x66000000);
   if(root instanceof EditText)root.setBackgroundTintList(ColorStateList.valueOf(accent));
   else if(root instanceof Button){root.setBackgroundTintList(ColorStateList.valueOf(palette.color(PANEL)));text.setTextColor(palette.ink(palette.color(PANEL)));}
  }
  if(root instanceof CompoundButton)((CompoundButton)root).setButtonTintList(ColorStateList.valueOf(accent));
  if(root instanceof SeekBar){((SeekBar)root).setProgressTintList(ColorStateList.valueOf(accent));((SeekBar)root).setThumbTintList(ColorStateList.valueOf(accent));}
  if(root instanceof Spinner){
   Spinner spinner=(Spinner)root;SpinnerAdapter adapter=spinner.getAdapter();
   spinner.setPopupBackgroundDrawable(new ColorDrawable(palette.color(PANEL)));
   if(adapter instanceof AppearanceSpinnerAdapter)((AppearanceSpinnerAdapter)adapter).palette=palette;
   else if(adapter!=null){int selected=spinner.getSelectedItemPosition();spinner.setAdapter(new AppearanceSpinnerAdapter(adapter,palette));spinner.setSelection(selected,false);}
  }
  if(root instanceof AbsListView){
   ((AbsListView)root).setSelector(new ColorDrawable((palette.color(SELECTION)&0xffffff)|0x55000000));
   ((ViewGroup)root).setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){
    public void onChildViewAdded(View parent,View child){tree(child,palette,surface);}
    public void onChildViewRemoved(View parent,View child){}
   });
  }
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)tree(group.getChildAt(i),palette,surface);}
  root.invalidate();
 }
 static void dialog(AlertDialog dialog,CadAppearance palette){
  if(dialog.getWindow()==null)return;
  int panel=palette.color(PANEL);dialog.getWindow().setBackgroundDrawable(new ColorDrawable(panel));
  tree(dialog.getWindow().getDecorView(),palette,panel);
  for(int which:new int[]{-1,-2,-3}){Button button=dialog.getButton(which);if(button!=null)button.setTextColor(palette.ink(panel));}
 }
 static void cube(View cube,CadAppearance p){
  int bg=p.color(CUBE);cube.setBackgroundColor(bg);
  if(cube instanceof ViewGroup){ViewGroup group=(ViewGroup)cube;for(int i=0;i<group.getChildCount();i++){
   View child=group.getChildAt(i);GradientDrawable shape=new GradientDrawable();shape.setColor(bg);shape.setCornerRadius(6);shape.setStroke(1,p.color(GRID));child.setBackground(shape);
   if(child instanceof TextView)((TextView)child).setTextColor(p.ink(bg));
  }}
 }
}
