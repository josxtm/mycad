package es.josxtm.mycad.cad;

/** World-space anchor and outward direction for a primitive face drag gizmo.
 * The handle is visual-only; the actual edit is PrimitiveMovePreview.forFace().
 */
public final class PrimitiveFaceHandle {
 public final String bodyId,faceId;
 private final double[] anchor,normal;
 private PrimitiveFaceHandle(String id,String faceId,double[] anchor,double[] normal){
  this.bodyId=id;this.faceId=faceId;
  this.anchor=anchor.clone();this.normal=normal.clone();
 }
 public static PrimitiveFaceHandle from(PrimitiveBRepBody body,String faceId){
  if(body==null||faceId==null||body.findFace(faceId)==null)
   throw new IllegalArgumentException("Unknown or stale face");
  double w=body.width,d=body.depth,h=body.height;
  double x=0,y=0,z=0;
  final double[] p;
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   BoxFaceSelection.Face face=BoxFaceSelection.Face.valueOf(
     faceId.substring((body.bodyId+"/face/").length()));
   switch(face){
    case X_MIN:x=-w/2;z=h/2;break;
    case X_MAX:x=w/2;z=h/2;break;
    case Y_MIN:y=-d/2;z=h/2;break;
    case Y_MAX:y=d/2;z=h/2;break;
    case Z_MIN:z=0;break;
    case Z_MAX:z=h;break;
    default:throw new IllegalArgumentException("Unsupported planar face");
   }
   p=body.pose().point(x,y,z);
  }else{
   CylinderBRep.Kind face=CylinderBRep.Kind.valueOf(
     faceId.substring((body.bodyId+"/face/").length()));
   switch(face){
    case CYLINDER_SIDE:x=w/2;z=h/2;break;
    case CYLINDER_BOTTOM:z=0;break;
    case CYLINDER_TOP:z=h;break;
    default:throw new IllegalArgumentException("Unsupported cylinder face");
   }
   p=body.pose().point(x,y,z);
  }
  double[] direction=body.findFace(faceId).outwardNormalAt(0);
  double len=Math.sqrt(direction[0]*direction[0]+direction[1]*direction[1]+
                       direction[2]*direction[2]);
  if(!Double.isFinite(len)||Math.abs(len-1)>1e-7)
   throw new IllegalArgumentException("Invalid face normal");
  return new PrimitiveFaceHandle(body.bodyId,faceId,p,direction);
 }
 public double[] anchor(){return anchor.clone();}
 public double[] normal(){return normal.clone();}
 /** GPU input: XYZ anchor, XYZ unit normal. */
 public float[] gpu(){
  float[] out=new float[6];
  for(int i=0;i<3;i++){
   if(!Double.isFinite(anchor[i])||!Double.isFinite(normal[i])||
      Math.abs(anchor[i])>Float.MAX_VALUE)
    throw new IllegalArgumentException("Face outside GPU space");
   out[i]=(float)anchor[i];
   out[i+3]=(float)normal[i];
  }
  return out;
 }
}
