import es.josxtm.mycad.cad.*;
import java.util.*;

public final class LoopExtrusionFeatureTest {
 static void ok(boolean cond,String msg){if(!cond)throw new AssertionError(msg);}
 static void reject(Runnable r,String msg){
  try{r.run();}catch(IllegalArgumentException good){return;}
  throw new AssertionError("Allowed invalid linked feature: "+msg);
 }
 static final String[] ids={
  "afc05d9b-7e37-4bbb-8cea-aa0000000001",
  "afc05d9b-7e37-4bbb-8cea-aa0000000002",
  "afc05d9b-7e37-4bbb-8cea-aa0000000003",
  "afc05d9b-7e37-4bbb-8cea-aa0000000004"
 };
 static Map<String,ClosedSketchExtrusion.Segment> square(){
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  lines.put(ids[0],new ClosedSketchExtrusion.Segment(0,0,0,5,0));
  lines.put(ids[1],new ClosedSketchExtrusion.Segment(0,5,0,5,5));
  lines.put(ids[2],new ClosedSketchExtrusion.Segment(0,5,5,0,5));
  lines.put(ids[3],new ClosedSketchExtrusion.Segment(0,0,5,0,0));
  return lines;
 }
 public static void main(String[] args){
  String featureId="afc05d9b-7e37-4bbb-8cea-aa0000000099";
  List<String> source=Arrays.asList(ids.clone());
  LoopExtrusionFeature f=new LoopExtrusionFeature(featureId,ids[2],0,6,source);
  ok(f.lineIds().equals(source),"source IDs order");
  ok(f.dependsOn(ids[0])&&!f.dependsOn(featureId),"dependencies");
  ok(Math.abs(f.regenerate(square()).volume-150)<1e-5,"original extrusion");
  LoopExtrusionFeature resized=f.withDistance(-4);
  ok(resized.id.equals(f.id)&&resized.selectedLineId.equals(f.selectedLineId),
     "stable feature and selected line IDs");
  ok(Math.abs(resized.regenerate(square()).volume-100)<1e-5,
     "opposite signed extrusion and positive volume");
  ok(f.distance==6,"immutable feature state");
  Map<String,ClosedSketchExtrusion.Segment> changed=square();
  changed.put(ids[1],new ClosedSketchExtrusion.Segment(0,5,0,6,5));
  changed.put(ids[2],new ClosedSketchExtrusion.Segment(0,6,5,0,5));
  ok(f.regenerate(changed).area>25,"sketch editing regenerates larger profile");
  // Malformed edits MUST fail without mutating either saved recipe or the mesh.
  Map<String,ClosedSketchExtrusion.Segment> invalid=square();
  invalid.remove(ids[1]);
  final Map<String,ClosedSketchExtrusion.Segment> orphan=invalid;
  reject(()->f.regenerate(orphan),"orphan sketch");
  invalid=square();
  invalid.put(ids[0],new ClosedSketchExtrusion.Segment(1,0,0,5,0));
  final Map<String,ClosedSketchExtrusion.Segment> wrongPlane=invalid;
  reject(()->f.regenerate(wrongPlane),"changed plane");
  invalid=square();
  invalid.put(ids[2],new ClosedSketchExtrusion.Segment(0,5,5,1,5));
  final Map<String,ClosedSketchExtrusion.Segment> open=invalid;
  reject(()->f.regenerate(open),"broken line loop");
  reject(()->f.withDistance(0),"zero distance");
  reject(()->f.withDistance(20000),"distance outside bounds");
  reject(()->new LoopExtrusionFeature(featureId,ids[0],0,2,
   Arrays.asList(ids[0],ids[1],ids[1])),"duplicate source ID");
  reject(()->new LoopExtrusionFeature(featureId,featureId,0,2,source),
   "selected ID outside source set");
  reject(()->new LoopExtrusionFeature("bad",ids[0],0,2,source),
   "invalid feature ID");
  boolean immutable=false;
  try{f.lineIds().clear();}catch(UnsupportedOperationException expected){immutable=true;}
  ok(immutable,"source IDs cannot mutate");
  System.out.println("LoopExtrusionFeatureTest: OK");
 }
}
