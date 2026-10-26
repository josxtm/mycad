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
  reject(()->new MeshRayIndex(null),"Null mesh");
  reject(()->new MeshRayIndex(new float[8]),"Unaligned triangle array");
  reject(()->index.pick(0,0,0,0,0,0),"Zero direction");
  reject(()->index.pick(0,0,0,Double.NaN,0,1),"NaN direction");
  reject(()->index.pick(Double.POSITIVE_INFINITY,0,0,1,0,0),"Infinity origin");
  System.out.println("MeshRayIndexTest: OK");
 }
}
