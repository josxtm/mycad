import es.josxtm.mycad.cad.*;
import es.josxtm.mycad.MultiBoxUnion;
import java.util.*;

/** Regression for ID-based feature dependencies after editing and reordering. */
public final class StableFeatureDependenciesTest {
 static void assertTrue(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static void illegal(Runnable action,String m){
  boolean thrown=false;try{action.run();}catch(IllegalArgumentException ex){thrown=true;}
  assertTrue(thrown,m);
 }
 static FaceExtrusionChain sample(int x){
  FaceExtrusionChain c=new FaceExtrusionChain(20,20,10,x,0,0);
  c.append(BoxFaceSelection.Face.Z_MAX,10);
  return c;
 }
 public static void main(String[] args){
  FaceExtrusionChain source=sample(0),unrelated=sample(80);
  String chainUuid=source.id,stepUuid=source.history().get(0).id;
  source.update(0,14);
  assertTrue(source.id.equals(chainUuid),"Chain identity modified");
  assertTrue(source.history().get(0).id.equals(stepUuid),"Editing a step changed its UUID");
  source.appendStep(new FaceExtrusionChain.Step(BoxFaceSelection.Face.X_MAX,3));
  assertTrue(!source.history().get(1).id.equals(stepUuid),"Duplicate step ID");
  illegal(()->source.appendStep(source.history().get(0)),"Duplicate step restored");
  ChainBooleanFeature linked=new ChainBooleanFeature(0,source.id,
   MultiBoxUnion.Operation.CHAIN_DIFFERENCE,0,0,0,7,7,10);
  List<FaceExtrusionChain> normal=Arrays.asList(source,unrelated);
  float[] before=linked.regenerate(normal);
  assertTrue(before.length>0,"Boolean must have output");
  List<FaceExtrusionChain> reordered=Arrays.asList(unrelated,source);
  assertTrue(linked.resolvedChainIndex(reordered)==1,"Wrong dependent chain on reorder");
  assertTrue(Arrays.equals(before,linked.regenerate(reordered)),"Reordered chains changed dependent geometry");
  ChainBooleanFeature edited=linked.withCutter(1,2,0,5,5,10)
   .withOperation(MultiBoxUnion.Operation.CHAIN_INTERSECTION);
  assertTrue(edited.chainId.equals(source.id),"Editing Boolean severed source dependency");
  assertTrue(edited.regenerate(reordered).length>0,"Linked intersection missing");
  illegal(()->linked.regenerate(Collections.singletonList(unrelated)),"Deleted chain silently redirected");
  ChainBooleanFeature legacy=new ChainBooleanFeature(0,MultiBoxUnion.Operation.CHAIN_DIFFERENCE,0,0,0,7,7,10);
  assertTrue(legacy.chainId==null&&legacy.resolvedChainIndex(normal)==0,"Backward compatible legacy index");
  ChainBooleanFeature migrated=legacy.linkedTo(source.id);
  assertTrue(migrated.resolvedChainIndex(reordered)==1,"Legacy migration not bound to source");
  assertTrue(ChainDependencyReport.inspect(normal,Collections.singletonList(linked)).isValid(),"Valid graph");
  assertTrue(!ChainDependencyReport.inspect(Collections.singletonList(unrelated),
    Collections.singletonList(linked)).isValid(),"Missing chain not caught");
  FaceExtrusionChain duplicate=new FaceExtrusionChain(source.id,20,20,10,100,0,0);
  assertTrue(!ChainDependencyReport.inspect(Arrays.asList(source,duplicate),
    Collections.emptyList()).isValid(),"Duplicate chain UUID accepted");
  FaceExtrusionChain duplicatedStep=new FaceExtrusionChain(20,20,10,120,0,0);
  duplicatedStep.appendStep(source.history().get(0));
  assertTrue(!ChainDependencyReport.inspect(Arrays.asList(source,duplicatedStep),
    Collections.emptyList()).isValid(),"Duplicate step UUID accepted");
  FaceExtrusionChain copy=new FaceExtrusionChain(source.id,20,20,10,0,0,0);
  for(FaceExtrusionChain.Step step:source.history())copy.appendStep(step);
  assertTrue(copy.history().get(0).id.equals(stepUuid),"History copy changed step UUID");
  System.out.println("StableFeatureDependenciesTest: OK");
 }
}