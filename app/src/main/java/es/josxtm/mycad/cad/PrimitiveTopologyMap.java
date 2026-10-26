package es.josxtm.mycad.cad;
import java.util.*;

/**
 * Maps renderer triangle indices to logical primitive faces with persistent body UUIDs.
 * Only primitive surfaces (boxes/cylinders) are mapped; imported/boolean meshes are skipped.
 * Logical face keys survive tessellation changes, part reorder and JSON round-trips,
 * as long as the original body UUID survives. Not a general B-Rep topological naming scheme.
 */
public final class PrimitiveTopologyMap {
 public enum Kind { BOX, CYLINDER_SIDE, CYLINDER_BOTTOM, CYLINDER_TOP }
 public static final class Hit {
  public final int partIndex,firstTriangle,triangleCount,localTriangle;
  public final String bodyId,faceId;
  public final Kind kind;
  public final BoxFaceSelection.Face boxFace;
  private Hit(int index,int first,int count,int local,String id,Kind kind,BoxFaceSelection.Face face){
   partIndex=index;firstTriangle=first;triangleCount=count;localTriangle=local;
   bodyId=id;this.kind=kind;boxFace=face;
   faceId=id+"/face/"+(face==null?kind.name():face.name());
  }
  /** Triangle to highlight within a box/cylinder segment, not the entire cap. */
  public int highlightTriangle(){
   if(kind==Kind.BOX)return firstTriangle+(localTriangle/2)*2;
   if(kind==Kind.CYLINDER_SIDE)return firstTriangle+(localTriangle/4)*4;
   return firstTriangle+localTriangle;
  }
 }
 private static final class BodyRange {
  final int index,start,count,segments;
  final String id;
  final boolean cylinder;
  BodyRange(int index,int start,int count,int segments,String id,boolean cylinder){
   this.index=index;this.start=start;this.count=count;this.segments=segments;this.id=id;this.cylinder=cylinder;
  }
 }
 private final ArrayList<BodyRange> ranges=new ArrayList<>();
 private final HashSet<String> bodyIds=new HashSet<>();
 private long end=0;
 public void addBox(int partIndex,String bodyId,int firstTriangle){
  add(partIndex,bodyId,firstTriangle,12,0,false);
 }
 public void addCylinder(int partIndex,String bodyId,int firstTriangle,int segments){
  if(segments<12||segments>128)throw new IllegalArgumentException("Invalid segments");
  add(partIndex,bodyId,firstTriangle,segments*4,segments,true);
 }
 private void add(int i,String id,int start,int count,int segments,boolean cylinder){
  // Validate first: a failed registration must not reserve a body ID.
  // Triangle indices are Java ints; the exclusive upper bound may be MAX_VALUE + 1.
  long nextEnd=(long)start+count;
  if(i<0||id==null||id.trim().isEmpty()||start<0||count<=0||
     nextEnd>(long)Integer.MAX_VALUE+1||bodyIds.contains(id)||
     (!ranges.isEmpty()&&start<end))
    throw new IllegalArgumentException("Invalid or overlapping body topology");
  ranges.add(new BodyRange(i,start,count,segments,id,cylinder));
  bodyIds.add(id);
  end=nextEnd;
 }
 public Hit resolve(int globalTriangle){
  if(globalTriangle<0)return null;
  for(BodyRange range:ranges){
   if(globalTriangle<range.start||((long)globalTriangle)>=(long)range.start+range.count)continue;
   int local=globalTriangle-range.start;
   if(!range.cylinder){
    BoxFaceSelection.Face face=BoxFaceSelection.faceForTriangle(local);
    return new Hit(range.index,range.start,range.count,local,range.id,Kind.BOX,face);
   }
   Kind kind=(local%4==0||local%4==1)?Kind.CYLINDER_SIDE:
             local%4==2?Kind.CYLINDER_BOTTOM:Kind.CYLINDER_TOP;
   return new Hit(range.index,range.start,range.count,local,range.id,kind,null);
  }
  return null;
 }
}
