package es.josxtm.mycad.cad;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Selects the frontmost analytic B-Rep patch across multiple stepped shells.
 * This is a model-space kernel operation; camera/screen conversion belongs
 * to the Android viewer and is deliberately not implemented here.
 */
public final class SteppedSceneRayPicker {
 private SteppedSceneRayPicker(){}

 /** A shell and its topology from the SAME regeneration. */
 public static final class Candidate {
  public final VariableHeightOrthogonalBRep shell;
  public final SteppedCoplanarPatchTopology.Topology topology;
  public Candidate(VariableHeightOrthogonalBRep shell,
      SteppedCoplanarPatchTopology.Topology topology){
   if(shell==null||topology==null)
    throw new IllegalArgumentException("Missing scene shell or topology");
   if(!shell.id().equals(topology.shellId))
    throw new IllegalArgumentException("Scene topology belongs to another shell");
   this.shell=shell;
   this.topology=topology;
  }
 }

 public static final class Hit {
  /** Index in the supplied candidate list; stable only until scene regeneration. */
  public final int candidateIndex;
  public final VariableHeightOrthogonalBRep shell;
  public final SteppedPatchRayPicker.Hit patchHit;
  private Hit(int index,VariableHeightOrthogonalBRep shell,
      SteppedPatchRayPicker.Hit patchHit){
   this.candidateIndex=index;
   this.shell=shell;
   this.patchHit=patchHit;
  }
 }

 /**
  * Returns the nearest forward ray intersection, or null for an empty scene
  * or a miss. Distances are world-space units even for non-unit directions.
  * Equal-distance hits choose the first candidate in input order. Duplicate
  * shell IDs are rejected to prevent ambiguous selection across instances.
  * Neither shell geometry nor the caller's candidate list is modified.
  */
 public static Hit pick(List<Candidate> candidates,double[] origin,double[] direction){
  if(candidates==null)throw new IllegalArgumentException("Missing scene");
  Set<String> shellIds=new HashSet<>();
  for(Candidate candidate:candidates){
   if(candidate==null||candidate.shell==null||candidate.topology==null)
    throw new IllegalArgumentException("Missing scene candidate");
   if(!candidate.shell.id().equals(candidate.topology.shellId))
    throw new IllegalArgumentException("Stale scene topology");
   if(!shellIds.add(candidate.shell.id()))
    throw new IllegalArgumentException("Duplicate scene shell ID");
  }
  Hit nearest=null;
  for(int i=0;i<candidates.size();i++){
   Candidate candidate=candidates.get(i);
   SteppedPatchRayPicker.Hit hit=SteppedPatchRayPicker.pick(
       candidate.shell,candidate.topology,origin,direction);
   if(hit==null)continue;
   double previous=nearest==null?Double.POSITIVE_INFINITY:nearest.patchHit.distance;
   double tolerance=1e-9*Math.max(1,Math.max(hit.distance,
       Double.isFinite(previous)?previous:0));
   if(nearest==null||hit.distance<previous-tolerance)
    nearest=new Hit(i,candidate.shell,hit);
  }
  return nearest;
 }
}
