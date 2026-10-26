package es.josxtm.mycad.cad;

/**
 * Immutable transactional preview of a primitive face/edge move.
 * No scene, undo stack, mesh or JSON changes occur during preview.
 * Re-evaluations always start from the ORIGINAL geometry, avoiding drift.
 */
public final class PrimitiveMovePreview {
 public enum Target { FACE, EDGE }
 public final PrimitiveBRepBody original;
 public final String entityId;
 public final Target target;

 private PrimitiveMovePreview(PrimitiveBRepBody original,String entityId,Target target){
  this.original=original;this.entityId=entityId;this.target=target;
 }
 public static PrimitiveMovePreview forFace(PrimitiveBRepBody body,String faceId){
  if(body==null||faceId==null||body.findFace(faceId)==null)
   throw new IllegalArgumentException("Unknown primitive face");
  return new PrimitiveMovePreview(body,faceId,Target.FACE);
 }
 public static PrimitiveMovePreview forEdge(PrimitiveBRepBody body,String edgeId){
  if(body==null||edgeId==null||body.findEdge(edgeId)==null)
   throw new IllegalArgumentException("Unknown primitive edge");
  return new PrimitiveMovePreview(body,edgeId,Target.EDGE);
 }
 /** Return the original unmodified body at neutral slider positions. */
 public PrimitiveBRepBody propose(double firstMm,double secondMm){
  if(!Double.isFinite(firstMm)||!Double.isFinite(secondMm))
   throw new IllegalArgumentException("Nonfinite offset");
  if(target==Target.FACE){
   if(secondMm!=0)throw new IllegalArgumentException("Face takes a single offset");
   if(firstMm==0)return original;
   return original.offsetFace(entityId,firstMm);
  }
  if(firstMm==0&&secondMm==0)return original;
  return original.offsetEdge(entityId,firstMm,secondMm);
 }
 /** Full source-state guard, so a stale dialog cannot commit over a new edit. */
 public boolean matches(PrimitiveBRepBody live){
  return live!=null&&original.kind==live.kind&&original.bodyId.equals(live.bodyId)&&
    Double.compare(original.width,live.width)==0&&
    Double.compare(original.depth,live.depth)==0&&
    Double.compare(original.height,live.height)==0&&
    Double.compare(original.x,live.x)==0&&Double.compare(original.y,live.y)==0&&
    Double.compare(original.z,live.z)==0&&Double.compare(original.rx,live.rx)==0&&
    Double.compare(original.ry,live.ry)==0&&Double.compare(original.rz,live.rz)==0;
 }
}
