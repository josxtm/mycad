package es.josxtm.mycad.cad;
/** Updates a source rectangle by moving one local extrusion side face. The sketch's plane does not change the local axes. */
public final class LinkedFaceResize {
 private LinkedFaceResize(){}
 public static double[] resize(int plane,double x1,double y1,double x2,double y2,BoxFaceSelection.Face face,double delta){
  if(plane<0||plane>2||face==null||!Double.isFinite(x1)||!Double.isFinite(x2)||!Double.isFinite(y1)||!Double.isFinite(y2)
    ||!Double.isFinite(delta))throw new IllegalArgumentException("Invalid linked face");
  double minX=Math.min(x1,x2),maxX=Math.max(x1,x2),minY=Math.min(y1,y2),maxY=Math.max(y1,y2);
  if(maxX-minX<=0||maxY-minY<=0)throw new IllegalArgumentException("Invalid rectangle");
  switch(face){
   case X_MIN:minX-=delta;break;
   case X_MAX:maxX+=delta;break;
   case Y_MIN:minY-=delta;break;
   case Y_MAX:maxY+=delta;break;
   default:throw new IllegalArgumentException("Only rectangle side faces can resize the sketch");
  }
  if(maxX-minX<=0||maxY-minY<=0||maxX-minX>10000||maxY-minY>10000)
   throw new IllegalArgumentException("Invalid resized rectangle");
  return new double[]{x2>=x1?minX:maxX,y2>=y1?minY:maxY,x2>=x1?maxX:minX,y2>=y1?maxY:minY};
 }
}