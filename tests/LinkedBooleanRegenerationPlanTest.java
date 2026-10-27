import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** All-or-nothing, deterministic regeneration of several linked B-Rep features. */
public final class LinkedBooleanRegenerationPlanTest {
 private static String id(String seed){
  return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
 }
 private static LoopExtrusionFeature feature(String seed,int plane,double h,
     double[][] corners,Map<String,ClosedSketchExtrusion.Segment> lines){
  ArrayList<String> ids=new ArrayList<>();
  for(int i=0;i<corners.length;i++){
   String edge=id(seed+"/edge/"+i);
   double[] a=corners[i],b=corners[(i+1)%corners.length];
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
   ids.add(edge);
  }
  return new LoopExtrusionFeature(id(seed),ids.get(0),plane,h,ids);
 }
 private static void ok(boolean condition,String label){
  if(!condition)throw new AssertionError(label);
 }
 private static void reject(Runnable runnable,String label){
  try{runnable.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Unsafe batch accepted: "+label);
 }
 public static void main(String[] args){
  double[][] l={{0,0},{4,0},{4,1},{1,1},{1,4},{0,4}};
  double[][] r={{2,0},{5,0},{5,3},{2,3}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   final int p=plane,s=sign;
   Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
   LoopExtrusionFeature a=feature("batch-a"+p+s,p,s*3,l,lines);
   LoopExtrusionFeature b=feature("batch-b"+p+s,p,s*3,r,lines);
   Map<String,LoopExtrusionFeature> features=new LinkedHashMap<>();
   features.put(a.id,a);features.put(b.id,b);
   LinkedConvexIntersection union=new LinkedConvexIntersection(
       id("batch-union"+p+s),a.id,b.id,
       LinkedConvexIntersection.Operation.UNION_ANALYTIC);
   LinkedConvexIntersection intersection=new LinkedConvexIntersection(
       id("batch-intersection"+p+s),a.id,b.id,
       LinkedConvexIntersection.Operation.INTERSECTION_ANALYTIC);
   LinkedBooleanRegenerationPlan.Plan first=LinkedBooleanRegenerationPlan.build(
       Arrays.asList(union,intersection),features,lines);
   LinkedBooleanRegenerationPlan.Plan reversed=LinkedBooleanRegenerationPlan.build(
       Arrays.asList(intersection,union),features,lines);
   // The preflight is valid only for the exact referenced source revision.
   ok(first.isCurrent(features,lines)&&reversed.isCurrent(features,lines),
       "unchanged source snapshot remains publishable");
   first.requireCurrent(features,lines);
   // The saved Boolean recipe is also part of the publication revision.
   // The older two-map check remains compatible with existing callers.
   java.util.List<LinkedConvexIntersection> recipes=Arrays.asList(union,intersection);
   ok(first.isCurrent(recipes,features,lines),
       "unchanged Boolean recipes and geometry remain current");
   first.requireCurrent(recipes,features,lines);
   ok(first.isCurrent(Arrays.asList(intersection,union),features,lines),
       "recipe collection order is irrelevant");
   LinkedConvexIntersection changedOperation=new LinkedConvexIntersection(
       union.id,a.id,b.id,LinkedConvexIntersection.Operation.DIFFERENCE_ANALYTIC);
   ok(!first.isCurrent(Arrays.asList(changedOperation,intersection),features,lines),
       "changing Boolean operation invalidates the staged result");
   reject(()->first.requireCurrent(Arrays.asList(changedOperation,intersection),
       features,lines),"stale Boolean operation cannot be published");
   LinkedConvexIntersection swappedOperands=new LinkedConvexIntersection(
       union.id,b.id,a.id,union.operation);
   ok(!first.isCurrent(Arrays.asList(swappedOperands,intersection),features,lines),
       "operand order is part of saved Boolean topology");
   ok(!first.isCurrent(Collections.singletonList(union),features,lines),
       "removing a Boolean recipe invalidates the batch");
   LinkedConvexIntersection addedRecipe=new LinkedConvexIntersection(
       id("extra-recipe"+p+s),a.id,b.id,
       LinkedConvexIntersection.Operation.INTERSECTION_ANALYTIC);
   ok(!first.isCurrent(Arrays.asList(union,intersection,addedRecipe),features,lines),
       "adding a Boolean recipe invalidates the batch");
   ok(!first.isCurrent(Arrays.asList(union,union),features,lines),
       "duplicate Boolean IDs are not a valid current recipe set");
   ok(first.isCurrent(recipes,features,lines),
       "failed recipe checks cannot mutate the successful plan");
   Map<String,ClosedSketchExtrusion.Segment> unrelatedLines=new LinkedHashMap<>(lines);
   unrelatedLines.put(id("unrelated-line"+p+s),
       new ClosedSketchExtrusion.Segment(p,20,20,21,20));
   ok(first.isCurrent(features,unrelatedLines),
       "unrelated sketch edits must not invalidate a Boolean batch");
   Map<String,LoopExtrusionFeature> unrelatedFeatures=new LinkedHashMap<>(features);
   unrelatedFeatures.put(id("unrelated-feature"+p+s),
       feature("unrelated-feature"+p+s,p,s*2,
           new double[][]{{20,20},{21,20},{20,21}},unrelatedLines));
   ok(first.isCurrent(unrelatedFeatures,unrelatedLines),
       "unrelated extrusions must not invalidate a Boolean batch");
   Map<String,LoopExtrusionFeature> staleHeight=new LinkedHashMap<>(features);
   staleHeight.put(a.id,a.withDistance(s*2));
   ok(!first.isCurrent(staleHeight,lines),
       "changed extrusion height invalidates staged B-Rep");
   ok(!first.isCurrent(recipes,staleHeight,lines),
       "full guard rejects stale extrusion height");
   reject(()->first.requireCurrent(staleHeight,lines),
       "cannot publish stale extrusion batch");
   Map<String,ClosedSketchExtrusion.Segment> staleLines=new LinkedHashMap<>(lines);
   String sourceLine=a.lineIds().get(0);
   ClosedSketchExtrusion.Segment original=lines.get(sourceLine);
   staleLines.put(sourceLine,new ClosedSketchExtrusion.Segment(
       original.plane,original.ax+0.00001,original.ay,
       original.bx+0.00001,original.by));
   ok(!first.isCurrent(features,staleLines),
       "sub-millimetre source edit invalidates staged B-Rep");
   reject(()->first.requireCurrent(features,staleLines),
       "cannot publish stale sketch batch");
   staleLines.remove(sourceLine);
   ok(!first.isCurrent(features,staleLines),
       "deleted source line invalidates staged B-Rep");
   Map<String,LoopExtrusionFeature> reordered=new LinkedHashMap<>(features);
   ArrayList<String> reversedLines=new ArrayList<>(a.lineIds());
   Collections.reverse(reversedLines);
   reordered.put(a.id,new LoopExtrusionFeature(a.id,a.selectedLineId,
       a.plane,a.distance,reversedLines));
   ok(!first.isCurrent(reordered,lines),
       "changed source edge order invalidates saved B-Rep topology");
   ok(first.isCurrent(features,lines),
       "stale checks must not modify original sources or successful plan");
   ok(first.size()==2&&first.triangleCount()>0,"two rebuilt closed solids");
   ok(new ArrayList<>(first.featureIds()).equals(new ArrayList<>(reversed.featureIds())),
       "stable ordering");
   for(String key:first.featureIds()){
    float[] expected=first.triangles(key);
    ok(Arrays.equals(expected,reversed.triangles(key)),"stable mesh");
    ok(TriangleMeshHealth.inspect(expected).isWatertight(),"closed shell");
    expected[0]=Float.NaN;
    ok(Float.isFinite(first.triangles(key)[0]),"defensive copy");
   }
   // Preflight must reject open, degenerate and inside-out triangle soups,
   // not just finite coordinates. The last successful plan remains immutable.
   reject(()->LinkedBooleanRegenerationPlan.requireClosedMesh(
       new float[]{0,0,0,1,0,0,0,1,0}),"open triangle");
   reject(()->LinkedBooleanRegenerationPlan.requireClosedMesh(
       new float[]{0,0,0,1,0,0,2,0,0}),"degenerate triangle");
   float[] insideOut=first.triangles(union.id);
   for(int k=0;k<insideOut.length;k+=9)
    for(int axis=0;axis<3;axis++){
     float tmp=insideOut[k+3+axis];
     insideOut[k+3+axis]=insideOut[k+6+axis];
     insideOut[k+6+axis]=tmp;
    }
   reject(()->LinkedBooleanRegenerationPlan.requireClosedMesh(insideOut),
       "inward-facing closed solid");
   // A positive aggregate volume must not mask an inverted small shell.
   float[] positive=first.triangles(union.id);
   float[] invertedSmall=first.triangles(intersection.id);
   for(int k=0;k<invertedSmall.length;k+=3)
    for(int axis=0;axis<3;axis++)invertedSmall[k+axis]+=1000;
   for(int k=0;k<invertedSmall.length;k+=9)
    for(int axis=0;axis<3;axis++){
     float tmp=invertedSmall[k+3+axis];
     invertedSmall[k+3+axis]=invertedSmall[k+6+axis];
     invertedSmall[k+6+axis]=tmp;
    }
   float[] mixedShells=Arrays.copyOf(positive,positive.length+invertedSmall.length);
   System.arraycopy(invertedSmall,0,mixedShells,positive.length,invertedSmall.length);
   TriangleMeshHealth.Report mixedHealth=TriangleMeshHealth.inspect(mixedShells);
   ok(mixedHealth.isWatertight()&&mixedHealth.shells==2&&
      mixedHealth.signedVolume>0&&mixedHealth.negativeVolumeShells==1,
      "mixed orientation is hidden by positive total volume");
   reject(()->LinkedBooleanRegenerationPlan.requireClosedMesh(mixedShells),
       "inverted disconnected shell in otherwise positive multi-shell result");
   ok(TriangleMeshHealth.inspect(first.triangles(union.id)).isWatertight(),
       "failed validation must not corrupt the saved plan");
   // Disconnected, individually closed shells are valid Boolean results.
   Map<String,ClosedSketchExtrusion.Segment> multiLines=new LinkedHashMap<>();
   LoopExtrusionFeature wide=feature("wide"+p+s,p,s*3,
       new double[][]{{0,0},{8,0},{8,4},{0,4}},multiLines);
   LoopExtrusionFeature strip=feature("strip"+p+s,p,s*3,
       new double[][]{{3,-1},{5,-1},{5,5},{3,5}},multiLines);
   Map<String,LoopExtrusionFeature> multiFeatures=new LinkedHashMap<>();
   multiFeatures.put(wide.id,wide);multiFeatures.put(strip.id,strip);
   LinkedConvexIntersection split=new LinkedConvexIntersection(
       id("split"+p+s),wide.id,strip.id,
       LinkedConvexIntersection.Operation.DIFFERENCE_MULTISHELL);
   LinkedBooleanRegenerationPlan.Plan multi=LinkedBooleanRegenerationPlan.build(
       Collections.singletonList(split),multiFeatures,multiLines);
   TriangleMeshHealth.Report splitHealth=TriangleMeshHealth.inspect(multi.triangles(split.id));
   ok(splitHealth.isWatertight()&&splitHealth.shells==2,
       "two independent closed B-Rep shells remain supported");
   boolean immutable=false;
   try{first.featureIds().clear();}
   catch(UnsupportedOperationException expected){immutable=true;}
   ok(immutable,"immutable feature ids");
   reject(()->first.triangles(id("unknown")),"unknown feature");
   reject(()->LinkedBooleanRegenerationPlan.build(
       Arrays.asList(union,union),features,lines),"duplicate IDs");
   reject(()->LinkedBooleanRegenerationPlan.build(
       Arrays.asList(union,null),features,lines),"null Boolean");
   Map<String,LoopExtrusionFeature> missing=new LinkedHashMap<>(features);
   missing.remove(b.id);
   reject(()->LinkedBooleanRegenerationPlan.build(
       Arrays.asList(union,intersection),missing,lines),"missing source");
   Map<String,LoopExtrusionFeature> partial=new LinkedHashMap<>(features);
   partial.put(b.id,b.withDistance(s*2));
   reject(()->LinkedBooleanRegenerationPlan.build(
       Arrays.asList(union,intersection),partial,lines),"partial height edit");
   // A failed preflight must leave the old plan and source maps untouched.
   ok(first.size()==2&&features.get(b.id)==b,"old plan survives failure");
   Map<String,LoopExtrusionFeature> coherent=new LinkedHashMap<>(features);
   coherent.put(a.id,a.withDistance(s*2));
   coherent.put(b.id,b.withDistance(s*2));
   LinkedBooleanRegenerationPlan.Plan updated=LinkedBooleanRegenerationPlan.build(
       Arrays.asList(union,intersection),coherent,lines);
   ok(updated.size()==2&&updated.triangleCount()>0,"coherent edit accepted");
   ok(!Arrays.equals(first.triangles(union.id),updated.triangles(union.id)),
       "regenerated geometry reflects edit");
   // A complete proposed line edit must rebuild all dependent operations
   // against the same coordinates, without modifying the old saved snapshot.
   LinkedConvexIntersection difference=new LinkedConvexIntersection(
       id("batch-difference"+p+s),a.id,b.id,
       LinkedConvexIntersection.Operation.DIFFERENCE_ANALYTIC);
   Map<String,ClosedSketchExtrusion.Segment> movedLines=new LinkedHashMap<>(lines);
   for(String lineId:b.lineIds()){
    ClosedSketchExtrusion.Segment edge=lines.get(lineId);
    movedLines.put(lineId,new ClosedSketchExtrusion.Segment(
        edge.plane,edge.ax+0.5,edge.ay,edge.bx+0.5,edge.by));
   }
   LinkedBooleanRegenerationPlan.Plan moved=LinkedBooleanRegenerationPlan.build(
       Arrays.asList(difference,intersection,union),features,movedLines);
   ok(moved.size()==3,"three dependent Booleans rebuilt together");
   double factor=Math.abs(s*3);
   double[] volumes={14.5*factor,1.5*factor,5.5*factor};
   String[] ids={union.id,intersection.id,difference.id};
   for(int k=0;k<ids.length;k++){
    TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(moved.triangles(ids[k]));
    ok(health.isWatertight(),"edited Boolean remains watertight");
    ok(Math.abs(health.signedVolume-volumes[k])<1e-3,
        "all Boolean volumes use proposed source coordinates");
   }
   ok(!Arrays.equals(first.triangles(union.id),moved.triangles(union.id)),
       "line edit changes Boolean geometry");
   ok(Arrays.equals(first.triangles(union.id),
       LinkedBooleanRegenerationPlan.build(
           Arrays.asList(union,intersection),features,lines).triangles(union.id)),
       "old source geometry and saved plan remain unchanged");
   ok(LinkedBooleanRegenerationPlan.build(Collections.emptyList(),features,lines)
       .size()==0,"empty dependency set");
  }
  System.out.println("LinkedBooleanRegenerationPlanTest: OK");
 }
}
