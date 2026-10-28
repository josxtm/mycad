import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Persisted source identities -> topology -> exactly the saved preview. */
public final class LinkedSteppedRecipeTest {
 private static String id(String seed){
  return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
 }
 private static LoopExtrusionFeature rectangle(String seed,int plane,double height,
     double x1,double y1,double x2,double y2,Map<String,ClosedSketchExtrusion.Segment> lines){
  double[][] points={{x1,y1},{x2,y1},{x2,y2},{x1,y2}};
  List<String> ids=new ArrayList<>();
  for(int i=0;i<4;i++){
   String edge=id(seed+i);ids.add(edge);
   double[] a=points[i],b=points[(i+1)%4];
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id(seed),ids.get(0),plane,height,ids);
 }
 private static void check(boolean condition){if(!condition)throw new AssertionError();}
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted stale or missing source");
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1})
   for(LinkedConvexIntersection.Operation operation:new LinkedConvexIntersection.Operation[]{
       LinkedConvexIntersection.Operation.UNION_STEPPED,
       LinkedConvexIntersection.Operation.DIFFERENCE_STEPPED}){
    Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
    LoopExtrusionFeature a=rectangle("a",plane,6*sign,0,0,4,4,lines);
    boolean union=operation==LinkedConvexIntersection.Operation.UNION_STEPPED;
    LoopExtrusionFeature b=union?rectangle("b",plane,3*sign,2,0,6,4,lines):
     rectangle("b",plane,3*sign,1,1,3,3,lines);
    Map<String,LoopExtrusionFeature> sources=new LinkedHashMap<>();
    sources.put(a.id,a);sources.put(b.id,b);
    LinkedConvexIntersection recipe=new LinkedConvexIntersection(id("recipe"),a.id,b.id,operation);
    VariableHeightOrthogonalBRep shell=recipe.regenerateStepped(sources,lines);
    check(Arrays.equals(shell.triangles(),recipe.regenerateTriangles(sources,lines)));
    check(shell.id().equals(recipe.regenerateStepped(sources,lines).id()));
    check(Math.abs(shell.volume()-(union?120:84))<1e-6);
    SteppedCoplanarPatchTopology.build(shell);
    LinkedBooleanRegenerationPlan.Plan plan=LinkedBooleanRegenerationPlan.build(
     Collections.singletonList(recipe),sources,lines);
    check(Arrays.equals(shell.triangles(),plan.publishIfCurrent(
     Collections.singletonList(recipe),sources,lines).get(recipe.id)));
    sources.put(a.id,a.withDistance(7*sign));
    reject(()->plan.publishIfCurrent(Collections.singletonList(recipe),sources,lines));
    check(!shell.id().equals(recipe.regenerateStepped(sources,lines).id()));
    check(recipe.id.equals(id("recipe"))); // persistent owner survives regeneration
    sources.remove(b.id);reject(()->recipe.regenerateStepped(sources,lines));
   }
  System.out.println("LinkedSteppedRecipeTest: OK");
 }
}
