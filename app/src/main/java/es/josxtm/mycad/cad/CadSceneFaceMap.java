package es.josxtm.mycad.cad;
import java.util.*;
/** Maps one flattened GPU triangle index back to a source body and logical face. */
public final class CadSceneFaceMap {
 public static final class Range {
  public final int partIndex, firstTriangle, triangleCount;
  public Range(int partIndex,int firstTriangle,int triangleCount){
   if(partIndex<0||firstTriangle<0||triangleCount<=0)throw new IllegalArgumentException("Invalid range");
   this.partIndex=partIndex;this.firstTriangle=firstTriangle;this.triangleCount=triangleCount;
  }
 }
 public static final class Selection {
  public final int partIndex,triangleInPart;
  public final BoxFaceSelection.Face face;
  Selection(int partIndex,int triangleInPart,BoxFaceSelection.Face face){this.partIndex=partIndex;this.triangleInPart=triangleInPart;this.face=face;}
 }
 private final List<Range> ranges=new ArrayList<>();
 public void addBox(int partIndex,int firstTriangle){ranges.add(new Range(partIndex,firstTriangle,12));}
 public Selection resolve(int globalTriangle){
  if(globalTriangle<0)return null;
  for(Range range:ranges){
   if(globalTriangle>=range.firstTriangle&&globalTriangle-range.firstTriangle<range.triangleCount){
    int local=globalTriangle-range.firstTriangle;
    return new Selection(range.partIndex,local,BoxFaceSelection.faceForTriangle(local));
   }
  }
  return null;
 }
}
