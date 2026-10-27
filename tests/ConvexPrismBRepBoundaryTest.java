import es.josxtm.mycad.cad.*;
import java.util.*;

/** Boundaries of the currently supported convex B-Rep intersection. */
public final class ConvexPrismBRepBoundaryTest {
 static PolygonPrismBRep prism(String key,int plane,double h,double[][] p){
  String id=UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
  ArrayList<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> segments=new LinkedHashMap<>();
  for(int i=0;i<p.length;i++){
   String e=UUID.nameUUIDFromBytes((key+"/edge/"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
   ids.add(e);
   double[] a=p[i],b=p[(i+1)%p.length];
   segments.put(e,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,h,ids).asBRep(segments);
 }
 static void reject(Runnable action,String name){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Expected safe rejection: "+name);
 }
 static void near(double actual,double expected,String name){
  if(Math.abs(actual-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(name+" expected "+expected+" got "+actual);
 }
 public static void main(String[] args){
  double[][] square={{0,0},{4,0},{4,4},{0,4}};
  double[][] corner={{4,4},{6,4},{6,6},{4,6}};
  double[][] far={{10,10},{12,10},{12,12},{10,12}};
  double[][] triangle={{1,1},{3,1},{1,3}};
  for(int plane=0;plane<3;plane++){
   final int fixed=plane;
   PolygonPrismBRep a=prism("boundary-a-"+plane,plane,4,square);
   reject(()->ConvexPrismBRepIntersection.intersect(a,prism("corner"+fixed,fixed,3,corner)),"point contact");
   reject(()->ConvexPrismBRepIntersection.intersect(a,prism("far"+fixed,fixed,3,far)),"disjoint");
   ConvexPrismBRepIntersection.Result inner=ConvexPrismBRepIntersection.intersect(a,prism("tri"+fixed,fixed,2,triangle));
   near(inner.area,2,"contained triangle area");
   near(inner.volume,4,"contained triangle volume");
   if(!TriangleMeshHealth.inspect(inner.triangles()).isWatertight())
    throw new AssertionError("contained triangle must remain watertight");
  }
  System.out.println("ConvexPrismBRepBoundaryTest: OK");
 }
}
