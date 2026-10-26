import es.josxtm.mycad.cad.*;
import java.util.*;

public final class MeshRayIndexTest {
 private static void eq(CadRayPicker.Hit expected,CadRayPicker.Hit actual,String label){
  if(expected==null||actual==null){
   if(expected!=actual)throw new AssertionError(label+" missing hit");return;
  }
  if(expected.triangle!=actual.triangle||
     Math.abs(expected.distance-actual.distance)>1e-7||
     Math.abs(expected.x-actual.x)>1e-7||
     Math.abs(expected.y-actual.y)>1e-7||
     Math.abs(expected.z-actual.z)>1e-7)
   throw new AssertionError(label+" triangle mismatch "+
     expected.triangle+" / "+actual.triangle);
 }
 private static void ray(float[] mesh,MeshRayIndex index,double ox,double oy,
                         double oz,double dx,double dy,double dz,String label){
  eq(CadRayPicker.pick(mesh,ox,oy,oz,dx,dy,dz),
     index.pick(ox,oy,oz,dx,dy,dz),label);
 }
 private static void reject(Runnable r,String label){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid "+label);
 }
 public static void main(String[] args){
  float[] many=new float[9*205];
  // Separate 64-triangle chunks in world X; only the 3rd is visible from origin.
  for(int i=0;i<205;i++){
   double x=100+(i/64)*100;
   int at=i*9;
   many[at]=(float)x; many[at+1]=-3;
   many[at+2]=0;
   many[at+3]=(float)x;many[at+4]=3;many[at+5]=0;
   many[at+6]=(float)x;many[at+7]=0;many[at+8]=5;
  }
  MeshRayIndex index=new MeshRayIndex(many);
  if(index.chunks()!=4)throw new AssertionError("Chunk count");
  ray(many,index,0,0,2,1,0,0,"First hit with all chunks");
  ray(many,index,0,0,2,10,0,0,"Scaled direction normalized");
  ray(many,index,250,0,2,1,0,0,"Origin between chunks");
  ray(many,index,350,0,2,-1,0,0,"Reversed ray");
  ray(many,index,0,100,2,1,0,0,"Miss all chunks");
  ray(many,index,350,0,2,0,1,0,"Ray parallel to x planes");
  ray(many,index,100,0,2,1,0,0,"Ray starting on near plane");
  ray(many,index,0,-3,0,1,0,0,"Exact shared edge");
  ray(many,index,0,0,5,1,0,0,"Triangle vertex");

  // Nonfinite/degenerate triangle must not poison the bounds or result.
  many[64*9]=Float.NaN;
  float[] damaged=many.clone();
  MeshRayIndex damagedIndex=new MeshRayIndex(damaged);
  ray(damaged,damagedIndex,0,0,2,1,0,0,"Skip nonfinite triangle");
  ray(damaged,damagedIndex,250,0,2,1,0,0,"Damaged chunk still usable");
  float[] blank=new float[0];
  ray(blank,new MeshRayIndex(blank),0,0,0,1,0,0,"No triangles");

  // A real rotated triangulation, not just axis-aligned planar strips.
  PrimitiveBRepBody body=PrimitiveBRepBody.box("rot",40,20,15,
    new RigidTransform(21,-12,34,31,26,63));
  float[] rotated=body.triangles(32);
  MeshRayIndex rotatedIndex=new MeshRayIndex(rotated);
  double[][] rays={
   {100,0,0,-1,0,0},{0,-100,0,0,1,0},
   {0,0,100,0,0,-1},{100,90,30,-1,-1,0},
   {20,20,10,2,-3,1},{-90,-80,-70,4,3,2}
  };
  for(int i=0;i<rays.length;i++){
   double[] q=rays[i];ray(rotated,rotatedIndex,q[0],q[1],q[2],
                         q[3],q[4],q[5],"Rotated "+i);
  }
  PrimitiveBRepBody cyl=PrimitiveBRepBody.cylinder("round",30,20,
    new RigidTransform(40,3,-10,42,-17,23));
  float[] ring=cyl.triangles(64);
  MeshRayIndex ringIndex=new MeshRayIndex(ring);
  ray(ring,ringIndex,40,3,100,0,0,-1,"Rotated cylinder cap");
  ray(ring,ringIndex,100,3,-10,-1,0,0,"Rotated cylinder side");
  // Two chunks contain the same face at exactly the same depth:
  // traversal order must not change the globally first triangle.
  float[] ties=new float[65*9];
  float[] tri={-1,-1,10,1,-1,10,0,1,10};
  System.arraycopy(tri,0,ties,0,9);
  System.arraycopy(tri,0,ties,64*9,9);
  ray(ties,new MeshRayIndex(ties),0,0,0,0,0,1,"Stable equal-depth tie");

  // A large mesh forces a multi-level spatial hierarchy. All 96 leaves
  // retain global triangle indices, even though spatial sorting reorders them.
  float[] large=new float[96*64*9];
  for(int i=0;i<96*64;i++){
   int chunk=i/64;
   // Reverse spatial order relative to source triangle IDs.
   float x=50+(95-chunk)*6;
   int at=i*9;
   large[at]=x;large[at+1]=-2;large[at+2]=0;
   large[at+3]=x;large[at+4]=2;large[at+5]=0;
   large[at+6]=x;large[at+7]=0;large[at+8]=4;
  }
  MeshRayIndex hierarchy=new MeshRayIndex(large);
  if(hierarchy.chunks()!=96)throw new AssertionError("Hierarchy leaf count");
  ray(large,hierarchy,0,0,1,1,0,0,"Hierarchy nearest hit reverse ordering");
  ray(large,hierarchy,700,0,1,-1,0,0,"Hierarchy reverse ray");
  ray(large,hierarchy,0,50,1,1,0,0,"Hierarchy broad miss");
  ray(large,hierarchy,400,0,1,0,0,1,"Hierarchy parallel miss");
  Random hierarchyRandom=new Random(604026L);
  for(int i=0;i<250;i++){
   double ox=hierarchyRandom.nextDouble()*800-100;
   double oy=hierarchyRandom.nextDouble()*10-5;
   double oz=hierarchyRandom.nextDouble()*10-3;
   double dx=hierarchyRandom.nextDouble()*2-1;
   double dy=hierarchyRandom.nextDouble()*2-1;
   double dz=hierarchyRandom.nextDouble()*2-1;
   ray(large,hierarchy,ox,oy,oz,dx,dy,dz,"Hierarchy random "+i);
  }

  // Regress the accelerated broad phase against the exact baseline
  // for deterministic randomized rays (hits, misses, diagonal directions).
  Random random=new Random(20261026L);
  for(int i=0;i<300;i++){
   double ox=random.nextDouble()*700-200;
   double oy=random.nextDouble()*20-10;
   double oz=random.nextDouble()*20-10;
   double dx=random.nextDouble()*2-1;
   double dy=random.nextDouble()*2-1;
   double dz=random.nextDouble()*2-1;
   ray(damaged,damagedIndex,ox,oy,oz,dx,dy,dz,"Randomized "+i);
  }
  // A tiny face in a different BVH leaf must agree with the linear
  // picker; its AABB and the narrow-phase must both preserve the hit.
  float[] microScene=new float[65*9];
  for(int i=0;i<64;i++){
   int at=i*9;
   microScene[at]=100;microScene[at+1]=0;microScene[at+2]=0;
   microScene[at+3]=101;microScene[at+4]=0;microScene[at+5]=0;
   microScene[at+6]=100;microScene[at+7]=1;microScene[at+8]=0;
  }
  float micro=1e-6f;
  int tinyAt=64*9;
  microScene[tinyAt]=0;microScene[tinyAt+1]=0;microScene[tinyAt+2]=0;
  microScene[tinyAt+3]=micro;microScene[tinyAt+4]=0;microScene[tinyAt+5]=0;
  microScene[tinyAt+6]=0;microScene[tinyAt+7]=micro;microScene[tinyAt+8]=0;
  MeshRayIndex microIndex=new MeshRayIndex(microScene);
  CadRayPicker.Hit tiny=microIndex.pick(micro/4,micro/4,0.01,0,0,-1);
  if(tiny==null||tiny.triangle!=64)
   throw new AssertionError("BVH must select micrometre face in last leaf");
  ray(microScene,microIndex,micro/4,micro/4,0.01,0,0,-1,
      "Micrometre face parity between BVH and linear picker");

  reject(()->new MeshRayIndex(null),"Null mesh");
  reject(()->new MeshRayIndex(new float[8]),"Unaligned triangle array");
  reject(()->index.pick(0,0,0,0,0,0),"Zero direction");
  reject(()->index.pick(0,0,0,Double.NaN,0,1),"NaN direction");
  reject(()->index.pick(Double.POSITIVE_INFINITY,0,0,1,0,0),"Infinity origin");
  System.out.println("MeshRayIndexTest: OK");
 }
}
