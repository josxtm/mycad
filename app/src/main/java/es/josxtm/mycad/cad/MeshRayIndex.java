package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Cached broad-phase ray index for existing triangle meshes.
 *
 * Splits a mesh into fixed 64-triangle chunks and builds a balanced spatial
 * hierarchy over their conservative axis-aligned bounds. A nearest-first ray
 * traversal skips entire distant subtrees before exact triangle intersections.
 *
 * Keeps source triangle ordering and original global triangle indices, so
 * topology mapping, imports and visible nearest-hit semantics are unchanged.
 * One instance per immutable mesh; rebuild if the XYZ coordinates change.
 */
public final class MeshRayIndex {
 private static final int CHUNK_TRIANGLES=64;
 private final float[] triangles;
 private final Block[] blocks;
 private final Node root;

 private static final class Block {
  final int first,end;
  final double[] lower,upper;
  Block(int first,int end,double[] lo,double[] hi){
   this.first=first;this.end=end;lower=lo;upper=hi;
  }
 }

 /** Balanced binary hierarchy. Leaves retain original global triangle ranges. */
 private static final class Node {
  final Block block;
  final Node left,right;
  final double[] lower,upper;
  final int first;
  Node(Block leaf){
   block=leaf;left=null;right=null;
   lower=leaf.lower;upper=leaf.upper;first=leaf.first;
  }
  Node(Node a,Node b){
   block=null;left=a;right=b;
   first=Math.min(a.first,b.first);
   lower=new double[3];upper=new double[3];
   for(int i=0;i<3;i++){
    lower[i]=Math.min(a.lower[i],b.lower[i]);
    upper[i]=Math.max(a.upper[i],b.upper[i]);
   }
  }
 }
 private static Node build(Block[] sorted,int start,int end){
  if(start==end)return null;
  if(end-start==1)return new Node(sorted[start]);
  // Spatial median: avoids scanning every chunk for every touch ray.
  double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
  double[] hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
  for(int i=start;i<end;i++)for(int axis=0;axis<3;axis++){
   double center=(sorted[i].lower[axis]+sorted[i].upper[axis])*0.5;
   lo[axis]=Math.min(lo[axis],center);
   hi[axis]=Math.max(hi[axis],center);
  }
  int axis=0;
  for(int i=1;i<3;i++)if(hi[i]-lo[i]>hi[axis]-lo[axis])axis=i;
  final int splitAxis=axis;
  Arrays.sort(sorted,start,end,(a,b)->{
   double ac=(a.lower[splitAxis]+a.upper[splitAxis])*0.5;
   double bc=(b.lower[splitAxis]+b.upper[splitAxis])*0.5;
   int cmp=Double.compare(ac,bc);
   return cmp!=0?cmp:Integer.compare(a.first,b.first);
  });
  int mid=start+(end-start)/2;
  return new Node(build(sorted,start,mid),build(sorted,mid,end));
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
  root=build(blocks.clone(),0,blocks.length);
 }
 public int chunks(){return blocks.length;}

 private static final class Candidate {
  final Node node;
  final double entry;
  Candidate(Node node,double entry){this.node=node;this.entry=entry;}
 }
 /** Distance to the conservative AABB, or NaN if the ray misses. */
 private static double entryDistance(Node node,double[] origin,double[] dir){
  double enter=0,exit=Double.POSITIVE_INFINITY;
  for(int i=0;i<3;i++){
   // Conservative padding prevents losing a hit at an exact shared edge
   // through floating-point cancellation in the slab calculation.
   double padding=1e-7*Math.max(1,
      Math.max(Math.abs(node.lower[i]),Math.abs(node.upper[i])));
   double lo=node.lower[i]-padding,hi=node.upper[i]+padding;
   if(Math.abs(dir[i])<1e-15){
    if(origin[i]<lo||origin[i]>hi)return Double.NaN;
   }else{
    double a=(lo-origin[i])/dir[i],b=(hi-origin[i])/dir[i];
    if(a>b){double tmp=a;a=b;b=tmp;}
    enter=Math.max(enter,a);
    exit=Math.min(exit,b);
    if(enter>exit)return Double.NaN;
   }
  }
  return exit>0?enter:Double.NaN;
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
  // Priority queue visits the closest hierarchy node first. A successful
  // triangle hit prunes whole farther subtrees, not merely individual chunks.
  // Allocations happen on taps, never in the OpenGL frame loop.
  if(root==null)return null;
  PriorityQueue<Candidate> candidates=new PriorityQueue<>((a,b)->{
   int byDistance=Double.compare(a.entry,b.entry);
   return byDistance!=0?byDistance:Integer.compare(a.node.first,b.node.first);
  });
  double firstEntry=entryDistance(root,o,d);
  if(Double.isNaN(firstEntry))return null;
  candidates.add(new Candidate(root,firstEntry));
  CadRayPicker.Hit best=null;
  while(!candidates.isEmpty()){
   Candidate candidate=candidates.poll();
   // Conservative padded bounds guarantee entry <= exact hit distance.
   if(best!=null&&candidate.entry>best.distance)break;
   Node node=candidate.node;
   if(node.block!=null){
    CadRayPicker.Hit hit=CadRayPicker.pickRange(triangles,node.block.first,node.block.end,
                                              ox,oy,oz,dx,dy,dz);
    // Keep the original first-triangle tie rule across independently
    // visited leaves, even when spatial order differs from mesh order.
    if(hit!=null&&(best==null||hit.distance<best.distance||
       hit.distance==best.distance&&hit.triangle<best.triangle))best=hit;
   }else{
    Node[] children={node.left,node.right};
    for(Node child:children){
     double entry=entryDistance(child,o,d);
     if(!Double.isNaN(entry)&&(best==null||entry<=best.distance))
      candidates.add(new Candidate(child,entry));
    }
   }
  }
  return best;
 }
}
