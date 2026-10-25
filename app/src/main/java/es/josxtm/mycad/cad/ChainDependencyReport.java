package es.josxtm.mycad.cad;
import java.util.*;
/** Pure Java validation of saved parametric history and Boolean source references. */
public final class ChainDependencyReport {
 private ChainDependencyReport(){}
 public static final class Result {
  public final int chainCount,stepCount,booleanCount;
  public final List<String> errors;
  Result(int c,int s,int b,List<String> errors){
   chainCount=c;stepCount=s;booleanCount=b;
   this.errors=Collections.unmodifiableList(new ArrayList<>(errors));
  }
  public boolean isValid(){return errors.isEmpty();}
 }
 public static Result inspect(List<FaceExtrusionChain> chains,List<ChainBooleanFeature> features){
  if(chains==null||features==null)throw new IllegalArgumentException("Missing CAD history");
  HashSet<String> chainIds=new HashSet<>(),stepIds=new HashSet<>(),booleanIds=new HashSet<>();
  ArrayList<String> errors=new ArrayList<>();
  int steps=0;
  for(int i=0;i<chains.size();i++){
   FaceExtrusionChain chain=chains.get(i);
   if(chain==null){errors.add("Cadena nula: "+i);continue;}
   if(!chainIds.add(chain.id))errors.add("UUID de cadena duplicado: "+chain.id);
   steps+=chain.size();
   for(FaceExtrusionChain.Step step:chain.history())
    if(!stepIds.add(step.id))errors.add("UUID de extrusión duplicado: "+step.id);
   try{chain.regenerate();}catch(RuntimeException e){
    errors.add("Geometría inválida de cadena: "+chain.id);
   }
  }
  for(int i=0;i<features.size();i++){
   ChainBooleanFeature f=features.get(i);
   if(f==null){errors.add("Booleana nula: "+i);continue;}
   if(!booleanIds.add(f.id))errors.add("UUID de booleana duplicado: "+f.id);
   try{f.resolvedChainIndex(chains);}catch(RuntimeException e){
    errors.add("Booleana sin cadena origen: "+i);
   }
  }
  return new Result(chains.size(),steps,features.size(),errors);
 }
 public static void requireValid(List<FaceExtrusionChain> chains,List<ChainBooleanFeature> features){
  Result r=inspect(chains,features);
  if(!r.isValid())throw new IllegalArgumentException(r.errors.get(0));
 }
}