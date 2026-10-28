package es.josxtm.mycad.cad;

/** Editable circular through-hole recipe; legacy Z-axis constructor remains compatible. */
public final class DrilledBoxRecipe {
 public final String id;
 public final double x,y,z,width,depth,height,holeX,holeY,diameter;
 public final OrientedThroughHole.Axis axis;
 public final double rotationX,rotationY,rotationZ;
 /** 0 = through-hole, otherwise blind depth in millimetres from the positive local-axis face. */
 public final double drillDepth;
 public final String sourceSketchId;
 public final double sourceHeight;
 public DrilledBoxRecipe(double x,double y,double z,double width,double depth,double height,
                         double holeX,double holeY,double diameter){
  this(x,y,z,width,depth,height,holeX,holeY,diameter,OrientedThroughHole.Axis.Z,0,0,0);
 }
 public DrilledBoxRecipe(double x,double y,double z,double width,double depth,double height,
                         double holeX,double holeY,double diameter,
                         OrientedThroughHole.Axis axis,double rotationX,double rotationY,double rotationZ){
  this(x,y,z,width,depth,height,holeX,holeY,diameter,axis,rotationX,rotationY,rotationZ,0);
 }
 public DrilledBoxRecipe(double x,double y,double z,double width,double depth,double height,
                         double holeX,double holeY,double diameter,
                         OrientedThroughHole.Axis axis,double rotationX,double rotationY,double rotationZ,
                         double drillDepth){
  this(java.util.UUID.randomUUID().toString(),x,y,z,width,depth,height,holeX,holeY,diameter,axis,rotationX,rotationY,rotationZ,drillDepth,"",0);
 }
 /** Restore a saved recipe without changing its result identity. */
 public DrilledBoxRecipe(String id,double x,double y,double z,double width,double depth,double height,
                         double holeX,double holeY,double diameter,OrientedThroughHole.Axis axis,
                         double rotationX,double rotationY,double rotationZ,double drillDepth){
  this(id,x,y,z,width,depth,height,holeX,holeY,diameter,axis,rotationX,rotationY,rotationZ,drillDepth,"",0);
 }
 private DrilledBoxRecipe(String id,double x,double y,double z,double width,double depth,double height,
                         double holeX,double holeY,double diameter,OrientedThroughHole.Axis axis,
                         double rotationX,double rotationY,double rotationZ,double drillDepth,
                         String sourceSketchId,double sourceHeight){
  if(id==null||!java.util.UUID.fromString(id).toString().equals(id))
   throw new IllegalArgumentException("Invalid drilling result UUID");
  this.id=id;
  if(sourceSketchId==null||(sourceSketchId.isEmpty()&&sourceHeight!=0)||(!sourceSketchId.isEmpty()&&
      (!java.util.UUID.fromString(sourceSketchId).toString().equals(sourceSketchId)||
       !Double.isFinite(sourceHeight)||sourceHeight<=0||sourceHeight>10000)))
   throw new IllegalArgumentException("Invalid linked drilling source");
  this.sourceSketchId=sourceSketchId;this.sourceHeight=sourceHeight;
  this.x=x;this.y=y;this.z=z;this.width=width;this.depth=depth;this.height=height;
  this.holeX=holeX;this.holeY=holeY;this.diameter=diameter;
  this.axis=axis;this.rotationX=rotationX;this.rotationY=rotationY;this.rotationZ=rotationZ;this.drillDepth=drillDepth;
  regenerate(); // Validate before exposing or saving recipe.
 }
 public DrilledBoxRecipe withHole(double offsetX,double offsetY,double diameter){
  return new DrilledBoxRecipe(id,x,y,z,width,depth,height,offsetX,offsetY,diameter,axis,rotationX,rotationY,rotationZ,drillDepth,sourceSketchId,sourceHeight);
 }
 public DrilledBoxRecipe withAxis(OrientedThroughHole.Axis next){
  return new DrilledBoxRecipe(id,x,y,z,width,depth,height,holeX,holeY,diameter,next,rotationX,rotationY,rotationZ,drillDepth,sourceSketchId,sourceHeight);
 }
 public DrilledBoxRecipe withDepth(double newDepth){
  return new DrilledBoxRecipe(id,x,y,z,width,depth,height,holeX,holeY,diameter,axis,
                              rotationX,rotationY,rotationZ,newDepth,sourceSketchId,sourceHeight);
 }
 public DrilledBoxRecipe linkedTo(String id,double extrusionHeight){
  return new DrilledBoxRecipe(this.id,x,y,z,width,depth,height,holeX,holeY,diameter,axis,
   rotationX,rotationY,rotationZ,drillDepth,id,extrusionHeight);
 }
 public float[] regenerate(){
  return OrientedThroughHole.triangles(width,depth,height,holeX,holeY,diameter,axis,
    new RigidTransform(x,y,z,rotationX,rotationY,rotationZ),drillDepth);
 }
 public double nominalVolume(){
  double length=axis==OrientedThroughHole.Axis.X?width:
                axis==OrientedThroughHole.Axis.Y?depth:height;
  return width*depth*height-Math.PI*diameter*diameter/4*(drillDepth==0?length:drillDepth);
 }
}
