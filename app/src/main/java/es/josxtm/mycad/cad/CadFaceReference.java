package es.josxtm.mycad.cad;
/** Stable logical prism face reference, attached to a persistent body ID when provided. */
public final class CadFaceReference {
 public final int partIndex;
 public final String bodyId;
 public final BoxFaceSelection.Face face;
 /** Legacy reference by index, only safe until bodies are inserted or reordered. */
 public CadFaceReference(int partIndex,BoxFaceSelection.Face face){this(partIndex,null,face);}
 public CadFaceReference(int partIndex,String bodyId,BoxFaceSelection.Face face){
  if(partIndex<0||face==null||(bodyId!=null&&bodyId.isEmpty()))
   throw new IllegalArgumentException("Part and face required");
  this.partIndex=partIndex;this.bodyId=bodyId;this.face=face;
 }
 public boolean isValidFor(int partCount){return partIndex<partCount;}
 /** Validate both the index and the persistent identifier after edits or project reload. */
 public boolean isValidFor(int partCount,String actualBodyId){
  return isValidFor(partCount)&&(bodyId==null||bodyId.equals(actualBodyId));
 }
 public String key(){return (bodyId==null?"index:"+partIndex:bodyId)+"/face/"+face.name();}
 public int firstLocalTriangle(){return BoxFaceSelection.firstTriangle(face);}
 @Override public boolean equals(Object obj){
  if(!(obj instanceof CadFaceReference))return false;
  CadFaceReference other=(CadFaceReference)obj;
  if(bodyId!=null&&other.bodyId!=null)return bodyId.equals(other.bodyId)&&face==other.face;
  return bodyId==null&&other.bodyId==null&&partIndex==other.partIndex&&face==other.face;
 }
 @Override public int hashCode(){return 31*(bodyId==null?partIndex:bodyId.hashCode())+face.ordinal();}
}