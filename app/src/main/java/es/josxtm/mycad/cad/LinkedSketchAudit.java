package es.josxtm.mycad.cad;
import java.util.*;

/** Read-only validation of Android sketch-to-part dependencies before any migration. */
public final class LinkedSketchAudit {
 private LinkedSketchAudit(){}
 public static final class Sketch {
  public final String id;
  public final int type,plane;
  public final double x1,y1,x2,y2;
  public Sketch(String id,int type,int plane,double x1,double y1,double x2,double y2){
   this.id=id;this.type=type;this.plane=plane;
   this.x1=x1;this.y1=y1;this.x2=x2;this.y2=y2;
  }
 }
 public static final class Body {
  public final String id,sourceSketchId;
  public final boolean circle;
  public final double w,d,h,x,y,z,rx,ry,rz;
  public Body(String id,String source,boolean circle,double w,double d,double h,
              double x,double y,double z,double rx,double ry,double rz){
   this.id=id;this.sourceSketchId=source;this.circle=circle;
   this.w=w;this.d=d;this.h=h;this.x=x;this.y=y;this.z=z;
   this.rx=rx;this.ry=ry;this.rz=rz;
  }
 }
 public static final class Report {
  public final int sketches,independentBodies,linkedBodies,errors,warnings;
  public final List<String> diagnostics;
  Report(int s,int independent,int linked,int errors,int warnings,List<String> diagnostics){
   sketches=s;independentBodies=independent;linkedBodies=linked;this.errors=errors;
   this.warnings=warnings;
   this.diagnostics=Collections.unmodifiableList(new ArrayList<>(diagnostics));
  }
  public boolean isValid(){return errors==0;}
 }
 private static boolean close(double a,double b){
  return Double.isFinite(a)&&Double.isFinite(b)&&Math.abs(a-b)<=0.002;
 }
 public static Report inspect(List<Sketch> sketches,List<Body> bodies){
  if(sketches==null||bodies==null)throw new IllegalArgumentException("Missing source data");
  Map<String,Sketch> byId=new HashMap<>();
  HashSet<String> bodyIds=new HashSet<>();
  List<String> notes=new ArrayList<>();
  int errors=0,warnings=0,linked=0,independent=0;
  for(Sketch sketch:sketches){
   if(sketch==null||sketch.id==null||sketch.id.isEmpty()||byId.putIfAbsent(sketch.id,sketch)!=null){
    errors++;notes.add("Boceto duplicado o sin identificador");continue;
   }
   if(sketch.type!=1&&sketch.type!=2&&sketch.type!=3){errors++;notes.add("Tipo de boceto no reconocido: "+sketch.id);}
  }
  for(Body body:bodies){
   if(body==null||body.id==null||body.id.isEmpty()||!bodyIds.add(body.id)){
    errors++;notes.add("Cuerpo duplicado o sin identificador");continue;
   }
   if(body.sourceSketchId==null||body.sourceSketchId.isEmpty()){independent++;continue;}
   linked++;
   Sketch source=byId.get(body.sourceSketchId);
   if(source==null){
    errors++;notes.add("Cuerpo "+body.id+": boceto origen inexistente");continue;
   }
   try{
    LinkedSketchSolidPlan.Body expected=LinkedSketchSolidPlan.build(
     source.type,source.plane,source.x1,source.y1,source.x2,source.y2,body.h);
    if(expected.circle!=body.circle||
       !close(expected.w,body.w)||!close(expected.d,body.d)||
       !close(expected.h,body.h)||!close(expected.x,body.x)||
       !close(expected.y,body.y)||!close(expected.z,body.z)||
       !close(expected.rx,body.rx)||!close(expected.ry,body.ry)||
       !close(expected.rz,body.rz)){
     warnings++;notes.add("Cuerpo "+body.id+": geometría distinta del perfil origen");
    }
   }catch(IllegalArgumentException ex){
    errors++;notes.add("Cuerpo "+body.id+": perfil o extrusión incompatibles");
   }
  }
  return new Report(sketches.size(),independent,linked,errors,warnings,notes);
 }
}
