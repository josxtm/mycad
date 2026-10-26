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
 /**
  * Place the drag gizmo at the actual analytic ray hit, rather than at a
  * fixed +X point on cylindrical sides. Revalidates the surface against the
  * CURRENT body so a stale selection cannot silently edit changed geometry.
  */
 public static PrimitiveFaceHandle fromHit(PrimitiveBRepBody body,
                                           AnalyticPrimitiveRayPicker.Hit hit){
  if(body==null||hit==null||!body.bodyId.equals(hit.bodyId))
   throw new IllegalArgumentException("Hit belongs to a different body");
  // A face can remain at the same world point after editing ANOTHER face.
  // Reject that stale selection as well, even if it still lies on the plane.
  if(!hit.matchesGeometry(body))
   throw new IllegalArgumentException("Hit was computed for older geometry");
  PrimitiveBRepBody.Face face=body.findFace(hit.faceId);
  if(face==null)throw new IllegalArgumentException("Stale face hit");
  double[] local=body.pose().inversePoint(hit.x,hit.y,hit.z);
  double eps=1e-7*Math.max(1,Math.max(body.width,Math.max(body.depth,body.height)));
  double lx=local[0],ly=local[1],lz=local[2];
  boolean onSurface;
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   boolean inBounds=Math.abs(lx)<=body.width/2+eps&&
     Math.abs(ly)<=body.depth/2+eps&&lz>=-eps&&lz<=body.height+eps;
   BoxFaceSelection.Face slot=BoxFaceSelection.Face.valueOf(face.kind);
   switch(slot){
    case X_MIN:onSurface=Math.abs(lx+body.width/2)<=eps;break;
    case X_MAX:onSurface=Math.abs(lx-body.width/2)<=eps;break;
    case Y_MIN:onSurface=Math.abs(ly+body.depth/2)<=eps;break;
    case Y_MAX:onSurface=Math.abs(ly-body.depth/2)<=eps;break;
    case Z_MIN:onSurface=Math.abs(lz)<=eps;break;
    case Z_MAX:onSurface=Math.abs(lz-body.height)<=eps;break;
    default:throw new IllegalArgumentException("Unknown face");
   }
   onSurface&=inBounds;
  }else{
   double radial=Math.hypot(lx,ly),radius=body.width/2;
   CylinderBRep.Kind slot=CylinderBRep.Kind.valueOf(face.kind);
   switch(slot){
    case CYLINDER_SIDE:
     onSurface=Math.abs(radial-radius)<=eps&&lz>=-eps&&lz<=body.height+eps;
     break;
    case CYLINDER_BOTTOM:
     onSurface=Math.abs(lz)<=eps&&radial<=radius+eps;break;
    case CYLINDER_TOP:
     onSurface=Math.abs(lz-body.height)<=eps&&radial<=radius+eps;break;
    default:throw new IllegalArgumentException("Unknown face");
   }
  }
  if(!onSurface)throw new IllegalArgumentException("Hit no longer on face");
  double[] normal=face.outwardNormalAt(Math.atan2(ly,lx));
  return new PrimitiveFaceHandle(body.bodyId,hit.faceId,hit.point(),normal);
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
