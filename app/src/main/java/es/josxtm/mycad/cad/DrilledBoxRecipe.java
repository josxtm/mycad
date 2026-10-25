package es.josxtm.mycad.cad;

/** Editable circular through-hole recipe; legacy Z-axis constructor remains compatible. */
public final class DrilledBoxRecipe {
 public final double x,y,z,width,depth,height,holeX,holeY,diameter;
 public final OrientedThroughHole.Axis axis;
 public final double rotationX,rotationY,rotationZ;
 public DrilledBoxRecipe(double x,double y,double z,double width,double depth,double height,
                         double holeX,double holeY,double diameter){
  this(x,y,z,width,depth,height,holeX,holeY,diameter,OrientedThroughHole.Axis.Z,0,0,0);
 }
 public DrilledBoxRecipe(double x,double y,double z,double width,double depth,double height,
                         double holeX,double holeY,double diameter,
                         OrientedThroughHole.Axis axis,double rotationX,double rotationY,double rotationZ){
  this.x=x;this.y=y;this.z=z;this.width=width;this.depth=depth;this.height=height;
  this.holeX=holeX;this.holeY=holeY;this.diameter=diameter;
  this.axis=axis;this.rotationX=rotationX;this.rotationY=rotationY;this.rotationZ=rotationZ;
  regenerate(); // Validate before exposing or saving recipe.
 }
 public DrilledBoxRecipe withHole(double offsetX,double offsetY,double diameter){
  return new DrilledBoxRecipe(x,y,z,width,depth,height,offsetX,offsetY,diameter,axis,rotationX,rotationY,rotationZ);
 }
 public DrilledBoxRecipe withAxis(OrientedThroughHole.Axis next){
  return new DrilledBoxRecipe(x,y,z,width,depth,height,holeX,holeY,diameter,next,rotationX,rotationY,rotationZ);
 }
 public float[] regenerate(){
  return OrientedThroughHole.triangles(width,depth,height,holeX,holeY,diameter,axis,
    new RigidTransform(x,y,z,rotationX,rotationY,rotationZ));
 }
 public double nominalVolume(){
  double length=axis==OrientedThroughHole.Axis.X?width:
                axis==OrientedThroughHole.Axis.Y?depth:height;
  return width*depth*height-Math.PI*diameter*diameter/4*length;
 }
}
