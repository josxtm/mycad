import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Regression cases for convex-result partial-overlap B-Rep unions. */
public final class ConvexPrismBRepConvexUnionTest {
 private static PolygonPrismBRep prism(String name,int plane,double height,double[][] points){
  String id=UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<points.length;i++){
   String edge=UUID.nameUUIDFromBytes((name+"/edge/"+i)
       .getBytes(StandardCharsets.UTF_8)).toString();
   ids.add(edge);
   double[] p=points[i],q=points[(i+1)%points.length];
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,p[0],p[1],q[0],q[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids).asBRep(lines);
 }
 private static void near(double actual,double expected,String label){
  if(Math.abs(actual-expected)>1e-5*Math.max(1,Math.abs(expected)))
   throw new AssertionError(label+": "+actual+" != "+expected);
 }
 private static void reject(Runnable operation,String label){
  try{operation.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Unsupported union accepted: "+label);
 }
 public static void main(String[] args){
  double[][] a={{0,0},{4,0},{4,3},{0,3}};
  double[][] b={{2,0},{6,0},{6,3},{2,3}};
  double[][] inner={{1,1},{2,1},{2,2},{1,2}};
  double[][] far={{10,0},{12,0},{12,3},{10,3}};
  double[][] touching={{4,0},{6,0},{6,3},{4,3}};
  double[][] l1={{0,0},{4,0},{4,2},{0,2}};
  double[][] l2={{2,0},{4,0},{4,4},{2,4}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   final int p=plane,s=sign;
   PolygonPrismBRep left=prism("left"+p+s,p,s*3,a);
   PolygonPrismBRep right=prism("right"+p+s,p,s*3,b);
   PolygonPrismBRep result=ConvexPrismBRepConvexUnion.union(left,right);
   PolygonPrismBRep reverse=ConvexPrismBRepConvexUnion.union(right,left);
   near(result.sectionArea(),18,"union section area");
   near(result.volume(),54,"union volume");
   near(reverse.volume(),54,"reverse union volume");
   if(!result.id().equals(reverse.id()))
    throw new AssertionError("Operand order changed union identity");
   if(result.faces().size()!=6||result.edges().size()!=12||
      result.halfEdges().size()!=24)
    throw new AssertionError("Union has invalid analytic half-edge topology");
   if(!TriangleMeshHealth.inspect(result.triangles()).isWatertight())
    throw new AssertionError("Union preview is not watertight");
   near(left.volume(),36,"source A unchanged");
   near(right.volume(),36,"source B unchanged");

   PolygonPrismBRep contained=prism("inner"+p+s,p,s*3,inner);
   PolygonPrismBRep nested=ConvexPrismBRepConvexUnion.union(left,contained);
   if(!nested.id().equals(left.id()))
    throw new AssertionError("Nested union should reuse containing topology");
   reject(()->ConvexPrismBRepConvexUnion.union(left,
     prism("concave"+p+s,p,s*3,l2)), "nonconvex union");
   reject(()->ConvexPrismBRepConvexUnion.union(
     prism("L-a"+p+s,p,s*3,l1),prism("L-b"+p+s,p,s*3,l2)),
     "L-shaped nonconvex union");
   reject(()->ConvexPrismBRepConvexUnion.union(left,
     prism("far"+p+s,p,s*3,far)),"disconnected");
   reject(()->ConvexPrismBRepConvexUnion.union(left,
     prism("touch"+p+s,p,s*3,touching)),"edge-only contact");
   reject(()->ConvexPrismBRepConvexUnion.union(left,
     prism("height"+p+s,p,s*2,b)),"different heights");
   reject(()->ConvexPrismBRepConvexUnion.union(left,
     prism("direction"+p+s,p,-s*3,b)),"opposite directions");
   reject(()->ConvexPrismBRepConvexUnion.union(left,
     prism("plane"+p+s,(p+1)%3,s*3,b)),"different planes");
  }
  System.out.println("ConvexPrismBRepConvexUnionTest: OK");
 }
}
