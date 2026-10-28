package es.josxtm.mycad.cad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable per-rendered-scene ownership map from GPU triangle ranges to
 * regenerated stepped B-Rep patches. Unregistered meshes still occlude
 * registered shells because the caller must pass the global visible mesh hit.
 *
 * Rebuild this registry whenever the renderer replaces its mesh list.
 */
public final class SteppedScenePatchRegistry {
 public static final class Binding {
  public final int meshIndex;
  public final VariableHeightOrthogonalBRep shell;
  public Binding(int meshIndex,VariableHeightOrthogonalBRep shell){
   if(meshIndex<0||shell==null)
    throw new IllegalArgumentException("Invalid stepped mesh binding");
   this.meshIndex=meshIndex;
   this.shell=shell;
  }
 }
 private final List<SteppedVisiblePatchBridge.Candidate> candidates;
 private SteppedScenePatchRegistry(List<SteppedVisiblePatchBridge.Candidate> candidates){
  this.candidates=Collections.unmodifiableList(new ArrayList<>(candidates));
 }
 public int registeredShellCount(){return candidates.size();}

 /**
  * A binding is accepted only when its shell preview exactly matches the
  * corresponding GPU mesh, including triangle order. Indices count every
  * mesh, including imported or non-B-Rep meshes.
  */
 public static SteppedScenePatchRegistry register(
     List<float[]> renderedMeshes,List<Binding> bindings){
  if(renderedMeshes==null||bindings==null)
   throw new IllegalArgumentException("Missing scene or stepped bindings");
  int[] offsets=new int[renderedMeshes.size()];
  int offset=0;
  for(int i=0;i<renderedMeshes.size();i++){
   float[] triangles=renderedMeshes.get(i);
   if(triangles==null||triangles.length%9!=0)
    throw new IllegalArgumentException("Invalid scene triangle buffer");
   int count=triangles.length/9;
   if(count>Integer.MAX_VALUE-offset)
    throw new IllegalArgumentException("Scene triangle index overflow");
   offsets[i]=offset;
   offset+=count;
  }
  Set<Integer> seen=new HashSet<>();
  List<SteppedVisiblePatchBridge.Candidate> registered=new ArrayList<>();
  for(Binding binding:bindings){
   if(binding==null||binding.meshIndex>=renderedMeshes.size()||
      !seen.add(binding.meshIndex))
    throw new IllegalArgumentException("Missing, duplicate or unknown stepped mesh");
   VariableHeightOrthogonalBRep shell=binding.shell;
   SteppedCoplanarPatchTopology.Topology topology=
       SteppedCoplanarPatchTopology.build(shell);
   registered.add(new SteppedVisiblePatchBridge.Candidate(
       offsets[binding.meshIndex],renderedMeshes.get(binding.meshIndex),
       shell,topology));
  }
  return new SteppedScenePatchRegistry(registered);
 }

 /** Never overrides a closer non-B-Rep mesh hit. */
 public SteppedVisiblePatchBridge.Hit resolve(ViewportMeshRayPicker.Hit visible){
  return SteppedVisiblePatchBridge.resolve(visible,candidates);
 }
}
