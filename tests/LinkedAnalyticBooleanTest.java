import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Regression for saved feature regeneration through the unified B-Rep kernel. */
public final class LinkedAnalyticBooleanTest {
 private static String uuid(String seed){
  return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
 }
 private static LoopExtrusionFeature feature(String seed,int plane,double height,
     double[][] poly,Map<String,ClosedSketchExtrusion.Segment> lines){
  String id=uuid(seed);
  List<String> ids=new ArrayList<>();
  for(int i=0;i<poly.length;i++){
   String edge=uuid(seed+"/line/"+i);
   double[] a=poly[i],b=poly[(i+1)%poly.length];
   ids.add(edge);
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids);
 }
 private static void near(double actual,double expected,String label){
  if(Math.abs(actual-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(label+": "+actual+" != "+expected);
 }
 private static void reject(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Unsafe linked Boolean accepted: "+label);
 }
 private static void verify(float[] mesh,double volume,String label){
  TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(mesh);
  if(!health.isWatertight()||health.shells!=1)
   throw new AssertionError(label+" is not a single closed shell");
  near(health.signedVolume,volume,label+" volume");
 }
 private static double[][] rect(double a,double b,double c,double d){
  return new double[][]{{a,b},{c,b},{c,d},{a,d}};
 }
 public static void main(String[] args){
  double[][] l={{0,0},{4,0},{4,1},{1,1},{1,4},{0,4}};
  double[][] r=rect(2,0,5,3);
  double[][] triangle={{0,0},{4,0},{2,4}};
  double[][] shifted={{1,0},{5,0},{3,4}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   final int p=plane,s=sign;
   Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
   LoopExtrusionFeature a=feature("analytic-a"+p+s,p,s*3,l,lines);
   LoopExtrusionFeature b=feature("analytic-b"+p+s,p,s*3,r,lines);
   Map<String,LoopExtrusionFeature> sources=new LinkedHashMap<>();
   sources.put(a.id,a);sources.put(b.id,b);
   for(LinkedConvexIntersection.Operation op:new LinkedConvexIntersection.Operation[]{
       LinkedConvexIntersection.Operation.UNION_ANALYTIC,
       LinkedConvexIntersection.Operation.INTERSECTION_ANALYTIC,
       LinkedConvexIntersection.Operation.DIFFERENCE_ANALYTIC}){
    LinkedConvexIntersection linked=new LinkedConvexIntersection(
        uuid("analytic-result"+p+s+op),a.id,b.id,op);
    double area=op==LinkedConvexIntersection.Operation.UNION_ANALYTIC?14:
        op==LinkedConvexIntersection.Operation.INTERSECTION_ANALYTIC?2:5;
    float[] original=linked.regenerateTriangles(sources,lines);
    verify(original,area*3,op+" original");
    if(!Arrays.equals(original,linked.regenerateTriangles(sources,lines)))
     throw new AssertionError(op+" nondeterministic regeneration");
    Map<String,LoopExtrusionFeature> changed=new LinkedHashMap<>(sources);
    changed.put(a.id,a.withDistance(s*2));
    reject(()->linked.regenerateTriangles(changed,lines),op+" partial height edit");
    changed.put(b.id,b.withDistance(s*2));
    verify(linked.regenerateTriangles(changed,lines),area*2,op+" coherent height edit");
    Map<String,ClosedSketchExtrusion.Segment> moved=new LinkedHashMap<>(lines);
    double[][] shiftedRect=rect(1,0,5,3);
    for(int i=0;i<shiftedRect.length;i++){
     double[] v=shiftedRect[i],w=shiftedRect[(i+1)%shiftedRect.length];
     moved.put(b.lineIds().get(i),new ClosedSketchExtrusion.Segment(
         p,v[0],v[1],w[0],w[1]));
    }
    double movedArea=op==LinkedConvexIntersection.Operation.UNION_ANALYTIC?16:
        op==LinkedConvexIntersection.Operation.INTERSECTION_ANALYTIC?3:4;
    verify(linked.regenerateTriangles(sources,moved),movedArea*3,op+" sketch edit");
    if(!Arrays.equals(original,linked.regenerateTriangles(sources,lines)))
     throw new AssertionError(op+" mutated source mesh");
    Map<String,LoopExtrusionFeature> missing=new LinkedHashMap<>(sources);
    missing.remove(b.id);
    reject(()->linked.regenerateTriangles(missing,lines),op+" missing source");
   }
   Map<String,ClosedSketchExtrusion.Segment> convexLines=new LinkedHashMap<>();
   LoopExtrusionFeature c=feature("analytic-c"+p+s,p,s*3,triangle,convexLines);
   LoopExtrusionFeature d=feature("analytic-d"+p+s,p,s*2,shifted,convexLines);
   Map<String,LoopExtrusionFeature> convexSources=new LinkedHashMap<>();
   convexSources.put(c.id,c);convexSources.put(d.id,d);
   LinkedConvexIntersection intersection=new LinkedConvexIntersection(
       uuid("analytic-intersection"+p+s),c.id,d.id,
       LinkedConvexIntersection.Operation.INTERSECTION_ANALYTIC);
   double expected=ConvexPrismBRepIntersection.intersect(
       c.asBRep(convexLines),d.asBRep(convexLines)).volume;
   verify(intersection.regenerateTriangles(convexSources,convexLines),expected,
       "unequal-height slanted intersection");
   LinkedConvexIntersection difference=new LinkedConvexIntersection(
       uuid("analytic-difference"+p+s),c.id,d.id,
       LinkedConvexIntersection.Operation.DIFFERENCE_ANALYTIC);
   reject(()->difference.regenerateTriangles(convexSources,convexLines),
       "unsupported unequal-height slanted difference");
  }
  if(new LinkedConvexIntersection(uuid("legacy-result"),uuid("legacy-a"),
      uuid("legacy-b")).operation!=LinkedConvexIntersection.Operation.INTERSECTION)
   throw new AssertionError("Legacy default operation changed");
  System.out.println("LinkedAnalyticBooleanTest: OK");
 }
}
