import es.josxtm.mycad.cad.*;
import java.util.*;
public final class ParametricFeatureGraphTest {
 static void yes(boolean cond,String message){if(!cond)throw new AssertionError(message);}
 static void rejects(Runnable action,String message){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError(message);
 }
 public static void main(String[] args){
  ParametricFeatureGraph.Builder graph=new ParametricFeatureGraph.Builder();
  graph.add("sk1",ParametricFeatureGraph.Kind.SKETCH,"Sketch");
  graph.add("ch1",ParametricFeatureGraph.Kind.CHAIN,"Chain");
  graph.add("b1",ParametricFeatureGraph.Kind.BODY,"Body1");
  graph.add("b2",ParametricFeatureGraph.Kind.BODY,"Body2");
  graph.add("f1",ParametricFeatureGraph.Kind.BOOLEAN,"Cut");
  graph.dependsOn("b1","sk1").dependsOn("b2","sk1").dependsOn("f1","ch1");
  ParametricFeatureGraph built=graph.build();
  yes(built.size()==5,"Missing nodes");
  yes(built.executionOrder().indexOf(built.find("sk1"))<
      built.executionOrder().indexOf(built.find("b1")),"Parent after dependent");
  List<ParametricFeatureGraph.Node> descendants=built.affectedBy("sk1");
  yes(descendants.size()==2&&descendants.get(0).id.equals("b1")&&
      descendants.get(1).id.equals("b2"),"Sketch invalidation");
  yes(built.affectedBy("ch1").size()==1&&built.affectedBy("ch1").get(0).id.equals("f1"),
      "Boolean invalidation");
  yes(built.affectedBy("b1").isEmpty(),"Unrelated feature impacted");
  yes(built.find("nonexistent")==null,"Absent feature");
  rejects(()->built.affectedBy("nonexistent"),"Missing source not rejected");
  rejects(()->graph.add("sk1",ParametricFeatureGraph.Kind.BODY,"Duplicate"),"Duplicate id");
  rejects(()->graph.dependsOn("b1","missing"),"Orphan dependency");
  rejects(()->graph.dependsOn("b1","b1"),"Self-cycle allowed");
  rejects(()->new ParametricFeatureGraph.Builder()
   .add("a",ParametricFeatureGraph.Kind.BODY,"a")
   .add("b",ParametricFeatureGraph.Kind.CHAIN,"b")
   .dependsOn("a","b").dependsOn("b","a").build(),"Cycle allowed");
  // Multi-level impact is transitively ordered.
  ParametricFeatureGraph chain=new ParametricFeatureGraph.Builder()
   .add("a",ParametricFeatureGraph.Kind.SKETCH,"a")
   .add("b",ParametricFeatureGraph.Kind.BODY,"b")
   .add("c",ParametricFeatureGraph.Kind.CHAIN,"c")
   .add("d",ParametricFeatureGraph.Kind.BOOLEAN,"d")
   .dependsOn("b","a").dependsOn("c","b").dependsOn("d","c").build();
  List<ParametricFeatureGraph.Node> impacted=chain.affectedBy("a");
  yes(impacted.size()==3&&impacted.get(0).id.equals("b")&&
      impacted.get(1).id.equals("c")&&impacted.get(2).id.equals("d"),"Transitive dependency order");
  boolean immutable=false;
  try{impacted.clear();}catch(UnsupportedOperationException expected){immutable=true;}
  yes(immutable,"Impact planning returned mutable collection");
  System.out.println("ParametricFeatureGraphTest: OK");
 }
}
