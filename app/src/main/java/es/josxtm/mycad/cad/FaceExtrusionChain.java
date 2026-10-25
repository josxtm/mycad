package es.josxtm.mycad.cad;
import java.util.*;
/** Parametric chain whose feature and step UUIDs survive edits and reconstruction. */
public final class FaceExtrusionChain {
 public static final class Step {
  public final String id;
  public final BoxFaceSelection.Face face;
  public final double distance;
  public Step(BoxFaceSelection.Face face,double distance){
   this(UUID.randomUUID().toString(),face,distance);
  }
  public Step(String id,BoxFaceSelection.Face face,double distance){
   validateId(id);
   if(face==null||!Double.isFinite(distance)||distance<=0||distance>10000)
    throw new IllegalArgumentException("Invalid extrusion");
   this.id=id;this.face=face;this.distance=distance;
  }
 }
 public static final class Solid {
  public final double w,d,h,x,y,z;
  Solid(double w,double d,double h,double x,double y,double z){
   this.w=w;this.d=d;this.h=h;this.x=x;this.y=y;this.z=z;
  }
 }
 public final String id;
 private final Solid root;
 private final List<Step> steps=new ArrayList<>();
 public FaceExtrusionChain(double w,double d,double h,double x,double y,double z){
  this(UUID.randomUUID().toString(),w,d,h,x,y,z);
 }
 public FaceExtrusionChain(String id,double w,double d,double h,double x,double y,double z){
  validateId(id);
  if(!Double.isFinite(w)||!Double.isFinite(d)||!Double.isFinite(h)||w<=0||d<=0||h<=0||
   !Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))
    throw new IllegalArgumentException("Invalid root");
  this.id=id;
  root=new Solid(w,d,h,x,y,z);
 }
 private static void validateId(String id){
  if(id==null||!UUID.fromString(id).toString().equals(id))
   throw new IllegalArgumentException("Invalid feature UUID");
 }
 public int size(){return steps.size();}
 public List<Step> history(){return Collections.unmodifiableList(new ArrayList<>(steps));}
 public void append(BoxFaceSelection.Face face,double distance){appendStep(new Step(face,distance));}
 /** Restore a step with its original identity when loading or copying a document. */
 public void appendStep(Step step){
  if(step==null||steps.size()>=50)throw new IllegalArgumentException("Too many steps");
  for(Step old:steps)if(old.id.equals(step.id))throw new IllegalArgumentException("Duplicate step UUID");
  steps.add(step);
 }
 public void update(int index,double distance){
  Step old=steps.get(index);
  steps.set(index,new Step(old.id,old.face,distance));
 }
 public void removeLast(){
  if(steps.isEmpty())throw new IllegalStateException("Empty history");
  steps.remove(steps.size()-1);
 }
 public List<Solid> regenerate(){
  List<Solid> result=new ArrayList<>();result.add(root);
  Solid parent=root;
  for(Step step:steps){
   FacePrismExtrusion.Result r=FacePrismExtrusion.create(parent.w,parent.d,parent.h,parent.x,parent.y,parent.z,0,0,0,step.face,step.distance);
   parent=new Solid(r.w,r.d,r.h,r.x,r.y,r.z);
   result.add(parent);
  }
  return Collections.unmodifiableList(result);
 }
}
