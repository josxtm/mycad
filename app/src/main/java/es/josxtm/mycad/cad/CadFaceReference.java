package es.josxtm.mycad.cad;
/** A stable-in-session reference to a logical face of one indexed CAD body. */
public final class CadFaceReference {
 public final int partIndex;
 public final BoxFaceSelection.Face face;
 public CadFaceReference(int partIndex,BoxFaceSelection.Face face){
  if(partIndex<0||face==null)throw new IllegalArgumentException("Part and face required");
  this.partIndex=partIndex;this.face=face;
 }
 public boolean isValidFor(int partCount){return partIndex<partCount;}
 public int firstLocalTriangle(){return BoxFaceSelection.firstTriangle(face);}
 @Override public boolean equals(Object obj){
  if(!(obj instanceof CadFaceReference))return false;
  CadFaceReference other=(CadFaceReference)obj;
  return partIndex==other.partIndex&&face==other.face;
 }
 @Override public int hashCode(){return 31*partIndex+face.ordinal();}
}