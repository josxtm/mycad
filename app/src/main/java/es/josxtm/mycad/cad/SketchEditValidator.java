package es.josxtm.mycad.cad;
/** Preflight checks for editing a legacy sketch with multiple linked extrusions. */
public final class SketchEditValidator {
 private SketchEditValidator(){}
 public static boolean canUpdateRectangle(int plane,double x1,double y1,double x2,double y2,double[] heights){
  if(plane<0||plane>=PlaneExtrusion.Plane.values().length||heights==null)return false;
  double x=Math.min(x1,x2),y=Math.min(y1,y2),w=Math.abs(x2-x1),d=Math.abs(y2-y1);
  CadKernel kernel=new PrimitiveKernel();
  if(heights.length==0)return PlaneExtrusion.extrude(kernel,PlaneExtrusion.Plane.values()[plane],x,y,0,w,d,1).isSuccess();
  for(double height:heights)
   if(!PlaneExtrusion.extrude(kernel,PlaneExtrusion.Plane.values()[plane],x,y,0,w,d,height).isSuccess())return false;
  return true;
 }
}
