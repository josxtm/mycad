import es.josxtm.mycad.cad.*;
import es.josxtm.mycad.MultiBoxUnion;
import java.util.*;
public final class ChainBooleanRebuildCacheTest {
 static void yes(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void rejects(Runnable work,String why){
  boolean thrown=false;try{work.run();}catch(IllegalArgumentException e){thrown=true;}
  yes(thrown,why);
 }
 public static void main(String[] args){
  FaceExtrusionChain a=new FaceExtrusionChain(10,10,10,0,0,0);
  FaceExtrusionChain b=new FaceExtrusionChain(10,10,10,80,0,0);
  a.append(BoxFaceSelection.Face.X_MAX,10);
  b.append(BoxFaceSelection.Face.X_MAX,10);
  ChainBooleanFeature cutA=new ChainBooleanFeature(0,a.id,MultiBoxUnion.Operation.CHAIN_DIFFERENCE,5,0,0,4,4,10);
  ChainBooleanFeature intersectA=new ChainBooleanFeature(0,a.id,MultiBoxUnion.Operation.CHAIN_INTERSECTION,5,0,0,8,6,10);
  ChainBooleanFeature cutB=new ChainBooleanFeature(1,b.id,MultiBoxUnion.Operation.CHAIN_DIFFERENCE,85,0,0,4,4,10);
  List<FaceExtrusionChain> chains=Arrays.asList(a,b);
  List<ChainBooleanFeature> features=Arrays.asList(cutA,intersectA,cutB);
  ChainBooleanRebuildCache.Result first=ChainBooleanRebuildCache.rebuild(chains,features,null);
  yes(first.state.count()==3&&first.rebuiltFeatureIds.size()==3&&first.reused==0,"Initial rebuild");
  yes(first.state.totalTriangles()>0,"Empty combined features");
  yes(first.state.matches(chains,features),"Newly generated snapshot should match inputs");
  yes(first.state.combinedPreview().length==first.state.totalTriangles()*9,"Preview triangles");
  float[] original=first.state.mesh(cutA.id);
  original[0]=Float.NaN;
  yes(Float.isFinite(first.state.mesh(cutA.id)[0]),"External mutation corrupts snapshot");
  ChainBooleanRebuildCache.Result same=ChainBooleanRebuildCache.rebuild(chains,features,first.state);
  yes(same.rebuiltFeatureIds.isEmpty()&&same.reused==3,"Identical run should reuse all");
  a.update(0,20);
  ChainBooleanRebuildCache.Result changed=ChainBooleanRebuildCache.rebuild(chains,features,same.state);
  yes(!same.state.matches(chains,features),"Prior snapshot should be stale after editing chain");
  yes(changed.rebuiltFeatureIds.size()==2&&changed.reused==1,"Only source A dependents rebuild");
  yes(changed.rebuiltFeatureIds.contains(cutA.id)&&changed.rebuiltFeatureIds.contains(intersectA.id),
    "Wrong dependency invalidated");
  yes(Arrays.equals(changed.state.mesh(cutB.id),same.state.mesh(cutB.id)),"Unchanged source B geometry");
  ChainBooleanRebuildCache.Result reordered=ChainBooleanRebuildCache.rebuild(
    Arrays.asList(b,a),Arrays.asList(cutB,cutA,intersectA),changed.state);
  yes(reordered.rebuiltFeatureIds.isEmpty()&&reordered.reused==3,"Reorder must not invalidate UUID bound source");
  yes(reordered.state.matches(Arrays.asList(b,a),Arrays.asList(cutB,cutA,intersectA)),"Reordered snapshot valid");
  ChainBooleanFeature adjusted=intersectA.withCutter(5,0,0,7,5,10);
  ChainBooleanRebuildCache.Result edited=ChainBooleanRebuildCache.rebuild(
    Arrays.asList(b,a),Arrays.asList(cutB,cutA,adjusted),reordered.state);
  yes(edited.rebuiltFeatureIds.size()==1&&edited.rebuiltFeatureIds.get(0).equals(intersectA.id),
    "Only edited Boolean should rebuild");
  yes(edited.reused==2,"Other outputs should remain cached");
  ChainBooleanRebuildCache.Result removed=ChainBooleanRebuildCache.rebuild(
    chains,Arrays.asList(cutA,cutB),edited.state);
  yes(removed.state.count()==2&&removed.state.mesh(intersectA.id)==null,
    "Removed feature must be removed from snapshot");
  // A failed update must not alter the last valid snapshot.
  ChainBooleanFeature invalidIntersection=new ChainBooleanFeature(
    0,a.id,MultiBoxUnion.Operation.CHAIN_INTERSECTION,900,900,900,2,2,2);
  rejects(()->ChainBooleanRebuildCache.rebuild(chains,Arrays.asList(cutA,invalidIntersection),removed.state),
    "Empty intersection must fail atomically");
  yes(removed.state.count()==2&&removed.state.mesh(cutA.id)!=null,"Failed update changed valid cache");
  rejects(()->ChainBooleanRebuildCache.rebuild(chains,Arrays.asList(cutA,cutA),removed.state),
    "Duplicate UUID must be rejected");
  rejects(()->ChainBooleanRebuildCache.rebuild(Collections.singletonList(b),
    Collections.singletonList(cutA),removed.state),"Orphan source must be rejected");
  ChainBooleanRebuildCache.Result empty=ChainBooleanRebuildCache.rebuild(chains,
    Collections.emptyList(),removed.state);
  yes(empty.state.count()==0&&empty.state.combinedPreview().length==0,"Empty result");
  System.out.println("ChainBooleanRebuildCacheTest: OK");
 }
}
