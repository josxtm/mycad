package es.josxtm.mycad;

import android.content.SharedPreferences;
import es.josxtm.mycad.ui.CadAppearance;

/** Preferences only: never reads or writes the CAD design, undo, or geometry. */
final class AppearancePreferences {
 final int index;final CadAppearance palette,custom;
 private AppearancePreferences(int index,CadAppearance custom){this.index=index;this.custom=custom;palette=index==5?custom:CadAppearance.preset(index);}
 static AppearancePreferences load(SharedPreferences prefs){
  int selected=CadAppearance.normalize(prefs.getInt("themeIndex",prefs.getBoolean("dark",false)?1:0));
  int[] colors=CadAppearance.preset(0).colors();
  for(CadAppearance.Role role:CadAppearance.Role.values()){
   int value=prefs.getInt("appearance.custom."+role.name(),colors[role.ordinal()]);
   if((value>>>24)==255)colors[role.ordinal()]=value;
  }
  return new AppearancePreferences(selected,CadAppearance.custom(colors));
 }
 static void save(SharedPreferences prefs,int index,CadAppearance custom){
  int valid=CadAppearance.normalize(index);CadAppearance active=valid==5?custom:CadAppearance.preset(valid);
  SharedPreferences.Editor edit=prefs.edit().putInt("themeIndex",valid).putBoolean("dark",active.dark());
  for(CadAppearance.Role role:CadAppearance.Role.values())edit.putInt("appearance.custom."+role.name(),custom.color(role));
  edit.apply();
 }
}
