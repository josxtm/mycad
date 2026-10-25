package es.josxtm.mycad.cad;
import java.util.*;
/** Parametric extrusion history. Each step attaches a cuboid to the preceding one. */
public final class FaceExtrusionChain {
 public static final class Step {
  public final BoxFaceSelection.Face face;public final double distance;
  public Step(BoxFaceSelection.Face face,double distance){
   if(face==null||!Double.isFinite(distance)||distance<=0||distance>10000)throw new IllegalArgumentException("Invalid extrusion");
   this.face=face;this.distance=distance;
  }
 }
 public static final class Solid {
  public final double w,d,h,x,y,z;
  Solid(double w,double d,double h,double x,double y,double z){this.w=w;this.d=d;this.h=h;this.x=x;this.y=y;this.z=z;}
 }
 private final Solid root;private final List<Step> steps=new ArrayList<>();
 public FaceExtrusionChain(double w,double d,double h,double x,double y,double z){
  if(!Double.isFinite(w)||!Double.isFinite(d)||!Double.isFinite(h)||w<=0||d<=0||h<=0||
   !Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Invalid root");
  root=new Solid(w,d,h,x,y,z);
 }
 public int size(){return steps.size();}
 public List<Step> history(){return Collections.unmodifiableList(new ArrayList<>(steps));}
 public void append(BoxFaceSelection.Face face,double distance){Step step=new Step(face,distance);steps.add(step);}
 public void update(int index,double distance){Step old=steps.get(index);steps.set(index,new Step(old.face,distance));}
 public void removeLast(){if(steps.isEmpty())throw new IllegalStateException("Empty history");steps.remove(steps.size()-1);}
 public List<Solid> regenerate(){
  List<Solid> result=new ArrayList<>();result.add(root);
  Solid parent=root;
  for(Step step:steps){
   FacePrismExtrusion.Result r=FacePrismExtrusion.create(parent.w,parent.d,parent.h,parent.x,parent.y,parent.z,0,0,0,step.face,step.distance);
   parent=new Solid(r.w,r.d,r.h,r.x,r.y,r.z);result.add(parent);
  }
  return Collections.unmodifiableList(result);
 }
}
