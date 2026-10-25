package es.josxtm.mycad.cad;
import java.util.*;

/**
 * Atomic, incremental rebuilding of saved chain Boolean features.
 *
 * Features are identified by UUID; the source-chain and cutter parameter
 * signatures are computed independently of list order. A changed source
 * invalidates its dependent Booleans. All replacement meshes are calculated
 * and checked before a new State is published. No Android dependencies.
 */
public final class ChainBooleanRebuildCache {
 private ChainBooleanRebuildCache(){}
 public static final int MAX_FEATURE_TRIANGLES=250000;
 public static final int MAX_PREVIEW_TRIANGLES=350000;

 public static final class State {
  private final Map<String,String> chains,features;
  private final Map<String,float[]> meshes;
  private State(Map<String,String> chains,Map<String,String> features,
                Map<String,float[]> meshes){
   this.chains=Collections.unmodifiableMap(new LinkedHashMap<>(chains));
   this.features=Collections.unmodifiableMap(new LinkedHashMap<>(features));
   LinkedHashMap<String,float[]> copy=new LinkedHashMap<>();
   for(Map.Entry<String,float[]> entry:meshes.entrySet())
    copy.put(entry.getKey(),entry.getValue().clone());
   this.meshes=Collections.unmodifiableMap(copy);
  }
  /** Test that the model still matches this completed background calculation. */
  public boolean matches(List<FaceExtrusionChain> chains,List<ChainBooleanFeature> features){
   try{
    ChainDependencyReport.requireValid(chains,features);
    if(!this.chains.equals(chainSignatures(chains))||features.size()!=this.features.size())
     return false;
    LinkedHashMap<String,String> signatures=new LinkedHashMap<>();
    for(ChainBooleanFeature feature:features){
     int source=feature.resolvedChainIndex(chains);
     signatures.put(feature.id,featureSignature(feature,chains.get(source).id));
    }
    return signatures.equals(this.features);
   }catch(RuntimeException error){return false;}
  }
  public int count(){return meshes.size();}
  public boolean contains(String id){return meshes.containsKey(id);}
  public float[] mesh(String id){
   float[] result=meshes.get(id);
   return result==null?null:result.clone();
  }
  public int totalTriangles(){
   int count=0;
   for(float[] mesh:meshes.values())count+=mesh.length/9;
   return count;
  }
  public float[] combinedPreview(){
   int triangles=totalTriangles();
   if(triangles>MAX_PREVIEW_TRIANGLES)
    throw new IllegalArgumentException("Too many preview triangles");
   float[] merged=new float[triangles*9];int at=0;
   for(float[] mesh:meshes.values()){
    System.arraycopy(mesh,0,merged,at,mesh.length);at+=mesh.length;
   }
   return merged;
  }
 }
 public static final class Result {
  public final State state;
  public final List<String> rebuiltFeatureIds;
  public final int reused;
  private Result(State state,List<String> rebuilt,int reused){
   this.state=state;
   rebuiltFeatureIds=Collections.unmodifiableList(new ArrayList<>(rebuilt));
   this.reused=reused;
  }
 }
 private static void append(StringBuilder buffer,double value){
  buffer.append(Long.toHexString(Double.doubleToLongBits(value))).append('|');
 }
 private static Map<String,String> chainSignatures(List<FaceExtrusionChain> chains){
  LinkedHashMap<String,String> fingerprints=new LinkedHashMap<>();
  for(FaceExtrusionChain chain:chains){
   StringBuilder key=new StringBuilder(chain.id).append('|');
   FaceExtrusionChain.Solid root=chain.regenerate().get(0);
   append(key,root.w);append(key,root.d);append(key,root.h);
   append(key,root.x);append(key,root.y);append(key,root.z);
   for(FaceExtrusionChain.Step step:chain.history()){
    key.append(step.id).append('/').append(step.face.name()).append('/');
    append(key,step.distance);
   }
   fingerprints.put(chain.id,key.toString());
  }
  return fingerprints;
 }
 private static String featureSignature(ChainBooleanFeature feature,String actualChainId){
  StringBuilder key=new StringBuilder(feature.id).append('/')
   .append(actualChainId).append('/').append(feature.operation.name()).append('/');
  append(key,feature.x);append(key,feature.y);append(key,feature.z);
  append(key,feature.w);append(key,feature.d);append(key,feature.h);
  return key.toString();
 }
 private static void validateMesh(float[] triangles){
  if(triangles==null||triangles.length==0||triangles.length%9!=0||
     triangles.length/9>MAX_FEATURE_TRIANGLES)
   throw new IllegalArgumentException("Invalid rebuilt Boolean mesh size");
  for(float value:triangles)
   if(!Float.isFinite(value))throw new IllegalArgumentException("Nonfinite rebuilt triangle");
 }
 public static Result rebuild(List<FaceExtrusionChain> chains,List<ChainBooleanFeature> features,
                              State previous){
  ChainDependencyReport.requireValid(chains,features);
  Map<String,String> nextChains=chainSignatures(chains);
  LinkedHashMap<String,String> nextFeatures=new LinkedHashMap<>();
  LinkedHashMap<String,float[]> nextMeshes=new LinkedHashMap<>();
  ArrayList<String> rebuilt=new ArrayList<>();
  int reused=0;
  for(ChainBooleanFeature feature:features){
   int sourceIndex=feature.resolvedChainIndex(chains);
   String actualChainId=chains.get(sourceIndex).id;
   String signature=featureSignature(feature,actualChainId);
   nextFeatures.put(feature.id,signature);
   float[] geometry=null;
   if(previous!=null&&signature.equals(previous.features.get(feature.id))&&
      Objects.equals(nextChains.get(actualChainId),previous.chains.get(actualChainId))){
    geometry=previous.meshes.get(feature.id);
   }
   if(geometry==null){
    geometry=feature.regenerate(chains);
    validateMesh(geometry);
    rebuilt.add(feature.id);
   }else reused++;
   nextMeshes.put(feature.id,geometry);
  }
  // Publication occurs only after every dependent result is valid.
  return new Result(new State(nextChains,nextFeatures,nextMeshes),rebuilt,reused);
 }
}
