package es.josxtm.mycad.cad;

/** Mapping rectangular 2D sketches to axis-aligned 3D boxes in millimetres. */
public final class PlaneExtrusion {
 private PlaneExtrusion(){}
 public enum Plane { XY, XZ, YZ }
 public static CadKernel.Result<CadKernel.Box> extrude(CadKernel kernel,Plane plane,
     double u,double v,double offset,double sizeU,double sizeV,double height){
  if(kernel==null||plane==null)return CadKernel.Result.invalid("Motor o plano ausente");
  if(!Double.isFinite(u)||!Double.isFinite(v)||!Double.isFinite(offset)
     ||!Double.isFinite(sizeU)||!Double.isFinite(sizeV)||!Double.isFinite(height)
     ||sizeU<=0||sizeV<=0||height==0)return CadKernel.Result.invalid("Perfil o altura inválidos");
  final double origin=height<0?offset+height:offset;
  final double thickness=Math.abs(height);
  switch(plane){
   case XY:return kernel.createBox(u,v,origin,sizeU,sizeV,thickness);
   case XZ:return kernel.createBox(u,origin,v,sizeU,thickness,sizeV);
   case YZ:return kernel.createBox(origin,u,v,thickness,sizeU,sizeV);
   default:return CadKernel.Result.invalid("Plano desconocido");
  }
 }
}
