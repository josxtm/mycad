package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Analytic GPU outline of a selected primitive edge. Returns a world-space
 * LINE_STRIP, independent of the triangle mesh used to display the solid.
 * The points are only a rendering aid; the edge remains analytic in B-Rep.
 */
public final class PrimitiveEdgeCurve {
 private PrimitiveEdgeCurve(){}
 private static float f(double value){
  if(!Double.isFinite(value)||Math.abs(value)>Float.MAX_VALUE)
   throw new IllegalArgumentException("Edge point outside GPU range");
  return (float)value;
 }
 private static void store(float[] dst,int at,double[] p){
  if(p==null||p.length!=3)throw new IllegalArgumentException("Invalid edge point");
  for(int i=0;i<3;i++)dst[at+i]=f(p[i]);
 }
 /** Include the first point again for a closed cylinder rim. */
 public static float[] points(PrimitiveBRepBody body,String edgeId,int circleSegments){
  if(body==null||edgeId==null||body.findEdge(edgeId)==null)
   throw new IllegalArgumentException("Unknown or stale edge");
  if(circleSegments<12||circleSegments>256)
   throw new IllegalArgumentException("Invalid outline quality");
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   CadKernel.Box box=new CadKernel.Box(body.bodyId,
     -body.width/2,-body.depth/2,0,body.width,body.depth,body.height);
   BoxBRep solid=BoxBRep.from(new TransformedBody(box,body.pose()));
   BoxBRep.Edge edge=solid.findEdge(edgeId);
   Map<String,double[]> byId=new HashMap<>();
   for(BoxBRep.Vertex vertex:solid.vertices())
    byId.put(vertex.id,vertex.position());
   float[] result=new float[6];
   store(result,0,byId.get(edge.startId));
   store(result,3,byId.get(edge.endId));
   return result;
  }
  CylinderBRep solid=CylinderBRep.from(body.bodyId,body.width,body.height,body.pose());
  CylinderBRep.CircularEdge edge=solid.findEdge(edgeId);
  float[] result=new float[(circleSegments+1)*3];
  for(int i=0;i<=circleSegments;i++)
   store(result,i*3,edge.pointAt(i*2*Math.PI/circleSegments));
  // Exact closure after float quantisation, avoiding a one-pixel seam.
  for(int i=0;i<3;i++)result[circleSegments*3+i]=result[i];
  return result;
 }
}
