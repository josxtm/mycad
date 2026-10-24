package es.josxtm.mycad;
import android.app.*;
import android.os.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
 ArrayList<Part> parts=new ArrayList<>(); Scene scene; int selected=-1; TextView info; boolean flat=false;
 class Part { boolean circle; float w,d,h,x; Part(boolean c,float a,float b,float z,float p){circle=c;w=a;d=b;h=z;x=p;} }
 public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(10,28,10,10);root.setBackgroundColor(Color.rgb(18,25,36));
 TextView title=new TextView(this);title.setText("MyCAD · 0.1");title.setTextSize(24);title.setTextColor(Color.WHITE);root.addView(title);
 info=new TextView(this);info.setTextColor(Color.LTGRAY);info.setText("Medidas en mm · Arrastra para girar · Pellizca para zoom");root.addView(info);
 scene=new Scene();root.addView(scene,new LinearLayout.LayoutParams(-1,0,1));
 LinearLayout row=new LinearLayout(this);root.addView(row);button(row,"Rectángulo",()->edit(false,false));button(row,"Círculo",()->edit(true,false));button(row,"2D / 3D",()->{flat=!flat;scene.invalidate();});
 LinearLayout row2=new LinearLayout(this);root.addView(row2);button(row2,"Piezas",()->choose());button(row2,"Editar",()->{if(selected>=0)edit(parts.get(selected).circle,true);else toast("Selecciona una pieza");});button(row2,"Eliminar",()->{if(selected>=0){parts.remove(selected);selected=-1;save();scene.invalidate();}});
 LinearLayout row3=new LinearLayout(this);root.addView(row3);button(row3,"Guardar",()->{save();toast("Diseño guardado en este móvil");});button(row3,"Abrir",()->load());button(row3,"Centrar",()->{scene.zoom=3;scene.yaw=.65f;scene.pitch=.6f;scene.invalidate();});
 setContentView(root);load();
 }
 void button(LinearLayout l,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setTextSize(12);l.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->r.run());}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 void choose(){if(parts.isEmpty()){toast("Añade una figura");return;}String[] a=new String[parts.size()];for(int i=0;i<a.length;i++){Part p=parts.get(i);a[i]=(i+1)+". "+(p.circle?"Círculo":"Rectángulo")+" "+p.w+" × "+p.d+" × "+p.h+" mm";}new AlertDialog.Builder(this).setTitle("Seleccionar pieza").setItems(a,(d,n)->{selected=n;scene.invalidate();}).show();}
 void edit(boolean circle,boolean existing){
 Part p=existing?parts.get(selected):new Part(circle,40,30,10,parts.size()*50);
 LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(32,12,32,12);
 String[] labels={circle?"Diámetro (mm)":"Ancho (mm)","Fondo (mm)","Espesor (mm)","Posición X (mm)"};
 float[] vals={p.w,p.d,p.h,p.x};EditText[] fields=new EditText[4];
 for(int i=0;i<4;i++){TextView t=new TextView(this);t.setText(labels[i]);box.addView(t);fields[i]=new EditText(this);fields[i].setInputType(8194|4096);fields[i].setText(""+vals[i]);box.addView(fields[i]);if(circle&&i==1){t.setVisibility(View.GONE);fields[i].setVisibility(View.GONE);}}
 AlertDialog dlg=new AlertDialog.Builder(this).setTitle(existing?"Editar pieza":"Figura y extrusión").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",null).create();
 dlg.setOnShowListener(v->dlg.getButton(-1).setOnClickListener(w->{try{float a=Float.parseFloat(fields[0].getText().toString().replace(',','.'));float d=circle?a:Float.parseFloat(fields[1].getText().toString().replace(',','.'));float h=Float.parseFloat(fields[2].getText().toString().replace(',','.'));float x=Float.parseFloat(fields[3].getText().toString().replace(',','.'));if(!Float.isFinite(a+d+h+x)||a<=0||d<=0||h<=0||a>10000||d>10000||h>10000||Math.abs(x)>10000)throw new Exception();Part q=new Part(circle,a,d,h,x);if(existing)parts.set(selected,q);else{parts.add(q);selected=parts.size()-1;}save();scene.invalidate();dlg.dismiss();}catch(Exception e){toast("Introduce medidas positivas hasta 10000 mm y posición válida");}}));dlg.show();
 }
 void save(){try{JSONArray a=new JSONArray();for(Part p:parts)a.put(new JSONObject().put("c",p.circle).put("w",p.w).put("d",p.d).put("h",p.h).put("x",p.x));getPreferences(0).edit().putString("design",a.toString()).apply();}catch(Exception e){toast("No se pudo guardar");}}
 void load(){try{JSONArray a=new JSONArray(getPreferences(0).getString("design","[]"));ArrayList<Part> list=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);list.add(new Part(p.getBoolean("c"),(float)p.getDouble("w"),(float)p.getDouble("d"),(float)p.getDouble("h"),(float)p.getDouble("x")));}parts=list;selected=parts.isEmpty()?-1:0;scene.invalidate();}catch(Exception e){toast("No se pudo abrir el diseño");}}
 class Face {float[][] vertices;float depth;int color;Face(float[][] v,int c){vertices=v;color=c;for(float[] p:v)depth+=p[2]/v.length;}}
 class Scene extends View{
 Paint paint=new Paint(3);float yaw=.65f,pitch=.6f,zoom=3,lastX,lastY;ScaleGestureDetector scale;
 Scene(){super(MainActivity.this);scale=new ScaleGestureDetector(MainActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){zoom=Math.max(.02f,Math.min(40,zoom*d.getScaleFactor()));invalidate();return true;}});}
 float[] project(float x,float y,float z){if(flat)return new float[]{getWidth()/2f+x*zoom,getHeight()/2f-y*zoom,z};float u=(float)(x*Math.cos(yaw)-y*Math.sin(yaw));float v=(float)(x*Math.sin(yaw)+y*Math.cos(yaw));return new float[]{getWidth()/2f+u*zoom,getHeight()/2f+(float)(v*Math.sin(pitch)-z*Math.cos(pitch))*zoom,(float)(v*Math.cos(pitch)+z*Math.sin(pitch))};}
 protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.rgb(24,33,46));paint.setStrokeWidth(1);paint.setColor(Color.rgb(44,57,73));for(int i=-200;i<=200;i+=10){float[] a=project(i,-200,0),b=project(i,200,0);c.drawLine(a[0],a[1],b[0],b[1],paint);a=project(-200,i,0);b=project(200,i,0);c.drawLine(a[0],a[1],b[0],b[1],paint);}
 ArrayList<Face> faces=new ArrayList<>();
 for(int j=0;j<parts.size();j++){Part p=parts.get(j);int n=p.circle?64:4;float[][] low=new float[n][],high=new float[n][];
 for(int i=0;i<n;i++){float x,y;if(p.circle){x=(float)Math.cos(i*2*Math.PI/n)*p.w/2;y=(float)Math.sin(i*2*Math.PI/n)*p.d/2;}else{x=(i==0||i==3)?-p.w/2:p.w/2;y=i<2?-p.d/2:p.d/2;}low[i]=project(x+p.x,y,0);high[i]=project(x+p.x,y,p.h);}
 int col=j==selected?Color.rgb(69,186,207):Color.rgb(105,135,165);
 if(!flat)for(int i=0;i<n;i++){int k=(i+1)%n;faces.add(new Face(new float[][]{low[i],low[k],high[k],high[i]},col));}
 faces.add(new Face(high,j==selected?Color.rgb(112,220,229):Color.rgb(154,180,204)));
 }
 Collections.sort(faces,(a,b)->Float.compare(a.depth,b.depth));
 for(Face f:faces){Path path=new Path();path.moveTo(f.vertices[0][0],f.vertices[0][1]);for(int i=1;i<f.vertices.length;i++)path.lineTo(f.vertices[i][0],f.vertices[i][1]);path.close();paint.setStyle(Paint.Style.FILL);paint.setColor(f.color);c.drawPath(path,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.rgb(23,65,85));c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}
 paint.setColor(Color.WHITE);paint.setTextSize(30);c.drawText(flat?"PLANTA · 2D":"VISTA · 3D",20,40,paint);if(parts.isEmpty())c.drawText("Añade una figura para empezar",20,getHeight()/2f,paint);
 }
 public boolean onTouchEvent(android.view.MotionEvent e){scale.onTouchEvent(e);if(e.getActionMasked()==0){lastX=e.getX();lastY=e.getY();}else if(e.getActionMasked()==2){if(e.getPointerCount()==1&&!scale.isInProgress()){yaw+=(e.getX()-lastX)*.01f;pitch=Math.max(-1.5f,Math.min(1.5f,pitch+(e.getY()-lastY)*.01f));invalidate();}lastX=e.getX();lastY=e.getY();}return true;}
 }
}
