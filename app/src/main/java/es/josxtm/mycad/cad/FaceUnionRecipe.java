package es.josxtm.mycad.cad;
/** A reversible geometric recipe for an axis-aligned face-extrusion union. */
public final class FaceUnionRecipe {
 public final String id;
 public final double width,depth,height,x,y,z,distance;
 public final BoxFaceSelection.Face face;
 public FaceUnionRecipe(double width,double depth,double height,double x,double y,double z,
                        BoxFaceSelection.Face face,double distance){
  this(java.util.UUID.randomUUID().toString(),width,depth,height,x,y,z,face,distance);
 }
 /** Restore a saved face-union recipe with its original result identity. */
 public FaceUnionRecipe(String id,double width,double depth,double height,double x,double y,double z,
                        BoxFaceSelection.Face face,double distance){
  if(id==null||!java.util.UUID.fromString(id).toString().equals(id))
   throw new IllegalArgumentException("Invalid face union result UUID");
  this.id=id;
  if(face==null||!Double.isFinite(width)||!Double.isFinite(depth)||!Double.isFinite(height)||
    !Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||
    width<=0||depth<=0||height<=0||width>10000||depth>10000||height>10000)
   throw new IllegalArgumentException("Invalid source");
  FacePrismExtrusion.create(width,depth,height,x,y,z,0,0,0,face,distance);
  this.width=width;this.depth=depth;this.height=height;this.x=x;this.y=y;this.z=z;this.face=face;this.distance=distance;
 }
 public FaceUnionRecipe withDistance(double amount){return new FaceUnionRecipe(id,width,depth,height,x,y,z,face,amount);}
 public double unionVolume(){
  FacePrismExtrusion.Result added=FacePrismExtrusion.create(width,depth,height,x,y,z,0,0,0,face,distance);
  return width*depth*height+added.w*added.d*added.h;
 }
}