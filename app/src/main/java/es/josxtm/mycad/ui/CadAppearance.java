package es.josxtm.mycad.ui;

import java.util.*;

/** Immutable presentation values. No dependency on documents, coordinates or CAD history. */
public final class CadAppearance {
 public enum Role { BACKGROUND, PANEL, CANVAS, TEXT, ACCENT, SELECTION, GRID, AXIS_X, AXIS_Y, AXIS_Z, MODEL, CUBE }
 public static final String[] NAMES={"Claro","Oscuro","Azul Técnico","Verde Oliva","Sepia / Papel Técnico","Personalizado"};
 private static final int[][] PRESETS={
  {0xfff8fafd,0xffe9eff6,0xfff2f5f9,0xff192637,0xff1263ae,0xffae5500,0xffa8b6c8,0xffba2537,0xff197539,0xff225fc2,0xff6996bd,0xffdde7f2},
  {0xff0e141e,0xff202e40,0xff151e2b,0xfff7faff,0xff8dcfff,0xffffcd5d,0xff52657d,0xffff808a,0xff8fe39e,0xff91baff,0xff65bdcf,0xff2b3d54},
  {0xff0a2341,0xff12395b,0xff0c2946,0xffebf5ff,0xff7edbff,0xffffd077,0xff446d8d,0xffff8797,0xff94e6b0,0xff98c9ff,0xff78bbdf,0xff1c486a},
  {0xff1c2a1d,0xff30442e,0xff233323,0xffe9f4e0,0xffc3de95,0xffffda89,0xff62795a,0xffffa391,0xffb4e38e,0xff9fcfff,0xff96b384,0xff3b5034},
  {0xfff5e7cb,0xffe7d0aa,0xfff8edd8,0xff41311e,0xff755027,0xffa43b17,0xffb6a27d,0xffa52b30,0xff47652d,0xff345e92,0xffb7915c,0xffe8d8b9}
 };
 private final int[] colors;
 private CadAppearance(int[] values){
  if(values==null||values.length!=Role.values().length)throw new IllegalArgumentException("Invalid appearance palette");
  colors=values.clone();for(int color:colors)if((color>>>24)!=255)throw new IllegalArgumentException("Opaque colors required");
 }
 public static int normalize(int index){return index>=0&&index<NAMES.length?index:0;}
 public static CadAppearance preset(int index){return new CadAppearance(PRESETS[index>=0&&index<5?index:0]);}
 public static CadAppearance custom(int[] colors){return new CadAppearance(colors);}
 public int color(Role role){if(role==null)throw new IllegalArgumentException("Missing color role");return colors[role.ordinal()];}
 public int[] colors(){return colors.clone();}
 public CadAppearance with(Role role,int color){int[] copy=colors();copy[role.ordinal()]=color;return custom(copy);}
 public boolean dark(){return luminance(color(Role.BACKGROUND))<.3;}
 /** Preserve chosen ink where legible; mixed custom surfaces get a safe foreground. */
 public int ink(int background){int text=color(Role.TEXT);return contrast(text,background)>=4.5?text:onColor(background);}
 public static int onColor(int background){return contrast(0xff000000,background)>contrast(0xffffffff,background)?0xff000000:0xffffffff;}
 private static double channel(int c){double v=c/255.;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
 public static double luminance(int c){return .2126*channel((c>>>16)&255)+.7152*channel((c>>>8)&255)+.0722*channel(c&255);}
 public static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 public static int parse(String value){
  if(value==null||!value.trim().matches("#?[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Usa un color #RRGGBB");
  return 0xff000000|(int)Long.parseLong(value.trim().replace("#",""),16);
 }
 public static String hex(int value){return String.format(Locale.ROOT,"#%06X",value&0xffffff);}
 public static int shade(int value,double scale){
  return 0xff000000|((int)Math.min(255,((value>>>16)&255)*scale)<<16)|((int)Math.min(255,((value>>>8)&255)*scale)<<8)|(int)Math.min(255,(value&255)*scale);
 }
}
