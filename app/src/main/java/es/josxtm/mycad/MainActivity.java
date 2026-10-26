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
 ArrayList<MeshEngine.Mesh> booleanMeshes=new ArrayList<>(); ArrayList<es.josxtm.mycad.cad.FaceUnionRecipe> unionRecipes=new ArrayList<>(); ArrayList<es.josxtm.mycad.cad.DrilledBoxRecipe> holeRecipes=new ArrayList<>(); ArrayList<es.josxtm.mycad.cad.FaceExtrusionChain> extrusionChains=new ArrayList<>(); ArrayList<es.josxtm.mycad.cad.ChainBooleanFeature> chainFeatures=new ArrayList<>(); es.josxtm.mycad.cad.ChainBooleanRebuildCache.State booleanRebuildSnapshot; boolean booleanRebuildBusy=false; String activeCylinderFaceId=null,activeAnalyticEdgeId=null; ArrayList<Part> parts=new ArrayList<>(); ArrayList<Sketch> sketches=new ArrayList<>(); int sketchTool=0,sketchPlane=0; int selectedSketch=-1; Scene scene; int selected=-1; TextView info; boolean flat=false; boolean darkMode=true;float rotateSensitivity=1f,zoomSensitivity=1f,dragSensitivity=1f;boolean gridVisible=true,snapEnabled=false,cameraLock=true,axesVisible=true;float gridStep=5f;int gridPlane=0,moveMode=0,moveAxis=0;boolean movie=false,movieAll=false,movieLoop=true;float movieSpeed=.6f; ArrayDeque<String> undo=new ArrayDeque<>(),redo=new ArrayDeque<>(); String pendingExport; boolean fileBusy=false; TextView nextStep;LinearLayout rootLayout,topBar,leftBar,rightBar;TextView titleView;
 static final int SAVE_FILE=101, OPEN_FILE=102, IMPORT_MESH=103, EXPORT_MESH=104; MeshEngine meshView; MeshEngine.Mesh importedMesh; boolean meshMode=false,cadGpuMesh=false,isolatedMeshPreview=false,faceDragEnabled=false; es.josxtm.mycad.cad.CadFaceReference activeCadFace; String meshExportFormat="stl"; MeshEngine.Mesh pendingMeshExport; int interactionMode=0; boolean rotationMode=false; TextView modeIndicator;
 class Sketch {String id=java.util.UUID.randomUUID().toString();int type,plane=0;float x1,y1,x2,y2;Sketch(int t,float a,float b,float c,float d){type=t;x1=a;y1=b;x2=c;y2=d;}}
 class Part { String id=java.util.UUID.randomUUID().toString();String sourceSketchId="";boolean circle; float w,d,h,x,y,z,rotationX=0,rotationY=0,rotationZ=0; Part(boolean c,float a,float b,float height,float p){circle=c;w=a;d=b;h=height;x=p;} }
 public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(10,28,10,10);rootLayout=root;root.setBackgroundColor(Color.rgb(18,25,36));
 TextView title=new TextView(this);title.setText("MyCAD · 0.6.0");titleView=title;title.setTextSize(17);title.setTextColor(Color.WHITE);
 LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top);topBar=top;
 top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
 icon(top,"↶","Deshacer",()->history(true));icon(top,"↷","Rehacer",()->history(false));icon(top,"▣","Guardar archivo",()->exportFile());icon(top,"⚙","Ajustes",()->settings());icon(top,"▶","Modo película",()->movieMenu());icon(top,"⋮","Menú de proyecto",()->projectMenu());
 info=new TextView(this);info.setTextColor(Color.LTGRAY);info.setTextSize(11);info.setText("Toca para seleccionar · Mantén pulsado para mover · Pellizca para zoom");root.addView(info);
 LinearLayout workspace=new LinearLayout(this);root.addView(workspace,new LinearLayout.LayoutParams(-1,0,1));
 LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);workspace.addView(left);leftBar=left;
 icon(left,"✎","Boceto 2D",()->chooseSketchPlane());icon(left,"↥","Extruir boceto",()->extrudeSketch());icon(left,"□","Crear rectángulo",()->edit(false,false));icon(left,"◯","Crear círculo",()->edit(true,false));icon(left,"✎","Editar pieza",()->{if(selected>=0)editLinkedOrPart();else toast("Selecciona una pieza");});icon(left,"☞","Modo navegar / mover / editar",()->chooseInteractionMode());icon(left,"✥","Mover / restricciones",()->moveOptions());icon(left,"⌫","Eliminar pieza",()->{if(selected>=0){checkpoint();parts.remove(selected);selected=-1;save();scene.invalidate();}else toast("Selecciona una pieza");});
 scene=new Scene();workspace.addView(scene,new LinearLayout.LayoutParams(0,-1,1));
 LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);workspace.addView(right);rightBar=right;
 icon(right,"☷","Árbol de cuerpos",()->bodyTree());icon(right,"∩","Analizar booleanas",()->booleanAnalysis());icon(right,"▤","Resultados booleanos",()->booleanMeshManager());icon(right,"▦","Inspeccionar caras",()->inspectSelectedFaces());icon(right,"⌑","Inspeccionar aristas",()->inspectSelectedEdges());icon(right,"◇","Alternar vista 2D / 3D",()->{flat=!flat;scene.invalidate();});icon(right,"⊡","Centrar vista",()->{scene.zoom=3;scene.yaw=.65f;scene.pitch=.6f;scene.invalidate();});icon(right,"▱","Visor 3D GPU",()->toggleMeshView());icon(right,"?","Guía",()->guide());
 LinearLayout modes=new LinearLayout(this);modes.setGravity(Gravity.CENTER);root.addView(modes);button(modes,"NAVEGAR",()->setInteractionMode(0));button(modes,"MOVER",()->setInteractionMode(1));button(modes,"EDITAR",()->setInteractionMode(2));button(modes,"GIRAR 3D",()->{rotationMode=true;setInteractionMode(0);scene.invalidate();rotateSelected3D();});button(modes,"XYZ mm",()->positionSelected());
 nextStep=new TextView(this);nextStep.setTextColor(Color.rgb(112,220,229));nextStep.setTextSize(11);root.addView(nextStep);
 root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(16+insets.getSystemWindowInsetLeft(),16+insets.getSystemWindowInsetTop(),16+insets.getSystemWindowInsetRight(),16+insets.getSystemWindowInsetBottom());return insets;});setContentView(root);root.requestApplyInsets();loadSettings();applyTheme();load();if(b!=null)pendingExport=b.getString("pendingExport");updateStep();
 }
 void button(LinearLayout l,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setTextSize(12);l.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->{if(fileBusy){toast("Espera a que termine la operación de archivo");return;}r.run();});}
 void icon(LinearLayout bar,String symbol,String name,Runnable action){TextView v=new TextView(this);v.setText(symbol);v.setTextSize(24);v.setTextColor(Color.rgb(224,235,245));v.setGravity(Gravity.CENTER);v.setContentDescription(name);v.setBackgroundColor(Color.rgb(34,47,64));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(44),dp(48));p.setMargins(dp(2),dp(2),dp(2),dp(2));bar.addView(v,p);v.setOnClickListener(w->{if(fileBusy){toast("Operación de archivo en curso");return;}action.run();});v.setOnLongClickListener(w->{toast(name);return true;});}
 int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 void projectMenu(){String[] items={"Nuevo diseño","Abrir archivo","Guardar archivo","Importar STL / OBJ","Exportar STL / OBJ","Visor 3D GPU","Guía de uso","Plan de versiones","Diagnóstico motor CAD","Alternar visor CAD experimental","Editar cara seleccionada","Extruir cara (nuevo cuerpo)","Historial de extrusiones","Booleanas guardadas","Perforar prisma (circular)","Booleanas prisma + cilindro","Estado de dependencias CAD","Auditar vínculos de bocetos","Árbol paramétrico e impactos","Reconstruir booleanas encadenadas","Editar cara cilíndrica B-Rep","Inspeccionar sólido B-Rep (caras y aristas)","Editar arista B-Rep seleccionada","Previsualizar cara/arista (controles táctiles)","Arrastrar cara directamente en 3D","Arrastrar arista directamente en 3D","Precisión de arrastre (imán)"};new AlertDialog.Builder(this).setTitle("Proyecto MyCAD").setItems(items,(d,n)->{switch(n){case 0:newDesign();break;case 1:confirmOpen();break;case 2:exportFile();break;case 3:importMesh();break;case 4:exportMeshMenu();break;case 5:toggleMeshView();break;case 6:guide();break;case 7:roadmap();break;case 8:cadEngineDiagnostic();break;case 9:toggleCadGpuMesh();break;case 10:editSelectedCadFace();break;case 11:extrudeSelectedCadFace();break;case 12:manageExtrusionChains();break;case 13:showSavedChainBooleans();break;case 14:drillSelectedBox();break;case 15:cylinderBoxBooleanMenu();break;case 16:showChainDependencyReport();break;case 17:showLinkedSketchAudit();break;case 18:showParametricFeatureTree();break;case 19:rebuildSavedChainBooleans();break;case 20:editSelectedCylinderFace();break;case 21:inspectSelectedAnalyticSolid();break;case 22:editSelectedAnalyticEdge();break;case 23:previewSelectedBRepMove();break;case 24:toggleFaceDragOnModel();break;case 25:toggleEdgeDragOnModel();break;case 26:chooseDragPrecision();break;}}).show();}
 es.josxtm.mycad.cad.ParametricFeatureGraph buildParametricFeatureGraph(){
  es.josxtm.mycad.cad.ParametricFeatureGraph.Builder builder=
   new es.josxtm.mycad.cad.ParametricFeatureGraph.Builder();
  for(int i=0;i<sketches.size();i++)
   builder.add(sketches.get(i).id,es.josxtm.mycad.cad.ParametricFeatureGraph.Kind.SKETCH,"Boceto "+(i+1));
  for(int i=0;i<parts.size();i++)
   builder.add(parts.get(i).id,es.josxtm.mycad.cad.ParametricFeatureGraph.Kind.BODY,
     "Cuerpo "+(i+1)+(parts.get(i).circle?" · cilindro":" · prisma"));
  for(int i=0;i<extrusionChains.size();i++)
   builder.add(extrusionChains.get(i).id,es.josxtm.mycad.cad.ParametricFeatureGraph.Kind.CHAIN,
     "Cadena "+(i+1));
  for(int i=0;i<chainFeatures.size();i++)
   builder.add(chainFeatures.get(i).id,es.josxtm.mycad.cad.ParametricFeatureGraph.Kind.BOOLEAN,
     "Booleana "+(i+1));
  for(Part p:parts)if(!p.sourceSketchId.isEmpty())
   builder.dependsOn(p.id,p.sourceSketchId);
  for(es.josxtm.mycad.cad.ChainBooleanFeature f:chainFeatures)
   builder.dependsOn(f.id,extrusionChains.get(f.resolvedChainIndex(extrusionChains)).id);
  return builder.build();
 }
 void showParametricFeatureTree(){
  final es.josxtm.mycad.cad.ParametricFeatureGraph graph;
  try{graph=buildParametricFeatureGraph();}
  catch(Exception error){
   new AlertDialog.Builder(this).setTitle("Vínculos paramétricos incorrectos")
    .setMessage("Hay referencias a objetos que ya no existen, identificadores duplicados "+
     "o dependencias inconsistentes. Revisa las auditorías de bocetos e historial.")
    .setPositiveButton("Cerrar",null).show();
   return;
  }
  java.util.List<es.josxtm.mycad.cad.ParametricFeatureGraph.Node> order=graph.executionOrder();
  if(order.isEmpty()){toast("El diseño no tiene operaciones paramétricas");return;}
  String[] names=new String[order.size()];
  for(int i=0;i<order.size();i++){
   es.josxtm.mycad.cad.ParametricFeatureGraph.Node node=order.get(i);
   names[i]=node.label+" · depende de "+node.parentIds().size();
  }
  new AlertDialog.Builder(this).setTitle("Árbol paramétrico · "+order.size()+" elementos")
   .setItems(names,(dialog,index)->{
    es.josxtm.mycad.cad.ParametricFeatureGraph.Node node=order.get(index);
    java.util.List<es.josxtm.mycad.cad.ParametricFeatureGraph.Node> impacted=
     graph.affectedBy(node.id);
    StringBuilder body=new StringBuilder("ID: "+node.id+"\\nTipo: "+node.kind.name()+
     "\\nDependencias: "+node.parentIds().size()+
     "\\nElementos afectados al modificar: "+impacted.size());
    for(int i=0;i<impacted.size()&&i<15;i++)body.append("\\n  • ").append(impacted.get(i).label);
    if(impacted.size()>15)body.append("\\n…");
    body.append("\\n\\nDiagnóstico: las dependencias son informativas; la regeneración "+
     "geométrica sigue limitada a las operaciones actualmente soportadas.");
    new AlertDialog.Builder(this).setTitle(node.label)
     .setMessage(body.toString()).setPositiveButton("Cerrar",null).show();
   }).setNegativeButton("Cerrar",null).show();
 }
 void showChainDependencyReport(){
  es.josxtm.mycad.cad.ChainDependencyReport.Result graph=
   es.josxtm.mycad.cad.ChainDependencyReport.inspect(extrusionChains,chainFeatures);
  String summary="Cadenas: "+graph.chainCount+"\\nPasos de extrusión: "+graph.stepCount+
   "\\nBooleanas vinculadas: "+graph.booleanCount+"\\nEstado: "+
   (graph.isValid()?"Referencias correctas":"REFERENCIAS INCORRECTAS");
  if(!graph.isValid())for(String error:graph.errors)summary+="\\n"+error;
  new AlertDialog.Builder(this).setTitle("Dependencias paramétricas")
   .setMessage(summary).setPositiveButton("Cerrar",null).show();
 }
 void showLinkedSketchAudit(){
  java.util.ArrayList<es.josxtm.mycad.cad.LinkedSketchAudit.Sketch> profiles=new java.util.ArrayList<>();
  for(Sketch sketch:sketches)profiles.add(new es.josxtm.mycad.cad.LinkedSketchAudit.Sketch(
   sketch.id,sketch.type,sketch.plane,sketch.x1,sketch.y1,sketch.x2,sketch.y2));
  java.util.ArrayList<es.josxtm.mycad.cad.LinkedSketchAudit.Body> solids=new java.util.ArrayList<>();
  for(Part part:parts)solids.add(new es.josxtm.mycad.cad.LinkedSketchAudit.Body(
   part.id,part.sourceSketchId,part.circle,part.w,part.d,part.h,part.x,part.y,
   part.z,part.rotationX,part.rotationY,part.rotationZ));
  es.josxtm.mycad.cad.LinkedSketchAudit.Report report=
   es.josxtm.mycad.cad.LinkedSketchAudit.inspect(profiles,solids);
  StringBuilder message=new StringBuilder("Bocetos: "+report.sketches+
   "\\nCuerpos vinculados: "+report.linkedBodies+
   "\\nCuerpos independientes: "+report.independentBodies+
   "\\nReferencias incorrectas: "+report.errors+
   "\\nDiferencias geométricas: "+report.warnings);
  for(int i=0;i<report.diagnostics.size()&&i<12;i++)
   message.append("\\n").append(report.diagnostics.get(i));
  if(report.diagnostics.size()>12)message.append("\\n…");
  message.append("\\n\\nSolo diagnóstico: no cambia ningún cuerpo.");
  new AlertDialog.Builder(this).setTitle("Auditoría de vínculos boceto / 3D")
   .setMessage(message.toString()).setPositiveButton("Cerrar",null).show();
 }
 void cadEngineDiagnostic(){
  try{
   java.util.ArrayList<es.josxtm.mycad.cad.CadIntegrationProbe.SketchData> ss=new java.util.ArrayList<>();
   java.util.ArrayList<es.josxtm.mycad.cad.CadIntegrationProbe.PartData> pp=new java.util.ArrayList<>();
   for(Sketch sketch:sketches)
    ss.add(new es.josxtm.mycad.cad.CadIntegrationProbe.SketchData(sketch.id,sketch.type,sketch.plane,sketch.x1,sketch.y1,sketch.x2,sketch.y2));
   for(Part part:parts)
    pp.add(new es.josxtm.mycad.cad.CadIntegrationProbe.PartData(part.sourceSketchId,part.h));
   es.josxtm.mycad.cad.CadIntegrationProbe.Report result=es.josxtm.mycad.cad.CadIntegrationProbe.inspect(ss,pp);
   String summary="Bocetos rectangulares compatibles: "+result.compatibleSketches
    +"\\nExtrusiones interpretadas: "+result.compatibleExtrusions
    +"\\nBocetos no compatibles: "+result.unsupportedSketches
    +"\\nPiezas sin vínculo: "+result.unlinkedParts
    +"\\nExtrusiones inválidas o sin boceto: "+result.invalidExtrusions
    +"\\n\\nSolo diagnóstico: no modifica el diseño ni sustituye al motor actual.";
   new AlertDialog.Builder(this).setTitle("Diagnóstico del motor CAD").setMessage(summary).setPositiveButton("Cerrar",null).show();
  }catch(Exception error){toast("No se pudo analizar el diseño");}
 }
 void chooseSketchPlane(){new AlertDialog.Builder(this).setTitle("Plano de boceto").setSingleChoiceItems(new String[]{"XY · planta","XZ · alzado","YZ · lateral"},sketchPlane,(d,n)->{sketchPlane=n;flat=true;sketchTool=0;scene.invalidate();info.setText("Plano "+new String[]{"XY","XZ","YZ"}[n]+" seleccionado · Elige herramienta de boceto");d.dismiss();sketchMenu();}).setNegativeButton("Cancelar",null).show();}
 void sketchMenu(){
  String[] items={"Salir de boceto","Línea","Rectángulo","Círculo","Acotar seleccionado",
   "Extruir seleccionado","Eliminar último boceto","Eliminar boceto seleccionado"};
  new AlertDialog.Builder(this).setTitle("Boceto táctil · plano "+new String[]{"XY","XZ","YZ"}[sketchPlane])
   .setItems(items,(dialog,choice)->{
    if(choice==4){dimensionSketch();return;}
    if(choice==5){extrudeSketch();return;}
    if(choice==6){requestSketchDeletion(false);return;}
    if(choice==7){requestSketchDeletion(true);return;}
    sketchTool=choice;
    if(choice>0){
     flat=true;info.setText("BOCETO · Arrastra con el dedo · Pellizca para zoom");
    }else info.setText("Toca para seleccionar · Mantén pulsado para mover");
    scene.invalidate();
   }).show();
 }
 java.util.ArrayList<es.josxtm.mycad.cad.SketchDeletionPlan.BodyLink> sketchBodyLinks(){
  java.util.ArrayList<es.josxtm.mycad.cad.SketchDeletionPlan.BodyLink> bodyLinks=
   new java.util.ArrayList<>();
  for(Part body:parts)bodyLinks.add(new es.josxtm.mycad.cad.SketchDeletionPlan.BodyLink(
   body.id,body.sourceSketchId));
  return bodyLinks;
 }
 void requestSketchDeletion(boolean selectedOnly){
  if(sketches.isEmpty()){toast("No hay bocetos para eliminar");return;}
  int index=selectedOnly?selectedSketch:sketches.size()-1;
  if(index<0||index>=sketches.size()){
   toast("Selecciona primero un boceto");return;
  }
  final Sketch target=sketches.get(index);
  final es.josxtm.mycad.cad.SketchDeletionPlan.Plan plan;
  try{
   plan=es.josxtm.mycad.cad.SketchDeletionPlan.inspect(target.id,sketchBodyLinks());
  }catch(Exception error){toast("Hay identificadores de cuerpos duplicados");return;}
  int count=plan.dependentBodyIds.size();
  String detail="Boceto "+(index+1)+" · "+count+" pieza(s) vinculada(s). "+
   "Puedes conservar los cuerpos como independientes o eliminarlos junto al boceto. "+
   "Ambas operaciones se pueden deshacer.";
  new AlertDialog.Builder(this).setTitle("Eliminar boceto y dependencias")
   .setMessage(detail)
   .setNegativeButton("Cancelar",null)
   .setNeutralButton("Conservar cuerpos",(dialog,which)->
    applySketchDeletion(target,plan,es.josxtm.mycad.cad.SketchDeletionPlan.Mode.KEEP_SOLIDS))
   .setPositiveButton("Eliminar también cuerpos",(dialog,which)->
    applySketchDeletion(target,plan,es.josxtm.mycad.cad.SketchDeletionPlan.Mode.CASCADE_DELETE))
   .show();
 }
 void applySketchDeletion(Sketch target,es.josxtm.mycad.cad.SketchDeletionPlan.Plan plan,
                          es.josxtm.mycad.cad.SketchDeletionPlan.Mode mode){
  int index=sketches.indexOf(target);
  if(index<0||!plan.stillMatches(sketchBodyLinks())){
   toast("El diseño ha cambiado; vuelve a seleccionar el boceto");return;
  }
  // The plan's result must be complete before changing any UI or body.
  final java.util.List<es.josxtm.mycad.cad.SketchDeletionPlan.BodyLink> updated=plan.after(mode);
  final java.util.HashSet<String> retained=new java.util.HashSet<>();
  for(es.josxtm.mycad.cad.SketchDeletionPlan.BodyLink b:updated)retained.add(b.bodyId);
  final String selectedId=selected>=0&&selected<parts.size()?parts.get(selected).id:null;
  checkpoint();
  sketches.remove(index);
  if(mode==es.josxtm.mycad.cad.SketchDeletionPlan.Mode.CASCADE_DELETE){
   parts.removeIf(body->!retained.contains(body.id));
  }else{
   for(Part body:parts)if(target.id.equals(body.sourceSketchId))body.sourceSketchId="";
  }
  if(selectedSketch==index)selectedSketch=-1;
  else if(selectedSketch>index)selectedSketch--;
  selected=-1;
  if(selectedId!=null)for(int i=0;i<parts.size();i++)
   if(selectedId.equals(parts.get(i).id)){selected=i;break;}
  activeCadFace=null;
  save();scene.invalidate();
  if(meshMode&&meshView!=null)renderCurrentCadMesh();
  toast(mode==es.josxtm.mycad.cad.SketchDeletionPlan.Mode.CASCADE_DELETE?
   "Boceto y extrusiones vinculadas eliminados":"Boceto eliminado · cuerpos independientes conservados");
 }
 Part makeExtrudedPart(Sketch q,float height){
  es.josxtm.mycad.cad.LinkedSketchSolidPlan.Body result=
   es.josxtm.mycad.cad.LinkedSketchSolidPlan.build(q.type,q.plane,q.x1,q.y1,q.x2,q.y2,height);
  Part body=new Part(result.circle,result.w,result.d,result.h,result.x);
  body.y=result.y;body.z=result.z;
  body.rotationX=result.rx;body.rotationY=result.ry;body.rotationZ=result.rz;
  body.sourceSketchId=q.id;
  return body;
 }
 void extrudeSketch(){
  if(selectedSketch<0||selectedSketch>=sketches.size()){toast("Selecciona un perfil de boceto");return;}
  Sketch q=sketches.get(selectedSketch);
  if(q.type==1){toast("Una línea abierta no se puede extruir");return;}
  float w=q.type==3?(float)Math.hypot(q.x2-q.x1,q.y2-q.y1)*2:Math.abs(q.x2-q.x1);
  float d=q.type==3?w:Math.abs(q.y2-q.y1);
  if(w<.1f||d<.1f||parts.size()>=500){toast("Perfil inválido o límite de piezas alcanzado");return;}
  final int previousSelection=selected;final boolean previousFlat=flat;
  final Part draft=makeExtrudedPart(q,20f);
  parts.add(draft);selected=parts.size()-1;flat=false;scene.invalidate();
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(dp(18),dp(8),dp(18),dp(8));
  TextView caption=new TextView(this);caption.setText("Altura positiva en mm (1–10000)");panel.addView(caption);
  EditText input=new EditText(this);input.setInputType(8194|4096);input.setText("20");
  panel.addView(input);
  SeekBar slider=new SeekBar(this);slider.setMax(199);slider.setProgress(19);panel.addView(slider);
  final boolean[] changing={false},accepted={false};
  slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onStartTrackingTouch(SeekBar bar){}
   public void onStopTrackingTouch(SeekBar bar){}
   public void onProgressChanged(SeekBar bar,int value,boolean fromUser){
    if(!fromUser||changing[0])return;
    changing[0]=true;input.setText(String.valueOf(value+1));changing[0]=false;
    Part p=makeExtrudedPart(q,value+1);
    updateDraft(draft,p);selected=parts.size()-1;scene.invalidate();
   }
  });
  input.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence a,int b,int c,int d){}
   public void onTextChanged(CharSequence a,int b,int before,int count){
    if(changing[0])return;
    try{
     float h=Float.parseFloat(a.toString().replace(',','.'));
     if(!Float.isFinite(h)||h<=0||h>10000)return;
     changing[0]=true;slider.setProgress(Math.max(0,Math.min(199,Math.round(h)-1)));changing[0]=false;
     updateDraft(draft,makeExtrudedPart(q,h));scene.invalidate();
    }catch(Exception ignored){}
   }
   public void afterTextChanged(android.text.Editable editable){}
  });
  AlertDialog dialog=new AlertDialog.Builder(this)
   .setTitle("Extrusión · vista previa 3D")
   .setView(panel)
   .setNegativeButton("Cancelar",null)
   .setPositiveButton("Crear sólido",null).create();
  dialog.setOnDismissListener(dismissed->{
   if(!accepted[0]){
    parts.remove(draftIndex(parts,draft));selected=previousSelection;flat=previousFlat;scene.invalidate();
   }
  });
  dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try{
    float h=Float.parseFloat(input.getText().toString().replace(',','.'));
    GeometryMath.requirePositive(h);
    es.josxtm.mycad.cad.CadKernel.Result<es.josxtm.mycad.cad.CadKernel.Box> checkedSolid=null;
    if(q.type==2){
     checkedSolid=es.josxtm.mycad.cad.PlaneExtrusion.extrude(
      new es.josxtm.mycad.cad.PrimitiveKernel(),
      es.josxtm.mycad.cad.PlaneExtrusion.Plane.values()[q.plane],
      Math.min(q.x1,q.x2),Math.min(q.y1,q.y2),0,
      Math.abs(q.x2-q.x1),Math.abs(q.y2-q.y1),h);
     if(!checkedSolid.isSuccess()){toast("El motor CAD rechazó la geometría");return;}
    }
    Part result=makeExtrudedPart(q,h);
    // Remove transient preview before taking the undo snapshot.
    int index=draftIndex(parts,draft);parts.remove(index);
    selected=previousSelection;flat=previousFlat;checkpoint();
    parts.add(result);selected=parts.size()-1;selectedSketch=-1;sketchTool=0;flat=false;
    accepted[0]=true;save();scene.invalidate();
    double volume=checkedSolid!=null?checkedSolid.value.volume():GeometryMath.cylinderVolume(w,h);
    info.setText(String.format(java.util.Locale.US,"Extrusión %.1f mm · volumen %.1f mm³",h,volume));
    dialog.dismiss();
   }catch(Exception ex){toast("Altura válida: mayor que 0 y hasta 10000 mm");}
  }));
  dialog.show();
 }
 void updateDraft(Part into,Part from){into.sourceSketchId=from.sourceSketchId;into.w=from.w;into.d=from.d;into.h=from.h;into.x=from.x;into.y=from.y;into.z=from.z;into.rotationX=from.rotationX;into.rotationY=from.rotationY;into.rotationZ=from.rotationZ;}
 int draftIndex(ArrayList<Part> all,Part draft){
  int direct=all.indexOf(draft);return direct>=0?direct:all.size()-1;
 }
 void dimensionSketch(){if(selectedSketch<0||selectedSketch>=sketches.size()){toast("Selecciona primero un boceto");return;}Sketch q=sketches.get(selectedSketch);if(q.type==1){EditText lengthInput=new EditText(this);lengthInput.setInputType(8194|4096);lengthInput.setText(String.format(java.util.Locale.US,"%.2f",Math.hypot(q.x2-q.x1,q.y2-q.y1)));new AlertDialog.Builder(this).setTitle("Longitud de línea · mm").setView(lengthInput).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(di,which)->{try{float length=Float.parseFloat(lengthInput.getText().toString().replace(',','.'));if(!Float.isFinite(length)||length<=0||length>10000)throw new Exception();float dx=q.x2-q.x1,dy=q.y2-q.y1;float oldLength=(float)Math.hypot(dx,dy);if(oldLength<.0001f)throw new Exception();checkpoint();q.x2=q.x1+dx*length/oldLength;q.y2=q.y1+dy*length/oldLength;rebuildLinkedParts(q);save();scene.invalidate();info.setText("Línea acotada · "+length+" mm");}catch(Exception ex){toast("Longitud inválida (0–10000 mm)");}}).show();return;}LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(8),dp(20),dp(8));EditText[] e=new EditText[2];String[] labels=q.type==3?new String[]{"Diámetro (mm)"}:new String[]{"Ancho (mm)","Fondo (mm)"};for(int i=0;i<labels.length;i++){TextView label=new TextView(this);label.setText(labels[i]);box.addView(label);e[i]=new EditText(this);e[i].setInputType(8194|4096);e[i].setText(""+(q.type==3?2*Math.hypot(q.x2-q.x1,q.y2-q.y1):i==0?Math.abs(q.x2-q.x1):Math.abs(q.y2-q.y1)));box.addView(e[i]);}new AlertDialog.Builder(this).setTitle("Acotar perfil").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(di,w)->{try{float a=Float.parseFloat(e[0].getText().toString().replace(',','.'));float b=e[1]==null?a:Float.parseFloat(e[1].getText().toString().replace(',','.'));if(!Float.isFinite(a+b)||a<=0||b<=0||a>10000||b>10000)throw new Exception();float nextX=q.type==3?q.x1+a/2:q.x1+(q.x2>=q.x1?a:-a);float nextY=q.type==3?q.y1:q.y1+(q.y2>=q.y1?b:-b);if(!validateLinkedSketchEdit(q,q.x1,q.y1,nextX,nextY))throw new Exception();checkpoint();q.x2=nextX;q.y2=nextY;rebuildLinkedParts(q);save();scene.invalidate();}catch(Exception ex){toast("Cota inválida");}}).show();}
 void loadSettings(){android.content.SharedPreferences p=getPreferences(MODE_PRIVATE);darkMode=p.getBoolean("dark",true);rotateSensitivity=p.getFloat("rot",1f);zoomSensitivity=p.getFloat("zoom",1f);dragSensitivity=p.getFloat("drag",1f);gridVisible=p.getBoolean("gridVisible",true);snapEnabled=p.getBoolean("snap",false);gridStep=p.getFloat("gridStep",5);gridPlane=p.getInt("gridPlane",0);axesVisible=p.getBoolean("axes",true);cameraLock=p.getBoolean("cameraLock",true);moveMode=p.getInt("moveMode",0);moveAxis=p.getInt("moveAxis",0);}
 void applyTheme(){int bg=darkMode?Color.rgb(18,25,36):Color.rgb(235,241,247),fg=darkMode?Color.WHITE:Color.rgb(25,38,55),bar=darkMode?Color.rgb(34,47,64):Color.rgb(211,222,233);rootLayout.setBackgroundColor(bg);titleView.setTextColor(fg);info.setTextColor(fg);for(LinearLayout group:new LinearLayout[]{topBar,leftBar,rightBar})for(int i=0;i<group.getChildCount();i++){View v=group.getChildAt(i);if(v instanceof TextView){((TextView)v).setTextColor(fg);v.setBackgroundColor(bar);}}scene.invalidate();}
 void storeSettings(){getPreferences(MODE_PRIVATE).edit().putBoolean("dark",darkMode).putFloat("rot",rotateSensitivity).putFloat("zoom",zoomSensitivity).putFloat("drag",dragSensitivity).putBoolean("gridVisible",gridVisible).putBoolean("snap",snapEnabled).putFloat("gridStep",gridStep).putInt("gridPlane",gridPlane).putBoolean("axes",axesVisible).putBoolean("cameraLock",cameraLock).putInt("moveMode",moveMode).putInt("moveAxis",moveAxis).apply();applyTheme();}

 void settings(){String[] opts={"Tema: "+(darkMode?"Oscuro":"Claro"),"Velocidad de giro: "+rotateSensitivity,"Sensibilidad zoom: "+zoomSensitivity,"Sensibilidad arrastre: "+dragSensitivity,"Cuadrícula y precisión","Bloquear cámara al editar: "+(cameraLock?"Sí":"No"),"Modo película"};new AlertDialog.Builder(this).setTitle("Ajustes de MyCAD").setItems(opts,(d,n)->{if(n==0){darkMode=!darkMode;storeSettings();settings();}else if(n==4)gridSettings();else if(n==5){cameraLock=!cameraLock;storeSettings();settings();}else if(n==6)movieMenu();else{String name=new String[]{"","Giro 3D","Zoom","Arrastre"}[n];float current=n==1?rotateSensitivity:n==2?zoomSensitivity:dragSensitivity;SeekBar bar=new SeekBar(this);bar.setMax(195);bar.setProgress(Math.round(current*100)-5);new AlertDialog.Builder(this).setTitle(name+" · 5 % a 200 %").setView(bar).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(di,w)->{float v=(bar.getProgress()+5)/100f;if(n==1)rotateSensitivity=v;else if(n==2)zoomSensitivity=v;else dragSensitivity=v;storeSettings();settings();}).show();}}).show();}
 void gridSettings(){String[] opts={"Mostrar cuadrícula: "+(gridVisible?"Sí":"No"),"Ajustar a cuadrícula: "+(snapEnabled?"Sí":"No"),"Paso: "+gridStep+" mm","Plano: "+new String[]{"XY","XZ","YZ"}[gridPlane],"Mostrar ejes: "+(axesVisible?"Sí":"No")};new AlertDialog.Builder(this).setTitle("Cuadrícula y precisión").setItems(opts,(d,n)->{if(n==0)gridVisible=!gridVisible;else if(n==1)snapEnabled=!snapEnabled;else if(n==4)axesVisible=!axesVisible;else if(n==2){String[] steps={"1 mm","5 mm","10 mm","Personalizado"};new AlertDialog.Builder(this).setTitle("Separación de cuadrícula").setItems(steps,(di,k)->{if(k<3){gridStep=new float[]{1,5,10}[k];storeSettings();gridSettings();}else{EditText field=new EditText(this);field.setInputType(8194|4096);field.setText(""+gridStep);new AlertDialog.Builder(this).setTitle("Paso personalizado (mm)").setView(field).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(dd,w)->{try{float v=Float.parseFloat(field.getText().toString().replace(',','.'));if(!Float.isFinite(v)||v<.1f||v>1000)throw new Exception();gridStep=v;storeSettings();gridSettings();}catch(Exception ex){toast("Introduce un paso entre 0,1 y 1000 mm");}}).show();}}).show();return;}else if(n==3){new AlertDialog.Builder(this).setTitle("Plano de cuadrícula").setSingleChoiceItems(new String[]{"XY","XZ","YZ"},gridPlane,(di,k)->{gridPlane=k;di.dismiss();storeSettings();gridSettings();}).show();return;}storeSettings();gridSettings();}).setNegativeButton("Cerrar",null).show();}
 void setInteractionMode(int n){interactionMode=n;scene.movingEntity=n==1;info.setText(new String[]{"NAVEGAR · Arrastra para girar · Pellizca para zoom","MOVER · Usa flechas XYZ o arrastra el cuerpo","EDITAR · Arrastra los controladores"}[n]);scene.invalidate();}
 void rotateSelectedZ(){if(selected<0){toast("Selecciona un cuerpo");return;}Part p=parts.get(selected);EditText e=new EditText(this);e.setInputType(8194|4096);e.setText(""+p.rotationZ);new AlertDialog.Builder(this).setTitle("Rotación Z (grados)").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{try{float a=Float.parseFloat(e.getText().toString().replace(',','.'));if(!Float.isFinite(a))throw new Exception();checkpoint();p.rotationZ=a;save();scene.invalidate();}catch(Exception ex){toast("Ángulo inválido");}}).show();}
 void rotateSelected3D(){if(selected<0){toast("Selecciona un cuerpo");return;}Part p=parts.get(selected);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),dp(8));EditText[] fields=new EditText[3];float[] vals={p.rotationX,p.rotationY,p.rotationZ};String[] labels={"X rojo · grados","Y verde · grados","Z azul · grados"};for(int k=0;k<3;k++){TextView t=new TextView(this);t.setText(labels[k]);box.addView(t);fields[k]=new EditText(this);fields[k].setInputType(8194|4096);fields[k].setText(""+vals[k]);box.addView(fields[k]);}new AlertDialog.Builder(this).setTitle("GIRAR 3D · XYZ").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{try{float[] a=new float[3];for(int k=0;k<3;k++){a[k]=Float.parseFloat(fields[k].getText().toString().replace(',','.'));if(!Float.isFinite(a[k])||Math.abs(a[k])>36000)throw new Exception();}checkpoint();p.rotationX=a[0];p.rotationY=a[1];p.rotationZ=a[2];save();scene.invalidate();}catch(Exception ex){toast("Introduce ángulos válidos");}}).show();}
 void booleanAnalysis(){
  if(selected<0||selected>=parts.size()||parts.size()<2){toast("Selecciona un prisma y crea otro para comparar");return;}
  final Part primary=parts.get(selected);
  if(primary.circle||primary.rotationX!=0||primary.rotationY!=0||primary.rotationZ!=0){
   toast("Por ahora solo prismas sin rotación");return;
  }
  ArrayList<Integer> candidates=new ArrayList<>();
  ArrayList<String> labels=new ArrayList<>();
  for(int i=0;i<parts.size();i++){
   if(i==selected)continue;
   Part p=parts.get(i);
   if(p.circle||p.rotationX!=0||p.rotationY!=0||p.rotationZ!=0)continue;
   candidates.add(i);labels.add("Cuerpo "+(i+1));
  }
  if(candidates.isEmpty()){toast("Se necesita otro prisma rectangular sin rotación");return;}
  new AlertDialog.Builder(this).setTitle("Análisis booleano · segundo prisma")
   .setItems(labels.toArray(new String[0]),(dialog,n)->{
    Part secondary=parts.get(candidates.get(n));
    try{
     BoxBoolean.Box a=new BoxBoolean.Box(primary.x,primary.y,primary.z,primary.w,primary.d,primary.h);
     BoxBoolean.Box b=new BoxBoolean.Box(secondary.x,secondary.y,secondary.z,secondary.w,secondary.d,secondary.h);
     String values=String.format(java.util.Locale.US,
      "Intersección: %.2f mm³\nUnión: %.2f mm³\nA menos B: %.2f mm³\n\nSolo análisis de volúmenes. No se ha creado ni modificado ningún sólido.",
      BoxBoolean.overlap(a,b),BoxBoolean.unionVolume(a,b),BoxBoolean.differenceVolume(a,b));
     new AlertDialog.Builder(this).setTitle("Operaciones booleanas")
       .setMessage(values+"\\n\\nPuedes visualizar la malla resultante en el visor GPU.")
       .setPositiveButton("Ver malla", (dd,which)->chooseBooleanPreview(a,b))
       .setNegativeButton("Cerrar",null).show();
    }catch(Exception ex){toast("No se pudo calcular la geometría");}
   }).setNegativeButton("Cancelar",null).show();
 }
 void chooseBooleanPreview(BoxBoolean.Box a,BoxBoolean.Box b){
  String[] modes={"Unir (A ∪ B)","Intersección (A ∩ B)","Restar (A − B)"};
  new AlertDialog.Builder(this).setTitle("Previsualizar resultado booleano")
   .setItems(modes,(d,choice)->{
    BoxBooleanMesh.Operation operation=choice==0?BoxBooleanMesh.Operation.UNION:choice==1?BoxBooleanMesh.Operation.INTERSECTION:BoxBooleanMesh.Operation.DIFFERENCE;
    float[] triangles=BoxBooleanMesh.build(a,b,operation);
    if(triangles.length==0){toast("El resultado es vacío");return;}
    if(meshView==null){
     meshView=new MeshEngine(this);
     meshView.setSurfaceTapListener((triangle,x,y,z)->showGpuSurfaceSelection(triangle,x,y,z));
     rootLayout.addView(meshView,rootLayout.indexOfChild(nextStep),new LinearLayout.LayoutParams(-1,dp(240)));
    }
    meshMode=true;meshView.setVisibility(View.VISIBLE);
    isolatedMeshPreview=true;activeCadFace=null;meshView.highlightFace(-1);
    meshView.setMeshes(java.util.Collections.singletonList(new MeshEngine.Mesh(triangles)));
    info.setText("Previsualización booleana · "+(triangles.length/9)+" triángulos");
    new AlertDialog.Builder(this).setTitle("Guardar resultado booleano")
     .setMessage("Guardar una copia de la malla en el proyecto. Los dos sólidos originales se conservarán. Podrás exportar todo como STL/OBJ.")
     .setPositiveButton("Guardar malla",(confirm,which)->{
      checkpoint();booleanMeshes.add(new MeshEngine.Mesh(triangles));unionRecipes.add(null);holeRecipes.add(null);save();
      renderCurrentCadMesh();
      info.setText("Malla booleana guardada · "+booleanMeshes.size()+" resultado(s)");
     }).setNegativeButton("Solo previsualizar",null).show();
   }).setNegativeButton("Cancelar",null).show();
 }
 void renderCurrentCadMesh(){
  if(meshView==null)return;
  faceDragEnabled=false;
  meshView.setFaceDragHandle(null,0,null);
  isolatedMeshPreview=false;
  activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;
  meshView.highlightFace(-1);meshView.highlightEdge(null);
  meshView.setMeshes(java.util.Collections.singletonList(currentMesh()));
 }
 void showMeshOnly(MeshEngine.Mesh mesh){
  if(meshView==null){meshView=new MeshEngine(this);meshView.setSurfaceTapListener((triangle,x,y,z)->showGpuSurfaceSelection(triangle,x,y,z));rootLayout.addView(meshView,rootLayout.indexOfChild(nextStep),new LinearLayout.LayoutParams(-1,dp(240)));}
  meshMode=true;meshView.setVisibility(View.VISIBLE);
  isolatedMeshPreview=true;activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;
  meshView.highlightFace(-1);
  meshView.setMeshes(java.util.Collections.singletonList(mesh));
  info.setText("Resultado booleano · "+mesh.triangles()+" triángulos");
 }
 void booleanMeshManager(){
  if(booleanMeshes.isEmpty()){toast("No hay resultados booleanos guardados");return;}
  String[] names=new String[booleanMeshes.size()];
  for(int i=0;i<names.length;i++){
   es.josxtm.mycad.cad.DrilledBoxRecipe drill=i<holeRecipes.size()?holeRecipes.get(i):null;
   names[i]="Malla "+(i+1)+" · "+booleanMeshes.get(i).triangles()+" triángulos";
   if(drill!=null)names[i]+=" · "+drill.axis.name()+" · "+
    (drill.drillDepth==0?"pasante":drill.drillDepth+" mm de profundidad");
  }
  new AlertDialog.Builder(this).setTitle("Resultados booleanos")
   .setItems(names,(dialog,index)->{
    MeshEngine.Mesh mesh=booleanMeshes.get(index);
    new AlertDialog.Builder(this).setTitle(names[index])
     .setItems(new String[]{"Ver solo este resultado","Exportar solo este resultado","Eliminar resultado","Editar extrusión fusionada","Editar perforación circular"},(d,choice)->{
      if(choice==0){showMeshOnly(mesh);}
      else if(choice==1){exportMeshMenuFor(mesh);}
      else if(choice==3){editFaceUnionRecipe(index);}
      else if(choice==4){editHoleRecipe(index);}
      else new AlertDialog.Builder(this).setTitle("Eliminar resultado")
       .setMessage("¿Eliminar esta malla del proyecto? Los prismas originales permanecerán.")
       .setNegativeButton("Cancelar",null)
       .setPositiveButton("Eliminar",(confirm,which)->{
        checkpoint();int removed=booleanMeshes.indexOf(mesh);booleanMeshes.remove(mesh);if(removed>=0&&removed<unionRecipes.size())unionRecipes.remove(removed);if(removed>=0&&removed<holeRecipes.size())holeRecipes.remove(removed);save();
        if(meshMode&&meshView!=null)renderCurrentCadMesh();
        toast("Resultado eliminado");
       }).show();
     }).show();
   }).setNegativeButton("Cerrar",null).show();
 }
 void cylinderBoxBooleanMenu(){
  if(selected<0||selected>=parts.size()||parts.get(selected).circle){
   toast("Selecciona primero un prisma rectangular");return;
  }
  final int boxIndex=selected;
  final Part box=parts.get(boxIndex);
  final java.util.ArrayList<Integer> indexes=new java.util.ArrayList<>();
  final java.util.ArrayList<String> names=new java.util.ArrayList<>();
  for(int i=0;i<parts.size();i++){
   Part p=parts.get(i);
   if(p.circle&&p.h>0&&p.w>0){
    indexes.add(i);
    names.add("Cilindro "+(i+1)+" · Ø"+p.w+" mm · "+p.h+" mm");
   }
  }
  if(indexes.isEmpty()){toast("Crea un cilindro para usarlo como cortador");return;}
  new AlertDialog.Builder(this).setTitle("Elegir cilindro cortador")
   .setItems(names.toArray(new String[0]),(d,which)->{
    final int cutterIndex=indexes.get(which);
    final Part cylinder=parts.get(cutterIndex);
    final es.josxtm.mycad.cad.CylinderBoxBoolean.Result result;
    try{
     result=es.josxtm.mycad.cad.CylinderBoxBoolean.calculate(
      box.w,box.d,box.h,new es.josxtm.mycad.cad.RigidTransform(
       box.x,box.y,box.z,box.rotationX,box.rotationY,box.rotationZ),
      cylinder.w,cylinder.h,new es.josxtm.mycad.cad.RigidTransform(
       cylinder.x,cylinder.y,cylinder.z,cylinder.rotationX,cylinder.rotationY,cylinder.rotationZ));
    }catch(Exception failure){
     new AlertDialog.Builder(this).setTitle("Operación no compatible")
      .setMessage("El cilindro debe estar alineado con un eje local del prisma, entrar por su cara positiva y caber completamente en la sección. No se modificó el diseño.")
      .setPositiveButton("Cerrar",null).show();return;
    }
    String[] options={"Previsualizar corte (A − cilindro)","Previsualizar intersección (A ∩ cilindro)",
     "Guardar corte editable","Guardar intersección como malla"};
    new AlertDialog.Builder(this).setTitle("Prisma + cilindro · "+result.cut.axis.name())
     .setMessage(String.format(java.util.Locale.US,
       "Material intersectado: %.2f mm³. Corte %s. El guardado sustituye los dos cuerpos originales.",
       result.intersectionVolume,result.cut.drillDepth==0?"pasante":result.cut.drillDepth+" mm"))
     .setItems(options,(choose,option)->{
      float[] surface=option%2==0?result.cut.regenerate():result.intersection;
      if(!es.josxtm.mycad.cad.TriangleMeshHealth.inspect(surface).isWatertight()){
       toast("La malla no es cerrada");return;
      }
      if(option<2){showMeshOnly(new MeshEngine.Mesh(surface));return;}
      if(parts.size()<=Math.max(boxIndex,cutterIndex)||
         parts.get(boxIndex)!=box||parts.get(cutterIndex)!=cylinder||
         booleanMeshes.size()>=100||!box.sourceSketchId.isEmpty()||
         !cylinder.sourceSketchId.isEmpty()||surface.length>18000){
       toast("No se puede guardar: revisa selección y vínculos");return;
      }
      new AlertDialog.Builder(this).setTitle("Confirmar operación")
       .setMessage("Se sustituirán el prisma y el cilindro por la malla resultante. Puedes deshacer.")
       .setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(confirm,w)->{
        if(parts.size()<=Math.max(boxIndex,cutterIndex)||
           parts.get(boxIndex)!=box||parts.get(cutterIndex)!=cylinder)return;
        checkpoint();
        parts.remove(Math.max(boxIndex,cutterIndex));
        parts.remove(Math.min(boxIndex,cutterIndex));
        booleanMeshes.add(new MeshEngine.Mesh(surface));
        unionRecipes.add(null);
        holeRecipes.add(option==2?result.cut:null);
        selected=-1;selectedSketch=-1;activeCadFace=null;
        save();scene.invalidate();
        if(meshMode&&meshView!=null)renderCurrentCadMesh();
        toast(option==2?"Corte circular guardado y editable":"Intersección guardada como malla");
       }).show();
     }).setNegativeButton("Cerrar",null).show();
   }).setNegativeButton("Cancelar",null).show();
 }
 void drillSelectedBox(){
  if(selected<0||selected>=parts.size()){toast("Selecciona primero un prisma");return;}
  final Part parent=parts.get(selected);final int index=selected;
  if(parent.circle||!parent.sourceSketchId.isEmpty()){
   toast("Perforación disponible en prismas independientes, sin boceto vinculado");return;
  }
  if(booleanMeshes.size()>=100){toast("Máximo 100 mallas booleanas");return;}
  LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);
  TextView axisTitle=new TextView(this);axisTitle.setText("Dirección del taladro respecto a la pieza");fields.addView(axisTitle);
  final Spinner axisChoice=new Spinner(this);
  axisChoice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
   new String[]{"Z · sección X/Y","X · sección Y/Z","Y · sección Z/X"}));
  fields.addView(axisChoice);
  final EditText[] inputs=new EditText[4];
  final String[] labels={"Desplazamiento U del centro (mm)","Desplazamiento V del centro (mm)","Diámetro del agujero (mm)","Profundidad (mm; 0 = pasante)"};
  double[] defaults={0,0,Math.min(parent.w,Math.min(parent.d,parent.h))/4,0};
  for(int i=0;i<inputs.length;i++){
   TextView label=new TextView(this);label.setText(labels[i]);fields.addView(label);
   EditText input=new EditText(this);input.setInputType(8194|4096);
   input.setText(String.valueOf(defaults[i]));inputs[i]=input;fields.addView(input);
  }
  android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(fields);
  new AlertDialog.Builder(this).setTitle("Perforación circular · pasante o ciega")
   .setMessage("Elige el eje local X/Y/Z. La perforación gira con la pieza. Profundidad 0 = pasante; positiva = taladro ciego desde la cara exterior positiva.")
   .setView(scroll).setNegativeButton("Cancelar",null)
   .setNeutralButton("Previsualizar",(dialog,which)->{
    try{
     double[] vals=new double[4];for(int i=0;i<4;i++)vals[i]=Double.parseDouble(inputs[i].getText().toString().replace(',','.'));
     es.josxtm.mycad.cad.OrientedThroughHole.Axis axis=drillAxisChoice(axisChoice.getSelectedItemPosition());
     es.josxtm.mycad.cad.DrilledBoxRecipe drill=new es.josxtm.mycad.cad.DrilledBoxRecipe(
      parent.x,parent.y,parent.z,parent.w,parent.d,parent.h,vals[0],vals[1],vals[2],
      axis,parent.rotationX,parent.rotationY,parent.rotationZ,vals[3]);
     float[] preview=drill.regenerate();
     if(!es.josxtm.mycad.cad.TriangleMeshHealth.inspect(preview).isWatertight())throw new IllegalArgumentException("Open mesh");
     showMeshOnly(new MeshEngine.Mesh(preview));
    }catch(Exception e){toast("Perforación inválida: el agujero debe caber en la sección");}
   })
   .setPositiveButton("Guardar perforación",(dialog,which)->{
    try{
     if(index>=parts.size()||parts.get(index)!=parent||booleanMeshes.size()>=100)throw new IllegalArgumentException();
     double[] vals=new double[4];for(int i=0;i<4;i++)vals[i]=Double.parseDouble(inputs[i].getText().toString().replace(',','.'));
     es.josxtm.mycad.cad.OrientedThroughHole.Axis axis=drillAxisChoice(axisChoice.getSelectedItemPosition());
     es.josxtm.mycad.cad.DrilledBoxRecipe recipe=new es.josxtm.mycad.cad.DrilledBoxRecipe(
      parent.x,parent.y,parent.z,parent.w,parent.d,parent.h,vals[0],vals[1],vals[2],
      axis,parent.rotationX,parent.rotationY,parent.rotationZ,vals[3]);
     float[] geometry=recipe.regenerate();
     if(geometry.length==0||geometry.length>18000||
      !es.josxtm.mycad.cad.TriangleMeshHealth.inspect(geometry).isWatertight())throw new IllegalArgumentException("Open drilling mesh");
     checkpoint();parts.remove(index);booleanMeshes.add(new MeshEngine.Mesh(geometry));
     unionRecipes.add(null);holeRecipes.add(recipe);
     selected=-1;selectedSketch=-1;activeCadFace=null;save();scene.invalidate();
     if(meshMode&&meshView!=null)renderCurrentCadMesh();
     toast("Perforación guardada · "+(recipe.drillDepth==0?"pasante":"ciega")+" · editable en Resultados booleanos");
    }catch(Exception e){toast("No se pudo guardar la perforación");}
   }).show();
 }
 es.josxtm.mycad.cad.OrientedThroughHole.Axis drillAxisChoice(int choice){
  return choice==1?es.josxtm.mycad.cad.OrientedThroughHole.Axis.X:
         choice==2?es.josxtm.mycad.cad.OrientedThroughHole.Axis.Y:
                   es.josxtm.mycad.cad.OrientedThroughHole.Axis.Z;
 }
 int drillAxisPosition(es.josxtm.mycad.cad.OrientedThroughHole.Axis axis){
  return axis==es.josxtm.mycad.cad.OrientedThroughHole.Axis.X?1:
         axis==es.josxtm.mycad.cad.OrientedThroughHole.Axis.Y?2:0;
 }
 void editHoleRecipe(int index){
  if(index<0||index>=holeRecipes.size()||holeRecipes.get(index)==null){toast("Esta malla no es una perforación editable");return;}
  final es.josxtm.mycad.cad.DrilledBoxRecipe previous=holeRecipes.get(index);
  final EditText[] inputs=new EditText[4];
  LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);
  TextView header=new TextView(this);header.setText("Eje de perforación (local)");fields.addView(header);
  final Spinner axisChoice=new Spinner(this);
  axisChoice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
   new String[]{"Z · sección X/Y","X · sección Y/Z","Y · sección Z/X"}));
  axisChoice.setSelection(drillAxisPosition(previous.axis));fields.addView(axisChoice);
  String[] labels={"Desplazamiento U (mm)","Desplazamiento V (mm)","Diámetro (mm)","Profundidad (mm; 0 = pasante)"};
  double[] initial={previous.holeX,previous.holeY,previous.diameter,previous.drillDepth};
  for(int i=0;i<inputs.length;i++){
   TextView label=new TextView(this);label.setText(labels[i]);fields.addView(label);
   inputs[i]=new EditText(this);inputs[i].setInputType(8194|4096);
   inputs[i].setText(String.valueOf(initial[i]));fields.addView(inputs[i]);
  }
  android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(fields);
  new AlertDialog.Builder(this).setTitle("Editar perforación circular")
   .setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Regenerar",(dialog,which)->{
    try{
     if(index>=holeRecipes.size()||holeRecipes.get(index)!=previous)throw new IllegalArgumentException();
     double[] vals=new double[4];for(int i=0;i<4;i++)vals[i]=Double.parseDouble(inputs[i].getText().toString().replace(',','.'));
     es.josxtm.mycad.cad.DrilledBoxRecipe next=new es.josxtm.mycad.cad.DrilledBoxRecipe(
      previous.x,previous.y,previous.z,previous.width,previous.depth,previous.height,
      vals[0],vals[1],vals[2],drillAxisChoice(axisChoice.getSelectedItemPosition()),
      previous.rotationX,previous.rotationY,previous.rotationZ,vals[3]);
     float[] mesh=next.regenerate();
     if(mesh.length==0||mesh.length>18000||
      !es.josxtm.mycad.cad.TriangleMeshHealth.inspect(mesh).isWatertight())throw new IllegalArgumentException("Open drilling mesh");
     checkpoint();holeRecipes.set(index,next);booleanMeshes.set(index,new MeshEngine.Mesh(mesh));
     save();scene.invalidate();if(meshMode&&meshView!=null)renderCurrentCadMesh();
     toast("Perforación regenerada · "+(next.drillDepth==0?"pasante":"ciega"));
    }catch(Exception error){toast("Eje, diámetro o posición inválidos");}
   }).show();
 }
 void editFaceUnionRecipe(int meshIndex){
  if(meshIndex<0||meshIndex>=unionRecipes.size()||unionRecipes.get(meshIndex)==null){
   toast("Esta malla no tiene historial de extrusión editable");return;
  }
  final es.josxtm.mycad.cad.FaceUnionRecipe recipe=unionRecipes.get(meshIndex);
  EditText input=new EditText(this);input.setInputType(8194|4096);
  input.setText(String.valueOf(recipe.distance));
  new AlertDialog.Builder(this).setTitle("Editar extrusión fusionada · mm")
   .setMessage("Regenera la malla desde el prisma original y la distancia nueva.")
   .setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Regenerar",(d,w)->{
    try{
     if(meshIndex>=unionRecipes.size()||unionRecipes.get(meshIndex)!=recipe)throw new IllegalArgumentException();
     double distance=Double.parseDouble(input.getText().toString().replace(',','.'));
     es.josxtm.mycad.cad.FaceUnionRecipe updated=recipe.withDistance(distance);
     es.josxtm.mycad.cad.FacePrismExtrusion.Result ext=
      es.josxtm.mycad.cad.FacePrismExtrusion.create(updated.width,updated.depth,updated.height,updated.x,updated.y,updated.z,0,0,0,updated.face,updated.distance);
     float[] generated=BoxBooleanMesh.build(
      new BoxBoolean.Box(updated.x,updated.y,updated.z,updated.width,updated.depth,updated.height),
      new BoxBoolean.Box(ext.x,ext.y,ext.z,ext.w,ext.d,ext.h),BoxBooleanMesh.Operation.UNION);
     if(generated.length==0)throw new IllegalArgumentException();
     checkpoint();booleanMeshes.set(meshIndex,new MeshEngine.Mesh(generated));unionRecipes.set(meshIndex,updated);
     save();scene.invalidate();
     if(meshMode&&meshView!=null)renderCurrentCadMesh();
     toast("Malla fusionada regenerada");
    }catch(Exception error){toast("No se pudo regenerar la extrusión");}
   }).show();
 }
 void exportMeshMenuFor(MeshEngine.Mesh target){
  new AlertDialog.Builder(this).setTitle("Exportar geometría")
   .setItems(new String[]{"STL binario","OBJ"},(dialog,choice)->{
    meshExportFormat=choice==0?"stl":"obj";pendingMeshExport=target;
    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType(choice==0?"model/stl":"text/plain");
    i.putExtra(Intent.EXTRA_TITLE,"MyCAD_resultado."+meshExportFormat);
    try{startActivityForResult(i,EXPORT_MESH);}
    catch(Exception e){pendingMeshExport=null;toast("No se pudo abrir el selector");}
   }).show();
 }
 void inspectSelectedEdges(){
  if(selected<0||selected>=parts.size()){toast("Selecciona primero un prisma");return;}
  final int bodyIndex=selected;
  final Part p=parts.get(bodyIndex);
  if(p.circle){toast("Las aristas curvas del cilindro se implementarán más adelante");return;}
  final String[] names={
   "X · inferior frontal","X · inferior posterior","X · superior frontal","X · superior posterior",
   "Y · inferior izquierda","Y · inferior derecha","Y · superior izquierda","Y · superior derecha",
   "Z · frontal izquierda","Z · frontal derecha","Z · posterior izquierda","Z · posterior derecha"
  };
  new AlertDialog.Builder(this).setTitle("Aristas del cuerpo "+(bodyIndex+1))
   .setItems(names,(dialog,index)->{
    if(bodyIndex>=parts.size()||parts.get(bodyIndex)!=p)return;
    try{
     es.josxtm.mycad.cad.PrimitiveEdges.Edge edge=es.josxtm.mycad.cad.PrimitiveEdges.edge(
      p.id,index,p.w,p.d,p.h,
      new es.josxtm.mycad.cad.RigidTransform(p.x,p.y,p.z,p.rotationX,p.rotationY,p.rotationZ));
     info.setText(String.format(java.util.Locale.US,"Arista %d · longitud %.2f mm",index+1,edge.length));
     String message=String.format(java.util.Locale.US,
      "Longitud: %.2f mm\nInicio XYZ: %.2f, %.2f, %.2f\nFin XYZ: %.2f, %.2f, %.2f\nReferencia lógica: %s",
      edge.length,edge.start[0],edge.start[1],edge.start[2],
      edge.end[0],edge.end[1],edge.end[2],edge.key);
     new AlertDialog.Builder(this).setTitle(names[index])
      .setMessage(message).setPositiveButton("Cerrar",null).show();
    }catch(Exception error){toast("No se pudo calcular la arista");}
   }).setNegativeButton("Cancelar",null).show();
 }
 void inspectSelectedFaces(){
  if(selected<0||selected>=parts.size()){toast("Selecciona primero un cuerpo");return;}
  Part p=parts.get(selected);
  if(p.circle){new AlertDialog.Builder(this).setTitle("Superficies del cilindro")
    .setItems(new String[]{"Tapa superior","Tapa inferior","Superficie lateral"},(d,n)->{
      info.setText("Cuerpo "+(selected+1)+" · "+new String[]{"Tapa superior","Tapa inferior","Superficie lateral"}[n]);
    }).show();return;}
  final String[] faceNames={"Superior (+Z)","Inferior (-Z)","Derecha (+X)","Izquierda (-X)","Posterior (+Y)","Frontal (-Y)"};
  new AlertDialog.Builder(this).setTitle("Caras del prisma "+(selected+1))
   .setItems(faceNames,(d,n)->{
    float area=n<2?p.w*p.d:n<4?p.d*p.h:p.w*p.h;
    info.setText(String.format(java.util.Locale.US,"%s · área %.2f mm²",faceNames[n],area));
    new AlertDialog.Builder(this).setTitle(faceNames[n])
     .setMessage(String.format(java.util.Locale.US,"Área nominal: %.2f mm²\nSelección informativa. No se modifica la geometría.",area))
     .setPositiveButton("Aceptar",null).show();
   }).setNegativeButton("Cancelar",null).show();
 }
 void bodyTree(){if(parts.isEmpty()){toast("Todavía no hay cuerpos");return;}String[] names=new String[parts.size()];for(int i=0;i<parts.size();i++){Part p=parts.get(i);names[i]=(i==selected?"● ":"○ ")+"Cuerpo "+(i+1)+" · "+(p.circle?"Cilindro":"Prisma")+(p.sourceSketchId.isEmpty()?"":" · Vinculado")+" · "+Math.round(p.w)+"×"+Math.round(p.d)+"×"+Math.round(p.h)+" mm";}new AlertDialog.Builder(this).setTitle("Árbol de cuerpos").setSingleChoiceItems(names,selected,(d,i)->{selected=i;selectedSketch=-1;scene.invalidate();d.dismiss();}).setPositiveButton("Cerrar",null).show();}
 void chooseInteractionMode(){new AlertDialog.Builder(this).setTitle("Modo de interacción").setSingleChoiceItems(new String[]{"NAVEGAR · Girar cámara con un dedo","MOVER · Arrastrar pieza seleccionada","EDITAR · Arrastrar controladores"},interactionMode,(d,n)->{setInteractionMode(n);d.dismiss();}).setNegativeButton("Cancelar",null).show();}
 void moveOptions(){if(selected<0){toast("Selecciona una pieza primero");return;}String[] opts={"Libre (plano de trabajo)","Ortogonal (X o Y dominante)","Seguir eje X / Y / Z","Radial (distancia al origen fija)","Distancia exacta (XYZ)","Posición exacta XYZ"};new AlertDialog.Builder(this).setTitle("Mover pieza · Restricciones").setItems(opts,(d,n)->{if(n==5){positionSelected();return;}if(n==4){moveExact();return;}moveMode=n;if(n==2){new AlertDialog.Builder(this).setTitle("Elegir eje").setSingleChoiceItems(new String[]{"X","Y","Z"},moveAxis,(di,k)->{moveAxis=k;di.dismiss();storeSettings();setInteractionMode(1);toast("Arrastra la pieza sobre el eje "+new String[]{"X","Y","Z"}[k]);}).show();}else{storeSettings();interactionMode=1;scene.movingEntity=true;toast("Modo "+opts[n]+" · arrastra la pieza");}}).show();}
 void moveExact(){if(selected<0)return;Part p=parts.get(selected);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText[] inputs=new EditText[3];for(int i=0;i<3;i++){TextView label=new TextView(this);label.setText(new String[]{"Desplazamiento X (mm)","Desplazamiento Y (mm)","Desplazamiento Z (mm)"}[i]);box.addView(label);inputs[i]=new EditText(this);inputs[i].setInputType(8194|4096);inputs[i].setText("0");box.addView(inputs[i]);}new AlertDialog.Builder(this).setTitle("Mover distancia exacta").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{try{float[] v=new float[3];for(int i=0;i<3;i++){v[i]=Float.parseFloat(inputs[i].getText().toString().replace(',','.'));if(!Float.isFinite(v[i]))throw new Exception();}checkpoint();p.x+=v[0];p.y+=v[1];p.z+=v[2];save();scene.invalidate();}catch(Exception ex){toast("Desplazamiento no válido");}}).show();}
 float snap(float v){return snapEnabled&&gridStep>0?Math.round(v/gridStep)*gridStep:v;}
 void movieMenu(){String[] opts={"Seleccionar pieza…","Girar pieza seleccionada","Girar conjunto completo","Velocidad de giro","Repetición: "+(movieLoop?"Sí":"No"),movie?"Pausar película":"Iniciar película"};new AlertDialog.Builder(this).setTitle("Modo película 3D").setItems(opts,(d,n)->{if(n==0){choose();return;}if(n==1||n==2){if(n==1&&selected<0){toast("Selecciona primero una pieza 3D");return;}if(movie)scene.stopMovie();movieAll=n==2;movie=true;flat=false;scene.startMovie();}else if(n==3){SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(Math.round(movieSpeed*50));new AlertDialog.Builder(this).setTitle("Velocidad película").setView(bar).setPositiveButton("Aplicar",(di,w)->{movieSpeed=.1f+bar.getProgress()/50f;}).show();}else if(n==4){movieLoop=!movieLoop;movieMenu();}else{movie=!movie;if(movie){flat=false;scene.startMovie();}else scene.stopMovie();}}).show();}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 void choose(){if(parts.isEmpty()){toast("Añade una figura");return;}String[] a=new String[parts.size()];for(int i=0;i<a.length;i++){Part p=parts.get(i);a[i]=(i+1)+". "+(p.circle?"Círculo":"Rectángulo")+" "+p.w+" × "+p.d+" × "+p.h+" mm";}new AlertDialog.Builder(this).setTitle("Seleccionar pieza").setItems(a,(d,n)->{selected=n;selectedSketch=-1;scene.invalidate();toast("Pieza "+(n+1)+" seleccionada");}).show();}
 void editLinkedOrPart(){
  if(selected<0||selected>=parts.size())return;
  Part p=parts.get(selected);
  if(p.sourceSketchId.isEmpty()){edit(p.circle,true);return;}
  new AlertDialog.Builder(this).setTitle("Editar extrusión vinculada")
   .setItems(new String[]{"Modificar altura (mm)","Editar perfil original","Editar cuerpo independientemente"},(d,n)->{
    if(n==0){editExtrusionHeight(p);return;}
    if(n==1){for(int i=0;i<sketches.size();i++)if(sketches.get(i).id.equals(p.sourceSketchId)){selectedSketch=i;scene.editSketch();return;}toast("Perfil original no encontrado");return;}
    edit(p.circle,true);
   }).show();
 }
 void editExtrusionHeight(Part p){
  EditText input=new EditText(this);input.setInputType(8194|4096);input.setText(String.valueOf(p.h));
  new AlertDialog.Builder(this).setTitle("Altura de extrusión · mm").setView(input).setNegativeButton("Cancelar",null)
   .setPositiveButton("Aplicar",(d,n)->{try{
    float height=Float.parseFloat(input.getText().toString().replace(',','.'));
    GeometryMath.requirePositive(height);
    if(!Float.isFinite(height)||height>10000)throw new IllegalArgumentException("Altura fuera de límites");
    if(!p.sourceSketchId.isEmpty()&&!p.circle){
     Sketch source=null;
     for(Sketch q:sketches)if(q.id.equals(p.sourceSketchId)){source=q;break;}
     if(source!=null){
      es.josxtm.mycad.cad.CadKernel.Result<es.josxtm.mycad.cad.CadKernel.Box> validation=
       es.josxtm.mycad.cad.PlaneExtrusion.extrude(
        new es.josxtm.mycad.cad.PrimitiveKernel(),
        es.josxtm.mycad.cad.PlaneExtrusion.Plane.values()[source.plane],
        Math.min(source.x1,source.x2),Math.min(source.y1,source.y2),0,
        Math.abs(source.x2-source.x1),Math.abs(source.y2-source.y1),height);
      if(!validation.isSuccess()){toast("Motor CAD: extrusión inválida");return;}
     }
    }
    if(!p.sourceSketchId.isEmpty()){
     Sketch linked=null;for(Sketch q:sketches)if(q.id.equals(p.sourceSketchId)){linked=q;break;}
     if(linked!=null)es.josxtm.mycad.cad.LinkedSketchSolidPlan.build(
      linked.type,linked.plane,linked.x1,linked.y1,linked.x2,linked.y2,height);
    }
    checkpoint();
    if(!p.sourceSketchId.isEmpty()){
     Sketch source=null;for(Sketch q:sketches)if(q.id.equals(p.sourceSketchId)){source=q;break;}
     if(source!=null)updateDraft(p,makeExtrudedPart(source,height));
     else p.h=height;
    }else p.h=height;
    save();scene.invalidate();
    activeCadFace=null;
    if(meshMode&&meshView!=null)renderCurrentCadMesh();
   }catch(Exception e){toast("Altura inválida");}}).show();
 }
 boolean validateLinkedSketchEdit(Sketch source,float x1,float y1,float x2,float y2){
  if(source.type==1)return Float.isFinite(x1)&&Float.isFinite(y1)&&
   Float.isFinite(x2)&&Float.isFinite(y2)&&Math.abs(x1)<=10000&&
   Math.abs(y1)<=10000&&Math.abs(x2)<=10000&&Math.abs(y2)<=10000;
  try{
   java.util.ArrayList<Double> heights=new java.util.ArrayList<>();
   for(Part part:parts)if(source.id.equals(part.sourceSketchId))heights.add((double)part.h);
   double[] values=new double[heights.size()];
   for(int i=0;i<values.length;i++)values[i]=heights.get(i);
   es.josxtm.mycad.cad.LinkedSketchSolidPlan.plan(
    source.type,source.plane,x1,y1,x2,y2,values);
   return true;
  }catch(IllegalArgumentException error){return false;}
 }
 void rebuildLinkedParts(Sketch source){
  // Generate every changed body before modifying even one dependent.
  java.util.ArrayList<Part> originals=new java.util.ArrayList<>(),staged=new java.util.ArrayList<>();
  for(Part part:parts)if(source.id.equals(part.sourceSketchId)){
   originals.add(part);staged.add(makeExtrudedPart(source,part.h));
  }
  for(int i=0;i<originals.size();i++)updateDraft(originals.get(i),staged.get(i));
 }
 void edit(boolean circle,boolean existing){
 if(fileBusy)return;if(!existing&&parts.size()>=500){toast("Máximo 500 piezas");return;}
 Part p=existing?parts.get(selected):new Part(circle,40,30,10,parts.size()*50);
 LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(32,12,32,12);
 String[] labels={circle?"Diámetro (mm)":"Ancho (mm)","Fondo (mm)","Espesor (mm)","Posición X (mm)"};
 float[] vals={p.w,p.d,p.h,p.x};EditText[] fields=new EditText[4];
 for(int i=0;i<4;i++){TextView t=new TextView(this);t.setText(labels[i]);box.addView(t);fields[i]=new EditText(this);fields[i].setInputType(8194|4096);fields[i].setText(""+vals[i]);box.addView(fields[i]);if(circle&&i==1){t.setVisibility(View.GONE);fields[i].setVisibility(View.GONE);}}
 AlertDialog dlg=new AlertDialog.Builder(this).setTitle(existing?"Editar pieza":"Figura y extrusión").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",null).create();
 dlg.setOnShowListener(v->dlg.getButton(-1).setOnClickListener(w->{try{float a=Float.parseFloat(fields[0].getText().toString().replace(',','.'));float d=circle?a:Float.parseFloat(fields[1].getText().toString().replace(',','.'));float h=Float.parseFloat(fields[2].getText().toString().replace(',','.'));float x=Float.parseFloat(fields[3].getText().toString().replace(',','.'));if(!Float.isFinite(a+d+h+x)||a<=0||d<=0||h<=0||a>10000||d>10000||h>10000||Math.abs(x)>10000)throw new Exception();Part q=new Part(circle,a,d,h,x);if(existing){q.id=p.id;q.y=p.y;q.z=p.z;q.rotationX=p.rotationX;q.rotationY=p.rotationY;q.rotationZ=p.rotationZ;q.sourceSketchId="";}checkpoint();if(existing)parts.set(selected,q);else{parts.add(q);selected=parts.size()-1;}save();scene.invalidate();dlg.dismiss();}catch(Exception e){toast("Introduce medidas positivas hasta 10000 mm y posición válida");}}));dlg.show();
 }

 void updateStep(){if(nextStep!=null)nextStep.setText(parts.isEmpty()?"1 · Crea una figura con medidas":"2 · Edita / revisa en 3D   →   3 · Guarda un archivo");}
 String encode() throws Exception {
 JSONArray a=new JSONArray();for(Part p:parts)a.put(new JSONObject().put("c",p.circle).put("w",p.w).put("d",p.d).put("h",p.h).put("x",p.x).put("y",p.y).put("z",p.z).put("rotationX",p.rotationX).put("rotationY",p.rotationY).put("rotationZ",p.rotationZ).put("sourceSketchId",p.sourceSketchId).put("bodyId",p.id));
 JSONArray sk=new JSONArray();for(Sketch q:sketches)sk.put(new JSONObject().put("t",q.type).put("id",q.id).put("plane",q.plane).put("x1",q.x1).put("y1",q.y1).put("x2",q.x2).put("y2",q.y2));JSONObject file=new JSONObject().put("format","mycad").put("version",1).put("units","mm").put("parts",a).put("sketches",sk).put("booleanMeshes",encodeBooleanMeshes());
 JSONArray recipes=new JSONArray();
 for(es.josxtm.mycad.cad.FaceUnionRecipe recipe:unionRecipes){
  if(recipe==null){recipes.put(JSONObject.NULL);continue;}
  recipes.put(new JSONObject().put("w",recipe.width).put("d",recipe.depth).put("h",recipe.height)
   .put("x",recipe.x).put("y",recipe.y).put("z",recipe.z)
   .put("face",recipe.face.name()).put("distance",recipe.distance));
 }
 file.put("faceUnionRecipes",recipes);
 JSONArray drills=new JSONArray();
 for(es.josxtm.mycad.cad.DrilledBoxRecipe drill:holeRecipes){
  if(drill==null){drills.put(JSONObject.NULL);continue;}
  drills.put(new JSONObject().put("x",drill.x).put("y",drill.y).put("z",drill.z)
   .put("w",drill.width).put("d",drill.depth).put("h",drill.height)
   .put("offsetX",drill.holeX).put("offsetY",drill.holeY).put("diameter",drill.diameter)
   .put("axis",drill.axis.name()).put("rotationX",drill.rotationX)
   .put("rotationY",drill.rotationY).put("rotationZ",drill.rotationZ)
   .put("drillDepth",drill.drillDepth));
 }
 file.put("throughHoleRecipes",drills);

 JSONArray chains=new JSONArray();
 for(es.josxtm.mycad.cad.FaceExtrusionChain chain:extrusionChains){
  java.util.List<es.josxtm.mycad.cad.FaceExtrusionChain.Solid> solids=chain.regenerate();
  es.josxtm.mycad.cad.FaceExtrusionChain.Solid root=solids.get(0);
  JSONObject entry=new JSONObject().put("id",chain.id).put("w",root.w).put("d",root.d).put("h",root.h).put("x",root.x).put("y",root.y).put("z",root.z);
  JSONArray steps=new JSONArray();for(es.josxtm.mycad.cad.FaceExtrusionChain.Step step:chain.history())steps.put(new JSONObject().put("id",step.id).put("face",step.face.name()).put("mm",step.distance));
  chains.put(entry.put("steps",steps));
 }
 file.put("extrusionChains",chains);
 JSONArray storedFeatures=new JSONArray();
 for(es.josxtm.mycad.cad.ChainBooleanFeature f:chainFeatures)
  storedFeatures.put(new JSONObject().put("id",f.id).put("chain",f.chainIndex).put("chainId",f.chainId==null?"":f.chainId).put("operation",f.operation.name())
   .put("x",f.x).put("y",f.y).put("z",f.z).put("w",f.w).put("d",f.d).put("h",f.h));
 file.put("chainBooleanFeatures",storedFeatures);
 try{
  java.util.ArrayList<es.josxtm.mycad.cad.CadIntegrationProbe.SketchData> profiles=new java.util.ArrayList<>();
  java.util.ArrayList<es.josxtm.mycad.cad.CadIntegrationProbe.PartData> solids=new java.util.ArrayList<>();
  for(Sketch q:sketches)profiles.add(new es.josxtm.mycad.cad.CadIntegrationProbe.SketchData(q.id,q.type,q.plane,q.x1,q.y1,q.x2,q.y2));
  for(Part p:parts)solids.add(new es.josxtm.mycad.cad.CadIntegrationProbe.PartData(p.sourceSketchId,p.h,p.x,p.y,p.z,p.rotationX,p.rotationY,p.rotationZ));
  es.josxtm.mycad.cad.CadIntegrationProbe.Report report=es.josxtm.mycad.cad.CadIntegrationProbe.inspect(profiles,solids);
  JSONArray bindings=new JSONArray();
  for(int i=0;i<report.compatiblePartIndices.size();i++){
   int index=report.compatiblePartIndices.get(i);
   Part part=parts.get(index);
   bindings.put(new JSONObject().put("partIndex",index)
    .put("bodyId",report.generatedBodyIds.get(i))
    .put("sourceSketchId",part.sourceSketchId)
    .put("x",part.x).put("y",part.y).put("z",part.z)
    .put("rotationX",part.rotationX).put("rotationY",part.rotationY).put("rotationZ",part.rotationZ)
    .put("worldBounds",new JSONArray(report.worldBounds.get(i))));
  }
  file.put("parametricBodyBindings",bindings);
  file.put("parametricPreview",report.parametricSnapshot);
  file.put("parametricPreviewCoverageComplete",report.readyForMigration());
 }catch(Exception ignored){
  // Preview is optional; never prevent a legacy project from being saved.
 }
 return file.toString(2);
 }
 JSONArray encodeBooleanMeshes()throws Exception{
  JSONArray all=new JSONArray();for(MeshEngine.Mesh mesh:booleanMeshes){
   JSONArray vertices=new JSONArray();for(float f:mesh.vertices)vertices.put((double)f);
   all.put(vertices);
  }return all;
 }
 class ParsedDesign {ArrayList<Part> parts;ArrayList<Sketch> sketches;ArrayList<MeshEngine.Mesh> meshes;ArrayList<es.josxtm.mycad.cad.FaceUnionRecipe> recipes;ArrayList<es.josxtm.mycad.cad.DrilledBoxRecipe> drills;ArrayList<es.josxtm.mycad.cad.FaceExtrusionChain> chains;ArrayList<es.josxtm.mycad.cad.ChainBooleanFeature> features;ParsedDesign(ArrayList<Part> p,ArrayList<Sketch> sk,ArrayList<MeshEngine.Mesh> m,ArrayList<es.josxtm.mycad.cad.FaceUnionRecipe> r,ArrayList<es.josxtm.mycad.cad.FaceExtrusionChain> c,ArrayList<es.josxtm.mycad.cad.ChainBooleanFeature> f,ArrayList<es.josxtm.mycad.cad.DrilledBoxRecipe> holes){parts=p;sketches=sk;meshes=m;recipes=r;chains=c;features=f;drills=holes;}}
 ParsedDesign decode(String data) throws Exception {
 Object raw=new JSONTokener(data).nextValue();JSONArray a;ArrayList<Sketch> parsedSketches=new ArrayList<>();ArrayList<MeshEngine.Mesh> parsedMeshes=new ArrayList<>();ArrayList<es.josxtm.mycad.cad.FaceUnionRecipe> parsedRecipes=new ArrayList<>();ArrayList<es.josxtm.mycad.cad.DrilledBoxRecipe> parsedDrills=new ArrayList<>();ArrayList<es.josxtm.mycad.cad.FaceExtrusionChain> parsedChains=new ArrayList<>();ArrayList<es.josxtm.mycad.cad.ChainBooleanFeature> parsedFeatures=new ArrayList<>();
 if(raw instanceof JSONArray)a=(JSONArray)raw;
 else if(raw instanceof JSONObject){JSONObject o=(JSONObject)raw;if(!"mycad".equals(o.getString("format"))||o.getInt("version")!=1||!"mm".equals(o.getString("units")))throw new IOException("Formato o versión no compatible");a=o.getJSONArray("parts");
 JSONArray meshArrays=o.optJSONArray("booleanMeshes");
 if(meshArrays!=null){
  if(meshArrays.length()>100)throw new IOException("Demasiadas mallas booleanas");
  for(int n=0;n<meshArrays.length();n++){
   JSONArray vertices=meshArrays.getJSONArray(n);
   if(vertices.length()%9!=0||vertices.length()>18000)throw new IOException("Malla inválida");
   float[] meshCoordinates=new float[vertices.length()];
   for(int k=0;k<meshCoordinates.length;k++){
    meshCoordinates[k]=(float)vertices.getDouble(k);
    if(!Float.isFinite(meshCoordinates[k])||Math.abs(meshCoordinates[k])>100000)throw new IOException("Vértice inválido");
   }
   parsedMeshes.add(new MeshEngine.Mesh(meshCoordinates));
  }
 }
 JSONArray chainArray=o.optJSONArray("extrusionChains");
 if(chainArray!=null){
  if(chainArray.length()>100)throw new IOException("Demasiadas cadenas");
  for(int ci=0;ci<chainArray.length();ci++){
   JSONObject entry=chainArray.getJSONObject(ci);
   try{
    es.josxtm.mycad.cad.FaceExtrusionChain chain=new es.josxtm.mycad.cad.FaceExtrusionChain(entry.optString("id",java.util.UUID.randomUUID().toString()),entry.getDouble("w"),entry.getDouble("d"),entry.getDouble("h"),entry.getDouble("x"),entry.getDouble("y"),entry.getDouble("z"));
    JSONArray steps=entry.getJSONArray("steps");
    if(steps.length()>50)throw new IOException("Demasiadas extrusiones");
    for(int si=0;si<steps.length();si++){JSONObject step=steps.getJSONObject(si);chain.appendStep(new es.josxtm.mycad.cad.FaceExtrusionChain.Step(step.optString("id",java.util.UUID.randomUUID().toString()),es.josxtm.mycad.cad.BoxFaceSelection.Face.valueOf(step.getString("face")),step.getDouble("mm")));}
    chain.regenerate();parsedChains.add(chain);
   }catch(IllegalArgumentException error){throw new IOException("Cadena inválida",error);}
  }
 }
 JSONArray stored=o.optJSONArray("chainBooleanFeatures");
 if(stored!=null){
  if(stored.length()>100)throw new IOException("Demasiadas operaciones booleanas");
  for(int fi=0;fi<stored.length();fi++){
   JSONObject f=stored.getJSONObject(fi);
   try{
    int legacyIndex=f.getInt("chain");
    String stableId=f.optString("chainId","");
    if(stableId.isEmpty()){
     if(legacyIndex<0||legacyIndex>=parsedChains.size())throw new IOException("Referencia a cadena inexistente");
     stableId=parsedChains.get(legacyIndex).id;
    }
    es.josxtm.mycad.cad.ChainBooleanFeature feature=new es.josxtm.mycad.cad.ChainBooleanFeature(
     f.optString("id",java.util.UUID.randomUUID().toString()),legacyIndex,stableId,MultiBoxUnion.Operation.valueOf(f.getString("operation")),
     f.getDouble("x"),f.getDouble("y"),f.getDouble("z"),f.getDouble("w"),f.getDouble("d"),f.getDouble("h"));
    feature.regenerate(parsedChains);
    parsedFeatures.add(feature);
   }catch(IllegalArgumentException e){throw new IOException("Operación booleana inválida",e);}
  }
 }
 JSONArray drillArray=o.optJSONArray("throughHoleRecipes");
 if(drillArray!=null){
  if(drillArray.length()!=parsedMeshes.size())throw new IOException("Perforaciones sin malla");
  for(int hi=0;hi<drillArray.length();hi++){
   if(drillArray.isNull(hi)){parsedDrills.add(null);continue;}
   JSONObject v=drillArray.getJSONObject(hi);
   try{
    parsedDrills.add(new es.josxtm.mycad.cad.DrilledBoxRecipe(
     v.getDouble("x"),v.getDouble("y"),v.getDouble("z"),
     v.getDouble("w"),v.getDouble("d"),v.getDouble("h"),
     v.getDouble("offsetX"),v.getDouble("offsetY"),v.getDouble("diameter"),
     es.josxtm.mycad.cad.OrientedThroughHole.Axis.valueOf(v.optString("axis","Z")),
     v.optDouble("rotationX",0),v.optDouble("rotationY",0),v.optDouble("rotationZ",0),
     v.optDouble("drillDepth",0)));
   }catch(IllegalArgumentException e){throw new IOException("Perforación inválida",e);}
  }
 }
 while(parsedDrills.size()<parsedMeshes.size())parsedDrills.add(null);
 JSONArray recipeArray=o.optJSONArray("faceUnionRecipes");
 if(recipeArray!=null){
  if(recipeArray.length()!=parsedMeshes.size())throw new IOException("Recetas sin malla");
  for(int ri=0;ri<recipeArray.length();ri++){
   if(recipeArray.isNull(ri)){parsedRecipes.add(null);continue;}
   JSONObject v=recipeArray.getJSONObject(ri);
   try{
    parsedRecipes.add(new es.josxtm.mycad.cad.FaceUnionRecipe(v.getDouble("w"),v.getDouble("d"),v.getDouble("h"),
     v.getDouble("x"),v.getDouble("y"),v.getDouble("z"),
     es.josxtm.mycad.cad.BoxFaceSelection.Face.valueOf(v.getString("face")),v.getDouble("distance")));
   }catch(IllegalArgumentException e){throw new IOException("Receta de unión inválida",e);}
  }
 }
 while(parsedRecipes.size()<parsedMeshes.size())parsedRecipes.add(null);
 JSONArray sk=o.optJSONArray("sketches");if(sk!=null){if(sk.length()>2000)throw new IOException("Demasiados bocetos");for(int i=0;i<sk.length();i++){JSONObject q=sk.getJSONObject(i);int t=q.getInt("t");float x1=(float)q.getDouble("x1"),y1=(float)q.getDouble("y1"),x2=(float)q.getDouble("x2"),y2=(float)q.getDouble("y2");if(t<1||t>3||!Float.isFinite(x1+y1+x2+y2)||Math.max(Math.max(Math.abs(x1),Math.abs(y1)),Math.max(Math.abs(x2),Math.abs(y2)))>10000)throw new IOException("Boceto inválido");Sketch loaded=new Sketch(t,x1,y1,x2,y2);loaded.id=q.optString("id",loaded.id);loaded.plane=q.optInt("plane",0);if(loaded.plane<0||loaded.plane>2)throw new IOException("Plano inválido");parsedSketches.add(loaded);}}}
 else throw new IOException("Archivo no válido");
 if(a.length()>500)throw new IOException("Máximo 500 piezas");
 java.util.HashSet<String> loadedBodyIds=new java.util.HashSet<>();
 ArrayList<Part> list=new ArrayList<>();
 for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);float w=(float)p.getDouble("w"),d=(float)p.getDouble("d"),h=(float)p.getDouble("h"),x=(float)p.getDouble("x");
 if(!Float.isFinite(w+d+h+x)||w<=0||d<=0||h<=0||w>10000||d>10000||h>10000||Math.abs(x)>10000)throw new IOException("Medidas no válidas");
 boolean circle=p.getBoolean("c");if(circle&&w!=d)throw new IOException("Diámetro inconsistente");
 Part item=new Part(circle,w,d,h,x);item.id=p.optString("bodyId",item.id);item.y=(float)p.optDouble("y",0);item.z=(float)p.optDouble("z",0);item.rotationX=(float)p.optDouble("rotationX",0);item.rotationY=(float)p.optDouble("rotationY",0);item.rotationZ=(float)p.optDouble("rotationZ",0);item.sourceSketchId=p.optString("sourceSketchId","");if(!Float.isFinite(item.y+item.z+item.rotationX+item.rotationY+item.rotationZ)||Math.abs(item.y)>10000||Math.abs(item.z)>10000||item.id.length()>100||!validBodyId(item.id))throw new IOException("Posición o ID de cuerpo inválidos");if(!loadedBodyIds.add(item.id))throw new IOException("Cuerpos duplicados");list.add(item);}try{es.josxtm.mycad.cad.ChainDependencyReport.requireValid(parsedChains,parsedFeatures);}catch(IllegalArgumentException e){throw new IOException("Historial paramétrico inválido",e);}return new ParsedDesign(list,parsedSketches,parsedMeshes,parsedRecipes,parsedChains,parsedFeatures,parsedDrills);
 }
 boolean validBodyId(String id){try{return java.util.UUID.fromString(id).toString().equals(id);}catch(Exception e){return false;}}
 void checkpoint(){try{undo.push(encode());if(undo.size()>50)undo.removeLast();redo.clear();}catch(Exception e){}}
 void history(boolean back){
  ArrayDeque<String> src=back?undo:redo,dst=back?redo:undo;
  if(src.isEmpty()){toast("Sin cambios");return;}
  try{
   // Decode the target revision BEFORE modifying either stack or the live project.
   String prior=encode();
   ParsedDesign restored=decode(src.peek());
   src.pop();dst.push(prior);
   activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;if(meshView!=null)meshView.highlightFace(-1);
   parts=restored.parts;sketches=restored.sketches;
   booleanMeshes=restored.meshes;unionRecipes=restored.recipes;holeRecipes=restored.drills;
   extrusionChains=restored.chains;chainFeatures=restored.features;
   selected=parts.isEmpty()?-1:0;selectedSketch=-1;
   save();scene.invalidate();
   if(meshMode&&meshView!=null)renderCurrentCadMesh();
  }catch(Exception error){toast("No se pudo recuperar la revisión; historial conservado");}
 }
 void positionSelected(){if(selected<0){toast("Selecciona una pieza");return;}Part p=parts.get(selected);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText[] fields=new EditText[3];float[] vals={p.x,p.y,p.z};for(int i=0;i<3;i++){TextView label=new TextView(this);label.setText(new String[]{"X (mm)","Y (mm)","Z (mm)"}[i]);box.addView(label);fields[i]=new EditText(this);fields[i].setInputType(8194|4096);fields[i].setText(""+vals[i]);box.addView(fields[i]);}new AlertDialog.Builder(this).setTitle("Mover pieza XYZ").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{try{float[] v=new float[3];for(int i=0;i<3;i++){v[i]=Float.parseFloat(fields[i].getText().toString().replace(',','.'));if(!Float.isFinite(v[i])||Math.abs(v[i])>10000)throw new Exception();}checkpoint();p.x=v[0];p.y=v[1];p.z=v[2];save();scene.invalidate();}catch(Exception e){toast("Coordenadas no válidas");}}).show();}
 void save(){try{getPreferences(MODE_PRIVATE).edit().putString("design",encode()).apply();updateStep();}catch(Exception e){toast("No se pudo guardar el borrador interno");}}
 void load(){activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;if(meshView!=null)meshView.highlightFace(-1);try{ParsedDesign restored=decode(getPreferences(MODE_PRIVATE).getString("design","[]"));parts=restored.parts;sketches=restored.sketches;booleanMeshes=restored.meshes;unionRecipes=restored.recipes;holeRecipes=restored.drills;extrusionChains=restored.chains;chainFeatures=restored.features;selected=parts.isEmpty()?-1:0;selectedSketch=-1;scene.invalidate();if(meshMode&&meshView!=null)renderCurrentCadMesh();updateStep();}catch(Exception e){toast("No se pudo recuperar el borrador");}}
 protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("pendingExport",pendingExport);}

 void addTri(java.util.ArrayList<Float> a,float[] p,float[] q,float[] r){for(float v:p)a.add(v);for(float v:q)a.add(v);for(float v:r)a.add(v);}
 void quad(java.util.ArrayList<Float> a,float[] p,float[] q,float[] r,float[] t){addTri(a,p,q,r);addTri(a,p,r,t);}
 void selectChainTriangle(int triangle){
  if(!cadGpuMesh||triangle<0)return;
  int offset=importedMesh==null?0:importedMesh.triangles();
  for(MeshEngine.Mesh m:booleanMeshes)offset+=m.triangles();
  for(Part p:parts)offset+=p.circle?es.josxtm.mycad.cad.CylinderSurface.triangleCount(48):12;
  int[] counts=new int[extrusionChains.size()];
  for(int i=0;i<counts.length;i++)counts[i]=extrusionChains.get(i).size();
  es.josxtm.mycad.cad.ChainTriangleMap.Hit hit=
   es.josxtm.mycad.cad.ChainTriangleMap.resolve(triangle,offset,counts);
  if(hit==null)return;
  final int chainIndex=hit.chainIndex,stepIndex=hit.stepIndex;
  final es.josxtm.mycad.cad.FaceExtrusionChain chain=extrusionChains.get(chainIndex);
  activeCadFace=null;selected=-1;selectedSketch=-1;
  if(meshView!=null)meshView.highlightFace(triangle-(hit.localTriangle%2));
  info.setText("Cadena "+(chainIndex+1)+" · extrusión "+(stepIndex+1)+" seleccionada");
  new AlertDialog.Builder(this).setTitle("Extrusión "+(stepIndex+1)+" · cadena "+(chainIndex+1))
   .setItems(new String[]{"Editar distancia","Abrir historial"},(d,n)->{
    if(chainIndex>=extrusionChains.size()||extrusionChains.get(chainIndex)!=chain||stepIndex>=chain.size())return;
    if(n==0)chainDistanceDialog(chain,chainIndex,stepIndex,chain.history().get(stepIndex).face);
    else editExtrusionChain(chainIndex);
   }).show();
 }
 void showGpuSurfaceSelection(int triangle,double x,double y,double z){
  if(isolatedMeshPreview){activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;if(meshView!=null)meshView.highlightEdge(null);info.setText("Vista previa · sin selección de piezas");return;}
  boolean handled=false;
  if(cadGpuMesh){
   es.josxtm.mycad.cad.PrimitiveTopologyMap map=new es.josxtm.mycad.cad.PrimitiveTopologyMap();
   int first=importedMesh==null?0:importedMesh.triangles();
   for(MeshEngine.Mesh m:booleanMeshes)first+=m.triangles();
   for(int i=0;i<parts.size();i++){
    Part p=parts.get(i);
    if(p.circle){
     map.addCylinder(i,p.id,first,48);
     first+=es.josxtm.mycad.cad.CylinderSurface.triangleCount(48);
    }else{
     map.addBox(i,p.id,first);
     first+=12;
    }
   }
   es.josxtm.mycad.cad.PrimitiveTopologyMap.Hit hit=map.resolve(triangle);
   if(hit!=null&&hit.partIndex<parts.size()&&parts.get(hit.partIndex).id.equals(hit.bodyId)){
    selected=hit.partIndex;selectedSketch=-1;scene.invalidate();
    String description;
    if(hit.kind==es.josxtm.mycad.cad.PrimitiveTopologyMap.Kind.BOX){
     activeCylinderFaceId=null;
     activeCadFace=new es.josxtm.mycad.cad.CadFaceReference(hit.partIndex,hit.bodyId,hit.boxFace);
     description="Prisma "+(hit.partIndex+1)+" · "+hit.boxFace.name().replace('_',' ');
    }else{
     activeCadFace=null;activeCylinderFaceId=hit.faceId;
     description="Cilindro "+(hit.partIndex+1)+" · "+
      (hit.kind==es.josxtm.mycad.cad.PrimitiveTopologyMap.Kind.CYLINDER_SIDE?"lateral":
       hit.kind==es.josxtm.mycad.cad.PrimitiveTopologyMap.Kind.CYLINDER_BOTTOM?"base":"tapa");
    }
    try{
     Part chosen=parts.get(hit.partIndex);
     es.josxtm.mycad.cad.PrimitiveBRepBody solid=analyticBody(chosen);
     es.josxtm.mycad.cad.AnalyticEdgePicker.Hit boundary=
      es.josxtm.mycad.cad.AnalyticEdgePicker.nearestEdge(
       solid,x,y,z,meshView==null?
       es.josxtm.mycad.cad.AnalyticEdgePicker.suggestedTolerance(solid):
       es.josxtm.mycad.cad.EdgeScreenTolerance.compute(
         solid,meshView.cameraDistance(),meshView.getHeight(),
         getResources().getDisplayMetrics().density));
     activeAnalyticEdgeId=boundary==null?null:boundary.edgeId;
     if(boundary!=null){
      description+=" · Arista "+boundary.edgeId.substring(boundary.edgeId.lastIndexOf('/')+1);
      if(meshView!=null)meshView.highlightEdge(
       es.josxtm.mycad.cad.PrimitiveEdgeCurve.points(solid,boundary.edgeId,64));
     }else if(meshView!=null)meshView.highlightEdge(null);
    }catch(IllegalArgumentException ignored){
     activeAnalyticEdgeId=null;
     if(meshView!=null)meshView.highlightEdge(null);
    }
    if(meshView!=null)meshView.highlightFace(hit.highlightTriangle());
    info.setText(String.format(java.util.Locale.US,"%s · X %.1f Y %.1f Z %.1f mm",description,x,y,z));
    handled=true;
   }
  }
  if(!handled){
   activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;
   if(meshView!=null){meshView.highlightFace(-1);meshView.highlightEdge(null);}
   info.setText(String.format(java.util.Locale.US,"Triángulo %d · X %.1f Y %.1f Z %.1f mm",triangle+1,x,y,z));
   selectChainTriangle(triangle);
  }
 }
 void manageExtrusionChains(){
  java.util.ArrayList<String> choices=new java.util.ArrayList<>();
  for(int i=0;i<extrusionChains.size();i++)choices.add("Cadena "+(i+1)+" · "+extrusionChains.get(i).size()+" extrusiones");
  choices.add("Crear cadena desde pieza seleccionada");
  choices.add("Reordenar cadenas sin romper dependencias");
  new AlertDialog.Builder(this).setTitle("Historial paramétrico")
   .setItems(choices.toArray(new String[0]),(dialog,index)->{
    if(index==extrusionChains.size()){
     if(selected<0||selected>=parts.size()||parts.get(selected).circle){toast("Selecciona un prisma rectangular");return;}
     Part p=parts.get(selected);
     if(p.rotationX!=0||p.rotationY!=0||p.rotationZ!=0){toast("Por ahora solo prismas sin rotación");return;}
     checkpoint();
     extrusionChains.add(new es.josxtm.mycad.cad.FaceExtrusionChain(p.w,p.d,p.h,p.x,p.y,p.z));
     save();toast("Cadena creada · añade extrusiones desde Historial");
    }else if(index==extrusionChains.size()+1){reorderExtrusionChains();}
    else editExtrusionChain(index);
   }).setNegativeButton("Cerrar",null).show();
 }
 void reorderExtrusionChains(){
  if(extrusionChains.size()<2){toast("Necesitas al menos dos cadenas");return;}
  String[] names=new String[extrusionChains.size()];
  for(int i=0;i<names.length;i++)names[i]="Cadena "+(i+1)+" · "+extrusionChains.get(i).size()+" extrusiones";
  new AlertDialog.Builder(this).setTitle("Elegir cadena para mover")
   .setItems(names,(dialog,index)->{
    final es.josxtm.mycad.cad.FaceExtrusionChain current=extrusionChains.get(index);
    new AlertDialog.Builder(this).setTitle("Reordenar "+names[index])
     .setItems(new String[]{"Mover hacia arriba","Mover hacia abajo"},(d,option)->{
      int destination=index+(option==0?-1:1);
      if(destination<0||destination>=extrusionChains.size()){toast("Ya está en el extremo");return;}
      if(extrusionChains.get(index)!=current){toast("La lista de cadenas ha cambiado");return;}
      java.util.Collections.swap(extrusionChains,index,destination);
      es.josxtm.mycad.cad.ChainDependencyReport.Result report=
       es.josxtm.mycad.cad.ChainDependencyReport.inspect(extrusionChains,chainFeatures);
      if(!report.isValid()){
       java.util.Collections.swap(extrusionChains,index,destination);
       toast("No se puede reordenar: referencias inválidas");return;
      }
      // Checkpoint captures state before mutation. Swapping twice restores original order.
      java.util.Collections.swap(extrusionChains,index,destination);
      checkpoint();
      java.util.Collections.swap(extrusionChains,index,destination);
      save();refreshChainScene();
      toast("Cadena reordenada · vínculos preservados");
     }).show();
   }).setNegativeButton("Cerrar",null).show();
 }
 void editExtrusionChain(int index){
  if(index<0||index>=extrusionChains.size())return;
  es.josxtm.mycad.cad.FaceExtrusionChain chain=extrusionChains.get(index);
  java.util.ArrayList<String> actions=new java.util.ArrayList<>();
  actions.add("Añadir extrusión");
  for(es.josxtm.mycad.cad.FaceExtrusionChain.Step step:chain.history())
   actions.add(step.face.name()+" · "+step.distance+" mm · editar");
  actions.add("Eliminar última extrusión");
  actions.add("Ver unión de toda la cadena");
  actions.add("Cortar cadena con prisma seleccionado");
  actions.add("Intersección con prisma seleccionado");
  actions.add("Guardar corte paramétrico");
  actions.add("Guardar intersección paramétrica");
  new AlertDialog.Builder(this).setTitle("Cadena "+(index+1))
   .setItems(actions.toArray(new String[0]),(dialog,choice)->{
    if(choice==actions.size()-1){saveChainBooleanFeature(index,MultiBoxUnion.Operation.CHAIN_INTERSECTION);return;}
    if(choice==actions.size()-2){saveChainBooleanFeature(index,MultiBoxUnion.Operation.CHAIN_DIFFERENCE);return;}
    if(choice==actions.size()-3){previewChainBoolean(chain,MultiBoxUnion.Operation.INTERSECTION);return;}
    if(choice==actions.size()-4){previewChainBoolean(chain,MultiBoxUnion.Operation.DIFFERENCE);return;}
    if(choice==actions.size()-5){showChainUnion(chain);return;}
    if(choice==actions.size()-6){
     if(chain.size()==0){toast("No hay extrusiones");return;}
     checkpoint();es.josxtm.mycad.cad.FaceExtrusionChain changed=copyExtrusionChain(chain);changed.removeLast();extrusionChains.set(index,changed);save();refreshChainScene();
    }else{
     int stepIndex=choice-1;
     final String[] faces={"X_MIN","X_MAX","Y_MIN","Y_MAX","Z_MIN","Z_MAX"};
     if(stepIndex<0){
      new AlertDialog.Builder(this).setTitle("Cara de la nueva extrusión")
       .setItems(faces,(d,n)->chainDistanceDialog(chain,index,-1,es.josxtm.mycad.cad.BoxFaceSelection.Face.valueOf(faces[n]))).show();
     }else chainDistanceDialog(chain,index,stepIndex,chain.history().get(stepIndex).face);
    }
   }).setNegativeButton("Cerrar",null).show();
 }
 void saveChainBooleanFeature(int chainIndex,MultiBoxUnion.Operation op){
  if(selected<0||selected>=parts.size()||chainFeatures.size()>=100){toast("Selecciona un prisma cortador válido");return;}
  Part p=parts.get(selected);
  if(p.circle||p.rotationX!=0||p.rotationY!=0||p.rotationZ!=0){toast("El cortador no debe tener rotación");return;}
  try{
   es.josxtm.mycad.cad.ChainBooleanFeature feature=new es.josxtm.mycad.cad.ChainBooleanFeature(chainIndex,extrusionChains.get(chainIndex).id,op,p.x,p.y,p.z,p.w,p.d,p.h);
   if(feature.regenerate(extrusionChains).length==0){toast("Operación sin resultado");return;}
   checkpoint();chainFeatures.add(feature);save();toast("Operación guardada en el proyecto");
  }catch(Exception ex){toast("No se pudo guardar la operación");}
 }
 void rebuildSavedChainBooleans(){
  if(booleanRebuildBusy){toast("Ya se está reconstruyendo");return;}
  if(chainFeatures.isEmpty()){toast("No hay operaciones booleanas encadenadas guardadas");return;}
  final java.util.ArrayList<es.josxtm.mycad.cad.FaceExtrusionChain> sources=new java.util.ArrayList<>();
  final java.util.ArrayList<es.josxtm.mycad.cad.ChainBooleanFeature> features=new java.util.ArrayList<>(chainFeatures);
  try{
   for(es.josxtm.mycad.cad.FaceExtrusionChain chain:extrusionChains)
    sources.add(copyExtrusionChain(chain));
  }catch(Exception error){toast("No se pudo preparar una copia del historial");return;}
  final es.josxtm.mycad.cad.ChainBooleanRebuildCache.State previous=booleanRebuildSnapshot;
  booleanRebuildBusy=true;
  new Thread(()->{
   try{
    final es.josxtm.mycad.cad.ChainBooleanRebuildCache.Result result=
     es.josxtm.mycad.cad.ChainBooleanRebuildCache.rebuild(sources,features,previous);
    final float[] preview=result.state.combinedPreview();
    runOnUiThread(()->{
     booleanRebuildBusy=false;
     if(!result.state.matches(extrusionChains,chainFeatures)){
      toast("El proyecto cambió durante el cálculo: previsualización descartada");return;
     }
     booleanRebuildSnapshot=result.state;
     if(preview.length>0)showMeshOnly(new MeshEngine.Mesh(preview));
     String summary="Operaciones encadenadas: "+result.state.count()+
      "\\nRegeneradas: "+result.rebuiltFeatureIds.size()+
      "\\nReutilizadas: "+result.reused+
      "\\nTriángulos de previsualización: "+result.state.totalTriangles()+
      "\\n\\nResultado temporal: no modifica ni duplica cuerpos guardados.";
     new AlertDialog.Builder(this).setTitle("Reconstrucción paramétrica")
      .setMessage(summary).setPositiveButton("Cerrar",null).show();
    });
   }catch(Exception error){
    runOnUiThread(()->{
     booleanRebuildBusy=false;
     new AlertDialog.Builder(this).setTitle("Reconstrucción no completada")
      .setMessage("Una operación ha fallado o la malla excede los límites del visor. "+
       "El proyecto y el último resultado válido se han conservado.")
      .setPositiveButton("Cerrar",null).show();
    });
   }
  }).start();
 }
 void showSavedChainBooleans(){
  if(chainFeatures.isEmpty()){toast("No hay operaciones guardadas");return;}
  String[] names=new String[chainFeatures.size()];
  for(int i=0;i<names.length;i++){
   es.josxtm.mycad.cad.ChainBooleanFeature f=chainFeatures.get(i);
   names[i]=(f.operation==MultiBoxUnion.Operation.CHAIN_DIFFERENCE?"Corte":"Intersección")+" · cadena "+(f.resolvedChainIndex(extrusionChains)+1);
  }
  new AlertDialog.Builder(this).setTitle("Booleanas paramétricas")
   .setItems(names,(dialog,index)->{
    final es.josxtm.mycad.cad.ChainBooleanFeature original=chainFeatures.get(index);
    new AlertDialog.Builder(this).setTitle(names[index])
     .setItems(new String[]{"Mostrar resultado regenerado","Editar cortador XYZ y medidas","Cambiar corte / intersección","Eliminar operación"},(d,choice)->{
      if(index>=chainFeatures.size()||chainFeatures.get(index)!=original){
       toast("La operación ha cambiado");return;
      }
      if(choice==0){
       try{float[] result=original.regenerate(extrusionChains);
        if(result.length==0){toast("Resultado vacío");return;}
        showMeshOnly(new MeshEngine.Mesh(result));
       }catch(Exception e){toast("Error al regenerar operación");}
      }else if(choice==1){editSavedChainBooleanCutter(index,original);}
      else if(choice==2){
       MultiBoxUnion.Operation next=original.operation==MultiBoxUnion.Operation.CHAIN_DIFFERENCE?
        MultiBoxUnion.Operation.CHAIN_INTERSECTION:MultiBoxUnion.Operation.CHAIN_DIFFERENCE;
       try{
        es.josxtm.mycad.cad.ChainBooleanFeature updated=original.withOperation(next);
        if(updated.regenerate(extrusionChains).length==0){toast("El resultado sería vacío");return;}
        checkpoint();chainFeatures.set(index,updated);save();
        if(meshMode&&meshView!=null)renderCurrentCadMesh();
        toast("Operación actualizada y regenerable");
       }catch(Exception error){toast("No se pudo cambiar la operación");}
      }else{
       new AlertDialog.Builder(this).setTitle("Eliminar operación paramétrica")
        .setMessage("La operación se eliminará del historial. Podrás deshacer este cambio.")
        .setNegativeButton("Cancelar",null)
        .setPositiveButton("Eliminar",(confirm,w)->{
         if(index>=chainFeatures.size()||chainFeatures.get(index)!=original)return;
         checkpoint();chainFeatures.remove(index);save();
         if(meshMode&&meshView!=null)renderCurrentCadMesh();
         toast("Operación eliminada");
        }).show();
      }
     }).show();
   }).setNegativeButton("Cerrar",null).show();
 }
 void editSavedChainBooleanCutter(int index,es.josxtm.mycad.cad.ChainBooleanFeature previous){
  final String[] labels={"X (mm)","Y (mm)","Z (mm)","Ancho X (mm)","Fondo Y (mm)","Alto Z (mm)"};
  double[] original={previous.x,previous.y,previous.z,previous.w,previous.d,previous.h};
  LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);
  final EditText[] inputs=new EditText[labels.length];
  for(int i=0;i<labels.length;i++){
   TextView name=new TextView(this);name.setText(labels[i]);fields.addView(name);
   EditText input=new EditText(this);input.setInputType(8194|4096);
   input.setText(String.valueOf(original[i]));inputs[i]=input;fields.addView(input);
  }
  android.widget.ScrollView scroller=new android.widget.ScrollView(this);scroller.addView(fields);
  new AlertDialog.Builder(this).setTitle("Editar cortador · "+(index+1))
   .setView(scroller).setNegativeButton("Cancelar",null).setPositiveButton("Regenerar",(d,w)->{
    try{
     if(index>=chainFeatures.size()||chainFeatures.get(index)!=previous)throw new IllegalArgumentException();
     double[] values=new double[inputs.length];
     for(int i=0;i<values.length;i++)values[i]=Double.parseDouble(inputs[i].getText().toString().replace(',','.'));
     es.josxtm.mycad.cad.ChainBooleanFeature edited=previous.withCutter(
      values[0],values[1],values[2],values[3],values[4],values[5]);
     if(edited.regenerate(extrusionChains).length==0){toast("El resultado sería vacío");return;}
     checkpoint();chainFeatures.set(index,edited);save();
     if(meshMode&&meshView!=null)renderCurrentCadMesh();
     toast("Cortador actualizado · historial conservado");
    }catch(Exception e){toast("Coordenadas o medidas inválidas");}
   }).show();
 }
 void previewChainBoolean(es.josxtm.mycad.cad.FaceExtrusionChain chain,MultiBoxUnion.Operation operation){
  if(selected<0||selected>=parts.size()){toast("Selecciona antes el prisma que servirá como cortador");return;}
  Part cutter=parts.get(selected);
  if(cutter.circle||cutter.rotationX!=0||cutter.rotationY!=0||cutter.rotationZ!=0){toast("Cortador: prisma rectangular sin rotación");return;}
  try{
   java.util.ArrayList<BoxBoolean.Box> operands=new java.util.ArrayList<>();
   for(es.josxtm.mycad.cad.FaceExtrusionChain.Solid solid:chain.regenerate())
    operands.add(new BoxBoolean.Box(solid.x,solid.y,solid.z,solid.w,solid.d,solid.h));
   BoxBoolean.Box tool=new BoxBoolean.Box(cutter.x,cutter.y,cutter.z,cutter.w,cutter.d,cutter.h);
   operands.add(tool);
   float[] mesh=MultiBoxUnion.build(operands,operation==MultiBoxUnion.Operation.DIFFERENCE?
    MultiBoxUnion.Operation.CHAIN_DIFFERENCE:MultiBoxUnion.Operation.CHAIN_INTERSECTION);
   if(mesh.length==0){toast("El resultado es vacío");return;}
   showMeshOnly(new MeshEngine.Mesh(mesh));
   info.setText(operation==MultiBoxUnion.Operation.DIFFERENCE?"Vista de corte rectangular":"Vista de intersección rectangular");
  }catch(Exception error){toast("No se pudo calcular la operación booleana");}
 }
 void showChainUnion(es.josxtm.mycad.cad.FaceExtrusionChain chain){
  try{
   java.util.ArrayList<BoxBoolean.Box> boxes=new java.util.ArrayList<>();
   for(es.josxtm.mycad.cad.FaceExtrusionChain.Solid solid:chain.regenerate())
    boxes.add(new BoxBoolean.Box(solid.x,solid.y,solid.z,solid.w,solid.d,solid.h));
   float[] union=MultiBoxUnion.build(boxes);
   if(union.length==0)throw new IllegalArgumentException();
   showMeshOnly(new MeshEngine.Mesh(union));
   info.setText("Vista unión de cadena · "+(union.length/9)+" triángulos · sin caras internas");
  }catch(Exception ex){toast("No se pudo crear la vista de unión");}
 }
 void chainDistanceDialog(es.josxtm.mycad.cad.FaceExtrusionChain chain,int index,int step,es.josxtm.mycad.cad.BoxFaceSelection.Face face){
  EditText amount=new EditText(this);amount.setInputType(8194|4096);
  amount.setText(step<0?"10":String.valueOf(chain.history().get(step).distance));
  new AlertDialog.Builder(this).setTitle(face.name()+" · milímetros").setView(amount)
   .setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(dialog,w)->{
    try{
     if(index>=extrusionChains.size()||extrusionChains.get(index)!=chain)throw new IllegalArgumentException();
     double distance=Double.parseDouble(amount.getText().toString().replace(',','.'));
     es.josxtm.mycad.cad.FaceExtrusionChain next=copyExtrusionChain(chain);
     if(step<0)next.append(face,distance);else next.update(step,distance);
     if(next.regenerate().size()>51)throw new IllegalArgumentException();
     checkpoint();extrusionChains.set(index,next);save();refreshChainScene();
    }catch(Exception error){toast("Distancia inválida");}
   }).show();
 }
 es.josxtm.mycad.cad.FaceExtrusionChain copyExtrusionChain(es.josxtm.mycad.cad.FaceExtrusionChain source){
  es.josxtm.mycad.cad.FaceExtrusionChain.Solid root=source.regenerate().get(0);
  es.josxtm.mycad.cad.FaceExtrusionChain copy=new es.josxtm.mycad.cad.FaceExtrusionChain(source.id,root.w,root.d,root.h,root.x,root.y,root.z);
  for(es.josxtm.mycad.cad.FaceExtrusionChain.Step step:source.history())copy.appendStep(step);
  return copy;
 }
 void refreshChainScene(){
  if(!meshMode)toggleMeshView();
  else if(meshView!=null)renderCurrentCadMesh();
  info.setText("Historial regenerado · visor GPU");
 }
 void appendChainTriangles(java.util.ArrayList<Float> out){
  for(es.josxtm.mycad.cad.FaceExtrusionChain chain:extrusionChains){
   java.util.List<es.josxtm.mycad.cad.FaceExtrusionChain.Solid> solids=chain.regenerate();
   for(int i=1;i<solids.size();i++){
    es.josxtm.mycad.cad.FaceExtrusionChain.Solid solid=solids.get(i);
    es.josxtm.mycad.cad.CadKernel.Result<es.josxtm.mycad.cad.CadKernel.Box> box=
     new es.josxtm.mycad.cad.PrimitiveKernel().createBox(-solid.w/2,-solid.d/2,0,solid.w,solid.d,solid.h);
    if(!box.isSuccess())continue;
    for(float f:es.josxtm.mycad.cad.BoxSurface.triangles(new es.josxtm.mycad.cad.TransformedBody(
      box.value,new es.josxtm.mycad.cad.RigidTransform(solid.x,solid.y,solid.z,0,0,0))))out.add(f);
   }
  }
 }
 void extrudeSelectedCadFace(){
  if(!cadGpuMesh||activeCadFace==null||!(activeCadFace.isValidFor(parts.size())&&activeCadFace.isValidFor(parts.size(),parts.get(activeCadFace.partIndex).id))){
   toast("Selecciona una cara en el visor CAD experimental");return;
  }
  final int index=activeCadFace.partIndex;
  final Part parent=parts.get(index);
  if(parent.circle||parts.size()>=500){toast("Solo prismas · máximo 500 piezas");return;}
  final es.josxtm.mycad.cad.BoxFaceSelection.Face face=activeCadFace.face;
  EditText input=new EditText(this);input.setInputType(8194|4096);input.setText("10");
  new AlertDialog.Builder(this).setTitle("Extruir cara "+face.name()+" · mm")
   .setMessage("Extruir crea un prisma independiente. Fusionar crea una malla booleana y sustituye el prisma original; puedes deshacer.")
   .setView(input).setNegativeButton("Cancelar",null)
   .setNeutralButton("Fusionar malla",(dialog,which)->commitFaceExtrusion(index,parent,face,input,true))
   .setPositiveButton("Extruir",(dialog,which)->commitFaceExtrusion(index,parent,face,input,false))
   .show();
 }
 void commitFaceExtrusion(int index,Part parent,es.josxtm.mycad.cad.BoxFaceSelection.Face face,EditText input,boolean fuse){
  try{
   if(index<0||index>=parts.size()||parts.get(index)!=parent||parts.size()>=500)
    throw new IllegalArgumentException("Selection changed");
   if(fuse&&(parent.rotationX!=0||parent.rotationY!=0||parent.rotationZ!=0||!parent.sourceSketchId.isEmpty())){
    toast("La fusión requiere un prisma sin rotación ni boceto vinculado");return;
   }
   double amount=Double.parseDouble(input.getText().toString().replace(',','.'));
   es.josxtm.mycad.cad.FacePrismExtrusion.Result result=
    es.josxtm.mycad.cad.FacePrismExtrusion.create(parent.w,parent.d,parent.h,parent.x,parent.y,parent.z,
     parent.rotationX,parent.rotationY,parent.rotationZ,face,amount);
   if(fuse){
    BoxBoolean.Box a=new BoxBoolean.Box(parent.x,parent.y,parent.z,parent.w,parent.d,parent.h);
    BoxBoolean.Box b=new BoxBoolean.Box(result.x,result.y,result.z,result.w,result.d,result.h);
    float[] united=BoxBooleanMesh.build(a,b,BoxBooleanMesh.Operation.UNION);
    if(united.length==0)throw new IllegalArgumentException("Empty union");
    checkpoint();
    parts.remove(index);booleanMeshes.add(new MeshEngine.Mesh(united));holeRecipes.add(null);unionRecipes.add(new es.josxtm.mycad.cad.FaceUnionRecipe(parent.w,parent.d,parent.h,parent.x,parent.y,parent.z,face,amount));
    selected=parts.isEmpty()?-1:Math.min(index,parts.size()-1);
    info.setText("Extrusión fusionada como malla · "+amount+" mm");
   }else{
    Part child=new Part(false,(float)result.w,(float)result.d,(float)result.h,(float)result.x);
    child.y=(float)result.y;child.z=(float)result.z;
    child.rotationX=parent.rotationX;child.rotationY=parent.rotationY;child.rotationZ=parent.rotationZ;
    checkpoint();parts.add(child);selected=parts.size()-1;
    info.setText("Nuevo prisma adyacente · "+amount+" mm");
   }
   selectedSketch=-1;activeCadFace=null;save();scene.invalidate();
   if(meshMode&&meshView!=null)renderCurrentCadMesh();
  }catch(Exception error){toast("Extrusión inválida o fuera de límites");}
 }
 void editSelectedCylinderFace(){
  if(!cadGpuMesh||!meshMode||isolatedMeshPreview||selected<0||selected>=parts.size()){
   toast("Selecciona una cara de cilindro en el visor CAD experimental");return;
  }
  final int partIndex=selected;
  final Part part=parts.get(partIndex);
  if(!part.circle){toast("Selecciona primero un cilindro");return;}
  if(!part.sourceSketchId.isEmpty()){
   toast("Cilindro vinculado a boceto: edita primero su perfil o extrusión");return;
  }
  final String faceId=activeCylinderFaceId;
  if(faceId==null||!faceId.startsWith(part.id+"/face/")){
   toast("Toca la cara lateral o una tapa del cilindro");return;
  }
  final es.josxtm.mycad.cad.CylinderBRep.Kind face;
  try{
   face=es.josxtm.mycad.cad.CylinderBRep.Kind.valueOf(
    faceId.substring((part.id+"/face/").length()));
   es.josxtm.mycad.cad.CylinderBRep.from(part.id,part.w,part.h,
    new es.josxtm.mycad.cad.RigidTransform(part.x,part.y,part.z,
     part.rotationX,part.rotationY,part.rotationZ)).findFace(faceId);
  }catch(Exception problem){toast("No se reconoce esta cara cilíndrica");return;}
  EditText input=new EditText(this);
  input.setInputType(8194|4096);
  input.setText("2");
  String description=face==es.josxtm.mycad.cad.CylinderBRep.Kind.CYLINDER_SIDE?
   "Lateral: desplaza el radio. +2 mm aumenta el diámetro 4 mm.":
   face==es.josxtm.mycad.cad.CylinderBRep.Kind.CYLINDER_TOP?
   "Tapa superior: cambia la altura y conserva fija la base.":
   "Tapa inferior: cambia la altura y conserva fija la tapa superior.";
  new AlertDialog.Builder(this).setTitle("Editar cara cilíndrica · mm")
   .setMessage(description+"\nPositivo hacia fuera, negativo hacia dentro.")
   .setView(input).setNegativeButton("Cancelar",null)
   .setPositiveButton("Aplicar",(dialog,which)->{
    try{
     if(partIndex>=parts.size()||parts.get(partIndex)!=part||selected!=partIndex||
        !faceId.equals(activeCylinderFaceId)||!part.sourceSketchId.isEmpty())
      throw new IllegalArgumentException("Selection changed");
     double mm=Double.parseDouble(input.getText().toString().replace(',','.'));
     if(mm==0)throw new IllegalArgumentException("No movement");
     es.josxtm.mycad.cad.PrimitiveBRepBody next=
      analyticBody(part).offsetFace(faceId,mm);
     float diameter=(float)next.width,height=(float)next.height;
     float nx=(float)next.x,ny=(float)next.y,nz=(float)next.z;
     if(!Float.isFinite(diameter)||!Float.isFinite(height)||
        !Float.isFinite(nx)||!Float.isFinite(ny)||!Float.isFinite(nz))
      throw new IllegalArgumentException("Invalid result");
     checkpoint();
     part.w=diameter;part.d=diameter;part.h=height;
     part.x=nx;part.y=ny;part.z=nz;
     activeCylinderFaceId=null;activeCadFace=null;
     save();scene.invalidate();
     if(meshMode&&meshView!=null)renderCurrentCadMesh();
     toast("Cara cilíndrica modificada · deshacer disponible");
    }catch(Exception error){toast("Edición no válida: revisa cotas y selección");}
   }).show();
 }
 es.josxtm.mycad.cad.PrimitiveBRepBody analyticBody(Part part){
  es.josxtm.mycad.cad.RigidTransform pose=new es.josxtm.mycad.cad.RigidTransform(
   part.x,part.y,part.z,part.rotationX,part.rotationY,part.rotationZ);
  return part.circle?
   es.josxtm.mycad.cad.PrimitiveBRepBody.cylinder(part.id,part.w,part.h,pose):
   es.josxtm.mycad.cad.PrimitiveBRepBody.box(part.id,part.w,part.d,part.h,pose);
 }
 /**
  * Numeric, constrained manipulation of a logical B-Rep edge.
  * Both adjacent faces are moved atomically; a real touchscreen drag
  * manipulator requires a later GPU projection/gesture stage.
  */
 /**
  * Two-phase edit: while the slider(s) move, only the GL preview is replaced.
  * The actual Part, undo/redo history and JSON remain untouched until Apply.
  * Cancel (including Android Back) restores the complete saved scene.
  */
 /** Exit the one-finger on-model face movement and restore persisted scene. */
 void finishFaceDrag(){
  faceDragEnabled=false;
  if(meshView!=null){
   meshView.setFaceDragHandle(null,0,null);
   if(meshMode)meshView.previewMesh(currentMesh());
  }
 }
 /**
  * A selected analytic face is directly draggable using an on-model handle.
  * The GL preview never mutates Part/JSON/history; touch UP asks to confirm.
  * Tapping this menu item again cancels the active operation.
  */
 /**
  * Edge on-model manipulator: choose either incident surface normal.
  * Each gesture moves only that constrained component of offsetEdge().
  * The opposite coordinate stays fixed, and unrelated geometry is untouched.
  */
 /** Configure symmetric snapping for on-model B-Rep face and edge dragging. */
 void chooseDragPrecision(){
  if(faceDragEnabled){
   toast("Termina o cancela primero el arrastre activo");return;
  }
  final String[] labels={
   "Libre (sin imán)",
   "Preciso · 0,1 mm",
   "Normal · 0,5 mm",
   "Grueso · 1 mm"
  };
  final float[] step={0f,.1f,.5f,1f};
  float saved=getPreferences(MODE_PRIVATE).getFloat("dragSnapMm",0f);
  int selectedIndex=0;
  for(int i=0;i<step.length;i++)
   if(Math.abs(saved-step[i])<1e-6f)selectedIndex=i;
  new AlertDialog.Builder(this).setTitle("Precisión del arrastre 3D")
   .setSingleChoiceItems(labels,selectedIndex,(dialog,choice)->{
    float value=step[choice];
    getPreferences(MODE_PRIVATE).edit().putFloat("dragSnapMm",value).apply();
    if(meshView!=null)meshView.setFaceSnapStepMm(value);
    dialog.dismiss();
    toast("Arrastre B-Rep · "+labels[choice]);
   }).setNegativeButton("Cancelar",null).show();
 }
 void toggleEdgeDragOnModel(){
  if(faceDragEnabled){finishFaceDrag();toast("Arrastre 3D cancelado");return;}
  if(fileBusy||!cadGpuMesh||!meshMode||meshView==null||
     isolatedMeshPreview||selected<0||selected>=parts.size()){
   toast("Abre el visor CAD 3D y selecciona una arista");return;
  }
  final int partIndex=selected;
  final Part part=parts.get(partIndex);
  if(!part.sourceSketchId.isEmpty()){
   toast("Pieza vinculada: edita su boceto antes de mover la arista");return;
  }
  final es.josxtm.mycad.cad.PrimitiveBRepBody original;
  final es.josxtm.mycad.cad.PrimitiveBRepBody.Edge edge;
  final String edgeId=activeAnalyticEdgeId;
  try{
   original=analyticBody(part);edge=original.findEdge(edgeId);
   if(edge==null)throw new IllegalArgumentException("No edge selected");
  }catch(Exception problem){
   toast("Toca una arista en el visor o selecciónala en Inspeccionar B-Rep");return;
  }
  String[] choices=new String[2];
  for(int i=0;i<2;i++){
   String face=edge.adjacentFaceIds().get(i);
   choices[i]="Desplazar "+face.substring(face.lastIndexOf('/')+1);
  }
  new AlertDialog.Builder(this).setTitle("Dirección de la arista")
   .setItems(choices,(menu,axis)->{
    try{
     if(fileBusy||selected!=partIndex||parts.get(partIndex)!=part||
        !part.id.equals(original.bodyId)||
        !edgeId.equals(activeAnalyticEdgeId)||
        !es.josxtm.mycad.cad.PrimitiveMovePreview.forEdge(original,edgeId)
         .matches(analyticBody(part)))
      throw new IllegalArgumentException("Edited since selected");
     final es.josxtm.mycad.cad.PrimitiveMovePreview session=
      es.josxtm.mycad.cad.PrimitiveMovePreview.forEdge(original,edgeId);
     final es.josxtm.mycad.cad.PrimitiveEdgeHandle handle=
      es.josxtm.mycad.cad.PrimitiveEdgeHandle.from(original,edgeId,axis);
     final double maxMotion=Math.max(2,Math.min(100,
       Math.min(original.width,Math.min(original.depth,original.height))*.8));
     faceDragEnabled=true;
     meshView.setFaceSnapStepMm(
      getPreferences(MODE_PRIVATE).getFloat("dragSnapMm",0f));
     meshView.setFaceDragHandle(handle.gpu(),maxMotion,
       (delta,finished,cancelled)->{
        if(!faceDragEnabled)return;
        if(cancelled){finishFaceDrag();return;}
        try{
         if(fileBusy||selected!=partIndex||partIndex>=parts.size()||
            parts.get(partIndex)!=part||!part.sourceSketchId.isEmpty()||
            !edgeId.equals(activeAnalyticEdgeId)||
            !session.matches(analyticBody(part)))
          throw new IllegalArgumentException("Stale selected edge");
         if(delta==0){
          meshView.previewMesh(experimentalCadMeshPreview(partIndex,original));
          info.setText("Arista B-Rep · posición original");
          if(finished)finishFaceDrag();
          return;
         }
         final double first=axis==0?delta:0;
         final double second=axis==1?delta:0;
         final es.josxtm.mycad.cad.PrimitiveBRepBody proposed=
          session.propose(first,second);
         meshView.previewMesh(experimentalCadMeshPreview(partIndex,proposed));
         info.setText(String.format(java.util.Locale.US,
           "Arista B-Rep · dirección %d · Δ %.2f mm · V %.2f mm³",
           axis+1,delta,proposed.volume()));
         if(!finished)return;
         new AlertDialog.Builder(this).setTitle("Confirmar desplazamiento de arista")
          .setMessage(String.format(java.util.Locale.US,
            "Desplazar %s %.2f mm. Confirmar una sola edición.",
            choices[axis],delta))
          .setCancelable(false)
          .setNegativeButton("Cancelar",(dialog,w)->finishFaceDrag())
          .setPositiveButton("Aplicar",(dialog,w)->{
           try{
            if(!faceDragEnabled||fileBusy||partIndex>=parts.size()||
               parts.get(partIndex)!=part||selected!=partIndex||
               !part.sourceSketchId.isEmpty()||
               !edgeId.equals(activeAnalyticEdgeId)||
               !session.matches(analyticBody(part)))
             throw new IllegalArgumentException("State changed");
            es.josxtm.mycad.cad.PrimitiveBRepBody result=session.propose(first,second);
            float width=(float)result.width,depth=(float)result.depth,
              height=(float)result.height,xx=(float)result.x,
              yy=(float)result.y,zz=(float)result.z;
            if(!Float.isFinite(width)||!Float.isFinite(depth)||
               !Float.isFinite(height)||!Float.isFinite(xx)||
               !Float.isFinite(yy)||!Float.isFinite(zz)||
               width<=0||depth<=0||height<=0)
             throw new IllegalArgumentException("Unrenderable solid");
            checkpoint();
            part.w=width;part.d=depth;part.h=height;
            part.x=xx;part.y=yy;part.z=zz;
            activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;
            save();scene.invalidate();
            toast("Arista movida · deshacer disponible");
           }catch(Exception problem){toast("Movimiento cancelado: medidas o selección inválidas");}
           finally{finishFaceDrag();}
          }).show();
        }catch(Exception invalid){
         if(finished)toast("No se puede mover esa arista con esa distancia");
         else info.setText("Arista B-Rep · desplazamiento fuera de límites");
         if(finished)finishFaceDrag();
         else meshView.previewMesh(experimentalCadMeshPreview(partIndex,original));
        }
       });
     info.setText("Arrastre de arista activo · mueve el control amarillo · menú para cancelar");
    }catch(Exception invalid){
     toast("La pieza o la arista cambiaron; selecciona de nuevo");
    }
   }).setNegativeButton("Cancelar",null).show();
 }
 void toggleFaceDragOnModel(){
  if(faceDragEnabled){
   finishFaceDrag();toast("Arrastre cancelado sin modificar la pieza");return;
  }
  if(fileBusy||!meshMode||!cadGpuMesh||meshView==null||
     isolatedMeshPreview||selected<0||selected>=parts.size()){
   toast("Activa el visor CAD experimental y selecciona una cara");return;
  }
  final int partIndex=selected;
  final Part part=parts.get(partIndex);
  if(!part.sourceSketchId.isEmpty()){
   toast("Edita el boceto para modificar esta pieza vinculada");return;
  }
  final es.josxtm.mycad.cad.PrimitiveBRepBody original;
  final es.josxtm.mycad.cad.PrimitiveMovePreview session;
  final es.josxtm.mycad.cad.PrimitiveFaceHandle handle;
  try{
   original=analyticBody(part);
   String id=part.circle?activeCylinderFaceId:
    activeCadFace==null?null:activeCadFace.key();
   session=es.josxtm.mycad.cad.PrimitiveMovePreview.forFace(original,id);
   handle=es.josxtm.mycad.cad.PrimitiveFaceHandle.from(original,id);
  }catch(Exception invalid){
   toast("Toca una cara en el visor 3D antes de activar el arrastre");return;
  }
  double maxMotion=Math.max(2,Math.min(100,
    Math.min(original.width,Math.min(original.depth,original.height))*.8));
  faceDragEnabled=true;
  meshView.setFaceSnapStepMm(
   getPreferences(MODE_PRIVATE).getFloat("dragSnapMm",0f));
  meshView.setFaceDragHandle(handle.gpu(),maxMotion,
   (deltaMm,finished,cancelled)->{
    if(!faceDragEnabled)return;
    if(cancelled){finishFaceDrag();return;}
    try{
     if(fileBusy||selected!=partIndex||partIndex>=parts.size()||
        parts.get(partIndex)!=part||!part.sourceSketchId.isEmpty()||
        !session.matches(analyticBody(part))||
        !(session.entityId.equals(part.circle?activeCylinderFaceId:
          activeCadFace==null?null:activeCadFace.key())))
      throw new IllegalArgumentException("Stale scene or selection");
     if(deltaMm==0){
      meshView.previewMesh(experimentalCadMeshPreview(partIndex,original));
      info.setText("Arrastre B-Rep · posición de origen");
      if(finished)finishFaceDrag();
      return;
     }
     final es.josxtm.mycad.cad.PrimitiveBRepBody proposal=
      session.propose(deltaMm,0);
     meshView.previewMesh(experimentalCadMeshPreview(partIndex,proposal));
     info.setText(String.format(java.util.Locale.US,
       "Cara B-Rep · Δ %.2f mm · volumen provisional %.2f mm³",
       deltaMm,proposal.volume()));
     if(!finished)return;
     new AlertDialog.Builder(this).setTitle("Confirmar movimiento 3D")
      .setMessage(String.format(java.util.Locale.US,
       "Desplazar cara %.2f mm. La pieza se guardará como una sola edición.",
       deltaMm))
      .setCancelable(false).setNegativeButton("Cancelar",(d,w)->finishFaceDrag())
      .setPositiveButton("Aplicar",(d,w)->{
       try{
        if(!faceDragEnabled||fileBusy||partIndex>=parts.size()||
           parts.get(partIndex)!=part||selected!=partIndex||
           !session.matches(analyticBody(part))||
           !session.entityId.equals(part.circle?activeCylinderFaceId:
             activeCadFace==null?null:activeCadFace.key()))
         throw new IllegalArgumentException("Changed before commit");
        es.josxtm.mycad.cad.PrimitiveBRepBody accepted=session.propose(deltaMm,0);
        float ww=(float)accepted.width,dd=(float)accepted.depth,
         hh=(float)accepted.height,xx=(float)accepted.x,
         yy=(float)accepted.y,zz=(float)accepted.z;
        if(!Float.isFinite(ww)||!Float.isFinite(dd)||!Float.isFinite(hh)||
           !Float.isFinite(xx)||!Float.isFinite(yy)||!Float.isFinite(zz)||
           ww<=0||dd<=0||hh<=0)throw new IllegalArgumentException();
        checkpoint();
        part.w=ww;part.d=dd;part.h=hh;
        part.x=xx;part.y=yy;part.z=zz;
        activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;
        save();scene.invalidate();
        toast("Cara desplazada · deshacer disponible");
       }catch(Exception error){
        toast("Movimiento no aplicado: selección o cotas inválidas");
       }finally{finishFaceDrag();}
      }).show();
    }catch(Exception invalid){
     if(finished)toast("Movimiento fuera de límites o selección modificada");
     else info.setText("Arrastre B-Rep · geometría no válida");
     if(finished)finishFaceDrag();
     else meshView.previewMesh(experimentalCadMeshPreview(partIndex,original));
    }
   });
  info.setText("Arrastre activo · mueve el punto amarillo sobre la cara · vuelve al menú para cancelar");
 }
 void previewSelectedBRepMove(){
  if(fileBusy||!cadGpuMesh||!meshMode||meshView==null||
     isolatedMeshPreview||selected<0||selected>=parts.size()){
   toast("Activa el visor CAD 3D y selecciona una cara o arista");return;
  }
  final int partIndex=selected;
  final Part part=parts.get(partIndex);
  if(!part.sourceSketchId.isEmpty()){
   toast("Pieza vinculada: modifica primero el boceto de origen");return;
  }
  final es.josxtm.mycad.cad.PrimitiveBRepBody initial;
  final es.josxtm.mycad.cad.PrimitiveMovePreview session;
  try{
   initial=analyticBody(part);
   String edgeId=activeAnalyticEdgeId;
   String faceId=part.circle?activeCylinderFaceId:
    activeCadFace==null?null:activeCadFace.key();
   if(edgeId!=null&&initial.findEdge(edgeId)!=null)
    session=es.josxtm.mycad.cad.PrimitiveMovePreview.forEdge(initial,edgeId);
   else
    session=es.josxtm.mycad.cad.PrimitiveMovePreview.forFace(initial,faceId);
  }catch(Exception invalid){
   toast("Toca una cara o arista del sólido antes de previsualizar");return;
  }
  final boolean edge=session.target==es.josxtm.mycad.cad.PrimitiveMovePreview.Target.EDGE;
  final String id=session.entityId;
  final int axes=edge?2:1;
  final double scale=Math.min(100,Math.max(5,
    Math.min(initial.width,Math.min(initial.depth,initial.height))*.6));
  final LinearLayout panel=new LinearLayout(this);
  panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(dp(14),dp(8),dp(14),dp(8));
  final TextView status=new TextView(this);
  status.setText("Mueve los controles para mostrar la pieza provisional.");
  panel.addView(status);
  final SeekBar[] sliders=new SeekBar[axes];
  for(int i=0;i<axes;i++){
   TextView title=new TextView(this);
   String faceLabel=edge?initial.findEdge(id).adjacentFaceIds().get(i):
    id;
   title.setText("Cara "+faceLabel.substring(faceLabel.lastIndexOf('/')+1));
   panel.addView(title);
   SeekBar seek=new SeekBar(this);
   seek.setMax(200);
   seek.setProgress(100);
   sliders[i]=seek;
   panel.addView(seek);
  }
  final es.josxtm.mycad.cad.PrimitiveBRepBody[] pending={null};
  final double[] offsets={0,0};
  final AlertDialog dialog=new AlertDialog.Builder(this)
   .setTitle(edge?"Previsualizar movimiento de arista":"Previsualizar movimiento de cara")
   .setView(panel)
   .setNegativeButton("Cancelar",null)
   .setPositiveButton("Aplicar",(d,which)->{
    try{
     if(pending[0]==null||partIndex>=parts.size()||
        parts.get(partIndex)!=part||selected!=partIndex||fileBusy||
        !part.sourceSketchId.isEmpty()||
        !session.matches(analyticBody(part))||
        !(edge?id.equals(activeAnalyticEdgeId):
          id.equals(part.circle?activeCylinderFaceId:
                   activeCadFace==null?null:activeCadFace.key())))
      throw new IllegalArgumentException("La pieza cambió desde la previsualización");
     es.josxtm.mycad.cad.PrimitiveBRepBody confirmed=
      session.propose(offsets[0],offsets[1]);
     float w=(float)confirmed.width,depth=(float)confirmed.depth,
      h=(float)confirmed.height,xx=(float)confirmed.x,
      yy=(float)confirmed.y,zz=(float)confirmed.z;
     if(!Float.isFinite(w)||!Float.isFinite(depth)||!Float.isFinite(h)||
        !Float.isFinite(xx)||!Float.isFinite(yy)||!Float.isFinite(zz)||
        w<=0||depth<=0||h<=0)
      throw new IllegalArgumentException("Resultado fuera de rango");
     checkpoint();
     part.w=w;part.d=depth;part.h=h;part.x=xx;part.y=yy;part.z=zz;
     activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;
     save();scene.invalidate();
     toast("Geometría confirmada · deshacer disponible");
    }catch(Exception invalid){
     toast("No se pudo aplicar: geometría o selección modificadas");
    }
   }).create();
  dialog.setOnDismissListener(d->{
   // Works after either Cancel or Apply: reflects the actual committed design.
   if(meshMode&&meshView!=null){
    try{meshView.previewMesh(currentMesh());}
    catch(Exception ignored){renderCurrentCadMesh();}
   }
  });
  dialog.show();
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
  final Runnable refresh=()->{
   double first=(sliders[0].getProgress()-100)*scale/100.0;
   double second=edge?(sliders[1].getProgress()-100)*scale/100.0:0;
   offsets[0]=first;offsets[1]=second;
   try{
    if(selected!=partIndex||parts.get(partIndex)!=part||
       !session.matches(analyticBody(part))||fileBusy)
     throw new IllegalArgumentException("Objeto cambiado");
    if(first==0&&second==0){
     pending[0]=null;
     meshView.previewMesh(experimentalCadMeshPreview(partIndex,initial));
     status.setText("Sin cambios · mueve un control para previsualizar");
    }else{
     es.josxtm.mycad.cad.PrimitiveBRepBody candidate=session.propose(first,second);
     MeshEngine.Mesh preview=experimentalCadMeshPreview(partIndex,candidate);
     pending[0]=candidate;
     meshView.previewMesh(preview);
     status.setText(String.format(java.util.Locale.US,
      "Δ %.2f / %.2f mm · volumen provisional %.2f mm³",
      first,second,candidate.volume()));
    }
   }catch(Exception invalid){
    pending[0]=null;
    status.setText("Previsualización no válida: mantén dimensiones positivas");
    try{meshView.previewMesh(experimentalCadMeshPreview(partIndex,initial));}
    catch(Exception ignored){}
   }
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(pending[0]!=null);
  };
  for(SeekBar slider:sliders)
   slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
    @Override public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
     refresh.run();
    }
    @Override public void onStartTrackingTouch(SeekBar bar){}
    @Override public void onStopTrackingTouch(SeekBar bar){}
   });
  refresh.run();
 }
 void editSelectedAnalyticEdge(){
  if(fileBusy||selected<0||selected>=parts.size()||isolatedMeshPreview){
   toast("Selecciona primero una arista de un sólido");return;
  }
  final int partIndex=selected;
  final Part part=parts.get(partIndex);
  if(!part.sourceSketchId.isEmpty()){
   toast("La pieza depende de un boceto: edita antes su perfil");return;
  }
  final String edgeId=activeAnalyticEdgeId;
  final es.josxtm.mycad.cad.PrimitiveBRepBody solid;
  final es.josxtm.mycad.cad.PrimitiveBRepBody.Edge edge;
  try{
   solid=analyticBody(part);
   edge=solid.findEdge(edgeId);
   if(edge==null)throw new IllegalArgumentException("Stale edge");
  }catch(Exception error){toast("Toca una arista en el visor CAD o elígela en la inspección B-Rep");return;}
  final float[] previous={part.w,part.d,part.h,part.x,part.y,part.z,
                          part.rotationX,part.rotationY,part.rotationZ};
  LinearLayout panel=new LinearLayout(this);
  panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(dp(16),dp(8),dp(16),dp(8));
  final EditText[] inputs=new EditText[2];
  for(int i=0;i<2;i++){
   String faceId=edge.adjacentFaceIds().get(i);
   TextView title=new TextView(this);
   title.setText("Desplazamiento de "+faceId.substring(faceId.lastIndexOf('/')+1)+" (mm)");
   panel.addView(title);
   inputs[i]=new EditText(this);
   inputs[i].setInputType(8194|4096);
   inputs[i].setText("0");
   panel.addView(inputs[i]);
  }
  new AlertDialog.Builder(this).setTitle("Editar arista · "+edge.kind)
   .setMessage("Desplaza sus dos caras adyacentes en milímetros.\n"+
     "Positivo: hacia fuera. Negativo: hacia dentro. "+
     "Deja 0 para mantener una cara fija.\n"+
     (part.circle?"En cilindros: lateral = radio; tapa = altura.":
                  "En prismas: dos direcciones ortogonales locales."))
   .setView(panel).setNegativeButton("Cancelar",null)
   .setPositiveButton("Aplicar",(dialog,which)->{
    try{
     if(partIndex>=parts.size()||parts.get(partIndex)!=part||
        selected!=partIndex||!edgeId.equals(activeAnalyticEdgeId)||
        !part.sourceSketchId.isEmpty())
      throw new IllegalArgumentException("Selección obsoleta");
     float[] now={part.w,part.d,part.h,part.x,part.y,part.z,
                  part.rotationX,part.rotationY,part.rotationZ};
     for(int i=0;i<now.length;i++)
      if(Float.compare(now[i],previous[i])!=0)
       throw new IllegalArgumentException("El cuerpo cambió durante la edición");
     double first=Double.parseDouble(inputs[0].getText().toString().trim().replace(',','.'));
     double second=Double.parseDouble(inputs[1].getText().toString().trim().replace(',','.'));
     es.josxtm.mycad.cad.PrimitiveBRepBody next=
      analyticBody(part).offsetEdge(edgeId,first,second);
     float w=(float)next.width,d=(float)next.depth,h=(float)next.height;
     float x=(float)next.x,y=(float)next.y,z=(float)next.z;
     if(!Float.isFinite(w)||!Float.isFinite(d)||!Float.isFinite(h)||
        !Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z)||
        w<=0||d<=0||h<=0)throw new IllegalArgumentException("Cotas no válidas");
     checkpoint();
     part.w=w;part.d=d;part.h=h;part.x=x;part.y=y;part.z=z;
     activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;
     save();scene.invalidate();
     if(meshMode&&meshView!=null)renderCurrentCadMesh();
     toast("Arista modificada · deshacer y guardar disponibles");
    }catch(Exception problem){
     toast("Edición de arista rechazada: comprueba cotas y selección");
    }
   }).show();
 }
 void inspectSelectedAnalyticSolid(){
  if(selected<0||selected>=parts.size()){
   toast("Selecciona un prisma o cilindro antes de inspeccionarlo");return;
  }
  final Part part=parts.get(selected);
  try{
   final es.josxtm.mycad.cad.PrimitiveBRepBody solid=analyticBody(part);
   final java.util.List<es.josxtm.mycad.cad.PrimitiveBRepBody.Face> surfaces=solid.faces();
   final java.util.List<es.josxtm.mycad.cad.PrimitiveBRepBody.Edge> boundaries=solid.edges();
   String[] names=new String[surfaces.size()+boundaries.size()];
   for(int i=0;i<surfaces.size();i++){
    es.josxtm.mycad.cad.PrimitiveBRepBody.Face f=surfaces.get(i);
    names[i]="Cara · "+f.kind+" · "+String.format(java.util.Locale.US,"%.2f mm²",f.area);
   }
   for(int i=0;i<boundaries.size();i++){
    es.josxtm.mycad.cad.PrimitiveBRepBody.Edge e=boundaries.get(i);
    names[surfaces.size()+i]=(e.id.equals(activeAnalyticEdgeId)?"▶ ":"")+"Arista "+e.kind+" · "+
     String.format(java.util.Locale.US,"%.2f mm",e.length);
   }
   String title=String.format(java.util.Locale.US,
    "B-Rep %s · V %.1f mm³ · S %.1f mm²",solid.kind,
    solid.volume(),solid.surfaceArea());
   // On Android, Builder.setMessage() hides the ListView configured by setItems().
   new AlertDialog.Builder(this).setTitle(title)
    .setItems(names,(dialog,index)->{
     if(index<surfaces.size()){
      es.josxtm.mycad.cad.PrimitiveBRepBody.Face f=surfaces.get(index);
      double[] n=f.outwardNormalAt(0);
      String details=String.format(java.util.Locale.US,
       "ID: %s\nÁrea: %.3f mm²\nNormal mundial (ángulo local 0): %.3f / %.3f / %.3f\nAristas:\n%s",
       f.id,f.area,n[0],n[1],n[2],f.edgeIds().toString());
      new AlertDialog.Builder(this).setTitle("Cara "+f.kind).setMessage(details)
       .setPositiveButton("Cerrar",null).show();
     }else{
      es.josxtm.mycad.cad.PrimitiveBRepBody.Edge e=boundaries.get(index-surfaces.size());
      String details=String.format(java.util.Locale.US,
       "ID: %s\nTipo: %s\nLongitud: %.3f mm\nCaras adyacentes:\n%s",
       e.id,e.kind,e.length,e.adjacentFaceIds().toString());
      // The inspector is also a precise selection path when touch tolerance is insufficient.
      activeAnalyticEdgeId=e.id;
      new AlertDialog.Builder(this).setTitle("Arista analítica").setMessage(details)
       .setNegativeButton("Cerrar",null)
       .setPositiveButton("Editar desplazamiento",(confirmation,which)->{
        if(selected>=0&&selected<parts.size()&&parts.get(selected)==part)
         editSelectedAnalyticEdge();
        else toast("Vuelve a seleccionar la pieza: ha cambiado");
       }).show();
     }
    }).setNegativeButton("Cerrar",null).show();
  }catch(Exception problem){toast("No se pudo inspeccionar la topología del sólido");}
 }
 void editSelectedCadFace(){
  if(activeCadFace==null||!cadGpuMesh||!(activeCadFace.isValidFor(parts.size())&&activeCadFace.isValidFor(parts.size(),parts.get(activeCadFace.partIndex).id))){
   toast("Toca primero una cara en el visor CAD experimental");return;
  }
  Part part=parts.get(activeCadFace.partIndex);
  if(part.circle){toast("Solo prismas por ahora");return;}
  if(activeCadFace.face==es.josxtm.mycad.cad.BoxFaceSelection.Face.Z_MAX){
   editExtrusionHeight(part);return;
  }
  if(!part.sourceSketchId.isEmpty()){
   Sketch source=null;
   for(Sketch q:sketches)if(q.id.equals(part.sourceSketchId)){source=q;break;}
   if(source==null||source.type!=2||source.plane<0||source.plane>2){
    toast("Edición vinculada solo para rectángulos XY/XZ/YZ");return;
   }
   final Sketch linked=source;
   final es.josxtm.mycad.cad.BoxFaceSelection.Face linkedFace=activeCadFace.face;
   if(linkedFace==es.josxtm.mycad.cad.BoxFaceSelection.Face.Z_MIN){
    toast("La base de la extrusión vinculada permanece fija");return;
   }
   final int partIndex=activeCadFace.partIndex;
   final Part original=part;
   EditText input=new EditText(this);input.setInputType(8194|4096);input.setText("5");
   new AlertDialog.Builder(this).setTitle("Editar boceto desde cara · mm")
    .setMessage("La cara opuesta permanece fija; se actualizan las extrusiones vinculadas.")
    .setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(dialog,which)->{
     try{
      if(partIndex>=parts.size()||parts.get(partIndex)!=original)throw new IllegalArgumentException();
      double delta=Double.parseDouble(input.getText().toString().replace(',','.'));
      double[] endpoints=es.josxtm.mycad.cad.LinkedFaceResize.resize(linked.plane,linked.x1,linked.y1,linked.x2,linked.y2,linkedFace,delta);
      float newX1=(float)endpoints[0],newY1=(float)endpoints[1],newX2=(float)endpoints[2],newY2=(float)endpoints[3];
      if(!validateLinkedSketchEdit(linked,newX1,newY1,newX2,newY2))
       throw new IllegalArgumentException();
      checkpoint();linked.x1=newX1;linked.y1=newY1;linked.x2=newX2;linked.y2=newY2;
      rebuildLinkedParts(linked);
      activeCadFace=null;save();scene.invalidate();
      if(meshMode&&meshView!=null)renderCurrentCadMesh();
     }catch(Exception error){toast("Cambio no válido para este boceto");}
    }).show();
   return;
  }
  final es.josxtm.mycad.cad.BoxFaceSelection.Face face=activeCadFace.face;
  final int partIndex=activeCadFace.partIndex;
  EditText input=new EditText(this);input.setInputType(8194|4096);input.setText("5");
  new AlertDialog.Builder(this).setTitle("Desplazar cara "+face.name()+" · mm")
   .setMessage("Positivo: hacia fuera. Negativo: hacia dentro.").setView(input)
   .setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(dialog,which)->{
    try{
     if(partIndex<0||partIndex>=parts.size()||parts.get(partIndex)!=part)throw new IllegalArgumentException();
     double delta=Double.parseDouble(input.getText().toString().replace(',','.'));
     es.josxtm.mycad.cad.PrimitiveBRepBody updated=analyticBody(part)
      .offsetFace(part.id+"/face/"+face.name(),delta);
     checkpoint();
     part.w=(float)updated.width;part.d=(float)updated.depth;part.h=(float)updated.height;
     part.x=(float)updated.x;part.y=(float)updated.y;part.z=(float)updated.z;
     activeCadFace=null;save();scene.invalidate();
     if(meshMode&&meshView!=null)renderCurrentCadMesh();
    }catch(Exception error){toast("Distancia inválida o fuera de los límites");}
   }).show();
 }
 void toggleCadGpuMesh(){
  if(faceDragEnabled)finishFaceDrag();
  activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;cadGpuMesh=!cadGpuMesh;
  if(meshMode&&meshView!=null)renderCurrentCadMesh();
  toast(cadGpuMesh?"Visor CAD experimental · prismas y cilindros XYZ":"Visor clásico restaurado");
 }
 MeshEngine.Mesh experimentalCadMesh(){
  return experimentalCadMeshPreview(-1,null);
 }
 /** Rebuild the full scene in memory. Never mutate Part, undo or JSON. */
 MeshEngine.Mesh experimentalCadMeshPreview(int previewIndex,
    es.josxtm.mycad.cad.PrimitiveBRepBody candidate){
  if(previewIndex>=parts.size()||previewIndex< -1||
     (previewIndex>=0&&candidate==null))
   throw new IllegalArgumentException("Invalid preview slot");
  java.util.ArrayList<Float> floats=new java.util.ArrayList<>();
  if(importedMesh!=null)for(float v:importedMesh.vertices)floats.add(v);
  for(MeshEngine.Mesh m:booleanMeshes)for(float v:m.vertices)floats.add(v);
  for(int i=0;i<parts.size();i++){
   Part part=parts.get(i);
   if(i==previewIndex){
    if(!part.id.equals(candidate.bodyId))
     throw new IllegalArgumentException("Stale preview body");
    for(float value:candidate.triangles(48))floats.add(value);
   }else{
    for(float value:analyticBody(part).triangles(48))floats.add(value);
   }
  }
  appendChainTriangles(floats);
  float[] coordinates=new float[floats.size()];
  for(int i=0;i<coordinates.length;i++)coordinates[i]=floats.get(i);
  return new MeshEngine.Mesh(coordinates);
 }
 MeshEngine.Mesh currentMesh(){
  if(cadGpuMesh)return experimentalCadMesh();
  java.util.ArrayList<Float> a=new java.util.ArrayList<>();
  if(importedMesh!=null)for(float v:importedMesh.vertices)a.add(v);
  for(MeshEngine.Mesh mesh:booleanMeshes)for(float value:mesh.vertices)a.add(value);
  for(Part p:parts){
   float z=p.z,top=z+p.h;
   if(p.circle){int n=48;float radius=p.w/2;for(int i=0;i<n;i++){double t=i*2*Math.PI/n,u=(i+1)*2*Math.PI/n;float[] b={p.x+(float)Math.cos(t)*radius,p.y+(float)Math.sin(t)*radius,z},c={p.x+(float)Math.cos(u)*radius,p.y+(float)Math.sin(u)*radius,z},d={b[0],b[1],top},e={c[0],c[1],top};quad(a,b,c,e,d);addTri(a,new float[]{p.x,p.y,z},c,b);addTri(a,new float[]{p.x,p.y,top},d,e);}}
   else{float x=p.x,y=p.y,w=p.w/2,h=p.d/2;float[][] v={{x-w,y-h,z},{x+w,y-h,z},{x+w,y+h,z},{x-w,y+h,z},{x-w,y-h,top},{x+w,y-h,top},{x+w,y+h,top},{x-w,y+h,top}};for(float[] pt:v){float dx=pt[0]-p.x,dy=pt[1]-p.y,angle=(float)Math.toRadians(p.rotationZ);pt[0]=p.x+(float)(dx*Math.cos(angle)-dy*Math.sin(angle));pt[1]=p.y+(float)(dx*Math.sin(angle)+dy*Math.cos(angle));}quad(a,v[3],v[2],v[1],v[0]);quad(a,v[4],v[5],v[6],v[7]);quad(a,v[0],v[1],v[5],v[4]);quad(a,v[1],v[2],v[6],v[5]);quad(a,v[2],v[3],v[7],v[6]);quad(a,v[3],v[0],v[4],v[7]);}
  }
  appendChainTriangles(a);
  float[] vertices=new float[a.size()];for(int i=0;i<vertices.length;i++)vertices[i]=a.get(i);return new MeshEngine.Mesh(vertices);
 }
 void toggleMeshView(){
  if(meshView==null){meshView=new MeshEngine(this);meshView.setSurfaceTapListener((triangle,x,y,z)->showGpuSurfaceSelection(triangle,x,y,z));rootLayout.addView(meshView,rootLayout.indexOfChild(nextStep),new LinearLayout.LayoutParams(-1,dp(240)));}
  meshMode=!meshMode;meshView.setVisibility(meshMode?View.VISIBLE:View.GONE);
  if(meshMode){renderCurrentCadMesh();toast("Visor GPU: arrastra para girar");}
 }
 void importMesh(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");try{startActivityForResult(i,IMPORT_MESH);}catch(Exception e){toast("No se pudo abrir el selector");}}
 void exportMeshMenu(){pendingMeshExport=null;if(importedMesh==null&&parts.isEmpty()&&booleanMeshes.isEmpty()){toast("Primero crea una pieza o importa una malla");return;}new AlertDialog.Builder(this).setTitle("Exportar malla importada").setItems(new String[]{"STL binario","OBJ"},(d,n)->{meshExportFormat=n==0?"stl":"obj";Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(n==0?"model/stl":"text/plain");i.putExtra(Intent.EXTRA_TITLE,"MyCAD_export."+meshExportFormat);try{startActivityForResult(i,EXPORT_MESH);}catch(Exception e){toast("No se pudo abrir el selector");}}).show();}
 void handleMeshResult(int request,int result,Intent data){
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  Uri uri=data.getData();if(fileBusy)return;fileBusy=true;
  if(request==IMPORT_MESH){new Thread(()->{try{
   String filename="modelo.stl";android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null);
   if(c!=null){try{if(c.moveToFirst())filename=c.getString(0);}finally{c.close();}}
   MeshEngine.Mesh m;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Archivo inaccesible");m=MeshIO.read(filename,in);}
   runOnUiThread(()->{importedMesh=m;fileBusy=false;if(!meshMode)toggleMeshView();else renderCurrentCadMesh();toast("Malla importada: "+m.triangles()+" triángulos");});
  }catch(Exception e){runOnUiThread(()->{fileBusy=false;new AlertDialog.Builder(this).setTitle("Error al importar").setMessage(e.getMessage()).setPositiveButton("Aceptar",null).show();});}}).start();}
  else{MeshEngine.Mesh m=pendingMeshExport!=null?pendingMeshExport:currentMesh();pendingMeshExport=null;String format=meshExportFormat;new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
   if(out==null)throw new IOException("Archivo inaccesible");if(format.equals("stl"))MeshIO.writeStl(m,out);else MeshIO.writeObj(m,out);
   runOnUiThread(()->{fileBusy=false;toast("Malla exportada correctamente");});
  }catch(Exception e){runOnUiThread(()->{fileBusy=false;toast("Error al exportar: "+e.getMessage());});}}).start();}
 }
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
 if(request==IMPORT_MESH||request==EXPORT_MESH){if(request==EXPORT_MESH&&(result!=RESULT_OK||data==null||data.getData()==null))pendingMeshExport=null;handleMeshResult(request,result,data);return;}if(request!=SAVE_FILE&&request!=OPEN_FILE)return;
 if(result!=RESULT_OK||data==null||data.getData()==null){if(request==SAVE_FILE)pendingExport=null;toast("Operación cancelada");return;}
 Uri uri=data.getData();String snapshot=pendingExport;pendingExport=null;fileBusy=true;info.setText(request==SAVE_FILE?"Guardando archivo…":"Abriendo archivo…");
 new Thread(()->{try{
 if(request==SAVE_FILE){if(snapshot==null)throw new IOException("Vuelve a pulsar Guardar");
 try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("No se pudo escribir");out.write(snapshot.getBytes(StandardCharsets.UTF_8));out.flush();}
 runOnUiThread(()->{fileBusy=false;info.setText("Archivo guardado · Copia externa creada");toast("Archivo guardado en la ubicación elegida");});
 }else{String text;try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
 if(in==null)throw new IOException("No se pudo leer");byte[] buf=new byte[8192];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>2*1024*1024)throw new IOException("Archivo demasiado grande");out.write(buf,0,n);}text=new String(out.toByteArray(),StandardCharsets.UTF_8);}
 ParsedDesign loaded=decode(text);
 runOnUiThread(()->{fileBusy=false;checkpoint();parts=loaded.parts;sketches=loaded.sketches;booleanMeshes=loaded.meshes;unionRecipes=loaded.recipes;holeRecipes=loaded.drills;extrusionChains=loaded.chains;chainFeatures=loaded.features;selected=parts.isEmpty()?-1:0;selectedSketch=-1;save();scene.invalidate();info.setText("Archivo abierto · "+parts.size()+" piezas");toast("Diseño abierto correctamente");});
 }
 }catch(Exception e){runOnUiThread(()->{fileBusy=false;info.setText("Operación fallida · El diseño actual se conserva");new AlertDialog.Builder(this).setTitle("No se pudo completar").setMessage("Comprueba que sea un archivo MyCAD válido y que la ubicación esté disponible. "+e.getMessage()).setPositiveButton("Aceptar",null).show();});}}).start();
 }
 void newDesign(){activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;if(meshView!=null)meshView.highlightFace(-1);movie=false;scene.stopMovie();if(fileBusy)return;new AlertDialog.Builder(this).setTitle("Nuevo diseño").setMessage("Se vaciará el lienzo. Guarda antes un archivo para conservar el diseño actual.").setNegativeButton("Cancelar",null).setPositiveButton("Crear nuevo",(d,w)->{checkpoint();parts.clear();sketches.clear();booleanMeshes.clear();unionRecipes.clear();holeRecipes.clear();extrusionChains.clear();chainFeatures.clear();sketchTool=0;selectedSketch=-1;selected=-1;save();scene.invalidate();info.setText("Diseño nuevo · Medidas en mm");}).show();}
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
 "AHORA · v0.4.2\nArchivos, guía y validación del ciclo guardar/abrir en el móvil.\n\n"+
 "SIGUIENTE · v0.4.3 (propuesta)\nDeshacer/rehacer, selección tocando, mover en X/Y/Z y ajustar la vista al conjunto.\n\n"+
 "DESPUÉS · v0.4 (propuesta)\nBocetos táctiles: líneas, rectángulos, círculos y cotas.\n\n"+
 "FASE 3D AVANZADA\nExtruir perfiles y evaluar un motor geométrico para unir, cortar y taladrar.\n\n"+
 "SALIDA A FABRICACIÓN\nExportación STL y planos PDF acotados; STEP requiere un motor CAD adecuado.\n\n"+
 "Las funciones futuras no están implementadas. Primero estabilizaremos cada fase con tus pruebas."
 ).setPositiveButton("Cerrar",null).show();}
 class Face {float[][] vertices;float depth;int color;Face(float[][] v,int c){vertices=v;color=c;for(float[] p:v)depth+=p[2]/v.length;}}
 class Scene extends View{
 Paint paint=new Paint(3);int selectedFaceBody=-1,selectedFaceIndex=-1;int partHandle=-1;float originalW,originalD,originalH,originalX,originalY,originalZ;boolean geometryChanged=false,entityDrag=false;int activeMoveAxis=-1,activeRotateAxis=-1;float rotationStart,rotationOriginal;float yaw=.65f,pitch=.6f,zoom=3,lastX,lastY,multiMidX,multiMidY,panX,panY;boolean movingEntity=false,editingHandle=false,gestureMulti=false;int handleIndex=-1,tapCount=0,lastTapEntity=-999;long lastTapTime=0;float movieYaw,movieOldYaw,movieOldPitch,movieOldZoom,moviePivotX,moviePivotY,moviePivotZ;boolean movieOldFlat,movieCameraSaved=false;long movieLast;android.os.Handler movieHandler=new android.os.Handler(android.os.Looper.getMainLooper());Runnable movieFrame;ScaleGestureDetector scale;
 boolean cubeDragging=false; float cubeDownX,cubeDownY;
 float cubeSize(){return dp(96);}
 float cubeLeft(){return getWidth()-cubeSize()-dp(12);}
 float cubeTop(){return dp(12);}
 boolean inCube(float x,float y){return !flat&&x>=cubeLeft()&&x<=cubeLeft()+cubeSize()&&y>=cubeTop()&&y<=cubeTop()+cubeSize();}
 void cubeView(int row,int col){
  flat=false;
  if(row==0&&col==1){yaw=0;pitch=1.5f;}
  else if(row==2&&col==1){yaw=0;pitch=-1.5f;}
  else if(row==1&&col==0){yaw=1.5707963f;pitch=0;}
  else if(row==1&&col==2){yaw=-1.5707963f;pitch=0;}
  else if(row==1&&col==1){yaw=0;pitch=0;}
  else {yaw=col==0?.75f:col==2?-.75f:0;pitch=row==0?.65f:-.65f;}
  invalidate();
 }
 void drawOrientationCube(Canvas c){
  if(flat)return;
  float size=cubeSize(),left=cubeLeft(),top=cubeTop(),cell=size/3f;
  paint.setStyle(Paint.Style.FILL);
  paint.setColor(darkMode?Color.rgb(47,64,83):Color.rgb(207,220,234));
  c.drawRoundRect(left,top,left+size,top+size,dp(8),dp(8),paint);
  paint.setColor(darkMode?Color.rgb(96,120,146):Color.rgb(132,153,174));
  paint.setStrokeWidth(dp(1));
  for(int k=1;k<3;k++){c.drawLine(left+k*cell,top,left+k*cell,top+size,paint);c.drawLine(left,top+k*cell,left+size,top+k*cell,paint);}
  String[][] labels={{"ISO","SUP","ISO"},{"IZQ","FREN","DER"},{"ISO","INF","ISO"}};
  paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(9));
  paint.setColor(darkMode?Color.WHITE:Color.rgb(20,36,50));
  for(int row=0;row<3;row++)for(int col=0;col<3;col++)c.drawText(labels[row][col],left+(col+.5f)*cell,top+(row+.57f)*cell,paint);
  paint.setTextAlign(Paint.Align.LEFT);
 }
 Scene(){super(MainActivity.this);scale=new ScaleGestureDetector(MainActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){zoom=Math.max(.02f,Math.min(40,zoom*(float)Math.pow(d.getScaleFactor(),zoomSensitivity)));invalidate();return true;}});}
 void movieCenter(){if(!movieAll&&selected>=0&&selected<parts.size()){Part p=parts.get(selected);moviePivotX=p.x;moviePivotY=p.y;moviePivotZ=p.z+p.h/2;zoom=Math.min(zoom,Math.max(.1f,Math.min(getWidth(),getHeight())/(Math.max(p.w,Math.max(p.d,p.h))*2.5f)));return;}if(parts.isEmpty()){moviePivotX=moviePivotY=moviePivotZ=0;return;}float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE,minZ=Float.MAX_VALUE,maxZ=-Float.MAX_VALUE;for(Part p:parts){minX=Math.min(minX,p.x-p.w/2);maxX=Math.max(maxX,p.x+p.w/2);minY=Math.min(minY,p.y-p.d/2);maxY=Math.max(maxY,p.y+p.d/2);minZ=Math.min(minZ,p.z);maxZ=Math.max(maxZ,p.z+p.h);}moviePivotX=(minX+maxX)/2;moviePivotY=(minY+maxY)/2;moviePivotZ=(minZ+maxZ)/2;float span=Math.max(maxX-minX,Math.max(maxY-minY,maxZ-minZ));zoom=Math.min(zoom,Math.max(.1f,Math.min(getWidth(),getHeight())/(Math.max(1,span)*2.5f)));}
 void startMovie(){if(!movieCameraSaved){movieOldYaw=yaw;movieOldPitch=pitch;movieOldZoom=zoom;movieOldFlat=flat;movieCameraSaved=true;}flat=false;movieCenter();movieYaw=yaw;movieLast=System.currentTimeMillis();if(movieFrame==null)movieFrame=new Runnable(){public void run(){if(!movie)return;long now=System.currentTimeMillis();yaw+=(now-movieLast)*.0005f*movieSpeed;movieLast=now;invalidate();if(!movieLoop&&yaw-movieYaw>=2*Math.PI){movie=false;stopMovie();return;}movieHandler.postDelayed(this,16);}};movieHandler.removeCallbacks(movieFrame);movieHandler.post(movieFrame);}
 void stopMovie(){if(movieFrame!=null)movieHandler.removeCallbacks(movieFrame);if(movieCameraSaved){yaw=movieOldYaw;pitch=movieOldPitch;zoom=movieOldZoom;flat=movieOldFlat;movieCameraSaved=false;invalidate();}}
 float[] rotatePoint(Part p,float x,float y,float z){double ax=Math.toRadians(p.rotationX),ay=Math.toRadians(p.rotationY),az=Math.toRadians(p.rotationZ);double cy=Math.cos(ax),sy=Math.sin(ax),yy=y*cy-z*sy,zz=y*sy+z*cy;double cx=Math.cos(ay),sx=Math.sin(ay),xx=x*cx+zz*sx,z2=-x*sx+zz*cx;double cz=Math.cos(az),sz=Math.sin(az);return new float[]{(float)(xx*cz-yy*sz),(float)(xx*sz+yy*cz),(float)z2};}
 float[] project(float x,float y,float z){if(movieCameraSaved){x-=moviePivotX;y-=moviePivotY;z-=moviePivotZ;}if(flat)return new float[]{getWidth()/2f+x*zoom+panX*zoom,getHeight()/2f-y*zoom-panY*zoom,z};float u=(float)(x*Math.cos(yaw)-y*Math.sin(yaw));float v=(float)(x*Math.sin(yaw)+y*Math.cos(yaw));return new float[]{getWidth()/2f+u*zoom+panX*zoom,getHeight()/2f+(float)(v*Math.sin(pitch)-z*Math.cos(pitch))*zoom-panY*zoom,(float)(v*Math.cos(pitch)+z*Math.sin(pitch))};}
 protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(darkMode?Color.rgb(24,33,46):Color.rgb(239,244,249));paint.setStrokeWidth(1);paint.setColor(darkMode?Color.rgb(44,57,73):Color.rgb(201,213,224));if(gridVisible){float step=Math.max(.1f,gridStep);int count=Math.min(160,(int)(400/step));for(int i=-count;i<=count;i++){float t=i*step;float[] a,b;if(gridPlane==0){a=project(t,-200,0);b=project(t,200,0);}else if(gridPlane==1){a=project(t,0,-200);b=project(t,0,200);}else{a=project(0,t,-200);b=project(0,t,200);}c.drawLine(a[0],a[1],b[0],b[1],paint);if(gridPlane==0){a=project(-200,t,0);b=project(200,t,0);}else if(gridPlane==1){a=project(-200,0,t);b=project(200,0,t);}else{a=project(0,-200,t);b=project(0,200,t);}c.drawLine(a[0],a[1],b[0],b[1],paint);}}if(axesVisible){float[][] origin={project(0,0,0),project(100,0,0),project(0,100,0),project(0,0,100)};for(int k=1;k<=3;k++){paint.setColor(new int[]{0,Color.RED,Color.GREEN,Color.BLUE}[k]);paint.setStrokeWidth(3);c.drawLine(origin[0][0],origin[0][1],origin[k][0],origin[k][1],paint);}}
 ArrayList<Face> faces=new ArrayList<>();
 for(int j=0;j<parts.size();j++){Part p=parts.get(j);int n=p.circle?64:4;float[][] low=new float[n][],high=new float[n][];
 for(int i=0;i<n;i++){float x,y;if(p.circle){x=(float)Math.cos(i*2*Math.PI/n)*p.w/2;y=(float)Math.sin(i*2*Math.PI/n)*p.d/2;}else{x=(i==0||i==3)?-p.w/2:p.w/2;y=i<2?-p.d/2:p.d/2;}float[] lo=rotatePoint(p,x,y,-p.h/2),hi=rotatePoint(p,x,y,p.h/2);low[i]=project(lo[0]+p.x,lo[1]+p.y,lo[2]+p.z+p.h/2);high[i]=project(hi[0]+p.x,hi[1]+p.y,hi[2]+p.z+p.h/2);}
 int col=j==selected?Color.rgb(69,186,207):Color.rgb(105,135,165);
 if(!flat)faces.add(new Face(low,col));
 if(!flat)for(int i=0;i<n;i++){int k=(i+1)%n;faces.add(new Face(new float[][]{low[i],low[k],high[k],high[i]},col));}
 faces.add(new Face(high,j==selected?Color.rgb(112,220,229):Color.rgb(154,180,204)));
 }
 Collections.sort(faces,(a,b)->Float.compare(a.depth,b.depth));
 for(Face f:faces){Path path=new Path();path.moveTo(f.vertices[0][0],f.vertices[0][1]);for(int i=1;i<f.vertices.length;i++)path.lineTo(f.vertices[i][0],f.vertices[i][1]);path.close();paint.setStyle(Paint.Style.FILL);paint.setColor(f.color);c.drawPath(path,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.rgb(23,65,85));c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}drawSelectedFace(c);if(selected>=0&&selected<parts.size()){if(rotationMode)drawRotationRings(c,parts.get(selected));else if(interactionMode==1)drawMoveGizmo(c,parts.get(selected));else if(interactionMode==2)drawPartHandles(c,parts.get(selected));}
 paint.setColor(darkMode?Color.WHITE:Color.rgb(25,38,55));paint.setTextSize(30);c.drawText(flat?"PLANTA · 2D":"VISTA · 3D",20,40,paint);paint.setStrokeWidth(3);paint.setStyle(Paint.Style.STROKE);for(int i=0;i<sketches.size();i++){paint.setColor(i==selectedSketch?Color.rgb(255,193,91):Color.rgb(112,220,229));paint.setStrokeWidth(i==selectedSketch?6:3);drawSketch(c,sketches.get(i));if(i==selectedSketch)drawHandles(c,sketches.get(i));}if(preview!=null)drawSketch(c,preview);paint.setStyle(Paint.Style.FILL);if(sketchTool>0){paint.setColor(Color.WHITE);c.drawText("BOCETO "+new String[]{"","LÍNEA","RECTÁNGULO","CÍRCULO"}[sketchTool],20,40,paint);}else if(parts.isEmpty()&&sketches.isEmpty())c.drawText("Añade una figura para empezar",20,getHeight()/2f,paint);
 drawOrientationCube(c);
 }
 Sketch preview;float mmX(float px){return (px-getWidth()/2f)/zoom;}float mmY(float py){return (getHeight()/2f-py)/zoom;}
 void handle(Canvas c,float x,float y){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(255,185,65));c.drawCircle(x,y,dp(6),paint);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);c.drawCircle(x,y,dp(6),paint);}
 void drawHandles(Canvas c,Sketch q){float[] a=sketchPoint(q,q.x1,q.y1),b=sketchPoint(q,q.x2,q.y2);if(q.type==1){handle(c,a[0],a[1]);handle(c,b[0],b[1]);}else if(q.type==2){handle(c,a[0],a[1]);handle(c,a[0],b[1]);handle(c,b[0],a[1]);handle(c,b[0],b[1]);handle(c,(a[0]+b[0])/2,(a[1]+b[1])/2);}else{handle(c,a[0],a[1]);handle(c,b[0],b[1]);}paint.setStyle(Paint.Style.STROKE);}
 int hitHandle(float sx,float sy){if(selectedSketch<0||selectedSketch>=sketches.size())return -1;Sketch q=sketches.get(selectedSketch);float[] a=sketchPoint(q,q.x1,q.y1),b=sketchPoint(q,q.x2,q.y2);float[][] pts=q.type==1||q.type==3?new float[][]{a,b}:new float[][]{a,{a[0],b[1]},{b[0],a[1]},b,{(a[0]+b[0])/2,(a[1]+b[1])/2}};for(int i=0;i<pts.length;i++)if(Math.hypot(sx-pts[i][0],sy-pts[i][1])<dp(22))return i;return -1;}
 void editSketch(){if(selectedSketch<0)return;Sketch q=sketches.get(selectedSketch);LinearLayout box=new LinearLayout(MainActivity.this);box.setOrientation(LinearLayout.VERTICAL);String[] names={"Inicio X","Inicio Y","Final X","Final Y"};float[] vals={q.x1,q.y1,q.x2,q.y2};EditText[] fields=new EditText[4];for(int i=0;i<4;i++){TextView label=new TextView(MainActivity.this);label.setText(names[i]+" (mm)");box.addView(label);fields[i]=new EditText(MainActivity.this);fields[i].setInputType(8194|4096);fields[i].setText(""+vals[i]);box.addView(fields[i]);}new AlertDialog.Builder(MainActivity.this).setTitle("Editar boceto · mm").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{try{float[] v=new float[4];for(int i=0;i<4;i++){v[i]=Float.parseFloat(fields[i].getText().toString().replace(',','.'));if(!Float.isFinite(v[i])||Math.abs(v[i])>10000)throw new Exception();}if(!validateLinkedSketchEdit(q,v[0],v[1],v[2],v[3]))throw new Exception();checkpoint();q.x1=v[0];q.y1=v[1];q.x2=v[2];q.y2=v[3];rebuildLinkedParts(q);save();invalidate();}catch(Exception ex){toast("Coordenadas no válidas");}}).show();}

 float[][] partPoints(Part p){float z=p.z+p.h;if(p.circle)return new float[][]{{p.x,p.y,z},{p.x+p.w/2,p.y,z},{p.x,p.y,z-p.h/2}};return new float[][]{{p.x+p.w/2,p.y,z},{p.x,p.y+p.d/2,z},{p.x,p.y,z-p.h/2},{p.x,p.y,z}};}
 void drawPartHandles(Canvas c,Part p){float[][] pts=partPoints(p);for(int i=0;i<pts.length;i++){float[] q=project(pts[i][0],pts[i][1],pts[i][2]);handle(c,q[0],q[1]);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setTextSize(dp(11));c.drawText(p.circle?new String[]{"C","R","H"}[i]:new String[]{"W","D","H","C"}[i],q[0]+dp(8),q[1]-dp(8),paint);}paint.setStyle(Paint.Style.FILL);}
 int hitPartHandle(float sx,float sy){if(selected<0||selected>=parts.size())return -1;float[][] pts=partPoints(parts.get(selected));for(int i=0;i<pts.length;i++){float[] q=project(pts[i][0],pts[i][1],pts[i][2]);if(Math.hypot(sx-q[0],sy-q[1])<dp(19))return i;}return -1;}
 void resizePart(float dx,float dy){if(selected<0)return;Part p=parts.get(selected);float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw),sp=(float)Math.sin(pitch),cp=(float)Math.cos(pitch);float mm=dragSensitivity/Math.max(.02f,zoom);float ux=flat?dx*mm:(dx*cy+dy*sy/Math.max(.15f,Math.abs(sp)))*mm;float uy=flat?-dy*mm:(-dx*sy+dy*cy/Math.max(.15f,Math.abs(sp)))*mm;float uz=-dy*mm/Math.max(.25f,Math.abs(cp));if(p.circle){if(partHandle==1){p.w=p.d=Math.max(.1f,Math.min(10000,snap(originalW+2*ux)));}else if(partHandle==2){p.h=Math.max(.1f,Math.min(10000,snap(originalH+uz)));}}else{if(partHandle==0)p.w=Math.max(.1f,Math.min(10000,snap(originalW+2*ux)));else if(partHandle==1)p.d=Math.max(.1f,Math.min(10000,snap(originalD+2*uy)));else if(partHandle==2)p.h=Math.max(.1f,Math.min(10000,snap(originalH+uz)));}info.setText(String.format(java.util.Locale.US,"ANCHO %.1f · FONDO %.1f · ALTO %.1f mm",p.w,p.d,p.h));invalidate();}
 void movePart(float dx,float dy){if(selected<0)return;Part p=parts.get(selected);float ux=dx/zoom*dragSensitivity,uy=-dy/zoom*dragSensitivity;if(!flat){float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw),sp=(float)Math.sin(pitch);float screenY=dy/Math.max(.2f,Math.abs(sp));ux=(dx*cy+screenY*sy)/zoom*dragSensitivity;uy=(-dx*sy+screenY*cy)/zoom*dragSensitivity;}float x=originalX,y=originalY,z=originalZ;if(moveMode==1){if(Math.abs(ux)>=Math.abs(uy))x+=ux;else y+=uy;}else if(moveMode==2){if(moveAxis==0)x+=ux;else if(moveAxis==1)y+=uy;else z+=-dy/Math.max(.1f,zoom)*dragSensitivity;}else if(moveMode==3){float radius=(float)Math.hypot(originalX,originalY);float angle=(float)Math.atan2(originalY,originalX)+(ux*originalY-uy*originalX)/(Math.max(1,radius*radius));x=radius*(float)Math.cos(angle);y=radius*(float)Math.sin(angle);}else{x+=ux;y+=uy;}p.x=snap(x);p.y=snap(y);p.z=snap(z);info.setText(String.format(java.util.Locale.US,"X %.1f · Y %.1f · Z %.1f mm",p.x,p.y,p.z));invalidate();}
 void drawRotationRings(Canvas c,Part p){float radius=Math.max(dp(62),Math.min(dp(112),Math.max(p.w,Math.max(p.d,p.h))*zoom*.65f));float[] center=project(p.x,p.y,p.z+p.h/2);for(int axis=0;axis<3;axis++){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(axis==activeRotateAxis?5:3));paint.setColor(new int[]{Color.rgb(235,90,90),Color.rgb(70,208,120),Color.rgb(75,140,255)}[axis]);android.graphics.Path path=new android.graphics.Path();for(int k=0;k<=72;k++){double t=k*Math.PI/36;float xx=axis==0?0:(float)Math.cos(t),yy=axis==1?0:(float)Math.sin(t),zz=axis==2?0:(float)(axis==0?Math.sin(t):Math.cos(t));float[] q=project(p.x+xx*radius/zoom,p.y+yy*radius/zoom,p.z+p.h/2+zz*radius/zoom);if(k==0)path.moveTo(q[0],q[1]);else path.lineTo(q[0],q[1]);}c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}paint.setColor(Color.WHITE);paint.setTextSize(dp(12));c.drawText(String.format(java.util.Locale.US,"X %.0f°  Y %.0f°  Z %.0f°",p.rotationX,p.rotationY,p.rotationZ),center[0]-dp(65),center[1]+radius+dp(24),paint);}
 int hitRotationRing(float x,float y){if(!rotationMode||selected<0)return -1;Part p=parts.get(selected);float radius=Math.max(dp(62),Math.min(dp(112),Math.max(p.w,Math.max(p.d,p.h))*zoom*.65f));float best=dp(22);int found=-1;for(int axis=0;axis<3;axis++){for(int k=0;k<72;k++){double t=k*Math.PI/36;float xx=axis==0?0:(float)Math.cos(t),yy=axis==1?0:(float)Math.sin(t),zz=axis==2?0:(float)(axis==0?Math.sin(t):Math.cos(t));float[] q=project(p.x+xx*radius/zoom,p.y+yy*radius/zoom,p.z+p.h/2+zz*radius/zoom);float d=(float)Math.hypot(x-q[0],y-q[1]);if(d<best){best=d;found=axis;}}}return found;}
  void drawMoveGizmo(Canvas c,Part p){float[] center=project(p.x,p.y,p.z+p.h/2);float len=dp(55);for(int axis=0;axis<3;axis++){float[] end=axis==0?project(p.x+25,p.y,p.z+p.h/2):axis==1?project(p.x,p.y+25,p.z+p.h/2):project(p.x,p.y,p.z+p.h/2+25);float vx=end[0]-center[0],vy=end[1]-center[1],norm=(float)Math.hypot(vx,vy);if(norm<1){vx=axis==2?0:1;vy=axis==2?-1:0;norm=1;}vx=vx/norm*len;vy=vy/norm*len;paint.setColor(new int[]{Color.rgb(235,90,90),Color.rgb(70,208,120),Color.rgb(75,140,255)}[axis]);paint.setStrokeWidth(dp(4));c.drawLine(center[0],center[1],center[0]+vx,center[1]+vy,paint);c.drawCircle(center[0]+vx,center[1]+vy,dp(11),paint);paint.setColor(Color.WHITE);paint.setTextSize(dp(13));c.drawText(new String[]{"X","Y","Z"}[axis],center[0]+vx-dp(5),center[1]+vy+dp(5),paint);}paint.setColor(Color.WHITE);paint.setTextSize(dp(11));c.drawText(String.format(java.util.Locale.US,"X %.1f  Y %.1f  Z %.1f mm",p.x,p.y,p.z),Math.max(5,center[0]-dp(70)),center[1]+dp(80),paint);}
 int hitMoveGizmo(float sx,float sy){if(selected<0||selected>=parts.size()||interactionMode!=1)return -1;Part p=parts.get(selected);float[] center=project(p.x,p.y,p.z+p.h/2);for(int axis=2;axis>=0;axis--){float[] end=axis==0?project(p.x+25,p.y,p.z+p.h/2):axis==1?project(p.x,p.y+25,p.z+p.h/2):project(p.x,p.y,p.z+p.h/2+25);float vx=end[0]-center[0],vy=end[1]-center[1],norm=(float)Math.hypot(vx,vy);if(norm<1){vx=axis==2?0:1;vy=axis==2?-1:0;norm=1;}float ex=center[0]+vx/norm*dp(55),ey=center[1]+vy/norm*dp(55);if(Math.hypot(sx-ex,sy-ey)<dp(24))return axis;}return -1;}
 float[] sketchPoint(Sketch q,float a,float b){if(flat)return new float[]{getWidth()/2f+a*zoom+panX*zoom,getHeight()/2f-b*zoom-panY*zoom,0};return q.plane==0?project(a,b,0):q.plane==1?project(a,0,b):project(0,a,b);}
 void drawSketch(Canvas c,Sketch q){float[] a=sketchPoint(q,q.x1,q.y1),b=sketchPoint(q,q.x2,q.y2);if(q.type==1)c.drawLine(a[0],a[1],b[0],b[1],paint);else if(q.type==2)c.drawRect(Math.min(a[0],b[0]),Math.min(a[1],b[1]),Math.max(a[0],b[0]),Math.max(a[1],b[1]),paint);else c.drawCircle(a[0],a[1],(float)Math.hypot(b[0]-a[0],b[1]-a[1]),paint);}
 int hitSketch(float sx,float sy){int best=-1;float distance=dp(18);for(int i=0;i<sketches.size();i++){Sketch q=sketches.get(i);float[] a=sketchPoint(q,q.x1,q.y1),b=sketchPoint(q,q.x2,q.y2);float d;if(q.type==1){float vx=b[0]-a[0],vy=b[1]-a[1],len=vx*vx+vy*vy;float t=len<.01f?0:Math.max(0,Math.min(1,((sx-a[0])*vx+(sy-a[1])*vy)/len));d=(float)Math.hypot(sx-a[0]-t*vx,sy-a[1]-t*vy);}else if(q.type==2){float l=Math.min(a[0],b[0]),r=Math.max(a[0],b[0]),t=Math.min(a[1],b[1]),bt=Math.max(a[1],b[1]);d=Math.min(Math.min(Math.abs(sx-l),Math.abs(sx-r)),Math.min(Math.abs(sy-t),Math.abs(sy-bt)));if(sx<l-distance||sx>r+distance||sy<t-distance||sy>bt+distance)d=Float.MAX_VALUE;}else{float radius=(float)Math.hypot(b[0]-a[0],b[1]-a[1]);d=Math.abs((float)Math.hypot(sx-a[0],sy-a[1])-radius);}if(d<distance){distance=d;best=i;}}return best;}
 long downAt;float downX,downY;boolean moved,drag;
 float[][] facePolygon(Part p,int face){
  float[][] corners=new float[8][];
  for(int i=0;i<4;i++){
   float x=(i==0||i==3)?-p.w/2:p.w/2,y=i<2?-p.d/2:p.d/2;
   float[] low=rotatePoint(p,x,y,-p.h/2),high=rotatePoint(p,x,y,p.h/2);
   corners[i]=project(low[0]+p.x,low[1]+p.y,low[2]+p.z+p.h/2);
   corners[i+4]=project(high[0]+p.x,high[1]+p.y,high[2]+p.z+p.h/2);
  }
  int[] ids;
  switch(face){
   case 0:ids=new int[]{4,5,6,7};break;
   case 1:ids=new int[]{0,1,2,3};break;
   case 2:ids=new int[]{1,2,6,5};break;
   case 3:ids=new int[]{3,0,4,7};break;
   case 4:ids=new int[]{2,3,7,6};break;
   default:ids=new int[]{0,1,5,4};break;
  }
  float[][] polygon=new float[4][];for(int i=0;i<4;i++)polygon[i]=corners[ids[i]];
  return polygon;
 }
 boolean insidePolygon(float x,float y,float[][] vertices){
  boolean inside=false;
  for(int i=0,j=vertices.length-1;i<vertices.length;j=i++){
   float[] a=vertices[i],b=vertices[j];
   if((a[1]>y)!=(b[1]>y)&&x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])inside=!inside;
  }
  return inside;
 }
 int[] pickFace(float x,float y){
  if(flat)return null;
  int[] result=null;float nearest=-Float.MAX_VALUE;
  for(int i=0;i<parts.size();i++){
   Part p=parts.get(i);if(p.circle)continue;
   for(int face=0;face<6;face++){
    float[][] polygon=facePolygon(p,face);if(!insidePolygon(x,y,polygon))continue;
    float depth=0;for(float[] v:polygon)depth+=v[2]/4f;
    if(depth>nearest){nearest=depth;result=new int[]{i,face};}
   }
  }
  return result;
 }
 void drawSelectedFace(Canvas c){
  if(flat||selectedFaceBody!=selected||selected<0||selected>=parts.size()||selectedFaceIndex<0||parts.get(selected).circle)return;
  float[][] v=facePolygon(parts.get(selected),selectedFaceIndex);
  Path outline=new Path();outline.moveTo(v[0][0],v[0][1]);
  for(int i=1;i<v.length;i++)outline.lineTo(v[i][0],v[i][1]);outline.close();
  paint.setStyle(Paint.Style.FILL);paint.setColor(Color.argb(90,255,197,62));c.drawPath(outline,paint);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setColor(Color.rgb(255,196,56));c.drawPath(outline,paint);
  paint.setStyle(Paint.Style.FILL);
 }
 int hit(float sx,float sy){for(int j=parts.size()-1;j>=0;j--){Part p=parts.get(j);float[] q=project(p.x,p.y,p.z+p.h/2);if(Math.hypot(q[0]-sx,q[1]-sy)<Math.max(30,Math.max(p.w,p.d)*zoom*.6f))return j;}return -1;}

 public boolean onTouchEvent(android.view.MotionEvent e){
  int action=e.getActionMasked();
  if(action==android.view.MotionEvent.ACTION_DOWN&&inCube(e.getX(),e.getY())){
   cubeDragging=true;cubeDownX=downX=e.getX();cubeDownY=downY=e.getY();return true;
  }
  if(cubeDragging){
   if(action==android.view.MotionEvent.ACTION_MOVE){
    float dx=e.getX()-cubeDownX,dy=e.getY()-cubeDownY;
    if(Math.hypot(dx,dy)>dp(2)){yaw+=dx*.014f;pitch=Math.max(-1.5f,Math.min(1.5f,pitch+dy*.014f));invalidate();}
    cubeDownX=e.getX();cubeDownY=e.getY();return true;
   }
   if(action==android.view.MotionEvent.ACTION_UP){
    if(inCube(e.getX(),e.getY())&&Math.hypot(e.getX()-downX,e.getY()-downY)<dp(6)){
     int col=Math.max(0,Math.min(2,(int)((e.getX()-cubeLeft())/(cubeSize()/3f))));
     int row=Math.max(0,Math.min(2,(int)((e.getY()-cubeTop())/(cubeSize()/3f))));
     cubeView(row,col);
    }
    cubeDragging=false;return true;
   }
   if(action==android.view.MotionEvent.ACTION_CANCEL){cubeDragging=false;return true;}
  }
  scale.onTouchEvent(e);
  if(action==android.view.MotionEvent.ACTION_POINTER_DOWN){gestureMulti=true;partHandle=-1;editingHandle=false;entityDrag=false;lastX=e.getX(0);lastY=e.getY(0);multiMidX=(e.getX(0)+e.getX(1))/2;multiMidY=(e.getY(0)+e.getY(1))/2;return true;}
  if(action==android.view.MotionEvent.ACTION_POINTER_UP){gestureMulti=true;return true;}
  if(action==android.view.MotionEvent.ACTION_CANCEL){gestureMulti=false;partHandle=-1;editingHandle=false;entityDrag=false;activeRotateAxis=-1;preview=null;return true;}
  if(action==android.view.MotionEvent.ACTION_UP&&gestureMulti){gestureMulti=false;return true;}
  if(gestureMulti){if(e.getPointerCount()>=2&&action==android.view.MotionEvent.ACTION_MOVE){float x=(e.getX(0)+e.getX(1))/2,y=(e.getY(0)+e.getY(1))/2;panX+=(x-multiMidX)/Math.max(.1f,zoom);panY-=(y-multiMidY)/Math.max(.1f,zoom);multiMidX=x;multiMidY=y;invalidate();}return true;}
  if(sketchTool>0){if(action==0){preview=new Sketch(sketchTool,mmX(e.getX()),mmY(e.getY()),mmX(e.getX()),mmY(e.getY()));preview.plane=sketchPlane;invalidate();}else if(action==2&&preview!=null){preview.x2=mmX(e.getX());preview.y2=mmY(e.getY());invalidate();}else if(action==1&&preview!=null){preview.x2=mmX(e.getX());preview.y2=mmY(e.getY());if(Math.hypot(preview.x2-preview.x1,preview.y2-preview.y1)>1&&sketches.size()<2000){checkpoint();sketches.add(preview);selectedSketch=sketches.size()-1;save();}preview=null;invalidate();}return true;}
  if(action==android.view.MotionEvent.ACTION_DOWN){lastX=downX=e.getX();lastY=downY=e.getY();moved=false;geometryChanged=false;activeMoveAxis=hitMoveGizmo(downX,downY);activeRotateAxis=hitRotationRing(downX,downY);if(activeRotateAxis>=0){Part p=parts.get(selected);rotationOriginal=activeRotateAxis==0?p.rotationX:activeRotateAxis==1?p.rotationY:p.rotationZ;}partHandle=interactionMode==2?hitPartHandle(downX,downY):-1;handleIndex=interactionMode==2&&partHandle<0?hitHandle(downX,downY):-1;editingHandle=handleIndex>=0;entityDrag=(interactionMode==2&&(partHandle>=0||editingHandle))||(interactionMode==1&&selected>=0&&(activeMoveAxis>=0||hit(downX,downY)==selected));if(entityDrag&&selected>=0){Part p=parts.get(selected);originalW=p.w;originalD=p.d;originalH=p.h;originalX=p.x;originalY=p.y;originalZ=p.z;}return true;}
  if(action==android.view.MotionEvent.ACTION_MOVE&&!scale.isInProgress()){
   float dx=e.getX()-lastX,dy=e.getY()-lastY,tx=e.getX()-downX,ty=e.getY()-downY;
   if(Math.hypot(tx,ty)>dp(6))moved=true;
   if((entityDrag||activeRotateAxis>=0)&&moved&&!geometryChanged){checkpoint();geometryChanged=true;}
   if(activeRotateAxis>=0&&moved&&selected>=0){Part p=parts.get(selected);float angle=rotationOriginal+(tx-ty)*.6f;if(activeRotateAxis==0)p.rotationX=angle;else if(activeRotateAxis==1)p.rotationY=angle;else p.rotationZ=angle;invalidate();}else if(interactionMode==2&&partHandle>=0&&moved)resizePart(tx,ty);
   else if(interactionMode==1&&entityDrag&&selected>=0&&moved){if(activeMoveAxis>=0){moveMode=2;moveAxis=activeMoveAxis;}movePart(tx,ty);}
   else if(interactionMode==0&&!flat){yaw+=dx*.01f*rotateSensitivity;pitch=Math.max(-1.5f,Math.min(1.5f,pitch+dy*.01f*rotateSensitivity));invalidate();}
   lastX=e.getX();lastY=e.getY();return true;
  }
  if(action==android.view.MotionEvent.ACTION_UP){if((entityDrag||activeRotateAxis>=0)&&moved&&geometryChanged)save();if(!moved&&activeRotateAxis<0){int n=hit(e.getX(),e.getY()),sk=hitSketch(e.getX(),e.getY());int[] face=interactionMode==0?pickFace(e.getX(),e.getY()):null;if(face!=null&&sk<0)n=face[0];selectedSketch=sk;selected=sk>=0?-1:n;selectedFaceBody=face!=null&&sk<0?face[0]:-1;selectedFaceIndex=face!=null&&sk<0?face[1]:-1;info.setText(selectedFaceIndex>=0?"Cuerpo "+(selected+1)+" · Cara "+new String[]{"Superior","Inferior","Derecha","Izquierda","Posterior","Frontal"}[selectedFaceIndex]:selected>=0?"Pieza seleccionada · "+new String[]{"Navegar","Mover","Editar"}[interactionMode]:"Selecciona una pieza o cambia de modo");invalidate();}partHandle=-1;editingHandle=false;entityDrag=false;activeMoveAxis=-1;activeRotateAxis=-1;return true;}
  return true;
 }
}
}
