package es.josxtm.mycad.cad;

import java.util.List;

/**
 * Pure-Java adapter from Android viewport touches to the existing indexed
 * triangle scene. Shares its clipped camera ray with stepped analytic B-Rep
 * selection, while preserving global triangle IDs for legacy UI actions.
 */
public final class ViewportMeshRayPicker {
 private ViewportMeshRayPicker(){}

 public static final class Candidate {
  public final MeshRayIndex index;
  public final int triangleCount;
  public Candidate(MeshRayIndex index,int triangleCount){
   if(index==null||triangleCount<0)
    throw new IllegalArgumentException("Invalid viewport mesh candidate");
   this.index=index;this.triangleCount=triangleCount;
  }
 }

 public static final class Hit {
  public final int triangle;
  public final double distance,x,y,z,ox,oy,oz,dx,dy,dz;
  private Hit(int triangle,CadRayPicker.Hit source,
      SteppedViewportRayPicker.Ray ray){
   this.triangle=triangle;distance=source.distance;
   x=source.x;y=source.y;z=source.z;
   double[] origin=ray.origin(),direction=ray.direction();
   ox=origin[0];oy=origin[1];oz=origin[2];
   dx=direction[0];dy=direction[1];dz=direction[2];
  }
  private Hit(Hit source,double tx,double ty,double tz){
   triangle=source.triangle;distance=source.distance;
   x=source.x+tx;y=source.y+ty;z=source.z+tz;
   ox=source.ox+tx;oy=source.oy+ty;oz=source.oz+tz;
   dx=source.dx;dy=source.dy;dz=source.dz;
   for(double value:new double[]{x,y,z,ox,oy,oz})if(!Double.isFinite(value))
    throw new IllegalArgumentException("Invalid translated viewport hit");
  }
  /** Rigid translation preserves direction, distance and global triangle ID. */
  public Hit translated(double tx,double ty,double tz){return new Hit(this,tx,ty,tz);}

 }

 /**
  * The closest visible mesh wins; ties retain the first candidate, matching
  * the GPU viewer's historical ordering. No mesh, matrix or camera is changed.
  * A hit beyond the far clipping plane is never reported.
  */
 public static Hit pick(List<Candidate> candidates,double[] viewProjection,
      int width,int height,double pixelX,double pixelY){
  if(candidates==null)
   throw new IllegalArgumentException("Missing viewport meshes");
  SteppedViewportRayPicker.Ray ray=SteppedViewportRayPicker.ray(
      viewProjection,width,height,pixelX,pixelY);
  double[] origin=ray.origin(),direction=ray.direction();
  Hit nearest=null;
  int offset=0;
  for(Candidate candidate:candidates){
   if(candidate==null||candidate.index==null||candidate.triangleCount<0)
    throw new IllegalArgumentException("Invalid viewport mesh candidate");
   if(candidate.triangleCount>Integer.MAX_VALUE-offset)
    throw new IllegalArgumentException("Viewport triangle index overflow");
   CadRayPicker.Hit hit=candidate.index.pick(origin[0],origin[1],origin[2],
       direction[0],direction[1],direction[2]);
   if(hit!=null&&hit.triangle>=0&&hit.triangle<candidate.triangleCount&&
      hit.distance<=ray.farDistance()+1e-9*Math.max(1,ray.farDistance())&&
      (nearest==null||hit.distance<nearest.distance))
    nearest=new Hit(offset+hit.triangle,hit,ray);
   offset+=candidate.triangleCount;
  }
  return nearest;
 }
}
