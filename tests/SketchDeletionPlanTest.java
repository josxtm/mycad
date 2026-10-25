import es.josxtm.mycad.cad.*;
import java.util.*;
public final class SketchDeletionPlanTest {
 static void ok(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 static void reject(Runnable fn,String message){
  try{fn.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError(message);
 }
 public static void main(String[] args){
  List<SketchDeletionPlan.BodyLink> source=Arrays.asList(
   new SketchDeletionPlan.BodyLink("a","sketch1"),
   new SketchDeletionPlan.BodyLink("b","sketch2"),
   new SketchDeletionPlan.BodyLink("c","sketch1"),
   new SketchDeletionPlan.BodyLink("d",""));
  SketchDeletionPlan.Plan deletion=SketchDeletionPlan.inspect("sketch1",source);
  ok(deletion.stillMatches(source),"Live list must match original snapshot");
  ok(deletion.dependentBodyIds.equals(Arrays.asList("a","c")),"Dependency order");
  List<SketchDeletionPlan.BodyLink> keep=deletion.after(SketchDeletionPlan.Mode.KEEP_SOLIDS);
  ok(keep.size()==4&&keep.get(0).sourceSketchId.isEmpty()&&keep.get(2).sourceSketchId.isEmpty(),
    "Detach linked bodies, retaining all IDs");
  ok(keep.get(0).bodyId.equals("a")&&keep.get(2).bodyId.equals("c"),"Preserve UUIDs");
  ok(keep.get(1).sourceSketchId.equals("sketch2")&&keep.get(3).sourceSketchId.isEmpty(),
    "Unrelated and independent bodies unchanged");
  List<SketchDeletionPlan.BodyLink> cascade=deletion.after(SketchDeletionPlan.Mode.CASCADE_DELETE);
  ok(cascade.size()==2&&cascade.get(0).bodyId.equals("b")&&cascade.get(1).bodyId.equals("d"),
    "Cascade only affected solids");
  ok(source.get(0).sourceSketchId.equals("sketch1"),"Pure planning must not mutate original");
  List<SketchDeletionPlan.BodyLink> reordered=Arrays.asList(source.get(1),source.get(0),source.get(2),source.get(3));
  ok(!deletion.stillMatches(reordered),"Reject stale plan after body reorder");
  ok(!deletion.stillMatches(keep),"Reject stale plan after detachment");
  ok(SketchDeletionPlan.inspect("unused",source).dependentBodyIds.isEmpty(),
    "Unlinked sketch may be deleted independently");
  reject(()->SketchDeletionPlan.inspect("sketch1",Arrays.asList(source.get(0),source.get(0))),
    "Duplicate UUID accepted");
  reject(()->deletion.after(null),"Missing operation accepted");
  boolean immutable=false;try{keep.clear();}catch(UnsupportedOperationException expected){immutable=true;}
  ok(immutable,"Mutation of output plan allowed");
  System.out.println("SketchDeletionPlanTest: OK");
 }
}
