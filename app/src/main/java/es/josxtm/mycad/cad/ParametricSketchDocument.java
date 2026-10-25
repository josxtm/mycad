package es.josxtm.mycad.cad;

import java.util.*;

/** Pure Java XY rectangular sketch-to-box feature graph with atomic revisions. */
public final class ParametricSketchDocument {
 public static final class Rectangle {
  public final String id; public final double x,y,width,depth;
  Rectangle(String id,double x,double y,double width,double depth){this.id=id;this.x=x;this.y=y;this.width=width;this.depth=depth;}
 }
 public static final class Extrusion {
  public final String id,sketchId,bodyId; public final double baseZ,height;
  Extrusion(String id,String sketchId,String bodyId,double baseZ,double height){this.id=id;this.sketchId=sketchId;this.bodyId=bodyId;this.baseZ=baseZ;this.height=height;}
 }
 private static final class State {
  final Map<String,Rectangle> sketches;final Map<String,Extrusion> extrusions;final Map<String,CadKernel.Box> bodies;
  State(Map<String,Rectangle> s,Map<String,Extrusion> e,Map<String,CadKernel.Box> b){
   sketches=Collections.unmodifiableMap(new LinkedHashMap<>(s));
   extrusions=Collections.unmodifiableMap(new LinkedHashMap<>(e));
   bodies=Collections.unmodifiableMap(new LinkedHashMap<>(b));
  }
 }
 private final CadKernel kernel;
 private final List<State> history=new ArrayList<>();
 private int cursor;
 public ParametricSketchDocument(CadKernel kernel){
  if(kernel==null)throw new IllegalArgumentException("Kernel requerido");
  this.kernel=kernel;
  history.add(new State(Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap()));
 }
 private State state(){return history.get(cursor);}
 public Map<String,Rectangle> sketches(){return state().sketches;}
 public Map<String,Extrusion> extrusions(){return state().extrusions;}
 public Map<String,CadKernel.Box> bodies(){return state().bodies;}
 public boolean undo(){if(cursor==0)return false;cursor--;return true;}
 public boolean redo(){if(cursor+1==history.size())return false;cursor++;return true;}
 private void commit(State s){while(history.size()>cursor+1)history.remove(history.size()-1);history.add(s);cursor++;}
 private static boolean validRectangle(double x,double y,double w,double d){
  return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(w)&&Double.isFinite(d)
   &&Math.abs(x)<=100000&&Math.abs(y)<=100000&&w>0&&d>0&&w<=100000&&d<=100000;
 }
 private CadKernel.Result<CadKernel.Box> rebuild(Map<String,Rectangle> sketches,Map<String,Extrusion> extrusions,String changedBody){
  Map<String,CadKernel.Box> built=new LinkedHashMap<>();CadKernel.Box selected=null;
  for(Extrusion feature:extrusions.values()){
   Rectangle sketch=sketches.get(feature.sketchId);
   if(sketch==null)return CadKernel.Result.invalid("Perfil dependiente inexistente");
   CadKernel.Result<CadKernel.Box> result=kernel.createBox(sketch.x,sketch.y,feature.baseZ,sketch.width,sketch.depth,feature.height);
   if(!result.isSuccess())return result;
   CadKernel.Box raw=result.value;
   CadKernel.Box box=new CadKernel.Box(feature.bodyId,raw.x,raw.y,raw.z,raw.width,raw.depth,raw.height);
   if(built.put(feature.bodyId,box)!=null)return CadKernel.Result.invalid("Cuerpo duplicado");
   if(feature.bodyId.equals(changedBody))selected=box;
  }
  commit(new State(sketches,extrusions,built));
  return CadKernel.Result.success(selected);
 }
 public String addRectangle(double x,double y,double width,double depth){
  if(!validRectangle(x,y,width,depth))return null;
  String id=UUID.randomUUID().toString();
  Map<String,Rectangle> next=new LinkedHashMap<>(sketches());
  next.put(id,new Rectangle(id,x,y,width,depth));
  commit(new State(next,extrusions(),bodies()));return id;
 }
 public CadKernel.Result<CadKernel.Box> extrude(String sketchId,double baseZ,double height){
  Rectangle sketch=sketches().get(sketchId);
  if(sketch==null)return CadKernel.Result.invalid("Boceto inexistente");
  String bodyId=UUID.randomUUID().toString();
  String featureId=UUID.randomUUID().toString();
  Map<String,Extrusion> next=new LinkedHashMap<>(extrusions());
  next.put(featureId,new Extrusion(featureId,sketchId,bodyId,baseZ,height));
  return rebuild(sketches(),next,bodyId);
 }
 public boolean updateRectangle(String sketchId,double x,double y,double width,double depth){
  if(!sketches().containsKey(sketchId)||!validRectangle(x,y,width,depth))return false;
  Map<String,Rectangle> next=new LinkedHashMap<>(sketches());
  next.put(sketchId,new Rectangle(sketchId,x,y,width,depth));
  CadKernel.Result<CadKernel.Box> result=rebuild(next,extrusions(),null);
  return result.isSuccess();
 }
 public boolean removeRectangle(String sketchId){
  if(!sketches().containsKey(sketchId))return false;
  Map<String,Rectangle> next=new LinkedHashMap<>(sketches());next.remove(sketchId);
  Map<String,Extrusion> dependents=new LinkedHashMap<>();
  for(Extrusion e:extrusions().values())if(!e.sketchId.equals(sketchId))dependents.put(e.id,e);
  return rebuild(next,dependents,null).isSuccess();
 }
 /** Versioned, deterministic text snapshot; separate from legacy Android JSON v1. */
 public String exportSnapshot(){
  StringBuilder result=new StringBuilder("MYCAD-PARAMETRIC|1\\n");
  for(Rectangle shape:sketches().values())
   result.append("R|").append(shape.id).append('|').append(shape.x).append('|')
    .append(shape.y).append('|').append(shape.width).append('|').append(shape.depth).append('\\n');
  for(Extrusion feature:extrusions().values())
   result.append("E|").append(feature.id).append('|').append(feature.sketchId).append('|')
    .append(feature.bodyId).append('|').append(feature.baseZ).append('|')
    .append(feature.height).append('\\n');
  return result.toString();
 }
 private static String uuid(String token){
  return UUID.fromString(token).toString();
 }
 /** Deserialize into a new document and validate every feature before committing. */
 public static ParametricSketchDocument importSnapshot(CadKernel kernel,String encoded){
  if(encoded==null||encoded.length()>2000000)throw new IllegalArgumentException("Documento excede límite");
  String[] lines=encoded.split("\\n",-1);
  if(lines.length==0||!"MYCAD-PARAMETRIC|1".equals(lines[0]))throw new IllegalArgumentException("Versión incorrecta");
  ParametricSketchDocument document=new ParametricSketchDocument(kernel);
  Map<String,Rectangle> sketches=new LinkedHashMap<>();
  Map<String,Extrusion> features=new LinkedHashMap<>();
  for(int i=1;i<lines.length;i++){
   String line=lines[i];
   if(line.isEmpty()){if(i!=lines.length-1)throw new IllegalArgumentException("Línea vacía");continue;}
   String[] cells=line.split("\\|",-1);
   try{
    if(cells.length==6&&"R".equals(cells[0])){
     String id=uuid(cells[1]);
     double x=Double.parseDouble(cells[2]),y=Double.parseDouble(cells[3]);
     double w=Double.parseDouble(cells[4]),d=Double.parseDouble(cells[5]);
     if(!validRectangle(x,y,w,d)||sketches.putIfAbsent(id,new Rectangle(id,x,y,w,d))!=null)
      throw new IllegalArgumentException("Rectángulo no válido o duplicado");
    }else if(cells.length==6&&"E".equals(cells[0])){
     String id=uuid(cells[1]),sketchId=uuid(cells[2]),bodyId=uuid(cells[3]);
     double base=Double.parseDouble(cells[4]),height=Double.parseDouble(cells[5]);
     if(features.putIfAbsent(id,new Extrusion(id,sketchId,bodyId,base,height))!=null)
      throw new IllegalArgumentException("Extrusión duplicada");
    }else throw new IllegalArgumentException("Registro no compatible");
   }catch(NumberFormatException error){throw new IllegalArgumentException("Número no válido en línea "+(i+1),error);}
   if(sketches.size()>2000||features.size()>500)throw new IllegalArgumentException("Demasiadas entidades");
  }
  if(!document.rebuild(sketches,features,null).isSuccess())
   throw new IllegalArgumentException("Referencias o geometría incorrectas");
  return document;
 }

}
