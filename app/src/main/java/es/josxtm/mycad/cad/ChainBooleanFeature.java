package es.josxtm.mycad.cad;
import es.josxtm.mycad.BoxBoolean;
import es.josxtm.mycad.MultiBoxUnion;
import java.util.*;
/** Rebuildable Boolean with stable feature and source-chain identities. */
public final class ChainBooleanFeature {
 /** UUID of this particular Boolean feature, preserved by parameter edits. */
 public final String id;
 /** Creation-time position, kept to decode pre-UUID JSON. */
 public final int chainIndex;
 public final String chainId;
 public final MultiBoxUnion.Operation operation;
 public final double x,y,z,w,d,h;
 public ChainBooleanFeature(int index,MultiBoxUnion.Operation operation,double x,double y,double z,double w,double d,double h){
  this(UUID.randomUUID().toString(),index,null,operation,x,y,z,w,d,h);
 }
 public ChainBooleanFeature(int index,String chainId,MultiBoxUnion.Operation operation,double x,double y,double z,double w,double d,double h){
  this(UUID.randomUUID().toString(),index,chainId,operation,x,y,z,w,d,h);
 }
 public ChainBooleanFeature(String id,int index,String chainId,MultiBoxUnion.Operation operation,double x,double y,double z,double w,double d,double h){
  if(id==null||!UUID.fromString(id).toString().equals(id))
   throw new IllegalArgumentException("Invalid Boolean feature UUID");
  if(index<0||index>=100||operation!=MultiBoxUnion.Operation.CHAIN_DIFFERENCE&&operation!=MultiBoxUnion.Operation.CHAIN_INTERSECTION)
   throw new IllegalArgumentException("Unsupported Boolean");
  if(chainId!=null&&!UUID.fromString(chainId).toString().equals(chainId))
   throw new IllegalArgumentException("Invalid source chain UUID");
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(w)||!Double.isFinite(d)||!Double.isFinite(h)||
    w<=0||d<=0||h<=0||w>10000||d>10000||h>10000)throw new IllegalArgumentException("Invalid cutter");
  this.id=id;chainIndex=index;this.chainId=chainId;this.operation=operation;
  this.x=x;this.y=y;this.z=z;this.w=w;this.d=d;this.h=h;
 }
 public ChainBooleanFeature linkedTo(String stableChainId){
  return new ChainBooleanFeature(id,chainIndex,stableChainId,operation,x,y,z,w,d,h);
 }
 public ChainBooleanFeature withCutter(double newX,double newY,double newZ,double newW,double newD,double newH){
  return new ChainBooleanFeature(id,chainIndex,chainId,operation,newX,newY,newZ,newW,newD,newH);
 }
 public ChainBooleanFeature withOperation(MultiBoxUnion.Operation next){
  return new ChainBooleanFeature(id,chainIndex,chainId,next,x,y,z,w,d,h);
 }
 public int resolvedChainIndex(List<FaceExtrusionChain> chains){
  if(chains==null)throw new IllegalArgumentException("Missing chain list");
  if(chainId!=null){
   for(int i=0;i<chains.size();i++)
    if(chains.get(i).id.equals(chainId))return i;
   throw new IllegalArgumentException("Missing linked chain UUID");
  }
  if(chainIndex>=chains.size())throw new IllegalArgumentException("Missing linked chain");
  return chainIndex;
 }
 public float[] regenerate(List<FaceExtrusionChain> chains){
  int resolved=resolvedChainIndex(chains);
  ArrayList<BoxBoolean.Box> boxes=new ArrayList<>();
  for(FaceExtrusionChain.Solid solid:chains.get(resolved).regenerate())
   boxes.add(new BoxBoolean.Box(solid.x,solid.y,solid.z,solid.w,solid.d,solid.h));
  boxes.add(new BoxBoolean.Box(x,y,z,w,d,h));
  return MultiBoxUnion.build(boxes,operation);
 }
}