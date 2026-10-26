package es.josxtm.mycad.cad;

/**
 * World anchor + one of the two incident face normals for a selected
 * analytic edge. Movement remains a constrained primitive offset, NOT a
 * free-form bend, bevel or fillet. Only a visual drag handle is generated.
 */
public final class PrimitiveEdgeHandle {
 public final String bodyId,edgeId,faceId;
 public final int incidentIndex;
 private final double[] anchor,normal;
 private PrimitiveEdgeHandle(String bodyId,String edgeId,String faceId,int incident,
                             double[] anchor,double[] normal){
  this.bodyId=bodyId;this.edgeId=edgeId;this.faceId=faceId;
  this.incidentIndex=incident;
  this.anchor=anchor.clone();this.normal=normal.clone();
 }
 public static PrimitiveEdgeHandle from(PrimitiveBRepBody body,String edgeId,
                                        int adjacentFaceIndex){
  if(body==null||edgeId==null||adjacentFaceIndex<0||adjacentFaceIndex>1)
   throw new IllegalArgumentException("Invalid edge handle");
  PrimitiveBRepBody.Edge edge=body.findEdge(edgeId);
  if(edge==null||edge.adjacentFaceIds().size()!=2)
   throw new IllegalArgumentException("Unknown/stale analytic edge");
  String faceId=edge.adjacentFaceIds().get(adjacentFaceIndex);
  PrimitiveBRepBody.Face face=body.findFace(faceId);
  if(face==null)throw new IllegalArgumentException("Missing adjacent face");
  double[] anchor;
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   CadKernel.Box box=new CadKernel.Box(body.bodyId,-body.width/2,
     -body.depth/2,0,body.width,body.depth,body.height);
   BoxBRep solid=BoxBRep.from(new TransformedBody(box,body.pose()));
   BoxBRep.Edge line=solid.findEdge(edgeId);
   double[] a=null,b=null;
   for(BoxBRep.Vertex v:solid.vertices()){
    if(v.id.equals(line.startId))a=v.position();
    if(v.id.equals(line.endId))b=v.position();
   }
   if(a==null||b==null)throw new IllegalArgumentException("Missing endpoints");
   anchor=new double[]{(a[0]+b[0])/2,(a[1]+b[1])/2,(a[2]+b[2])/2};
  }else{
   CylinderBRep solid=CylinderBRep.from(body.bodyId,body.width,body.height,body.pose());
   CylinderBRep.CircularEdge rim=solid.findEdge(edgeId);
   if(rim==null)throw new IllegalArgumentException("Missing circle");
   // Anchor is ON the circle, at angle 0 in the local coordinate frame.
   anchor=rim.pointAt(0);
  }
  double[] normal=face.outwardNormalAt(0);
  double length=Math.sqrt(normal[0]*normal[0]+normal[1]*normal[1]+
                          normal[2]*normal[2]);
  if(!Double.isFinite(length)||Math.abs(length-1)>1e-7)
   throw new IllegalArgumentException("Invalid adjacent face normal");
  return new PrimitiveEdgeHandle(body.bodyId,edgeId,faceId,
                                 adjacentFaceIndex,anchor,normal);
 }
 public double[] anchor(){return anchor.clone();}
 public double[] normal(){return normal.clone();}
 public float[] gpu(){
  float[] out=new float[6];
  for(int k=0;k<3;k++){
   if(!Double.isFinite(anchor[k])||!Double.isFinite(normal[k])||
      Math.abs(anchor[k])>Float.MAX_VALUE)
    throw new IllegalArgumentException("Coordinate not renderable");
   out[k]=(float)anchor[k];out[k+3]=(float)normal[k];
  }
  return out;
 }
}
