package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Cached broad-phase ray index for existing triangle meshes.
 *
 * Splits a mesh into fixed 64-triangle chunks; precomputes conservative
 * axis-aligned bounds for each chunk; only the chunks hit by the world-space
 * ray go through exact CadRayPicker/Möller-Trumbore intersections.
 *
 * Keeps source triangle ordering and original global triangle indices, so
 * topology mapping, imports and visible nearest-hit semantics are unchanged.
 * One instance per immutable mesh; rebuild if the XYZ coordinates change.
 */
public final class MeshRayIndex {
 private static final int CHUNK_TRIANGLES=64;
 private final float[] triangles;
 private final Block[] blocks;

 private static final class Block {
  final int first,end;
  final double[] lower,upper;
  Block(int first,int end,double[] lo,double[] hi){
   this.first=first;this.end=end;lower=lo;upper=hi;
  }
 }
 public MeshRayIndex(float[] triangles){
  if(triangles==null||triangles.length%9!=0)
   throw new IllegalArgumentException("Invalid index geometry");
  this.triangles=triangles;
  ArrayList<Block> chunks=new ArrayList<>();
  int n=triangles.length/9;
  for(int first=0;first<n;first+=CHUNK_TRIANGLES){
   int end=Math.min(n,first+CHUNK_TRIANGLES);
   double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},
            hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
   boolean anyValid=false;
   for(int face=first;face<end;face++){
    int at=face*9;
    boolean valid=true;
    for(int k=0;k<9;k++)if(!Float.isFinite(triangles[at+k])){
     valid=false;break;
    }
    if(!valid)continue;
    anyValid=true;
    for(int k=0;k<9;k++){
     int axis=k%3;
     double value=triangles[at+k];
     lo[axis]=Math.min(lo[axis],value);
     hi[axis]=Math.max(hi[axis],value);
    }
   }
   if(anyValid)chunks.add(new Block(first,end,lo,hi));
  }
  blocks=chunks.toArray(new Block[0]);
 }
 public int chunks(){return blocks.length;}

 private static boolean intersects(Block block,double[] origin,double[] dir,
                                   double closest){
  double enter=0,exit=closest;
  for(int i=0;i<3;i++){
   // Conservative padding prevents losing a hit at an exact shared edge
   // through floating-point cancellation in the slab calculation.
   double padding=1e-7*Math.max(1,
      Math.max(Math.abs(block.lower[i]),Math.abs(block.upper[i])));
   double lo=block.lower[i]-padding,hi=block.upper[i]+padding;
   if(Math.abs(dir[i])<1e-15){
    if(origin[i]<lo||origin[i]>hi)return false;
   }else{
    double a=(lo-origin[i])/dir[i],b=(hi-origin[i])/dir[i];
    if(a>b){double tmp=a;a=b;b=tmp;}
    enter=Math.max(enter,a);
    exit=Math.min(exit,b);
    if(enter>exit)return false;
   }
  }
  return exit>0;
 }
 public CadRayPicker.Hit pick(double ox,double oy,double oz,
                              double dx,double dy,double dz){
  if(!Double.isFinite(ox)||!Double.isFinite(oy)||!Double.isFinite(oz)||
     !Double.isFinite(dx)||!Double.isFinite(dy)||!Double.isFinite(dz))
   throw new IllegalArgumentException("Invalid ray");
  double len=Math.hypot(dx,Math.hypot(dy,dz));
  if(!Double.isFinite(len)||len<1e-10)
   throw new IllegalArgumentException("Invalid ray direction");
  double[] o={ox,oy,oz},d={dx/len,dy/len,dz/len};
  CadRayPicker.Hit best=null;
  for(Block block:blocks){
   if(!intersects(block,o,d,best==null?Double.POSITIVE_INFINITY:best.distance))
    continue;
   CadRayPicker.Hit hit=CadRayPicker.pickRange(triangles,block.first,block.end,
                                             ox,oy,oz,dx,dy,dz);
   if(hit!=null&&(best==null||hit.distance<best.distance))best=hit;
  }
  return best;
 }
}
