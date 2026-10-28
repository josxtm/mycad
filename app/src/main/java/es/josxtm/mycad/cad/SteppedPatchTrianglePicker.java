package es.josxtm.mycad.cad;

/**
 * Resolves a triangle selected from the stepped shell preview to its analytic,
 * coplanar B-Rep patch. The preview is emitted as two triangles per source
 * quad, in exactly the same order as VariableHeightOrthogonalBRep.faces().
 *
 * Triangle indices are transient: resolve against the topology of the same
 * shell after each regeneration. Source face IDs and patch boundaries remain
 * available on the returned read-only PatchFace.
 */
public final class SteppedPatchTrianglePicker {
 private SteppedPatchTrianglePicker(){}

 /** Returns null for a missed/out-of-range triangle, never for broken topology. */
 public static SteppedCoplanarPatchTopology.PatchFace resolve(
     VariableHeightOrthogonalBRep shell,
     SteppedCoplanarPatchTopology.Topology topology,int localTriangle){
  if(shell==null||topology==null)
   throw new IllegalArgumentException("Missing shell or patch topology");
  if(!shell.id().equals(topology.shellId))
   throw new IllegalArgumentException("Patch topology belongs to another shell");
  // apply() emits two triangles per quad; the source face list follows that order.
  long triangleCount=(long)shell.faces().size()*2;
  if(localTriangle<0||localTriangle>=triangleCount)return null;
  String faceId=shell.faces().get(localTriangle/2).id;
  int patchIndex=topology.patchIndexForSourceFace(faceId);
  return topology.patches.get(patchIndex);
 }
}
