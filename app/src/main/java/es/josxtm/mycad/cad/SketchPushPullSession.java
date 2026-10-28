package es.josxtm.mycad.cad;

/** Immutable profile snapshot for a positive, one-finger sketch extrusion.
 * Preview geometry is detached from the document until the UI commits it.
 */
public final class SketchPushPullSession {
 public final String sketchId;
 private final int type,plane;
 private final double x1,y1,x2,y2;
 public SketchPushPullSession(String id,int type,int plane,double x1,double y1,double x2,double y2){
  if(id==null||id.isEmpty())throw new IllegalArgumentException("Missing sketch identity");
  LinkedSketchSolidPlan.build(type,plane,x1,y1,x2,y2,1);
  sketchId=id;this.type=type;this.plane=plane;
  this.x1=x1;this.y1=y1;this.x2=x2;this.y2=y2;
 }
 public boolean matches(String id,int type,int plane,double x1,double y1,double x2,double y2){
  return sketchId.equals(id)&&this.type==type&&this.plane==plane&&
   this.x1==x1&&this.y1==y1&&this.x2==x2&&this.y2==y2;
 }
 /** Upward screen travel, in density-independent pixels; 8 dp per mm.
  * Integer-mm snapping allows an exact 10 mm extrusion without a keyboard.
  * Returning to the start yields zero, which the caller treats as cancellation.
  */
 public double heightForTravel(double upwardDp){
  return Math.max(0,DragPrecision.quantize(upwardDp/8,10000,1));
 }
 public LinkedSketchSolidPlan.Body propose(double height){
  return LinkedSketchSolidPlan.build(type,plane,x1,y1,x2,y2,height);
 }
}
