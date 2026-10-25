package es.josxtm.mycad.cad;
import es.josxtm.mycad.BoxBoolean;
import es.josxtm.mycad.MultiBoxUnion;
import java.util.*;
/** Rebuildable Boolean on a saved chain, using a captured axis-aligned tool. */
public final class ChainBooleanFeature {
 public final int chainIndex;public final MultiBoxUnion.Operation operation;
 public final double x,y,z,w,d,h;
 public ChainBooleanFeature(int index,MultiBoxUnion.Operation operation,double x,double y,double z,double w,double d,double h){
  if(index<0||index>=100||operation!=MultiBoxUnion.Operation.CHAIN_DIFFERENCE&&operation!=MultiBoxUnion.Operation.CHAIN_INTERSECTION)
   throw new IllegalArgumentException("Unsupported Boolean");
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(w)||!Double.isFinite(d)||!Double.isFinite(h)||
    w<=0||d<=0||h<=0||w>10000||d>10000||h>10000)throw new IllegalArgumentException("Invalid cutter");
  chainIndex=index;this.operation=operation;this.x=x;this.y=y;this.z=z;this.w=w;this.d=d;this.h=h;
 }
 public float[] regenerate(List<FaceExtrusionChain> chains){
  if(chains==null||chainIndex>=chains.size())throw new IllegalArgumentException("Missing linked chain");
  ArrayList<BoxBoolean.Box> boxes=new ArrayList<>();
  for(FaceExtrusionChain.Solid solid:chains.get(chainIndex).regenerate())
   boxes.add(new BoxBoolean.Box(solid.x,solid.y,solid.z,solid.w,solid.d,solid.h));
  boxes.add(new BoxBoolean.Box(x,y,z,w,d,h));
  return MultiBoxUnion.build(boxes,operation);
 }
}