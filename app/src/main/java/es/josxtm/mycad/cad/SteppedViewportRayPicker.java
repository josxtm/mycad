package es.josxtm.mycad.cad;

import java.util.List;

/**
 * Converts a touch in a top-left-origin viewport into a world-space ray and
 * resolves the nearest stepped B-Rep patch. Matrices use OpenGL column-major
 * order (projection * view); depth is OpenGL NDC [-1, 1].
 * No Android or OpenGL state is accessed by this pure geometry adapter.
 */
public final class SteppedViewportRayPicker {
 private SteppedViewportRayPicker(){}

 public static final class Ray {
  private final double[] origin, direction;
  private final double farDistance;
  private Ray(double[] origin,double[] direction,double farDistance){
   this.origin=origin.clone();this.direction=direction.clone();
   this.farDistance=farDistance;
  }
  public double[] origin(){return origin.clone();}
  /** Unit world-space direction; returned as a defensive copy. */
  public double[] direction(){return direction.clone();}
  /** Length of the visible near-to-far clipping segment in world units. */
  public double farDistance(){return farDistance;}
 }

 /**
  * Pixel coordinates are measured from the top-left viewport corner. The
  * returned ray starts on the near clipping plane, so clipped objects cannot
  * be selected. Both orthographic and perspective projections are supported.
  * The caller must provide the view-projection matrix of the rendered frame.
  */
 public static Ray ray(double[] viewProjection,int width,int height,
                       double pixelX,double pixelY){
  if(viewProjection==null||viewProjection.length!=16||width<=0||height<=0||
      !Double.isFinite(pixelX)||!Double.isFinite(pixelY)||
      pixelX<0||pixelX>width||pixelY<0||pixelY>height)
   throw new IllegalArgumentException("Invalid viewport or touch coordinate");
  double[] inverse=invert(viewProjection);
  double x=2*pixelX/width-1;
  double y=1-2*pixelY/height;
  double[] near=unproject(inverse,x,y,-1);
  double[] far=unproject(inverse,x,y,1);
  double[] direction={far[0]-near[0],far[1]-near[1],far[2]-near[2]};
  double norm=Math.hypot(Math.hypot(direction[0],direction[1]),direction[2]);
  if(!(norm>1e-12)||!Double.isFinite(norm))
   throw new IllegalArgumentException("Degenerate viewport ray");
  for(int i=0;i<3;i++)direction[i]/=norm;
  return new Ray(near,direction,norm);
 }

 /** Selects the visible patch without modifying the scene or the matrix. */
 public static SteppedSceneRayPicker.Hit pick(
     List<SteppedSceneRayPicker.Candidate> candidates,
     double[] viewProjection,int width,int height,double pixelX,double pixelY){
  Ray ray=ray(viewProjection,width,height,pixelX,pixelY);
  SteppedSceneRayPicker.Hit hit=SteppedSceneRayPicker.pick(
      candidates,ray.origin(),ray.direction());
  // The model-space picker uses an infinite ray. Honor the viewport far clip.
  return hit!=null&&hit.patchHit.distance<=ray.farDistance+1e-9?hit:null;
 }

 private static double[] unproject(double[] inverse,double x,double y,double z){
  double[] result=new double[4];
  for(int row=0;row<4;row++)
   result[row]=inverse[row]*x+inverse[4+row]*y+
       inverse[8+row]*z+inverse[12+row];
  double w=result[3];
  if(!Double.isFinite(w)||w==0)
   throw new IllegalArgumentException("Invalid homogeneous depth");
  double[] point={result[0]/w,result[1]/w,result[2]/w};
  for(double value:point)
   if(!Double.isFinite(value))
    throw new IllegalArgumentException("Nonfinite unprojected point");
  return point;
 }

 /** Gauss-Jordan inversion with partial pivoting; input is never modified. */
 private static double[] invert(double[] matrix){
  double[][] a=new double[4][8];
  double scale=0;
  for(int row=0;row<4;row++){
   for(int col=0;col<4;col++){
    double value=matrix[col*4+row];
    if(!Double.isFinite(value))
     throw new IllegalArgumentException("Nonfinite view-projection matrix");
    a[row][col]=value;
    scale=Math.max(scale,Math.abs(value));
   }
   a[row][row+4]=1;
  }
  if(scale==0)throw new IllegalArgumentException("Singular view-projection matrix");
  for(int col=0;col<4;col++){
   int pivot=col;
   for(int row=col+1;row<4;row++)
    if(Math.abs(a[row][col])>Math.abs(a[pivot][col]))pivot=row;
   if(Math.abs(a[pivot][col])<=scale*1e-14)
    throw new IllegalArgumentException("Singular view-projection matrix");
   double[] tmp=a[col];a[col]=a[pivot];a[pivot]=tmp;
   double divisor=a[col][col];
   for(int k=0;k<8;k++)a[col][k]/=divisor;
   for(int row=0;row<4;row++)if(row!=col){
    double factor=a[row][col];
    for(int k=0;k<8;k++)a[row][k]-=factor*a[col][k];
   }
  }
  double[] inverse=new double[16];
  for(int row=0;row<4;row++)for(int col=0;col<4;col++){
   double value=a[row][col+4];
   if(!Double.isFinite(value))
    throw new IllegalArgumentException("Unstable view-projection inverse");
   inverse[col*4+row]=value;
  }
  return inverse;
 }
}
