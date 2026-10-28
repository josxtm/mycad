package es.josxtm.mycad.cad;

import java.util.*;

/** Immutable render generation. Every mesh, camera target, overlay and picking
 * ray uses ONE double-precision world origin. Triangle order is preserved.
 * Original world geometry is never translated in place or used as local data. */
public final class RelativeSceneCoordinates {
 public final double originX,originY,originZ;
 private final double[] bounds;
 private final List<float[]> local;
 private final List<MeshRayIndex> indices;
 private RelativeSceneCoordinates(List<double[]> world){
  double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
  double[] hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
  long count=0;
  for(double[] mesh:world){
   if(mesh==null||mesh.length%9!=0||(count+=mesh.length)>9_000_000)
    throw new IllegalArgumentException("Invalid relative scene mesh");
   for(int i=0;i<mesh.length;i++){
    double value=mesh[i];if(!Double.isFinite(value))throw new IllegalArgumentException("Nonfinite world geometry");
    int axis=i%3;lo[axis]=Math.min(lo[axis],value);hi[axis]=Math.max(hi[axis],value);
   }
  }
  if(count==0){Arrays.fill(lo,0);Arrays.fill(hi,0);}
  // Half first avoids overflow when both bounds have the same large sign.
  originX=lo[0]/2+hi[0]/2;originY=lo[1]/2+hi[1]/2;originZ=lo[2]/2+hi[2]/2;
  bounds=new double[]{lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]};
  List<float[]> translated=new ArrayList<>();List<MeshRayIndex> trees=new ArrayList<>();
  for(double[] mesh:world){
   float[] coords=RelativeMeshCoordinates.relative(mesh,originX,originY,originZ);
   translated.add(coords);trees.add(null);
  }
  local=Collections.unmodifiableList(translated);indices=trees;
 }
 public static RelativeSceneCoordinates from(List<double[]> world){
  if(world==null)throw new IllegalArgumentException("Missing world scene");
  return new RelativeSceneCoordinates(world);
 }
 public int size(){return local.size();}
 public double[] worldBounds(){return bounds.clone();}
 public float[] triangles(int mesh){return local.get(mesh).clone();}
 /** Renderer-owned cache: callers cannot mutate the stored coordinates. */
 public java.nio.FloatBuffer buffer(int mesh,FloatVertexBufferCache cache,Object owner){
  if(cache==null)throw new IllegalArgumentException("Missing render cache");
  return cache.triangles(owner,local.get(mesh));
 }
 public MeshRayIndex rayIndex(int mesh){
  MeshRayIndex index=indices.get(mesh);
  if(index==null){index=new MeshRayIndex(local.get(mesh));indices.set(mesh,index);}
  return index;
 }
 public float[] localPoint(double x,double y,double z){
  double[] delta={x-originX,y-originY,z-originZ};float[] point=new float[3];
  for(int i=0;i<3;i++){
   if(!Double.isFinite(delta[i])||Math.abs(delta[i])>Float.MAX_VALUE)throw new IllegalArgumentException("Invalid local point");
   point[i]=(float)delta[i];
  }
  return point;
 }
 public float[] localPoints(float[] world){
  if(world==null||world.length%3!=0)throw new IllegalArgumentException("Invalid overlay");
  float[] out=new float[world.length];
  for(int i=0;i<world.length;i+=3){float[] p=localPoint(world[i],world[i+1],world[i+2]);System.arraycopy(p,0,out,i,3);}
  return out;
 }
 public ViewportMeshRayPicker.Hit worldHit(ViewportMeshRayPicker.Hit localHit){
  return localHit==null?null:localHit.translated(originX,originY,originZ);
 }
}
