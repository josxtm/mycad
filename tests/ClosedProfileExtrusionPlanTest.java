import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class ClosedProfileExtrusionPlanTest {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static void invalid(Runnable work){try{work.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Invalid operation accepted");}
 private static String id(String name){return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();}
 private static Map<String,ClosedSketchExtrusion.Segment> profile(int plane,boolean concave){
  double x=9000.123456789,y=8000.123456789;
  double[][] p=concave?new double[][]{{0,0},{40,0},{40,10},{20,10},{20,30},{0,30}}:new double[][]{{0,0},{40,0},{40,30},{0,30}};
  Map<String,ClosedSketchExtrusion.Segment> map=new LinkedHashMap<>();
  for(int i=0;i<p.length;i++){double[] a=p[i],b=p[(i+1)%p.length];map.put(id("line"+i),new ClosedSketchExtrusion.Segment(plane,x+a[0],y+a[1],x+b[0],y+b[1]));}
  return map;
 }
 public static void main(String[] args){
  for(int p=0;p<3;p++)for(boolean concave:new boolean[]{false,true})for(int sign:new int[]{-1,1}){
   Map<String,ClosedSketchExtrusion.Segment> lines=profile(p,concave);String selected=lines.keySet().iterator().next();
   ClosedProfileExtrusionPlan plan=ClosedProfileExtrusionPlan.capture(lines,selected);
   double height=sign*10.123456789;ClosedProfileExtrusionPlan.Prepared prepared=plan.prepare(id("solid"),height,lines);
   check(prepared.feature.distance==height&&prepared.solid.distance()==height,"Height rounded before kernel");
   check(prepared.feature.plane==p&&prepared.solid.plane()==p,"Sketch plane lost");
   check(prepared.feature.lineIds().size()==lines.size(),"Profile sources lost");
   check(Math.abs(prepared.solid.volume()-(concave?800:1200)*Math.abs(height))<1e-7,"Real millimetre volume");
   float[] local=RelativeSceneCoordinates.from(Collections.singletonList(prepared.solid.triangleCoordinates())).triangles(0);
   check(TriangleMeshHealth.inspect(local).isWatertight(),"Extrusion is not a closed oriented solid");
   List<String> reversed=new ArrayList<>(lines.keySet());Collections.reverse(reversed);
   Map<String,ClosedSketchExtrusion.Segment> reordered=new LinkedHashMap<>();for(String key:reversed)reordered.put(key,lines.get(key));
   check(plan.isCurrent(reordered),"Source list order treated as identity");
   check(Arrays.equals(prepared.solid.triangleCoordinates(),plan.prepare(id("solid"),height,reordered).solid.triangleCoordinates()),"Reordering changed source geometry");
   LoopExtrusionFeature edited=prepared.feature.withDistance(height*2);TopologySnapshot before=TopologySnapshot.from(prepared.solid),after=TopologySnapshot.from(edited.asBRep(lines));
   for(TopologySnapshot.Kind kind:TopologySnapshot.Kind.values())check(before.ids(kind).equals(after.ids(kind)),"Height edit replaced persistent IDs");
   String face=before.ids(TopologySnapshot.Kind.FACE).iterator().next();TopologySnapshot.Reference reference=before.reference(TopologySnapshot.Kind.FACE,face);
   check(after.resolveExact(reference)==null&&after.resolvePersistent(reference)!=null,"Stale geometry reference reused");
   ClosedSketchExtrusion.Segment first=lines.get(selected);
   Map<String,ClosedSketchExtrusion.Segment> changed=new LinkedHashMap<>(lines);
   changed.put(selected,new ClosedSketchExtrusion.Segment(p,first.ax+1e-7,first.ay,first.bx,first.by));
   check(!plan.isCurrent(changed),"Double coordinate edit hidden by float rounding");invalid(()->plan.prepare(id("solid"),height,changed));
   Map<String,ClosedSketchExtrusion.Segment> missing=new LinkedHashMap<>(lines);missing.remove(selected);
   invalid(()->plan.prepare(id("solid"),height,missing));
   Map<String,ClosedSketchExtrusion.Segment> extra=new LinkedHashMap<>(lines);extra.put(id("new"),new ClosedSketchExtrusion.Segment(p,first.ax,first.ay,first.ax-2,first.ay));
   invalid(()->plan.prepare(id("solid"),height,extra));
   invalid(()->plan.prepare(selected,height,lines));invalid(()->plan.prepare(id("solid"),0,lines));invalid(()->plan.prepare(id("solid"),Double.NaN,lines));
   invalid(()->plan.prepare(id("solid"),10001,lines));
   check(plan.isCurrent(lines),"Failed proposal mutated the captured selection");
   check(Arrays.equals(prepared.solid.triangleCoordinates(),plan.prepare(id("solid"),height,lines).solid.triangleCoordinates()),"Failure changed later valid output");
  }
  Map<String,ClosedSketchExtrusion.Segment> mixed=profile(0,false);String selected=mixed.keySet().iterator().next();
  mixed.put(id("other-plane"),new ClosedSketchExtrusion.Segment(1,0,0,20,0));invalid(()->ClosedProfileExtrusionPlan.capture(mixed,selected));
  invalid(()->ClosedProfileExtrusionPlan.capture(Collections.emptyMap(),selected));
  Map<String,ClosedSketchExtrusion.Segment> bad=profile(0,false);bad.put("not-a-uuid",new ClosedSketchExtrusion.Segment(0,100,0,120,0));
  invalid(()->ClosedProfileExtrusionPlan.capture(bad,selected));
  System.out.println("ClosedProfileExtrusionPlanTest: OK");
 }
}
