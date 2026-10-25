package es.josxtm.mycad.cad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Independent, atomic parametric document for box features. No Android dependencies. */
public final class CadDocument {
 public static final class Feature {
  public final String id;
  public final String bodyId;
  public final double x,y,z,width,depth,height;
  private Feature(String id,String bodyId,double x,double y,double z,double w,double d,double h){
   this.id=id;this.bodyId=bodyId;this.x=x;this.y=y;this.z=z;this.width=w;this.depth=d;this.height=h;
  }
  private Feature withHeight(double h){return new Feature(id,bodyId,x,y,z,width,depth,h);}
 }
 private static final class Revision {
  final List<Feature> features;
  final Map<String,CadKernel.Box> bodies;
  Revision(List<Feature> features,Map<String,CadKernel.Box> bodies){
   this.features=Collections.unmodifiableList(new ArrayList<>(features));
   this.bodies=Collections.unmodifiableMap(new LinkedHashMap<>(bodies));
  }
 }
 private final CadKernel kernel;
 private final ArrayList<Revision> revisions=new ArrayList<>();
 private int revisionIndex=0;
 public CadDocument(CadKernel kernel){
  if(kernel==null)throw new IllegalArgumentException("Kernel ausente");
  this.kernel=kernel;
  revisions.add(new Revision(Collections.emptyList(),Collections.emptyMap()));
 }
 public int revision(){return revisionIndex;}
 public List<Feature> features(){return revisions.get(revisionIndex).features;}
 public Map<String,CadKernel.Box> bodies(){return revisions.get(revisionIndex).bodies;}
 public CadKernel.Box body(String id){return bodies().get(id);}
 public boolean canUndo(){return revisionIndex>0;}
 public boolean canRedo(){return revisionIndex<revisions.size()-1;}
 public boolean undo(){if(!canUndo())return false;revisionIndex--;return true;}
 public boolean redo(){if(!canRedo())return false;revisionIndex++;return true;}
 private void commit(Revision r){
  while(revisions.size()>revisionIndex+1)revisions.remove(revisions.size()-1);
  revisions.add(r);revisionIndex++;
 }
 private CadKernel.Result<CadKernel.Box> rebuild(List<Feature> input,String changedBody){
  LinkedHashMap<String,CadKernel.Box> output=new LinkedHashMap<>();
  CadKernel.Box selected=null;
  for(Feature f:input){
   CadKernel.Result<CadKernel.Box> result=kernel.createBox(f.x,f.y,f.z,f.width,f.depth,f.height);
   if(!result.isSuccess())return result;
   // Maintain the feature's stable body identity across full regeneration.
   CadKernel.Box raw=result.value;
   CadKernel.Box body=new CadKernel.Box(f.bodyId,raw.x,raw.y,raw.z,raw.width,raw.depth,raw.height);
   if(output.put(f.bodyId,body)!=null)return CadKernel.Result.invalid("Identificador de cuerpo repetido");
   if(f.bodyId.equals(changedBody))selected=body;
  }
  commit(new Revision(input,output));
  return CadKernel.Result.success(selected);
 }
 public CadKernel.Result<CadKernel.Box> addBox(double x,double y,double z,double width,double depth,double height){
  CadKernel.Result<CadKernel.Box> valid=kernel.createBox(x,y,z,width,depth,height);
  if(!valid.isSuccess())return valid;
  String bodyId=UUID.randomUUID().toString();
  Feature feature=new Feature(UUID.randomUUID().toString(),bodyId,x,y,z,width,depth,height);
  ArrayList<Feature> proposed=new ArrayList<>(features());proposed.add(feature);
  return rebuild(proposed,bodyId);
 }
 public CadKernel.Result<CadKernel.Box> setBoxHeight(String bodyId,double height){
  if(bodyId==null||!bodies().containsKey(bodyId))return CadKernel.Result.invalid("Cuerpo inexistente");
  CadKernel.Result<CadKernel.Box> checked=kernel.resizeHeight(body(bodyId),height);
  if(!checked.isSuccess())return checked;
  ArrayList<Feature> proposed=new ArrayList<>();
  for(Feature f:features())proposed.add(f.bodyId.equals(bodyId)?f.withHeight(height):f);
  return rebuild(proposed,bodyId);
 }
 public boolean removeBody(String bodyId){
  if(!bodies().containsKey(bodyId))return false;
  ArrayList<Feature> proposed=new ArrayList<>();
  for(Feature f:features())if(!f.bodyId.equals(bodyId))proposed.add(f);
  rebuild(proposed,null);return true;
 }
}
