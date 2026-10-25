package es.josxtm.mycad.cad;
/** Editable recipe for a vertical circular through-hole in an axis-aligned box. */
public final class DrilledBoxRecipe {
 public final double x,y,z,width,depth,height,holeX,holeY,diameter;
 public DrilledBoxRecipe(double x,double y,double z,double width,double depth,double height,
                         double holeX,double holeY,double diameter){
  CircularThroughHole.triangles(x,y,z,width,depth,height,holeX,holeY,diameter);
  this.x=x;this.y=y;this.z=z;this.width=width;this.depth=depth;this.height=height;
  this.holeX=holeX;this.holeY=holeY;this.diameter=diameter;
 }
 public DrilledBoxRecipe withHole(double offsetX,double offsetY,double diameter){
  return new DrilledBoxRecipe(x,y,z,width,depth,height,offsetX,offsetY,diameter);
 }
 public float[] regenerate(){return CircularThroughHole.triangles(x,y,z,width,depth,height,holeX,holeY,diameter);}
 public double nominalVolume(){return width*depth*height-Math.PI*diameter*diameter/4*height;}
}
