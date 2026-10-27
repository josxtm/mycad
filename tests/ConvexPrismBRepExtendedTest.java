import es.josxtm.mycad.cad.*;
import java.util.*;

/** Regression cases for existing convex clipping, not a general Boolean engine. */
public final class ConvexPrismBRepExtendedTest {
 static final double TOL=1e-4;
 static void near(double actual,double expected,String label){
  if(Math.abs(actual-expected)>TOL*Math.max(1,Math.abs(expected)))
   throw new AssertionError(label+": "+actual+" != "+expected);
 }
 static PolygonPrismBRep prism(String key,int plane,double height,double[][] p){
  String id=UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
  ArrayList<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> edges=new LinkedHashMap<>();
  for(int i=0;i<p.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
   ids.add(edge);
   double[] a=p[i],b=p[(i+1)%p.length];
   edges.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids).asBRep(edges);
 }
 public static void main(String[] args){
  double[][] square={{0,0},{4,0},{4,4},{0,4}};
  double[][] diamond={{2,0},{4,2},{2,4},{0,2}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep a=prism("square"+plane+sign,plane,sign*5,square);
   PolygonPrismBRep b=prism("diamond"+plane+sign,plane,sign*2,diamond);
   ConvexPrismBRepIntersection.Result ab=ConvexPrismBRepIntersection.intersect(a,b);
   ConvexPrismBRepIntersection.Result ba=ConvexPrismBRepIntersection.intersect(b,a);
   near(ab.area,8,"non-orthogonal diamond area");
   near(ab.volume,16,"different-height intersection volume");
   near(ab.distance,sign*2,"signed overlap height");
   near(ba.volume,ab.volume,"commutative volume");
   TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(ab.triangles());
   if(!report.isWatertight()||report.shells!=1)
    throw new AssertionError("Non-orthogonal B-Rep must be watertight");
  }
  System.out.println("ConvexPrismBRepExtendedTest: OK");
 }
}
