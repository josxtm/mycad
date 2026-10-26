import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class LinkedMultiShellBooleanTest {
 private static final String A="b6b3f401-0000-4000-8000-000000000001",
                             B="b6b3f401-0000-4000-8000-000000000002",
                             RESULT="b6b3f401-0000-4000-8000-000000000099";
 private static void check(boolean ok,String why){
  if(!ok)throw new AssertionError(why);
 }
 private static void near(double got,double expected,String why){
  if(Math.abs(got-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(why+": "+got+" vs "+expected);
 }
 private static void reject(Runnable r,String why){
  try{r.run();}catch(IllegalArgumentException allowed){return;}
  throw new AssertionError(why);
 }
 private static LoopExtrusionFeature feature(String id,int p,double height,
     double[][] ring,Map<String,ClosedSketchExtrusion.Segment> lines){
  ArrayList<String> ids=new ArrayList<>();
  for(int i=0;i<ring.length;i++){
   String name=UUID.nameUUIDFromBytes((id+"/line/"+i)
     .getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=ring[i],b=ring[(i+1)%ring.length];
   ids.add(name);
   lines.put(name,new ClosedSketchExtrusion.Segment(p,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),p,height,ids);
 }
 static double[][] rect(double a,double b,double c,double d){
  return new double[][]{{a,b},{c,b},{c,d},{a,d}};
 }
 public static void main(String[] args){
  for(int p=0;p<3;p++)for(double height:new double[]{3,-3}){
   Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
   LoopExtrusionFeature a=feature(A,p,height,rect(0,0,8,4),lines);
   LoopExtrusionFeature b=feature(B,p,height,rect(3,-1,5,5),lines);
   Map<String,LoopExtrusionFeature> bodies=new LinkedHashMap<>();
   bodies.put(A,a);bodies.put(B,b);
   LinkedConvexIntersection operation=new LinkedConvexIntersection(
     RESULT,A,B,LinkedConvexIntersection.Operation.DIFFERENCE_MULTISHELL);
   float[] before=operation.regenerateTriangles(bodies,lines);
   TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(before);
   check(health.isWatertight()&&health.shells==2,
      "Linked Boolean creates two closed independent shells");
   near(health.signedVolume,72,"Linked difference volume");
   check(Arrays.equals(before,operation.regenerateTriangles(bodies,lines)),
      "Deterministic regeneration equal to saved JSON floats");
   Map<String,LoopExtrusionFeature> heights=new LinkedHashMap<>(bodies);
   heights.put(B,b.withDistance(height/2));
   reject(()->operation.regenerateTriangles(heights,lines),
      "Reject mismatched heights before any source edit");
   heights.put(A,a.withDistance(height/2));
   near(TriangleMeshHealth.inspect(operation.regenerateTriangles(heights,lines))
        .signedVolume,36,"Rebuild both source heights atomically");
   Map<String,ClosedSketchExtrusion.Segment> missing=new LinkedHashMap<>(lines);
   missing.remove(a.lineIds().get(0));
   reject(()->operation.regenerateTriangles(bodies,missing),
      "Reject deleting underlying UUID source sketch");
   LinkedConvexIntersection union=new LinkedConvexIntersection(RESULT,A,B,
     LinkedConvexIntersection.Operation.UNION_MULTISHELL);
   // These two source profiles overlap; multi mode must still preserve an
   // exact watertight single result rather than synthesizing a fake second.
   check(TriangleMeshHealth.inspect(union.regenerateTriangles(bodies,lines))
      .isWatertight(),"Multi mode accepts single connected union");
   LinkedConvexIntersection intersect=new LinkedConvexIntersection(RESULT,A,B,
     LinkedConvexIntersection.Operation.INTERSECTION_MULTISHELL);
   check(TriangleMeshHealth.inspect(intersect.regenerateTriangles(bodies,lines))
      .isWatertight(),"Multi mode accepts single intersection");
   check(Arrays.equals(before,operation.regenerateTriangles(bodies,lines)),
     "Regeneration does not mutate previously returned float array");
  }
  check(new LinkedConvexIntersection(RESULT,A,B).operation==
    LinkedConvexIntersection.Operation.INTERSECTION,"Original JSON default unchanged");
  System.out.println("LinkedMultiShellBooleanTest: OK");
 }
}
