package es.josxtm.mycad;
import android.app.*;
import android.os.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import android.content.Intent;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
 ArrayList<Part> parts=new ArrayList<>(); Scene scene; int selected=-1; TextView info; boolean flat=false; ArrayDeque<String> undo=new ArrayDeque<>(),redo=new ArrayDeque<>(); String pendingExport; boolean fileBusy=false; TextView nextStep;
 static final int SAVE_FILE=101, OPEN_FILE=102;
 class Part { boolean circle; float w,d,h,x,y,z; Part(boolean c,float a,float b,float height,float p){circle=c;w=a;d=b;h=height;x=p;} }
 public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(10,28,10,10);root.setBackgroundColor(Color.rgb(18,25,36));
 TextView title=new TextView(this);title.setText("MyCAD · 0.4.1");title.setTextSize(17);title.setTextColor(Color.WHITE);
 LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top);
 top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
 icon(top,"↶","Deshacer",()->history(true));icon(top,"↷","Rehacer",()->history(false));icon(top,"▣","Guardar archivo",()->exportFile());icon(top,"⋮","Menú de proyecto",()->projectMenu());
 info=new TextView(this);info.setTextColor(Color.LTGRAY);info.setTextSize(11);info.setText("Toca para seleccionar · Mantén pulsado para mover · Pellizca para zoom");root.addView(info);
 LinearLayout workspace=new LinearLayout(this);root.addView(workspace,new LinearLayout.LayoutParams(-1,0,1));
 LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);workspace.addView(left);
 icon(left,"□","Crear rectángulo",()->edit(false,false));icon(left,"◯","Crear círculo",()->edit(true,false));icon(left,"✎","Editar pieza",()->{if(selected>=0)edit(parts.get(selected).circle,true);else toast("Selecciona una pieza");});icon(left,"✥","Mover XYZ",()->positionSelected());icon(left,"⌫","Eliminar pieza",()->{if(selected>=0){checkpoint();parts.remove(selected);selected=-1;save();scene.invalidate();}else toast("Selecciona una pieza");});
 scene=new Scene();workspace.addView(scene,new LinearLayout.LayoutParams(0,-1,1));
 LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);workspace.addView(right);
 icon(right,"☷","Lista de piezas",()->choose());icon(right,"◇","Alternar vista 2D / 3D",()->{flat=!flat;scene.invalidate();});icon(right,"⊡","Centrar vista",()->{scene.zoom=3;scene.yaw=.65f;scene.pitch=.6f;scene.invalidate();});icon(right,"?","Guía",()->guide());
 nextStep=new TextView(this);nextStep.setTextColor(Color.rgb(112,220,229));nextStep.setTextSize(11);root.addView(nextStep);
 root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(16+insets.getSystemWindowInsetLeft(),16+insets.getSystemWindowInsetTop(),16+insets.getSystemWindowInsetRight(),16+insets.getSystemWindowInsetBottom());return insets;});setContentView(root);root.requestApplyInsets();load();if(b!=null)pendingExport=b.getString("pendingExport");updateStep();
 }
 void button(LinearLayout l,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setTextSize(12);l.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->{if(fileBusy){toast("Espera a que termine la operación de archivo");return;}r.run();});}
 void icon(LinearLayout bar,String symbol,String name,Runnable action){TextView v=new TextView(this);v.setText(symbol);v.setTextSize(24);v.setTextColor(Color.rgb(224,235,245));v.setGravity(Gravity.CENTER);v.setContentDescription(name);v.setBackgroundColor(Color.rgb(34,47,64));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(44),dp(48));p.setMargins(dp(2),dp(2),dp(2),dp(2));bar.addView(v,p);v.setOnClickListener(w->{if(fileBusy){toast("Operación de archivo en curso");return;}action.run();});v.setOnLongClickListener(w->{toast(name);return true;});}
 int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 void projectMenu(){String[] items={"Nuevo diseño","Abrir archivo","Guardar archivo","Guía de uso","Plan de versiones"};new AlertDialog.Builder(this).setTitle("Proyecto MyCAD").setItems(items,(d,n)->{switch(n){case 0:newDesign();break;case 1:confirmOpen();break;case 2:exportFile();break;case 3:guide();break;case 4:roadmap();break;}}).show();}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 void choose(){if(parts.isEmpty()){toast("Añade una figura");return;}String[] a=new String[parts.size()];for(int i=0;i<a.length;i++){Part p=parts.get(i);a[i]=(i+1)+". "+(p.circle?"Círculo":"Rectángulo")+" "+p.w+" × "+p.d+" × "+p.h+" mm";}new AlertDialog.Builder(this).setTitle("Seleccionar pieza").setItems(a,(d,n)->{selected=n;scene.invalidate();}).show();}
 void edit(boolean circle,boolean existing){
 if(fileBusy)return;if(!existing&&parts.size()>=500){toast("Máximo 500 piezas");return;}
 Part p=existing?parts.get(selected):new Part(circle,40,30,10,parts.size()*50);
 LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(32,12,32,12);
 String[] labels={circle?"Diámetro (mm)":"Ancho (mm)","Fondo (mm)","Espesor (mm)","Posición X (mm)"};
 float[] vals={p.w,p.d,p.h,p.x};EditText[] fields=new EditText[4];
 for(int i=0;i<4;i++){TextView t=new TextView(this);t.setText(labels[i]);box.addView(t);fields[i]=new EditText(this);fields[i].setInputType(8194|4096);fields[i].setText(""+vals[i]);box.addView(fields[i]);if(circle&&i==1){t.setVisibility(View.GONE);fields[i].setVisibility(View.GONE);}}
 AlertDialog dlg=new AlertDialog.Builder(this).setTitle(existing?"Editar pieza":"Figura y extrusión").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",null).create();
 dlg.setOnShowListener(v->dlg.getButton(-1).setOnClickListener(w->{try{float a=Float.parseFloat(fields[0].getText().toString().replace(',','.'));float d=circle?a:Float.parseFloat(fields[1].getText().toString().replace(',','.'));float h=Float.parseFloat(fields[2].getText().toString().replace(',','.'));float x=Float.parseFloat(fields[3].getText().toString().replace(',','.'));if(!Float.isFinite(a+d+h+x)||a<=0||d<=0||h<=0||a>10000||d>10000||h>10000||Math.abs(x)>10000)throw new Exception();Part q=new Part(circle,a,d,h,x);checkpoint();if(existing)parts.set(selected,q);else{parts.add(q);selected=parts.size()-1;}save();scene.invalidate();dlg.dismiss();}catch(Exception e){toast("Introduce medidas positivas hasta 10000 mm y posición válida");}}));dlg.show();
 }

 void updateStep(){if(nextStep!=null)nextStep.setText(parts.isEmpty()?"1 · Crea una figura con medidas":"2 · Edita / revisa en 3D   →   3 · Guarda un archivo");}
 String encode() throws Exception {
 JSONArray a=new JSONArray();for(Part p:parts)a.put(new JSONObject().put("c",p.circle).put("w",p.w).put("d",p.d).put("h",p.h).put("x",p.x).put("y",p.y).put("z",p.z));
 return new JSONObject().put("format","mycad").put("version",1).put("units","mm").put("parts",a).toString(2);
 }
 ArrayList<Part> decode(String data) throws Exception {
 Object raw=new JSONTokener(data).nextValue();JSONArray a;
 if(raw instanceof JSONArray)a=(JSONArray)raw;
 else if(raw instanceof JSONObject){JSONObject o=(JSONObject)raw;if(!"mycad".equals(o.getString("format"))||o.getInt("version")!=1||!"mm".equals(o.getString("units")))throw new IOException("Formato o versión no compatible");a=o.getJSONArray("parts");}
 else throw new IOException("Archivo no válido");
 if(a.length()>500)throw new IOException("Máximo 500 piezas");
 ArrayList<Part> list=new ArrayList<>();
 for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);float w=(float)p.getDouble("w"),d=(float)p.getDouble("d"),h=(float)p.getDouble("h"),x=(float)p.getDouble("x");
 if(!Float.isFinite(w+d+h+x)||w<=0||d<=0||h<=0||w>10000||d>10000||h>10000||Math.abs(x)>10000)throw new IOException("Medidas no válidas");
 boolean circle=p.getBoolean("c");if(circle&&w!=d)throw new IOException("Diámetro inconsistente");
 Part item=new Part(circle,w,d,h,x);item.y=(float)p.optDouble("y",0);item.z=(float)p.optDouble("z",0);list.add(item);}return list;
 }
 void checkpoint(){try{undo.push(encode());if(undo.size()>50)undo.removeLast();redo.clear();}catch(Exception e){}}
 void history(boolean back){ArrayDeque<String> src=back?undo:redo,dst=back?redo:undo;if(src.isEmpty()){toast("Sin cambios");return;}try{dst.push(encode());parts=decode(src.pop());selected=parts.isEmpty()?-1:0;save();scene.invalidate();}catch(Exception e){toast("Error al recuperar cambio");}}
 void positionSelected(){if(selected<0){toast("Selecciona una pieza");return;}Part p=parts.get(selected);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText[] fields=new EditText[3];float[] vals={p.x,p.y,p.z};for(int i=0;i<3;i++){TextView label=new TextView(this);label.setText(new String[]{"X (mm)","Y (mm)","Z (mm)"}[i]);box.addView(label);fields[i]=new EditText(this);fields[i].setInputType(8194|4096);fields[i].setText(""+vals[i]);box.addView(fields[i]);}new AlertDialog.Builder(this).setTitle("Mover pieza XYZ").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{try{float[] v=new float[3];for(int i=0;i<3;i++){v[i]=Float.parseFloat(fields[i].getText().toString().replace(',','.'));if(!Float.isFinite(v[i])||Math.abs(v[i])>10000)throw new Exception();}checkpoint();p.x=v[0];p.y=v[1];p.z=v[2];save();scene.invalidate();}catch(Exception e){toast("Coordenadas no válidas");}}).show();}
 void save(){try{getPreferences(MODE_PRIVATE).edit().putString("design",encode()).apply();updateStep();}catch(Exception e){toast("No se pudo guardar el borrador interno");}}
 void load(){try{parts=decode(getPreferences(MODE_PRIVATE).getString("design","[]"));selected=parts.isEmpty()?-1:0;scene.invalidate();updateStep();}catch(Exception e){toast("No se pudo recuperar el borrador");}}
 protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("pendingExport",pendingExport);}
 void exportFile(){
 if(fileBusy)return;
 try{pendingExport=encode();Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");intent.putExtra(Intent.EXTRA_TITLE,"Mi_diseno.mycad.json");startActivityForResult(intent,SAVE_FILE);}
 catch(Exception e){pendingExport=null;toast("No se pudo abrir el selector para guardar");}
 }
 void confirmOpen(){
 if(fileBusy)return;
 new AlertDialog.Builder(this).setTitle("Abrir archivo MyCAD").setMessage("El archivo sustituirá el diseño actual. Guarda antes un archivo si quieres conservarlo. El borrador interno no es una copia independiente.").setNegativeButton("Cancelar",null).setPositiveButton("Elegir archivo",(d,w)->{
 try{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");startActivityForResult(intent,OPEN_FILE);}catch(Exception e){toast("No se pudo abrir el selector de archivos");}
 }).show();
 }
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
 if(request!=SAVE_FILE&&request!=OPEN_FILE)return;
 if(result!=RESULT_OK||data==null||data.getData()==null){if(request==SAVE_FILE)pendingExport=null;toast("Operación cancelada");return;}
 Uri uri=data.getData();String snapshot=pendingExport;pendingExport=null;fileBusy=true;info.setText(request==SAVE_FILE?"Guardando archivo…":"Abriendo archivo…");
 new Thread(()->{try{
 if(request==SAVE_FILE){if(snapshot==null)throw new IOException("Vuelve a pulsar Guardar");
 try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("No se pudo escribir");out.write(snapshot.getBytes(StandardCharsets.UTF_8));out.flush();}
 runOnUiThread(()->{fileBusy=false;info.setText("Archivo guardado · Copia externa creada");toast("Archivo guardado en la ubicación elegida");});
 }else{String text;try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
 if(in==null)throw new IOException("No se pudo leer");byte[] buf=new byte[8192];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>2*1024*1024)throw new IOException("Archivo demasiado grande");out.write(buf,0,n);}text=new String(out.toByteArray(),StandardCharsets.UTF_8);}
 ArrayList<Part> loaded=decode(text);
 runOnUiThread(()->{fileBusy=false;checkpoint();parts=loaded;selected=parts.isEmpty()?-1:0;save();scene.invalidate();info.setText("Archivo abierto · "+parts.size()+" piezas");toast("Diseño abierto correctamente");});
 }
 }catch(Exception e){runOnUiThread(()->{fileBusy=false;info.setText("Operación fallida · El diseño actual se conserva");new AlertDialog.Builder(this).setTitle("No se pudo completar").setMessage("Comprueba que sea un archivo MyCAD válido y que la ubicación esté disponible. "+e.getMessage()).setPositiveButton("Aceptar",null).show();});}}).start();
 }
 void newDesign(){if(fileBusy)return;new AlertDialog.Builder(this).setTitle("Nuevo diseño").setMessage("Se vaciará el lienzo. Guarda antes un archivo para conservar el diseño actual.").setNegativeButton("Cancelar",null).setPositiveButton("Crear nuevo",(d,w)->{checkpoint();parts.clear();selected=-1;save();scene.invalidate();info.setText("Diseño nuevo · Medidas en mm");}).show();}
 void guide(){new AlertDialog.Builder(this).setTitle("Guía · De la idea al archivo").setMessage(
 "1. NUEVO: empieza con el lienzo vacío.\n\n"+
 "2. RECTÁNGULO o CÍRCULO: introduce medidas en mm y el espesor. APLICAR crea la pieza. Por ahora se introducen medidas; no se dibuja a mano.\n\n"+
 "3. PIEZAS: selecciona una. La seleccionada se muestra turquesa. EDITAR modifica sus medidas y su posición X (izquierda/derecha).\n\n"+
 "4. 2D / 3D: revisa la planta o el volumen. Arrastra en 3D para girar; pellizca para acercar. CENTRAR restablece la vista.\n\n"+
 "5. GUARDAR: elige nombre y carpeta en el selector de Android y confirma GUARDAR allí. Se crea un archivo .mycad.json.\n\n"+
 "6. ABRIR: confirma y selecciona un archivo .mycad.json. Cancela sin perder el diseño.\n\n"+
 "El borrador interno se recupera al iniciar, pero NO sustituye a guardar archivos. Los cambios posteriores requieren volver a GUARDAR. No se guardan todavía la cámara ni la selección en el archivo."
 ).setPositiveButton("Entendido",null).show();}
 void roadmap(){new AlertDialog.Builder(this).setTitle("Plan de desarrollo").setMessage(
 "AHORA · v0.4.1\nArchivos, guía y validación del ciclo guardar/abrir en el móvil.\n\n"+
 "SIGUIENTE · v0.4.2 (propuesta)\nDeshacer/rehacer, selección tocando, mover en X/Y/Z y ajustar la vista al conjunto.\n\n"+
 "DESPUÉS · v0.4 (propuesta)\nBocetos táctiles: líneas, rectángulos, círculos y cotas.\n\n"+
 "FASE 3D AVANZADA\nExtruir perfiles y evaluar un motor geométrico para unir, cortar y taladrar.\n\n"+
 "SALIDA A FABRICACIÓN\nExportación STL y planos PDF acotados; STEP requiere un motor CAD adecuado.\n\n"+
 "Las funciones futuras no están implementadas. Primero estabilizaremos cada fase con tus pruebas."
 ).setPositiveButton("Cerrar",null).show();}
 class Face {float[][] vertices;float depth;int color;Face(float[][] v,int c){vertices=v;color=c;for(float[] p:v)depth+=p[2]/v.length;}}
 class Scene extends View{
 Paint paint=new Paint(3);float yaw=.65f,pitch=.6f,zoom=3,lastX,lastY;ScaleGestureDetector scale;
 Scene(){super(MainActivity.this);scale=new ScaleGestureDetector(MainActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){zoom=Math.max(.02f,Math.min(40,zoom*d.getScaleFactor()));invalidate();return true;}});}
 float[] project(float x,float y,float z){if(flat)return new float[]{getWidth()/2f+x*zoom,getHeight()/2f-y*zoom,z};float u=(float)(x*Math.cos(yaw)-y*Math.sin(yaw));float v=(float)(x*Math.sin(yaw)+y*Math.cos(yaw));return new float[]{getWidth()/2f+u*zoom,getHeight()/2f+(float)(v*Math.sin(pitch)-z*Math.cos(pitch))*zoom,(float)(v*Math.cos(pitch)+z*Math.sin(pitch))};}
 protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.rgb(24,33,46));paint.setStrokeWidth(1);paint.setColor(Color.rgb(44,57,73));for(int i=-200;i<=200;i+=10){float[] a=project(i,-200,0),b=project(i,200,0);c.drawLine(a[0],a[1],b[0],b[1],paint);a=project(-200,i,0);b=project(200,i,0);c.drawLine(a[0],a[1],b[0],b[1],paint);}
 ArrayList<Face> faces=new ArrayList<>();
 for(int j=0;j<parts.size();j++){Part p=parts.get(j);int n=p.circle?64:4;float[][] low=new float[n][],high=new float[n][];
 for(int i=0;i<n;i++){float x,y;if(p.circle){x=(float)Math.cos(i*2*Math.PI/n)*p.w/2;y=(float)Math.sin(i*2*Math.PI/n)*p.d/2;}else{x=(i==0||i==3)?-p.w/2:p.w/2;y=i<2?-p.d/2:p.d/2;}low[i]=project(x+p.x,y+p.y,p.z);high[i]=project(x+p.x,y+p.y,p.z+p.h);}
 int col=j==selected?Color.rgb(69,186,207):Color.rgb(105,135,165);
 if(!flat)faces.add(new Face(low,col));
 if(!flat)for(int i=0;i<n;i++){int k=(i+1)%n;faces.add(new Face(new float[][]{low[i],low[k],high[k],high[i]},col));}
 faces.add(new Face(high,j==selected?Color.rgb(112,220,229):Color.rgb(154,180,204)));
 }
 Collections.sort(faces,(a,b)->Float.compare(a.depth,b.depth));
 for(Face f:faces){Path path=new Path();path.moveTo(f.vertices[0][0],f.vertices[0][1]);for(int i=1;i<f.vertices.length;i++)path.lineTo(f.vertices[i][0],f.vertices[i][1]);path.close();paint.setStyle(Paint.Style.FILL);paint.setColor(f.color);c.drawPath(path,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.rgb(23,65,85));c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}
 paint.setColor(Color.WHITE);paint.setTextSize(30);c.drawText(flat?"PLANTA · 2D":"VISTA · 3D",20,40,paint);if(parts.isEmpty())c.drawText("Añade una figura para empezar",20,getHeight()/2f,paint);
 }
 long downAt;float downX,downY;boolean moved,drag;
 int hit(float sx,float sy){for(int j=parts.size()-1;j>=0;j--){Part p=parts.get(j);float[] q=project(p.x,p.y,p.z+p.h/2);if(Math.hypot(q[0]-sx,q[1]-sy)<Math.max(30,Math.max(p.w,p.d)*zoom*.6f))return j;}return -1;}
 public boolean onTouchEvent(android.view.MotionEvent e){scale.onTouchEvent(e);int a=e.getActionMasked();if(a==0){lastX=downX=e.getX();lastY=downY=e.getY();downAt=System.currentTimeMillis();moved=false;drag=false;}else if(a==2&&e.getPointerCount()==1&&!scale.isInProgress()){float dx=e.getX()-lastX,dy=e.getY()-lastY;if(Math.hypot(e.getX()-downX,e.getY()-downY)>10)moved=true;if(!drag&&System.currentTimeMillis()-downAt>400&&selected>=0&&hit(downX,downY)==selected){checkpoint();drag=true;}if(drag){Part p=parts.get(selected);float u=dx/zoom,v=dy/zoom;if(flat){p.x+=u;p.y-=v;}else{p.x+=(float)(u*Math.cos(yaw)+v*Math.sin(yaw));p.y+=(float)(-u*Math.sin(yaw)+v*Math.cos(yaw));}invalidate();}else if(!flat){yaw+=dx*.01f;pitch=Math.max(-1.5f,Math.min(1.5f,pitch+dy*.01f));invalidate();}lastX=e.getX();lastY=e.getY();}else if(a==1){if(drag)save();else if(!moved){int n=hit(e.getX(),e.getY());if(n>=0){selected=n;invalidate();}}}return true;}
 }
}
