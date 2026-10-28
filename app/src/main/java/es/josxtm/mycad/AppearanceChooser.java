package es.josxtm.mycad;

import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import es.josxtm.mycad.ui.CadAppearance;
import static es.josxtm.mycad.ui.CadAppearance.Role.*;

/** Transactional appearance editor: preview/Cancel never persist changes. */
final class AppearanceChooser {
 private static final String[] ROLE_NAMES={"Fondo general","Paneles y botones","Área de dibujo","Texto","Acento e iconos activos","Selección y tiradores","Cuadrícula","Eje X","Eje Y","Eje Z","Cuerpos 3D","ViewCube"};
 static void show(MainActivity owner){
  final int[] selected={owner.themeIndex};final CadAppearance[] custom={owner.customAppearance};
  LinearLayout list=column(owner);TextView explanation=new TextView(owner);explanation.setText("Elige una vista previa y pulsa Aplicar. El diseño CAD no cambia.");list.addView(explanation);
  LinearLayout[] cards=new LinearLayout[6];AppearancePreview[] previews=new AppearancePreview[6];
  for(int i=0;i<6;i++){
   final int index=i;CadAppearance palette=i==5?custom[0]:CadAppearance.preset(i);
   LinearLayout card=column(owner);cards[i]=card;card.setTag(AppearanceStyler.PREVIEW);card.setContentDescription("Tema "+CadAppearance.NAMES[i]);
   TextView title=new TextView(owner);title.setText(CadAppearance.NAMES[i]);title.setTextSize(16);title.setTextColor(palette.ink(palette.color(PANEL)));card.addView(title);
   AppearancePreview preview=new AppearancePreview(owner,palette);previews[i]=preview;card.addView(preview,new LinearLayout.LayoutParams(-1,owner.dp(96)));
   card.setOnClickListener(v->{selected[0]=index;decorate(owner,cards,previews,selected[0]);});
   if(i==5){Button edit=new Button(owner);edit.setText("Elegir colores…");edit.setTextColor(palette.ink(palette.color(PANEL)));card.addView(edit);
    edit.setOnClickListener(v->editCustom(owner,custom[0],value->{custom[0]=value;selected[0]=5;previews[5].palette=value;decorate(owner,cards,previews,5);}));}
   list.addView(card,new LinearLayout.LayoutParams(-1,-2));
  }
  decorate(owner,cards,previews,selected[0]);ScrollView scroll=new ScrollView(owner);scroll.addView(list);
  new AppearanceDialogBuilder(owner).setTitle("Apariencia · Temas").setView(scroll)
   .setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->owner.selectAppearance(selected[0],custom[0])).show();
 }
 private static LinearLayout column(MainActivity owner){LinearLayout box=new LinearLayout(owner);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(owner.dp(12),owner.dp(8),owner.dp(12),owner.dp(8));return box;}
 private static void decorate(MainActivity owner,LinearLayout[] cards,AppearancePreview[] previews,int selected){
  for(int i=0;i<cards.length;i++)if(cards[i]!=null){CadAppearance p=previews[i].palette;
   GradientDrawable bg=new GradientDrawable();bg.setColor(p.color(PANEL));bg.setCornerRadius(owner.dp(8));bg.setStroke(owner.dp(i==selected?3:1),p.color(i==selected?ACCENT:GRID));cards[i].setBackground(bg);cards[i].setSelected(i==selected);
   ((TextView)cards[i].getChildAt(0)).setTextColor(p.ink(p.color(PANEL)));previews[i].invalidate();
   if(i==5&&cards[i].getChildCount()>2){View b=cards[i].getChildAt(2);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(p.color(PANEL)));((TextView)b).setTextColor(p.ink(p.color(PANEL)));}
  }
 }
 interface ApplyCustom {void apply(CadAppearance value);}
 private static void editCustom(MainActivity owner,CadAppearance original,ApplyCustom done){
  final CadAppearance[] draft={original};LinearLayout list=column(owner);AppearancePreview preview=new AppearancePreview(owner,original);list.addView(preview,new LinearLayout.LayoutParams(-1,owner.dp(140)));
  for(CadAppearance.Role role:CadAppearance.Role.values()){
   Button button=new Button(owner);button.setText(ROLE_NAMES[role.ordinal()]+" · "+CadAppearance.hex(draft[0].color(role)));button.setContentDescription("Color "+ROLE_NAMES[role.ordinal()]);list.addView(button);
   button.setOnClickListener(v->editColor(owner,ROLE_NAMES[role.ordinal()],draft[0].color(role),value->{draft[0]=draft[0].with(role,value);preview.palette=draft[0];preview.invalidate();button.setText(ROLE_NAMES[role.ordinal()]+" · "+CadAppearance.hex(value));}));
  }
  ScrollView scroll=new ScrollView(owner);scroll.addView(list);
  new AppearanceDialogBuilder(owner).setTitle("Personalizado · Colores").setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Usar colores",(d,w)->done.apply(draft[0])).show();
 }
 interface ApplyColor {void apply(int color);}
 private static void editColor(MainActivity owner,String label,int initial,ApplyColor done){
  LinearLayout box=column(owner);EditText value=new EditText(owner);value.setSingleLine();value.setText(CadAppearance.hex(initial));value.setHint("#RRGGBB");value.setContentDescription("Color hexadecimal");box.addView(value);
  View swatch=new View(owner);swatch.setTag(AppearanceStyler.PREVIEW);swatch.setBackgroundColor(initial);swatch.setContentDescription("Vista previa del color");box.addView(swatch,new LinearLayout.LayoutParams(-1,owner.dp(56)));
  value.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){try{swatch.setBackgroundColor(CadAppearance.parse(s.toString()));value.setError(null);}catch(IllegalArgumentException invalid){value.setError("Usa #RRGGBB");}}public void afterTextChanged(Editable text){}});
  AlertDialog dialog=new AppearanceDialogBuilder(owner).setTitle(label+" · #RRGGBB").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aceptar",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{done.apply(CadAppearance.parse(value.getText().toString()));dialog.dismiss();}catch(IllegalArgumentException invalid){value.setError("Usa #RRGGBB");}}));dialog.show();
 }
}
