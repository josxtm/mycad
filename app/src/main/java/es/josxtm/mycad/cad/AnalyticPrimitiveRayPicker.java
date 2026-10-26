package es.josxtm.mycad.cad;

/**
 * Exact intersection with the analytic faces of a box or circular cylinder.
 * Coordinates, offsets and hit distances are millimetres in the WORLD frame.
 * No tessellation, triangle IDs or Android dependencies.
 *
 * The first positive (visible) surface is returned, including when the ray
 * starts inside the solid. This is not picking for imported/Boolean meshes.
 */
public final class AnalyticPrimitiveRayPicker {
 private static final double EPS=1e-9;
 private AnalyticPrimitiveRayPicker(){}

 public static final class Hit {
  public final String bodyId,faceId;
  public final double distance,x,y,z,nx,ny,nz;
  private Hit(String bodyId,String faceId,double distance,double[] point,double[] normal){
   this.bodyId=bodyId;this.faceId=faceId;this.distance=distance;
   x=point[0];y=point[1];z=point[2];
   nx=normal[0];ny=normal[1];nz=normal[2];
  }
  public double[] point(){return new double[]{x,y,z};}
  /** Outward world-space unit normal at the exact hit (including curved sides). */
  public double[] normal(){return new double[]{nx,ny,nz};}
 }

 private static Hit choose(Hit best,PrimitiveBRepBody body,String slot,
                           double t,double[] localOrigin,double[] localDir,
                           double[] worldOrigin,double[] worldDir,
                           double unit){
  if(!Double.isFinite(t)||t<=EPS)return best;
  double lx=localOrigin[0]+t*localDir[0],ly=localOrigin[1]+t*localDir[1],
         lz=localOrigin[2]+t*localDir[2];
  double boundaryEps=1e-8*Math.max(1,Math.max(body.width,Math.max(body.depth,body.height)));
  boolean valid;
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   valid=Math.abs(lx)<=body.width/2+boundaryEps&&
         Math.abs(ly)<=body.depth/2+boundaryEps&&
         lz>=-boundaryEps&&lz<=body.height+boundaryEps;
  }else{
   double rad=body.width/2;
   double radial=Math.hypot(lx,ly);
   valid=lz>=-boundaryEps&&lz<=body.height+boundaryEps&&
        (slot.equals("CYLINDER_SIDE")?
         Math.abs(radial-rad)<=1e-9*Math.max(1,rad):
         radial<=rad+boundaryEps);
  }
  if(!valid)return best;
  double distance=t/unit;
  double[] p={worldOrigin[0]+worldDir[0]*distance,
              worldOrigin[1]+worldDir[1]*distance,
              worldOrigin[2]+worldDir[2]*distance};
  if(!Double.isFinite(distance)||!Double.isFinite(p[0])||
     !Double.isFinite(p[1])||!Double.isFinite(p[2]))return best;
  String faceId=body.bodyId+"/face/"+slot;
  PrimitiveBRepBody.Face face=body.findFace(faceId);
  if(face==null)return best;
  double[] normal=face.outwardNormalAt(Math.atan2(ly,lx));
  Hit next=new Hit(body.bodyId,faceId,distance,p,normal);
  if(best==null||distance<best.distance-1e-8)return next;
  // Deterministic tie at a shared rim/corner; face IDs are persistent.
  if(Math.abs(distance-best.distance)<=1e-8&&
     next.faceId.compareTo(best.faceId)<0)return next;
  return best;
 }

 public static Hit pick(PrimitiveBRepBody body,double ox,double oy,double oz,
                        double dx,double dy,double dz){
  if(body==null||!Double.isFinite(ox)||!Double.isFinite(oy)||
     !Double.isFinite(oz)||!Double.isFinite(dx)||
     !Double.isFinite(dy)||!Double.isFinite(dz))
   throw new IllegalArgumentException("Invalid analytic ray");
  double unit=Math.hypot(dx,Math.hypot(dy,dz));
  if(!Double.isFinite(unit)||unit<1e-10)
   throw new IllegalArgumentException("Degenerate ray direction");
  double[] o=body.pose().inversePoint(ox,oy,oz);
  double[] end=body.pose().inversePoint(ox+dx/unit,oy+dy/unit,oz+dz/unit);
  double[] dir={end[0]-o[0],end[1]-o[1],end[2]-o[2]};
  double[] worldOrigin={ox,oy,oz};
  double[] worldDir={dx/unit,dy/unit,dz/unit};
  // The local ray is effectively unit length, but explicitly convert t to
  // world distance when inverse transform is applied to a normalized step.
  Hit best=null;
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   String[] slots={"X_MIN","X_MAX","Y_MIN","Y_MAX","Z_MIN","Z_MAX"};
   double[] limits={-body.width/2,body.width/2,-body.depth/2,
                    body.depth/2,0,body.height};
   for(int i=0;i<6;i++){
    int axis=i/2;
    if(Math.abs(dir[axis])<=1e-12)continue;
    double t=(limits[i]-o[axis])/dir[axis];
    best=choose(best,body,slots[i],t,o,dir,worldOrigin,worldDir,1);
   }
  }else{
   double r=body.width/2,a=dir[0]*dir[0]+dir[1]*dir[1],
          b=2*(o[0]*dir[0]+o[1]*dir[1]),
          c=o[0]*o[0]+o[1]*o[1]-r*r;
   if(a>1e-16){
    double disc=b*b-4*a*c;
    // A broad relative epsilon can turn a near-tangent MISS into a false hit.
    // Accept only round-off on the quadratic terms, then verify the radius.
    double discScale=Math.max(1,Math.max(Math.abs(b*b),Math.abs(4*a*c)));
    if(disc>=-64*Math.ulp(discScale)){
     disc=Math.max(0,disc);
     // Numerically stable quadratic roots when b is large.
     double sqrt=Math.sqrt(disc),q=-.5*(b+Math.copySign(sqrt,b));
     if(Math.abs(q)>1e-14){
      best=choose(best,body,"CYLINDER_SIDE",q/a,o,dir,worldOrigin,worldDir,1);
      best=choose(best,body,"CYLINDER_SIDE",c/q,o,dir,worldOrigin,worldDir,1);
     }else{
      best=choose(best,body,"CYLINDER_SIDE",-b/(2*a),o,dir,worldOrigin,worldDir,1);
     }
    }
   }
   if(Math.abs(dir[2])>1e-12){
    best=choose(best,body,"CYLINDER_BOTTOM",-o[2]/dir[2],
                o,dir,worldOrigin,worldDir,1);
    best=choose(best,body,"CYLINDER_TOP",(body.height-o[2])/dir[2],
                o,dir,worldOrigin,worldDir,1);
   }
  }
  return best;
 }
}
