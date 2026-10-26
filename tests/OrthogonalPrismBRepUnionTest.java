import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class OrthogonalPrismBRepUnionTest {
 private static final String A="ca1d9876-0000-4000-8000-000000000001",
                             B="ca1d9876-0000-4000-8000-000000000002";
 private static void ok(boolean valid,String title){
  if(!valid)throw new AssertionError(title);
 }
 private static void close(double value,double expected,String title){
  if(Math.abs(value-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(title+": "+value+" != "+expected);
 }
 private static void fail(Runnable action,String title){
  try{action.run();}catch(IllegalArgumentException good){return;}
  throw new AssertionError("Rejected case passed: "+title);
 }
 private static PolygonPrismBRep profile(String id,int plane,double h,double[][] xy){
  ArrayList<String> names=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<xy.length;i++){
   String line=UUID.nameUUIDFromBytes((id+"/line/"+i).getBytes(StandardCharsets.UTF_8))
      .toString();
   int next=(i+1)%xy.length;
   names.add(line);
   lines.put(line,new ClosedSketchExtrusion.Segment(plane,
      xy[i][0],xy[i][1],xy[next][0],xy[next][1]));
  }
  return new LoopExtrusionFeature(id,names.get(0),plane,h,names).asBRep(lines);
 }
 private static PolygonPrismBRep rectangle(String id,int plane,double h,
    double x1,double y1,double x2,double y2){
  return profile(id,plane,h,new double[][]{
   {x1,y1},{x2,y1},{x2,y2},{x1,y2}
  });
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(double sign:new double[]{1,-1}){
   final int fixedPlane=plane;
   final double height=sign*3;
   PolygonPrismBRep a=rectangle(A,plane,height,0,0,4,2);
   PolygonPrismBRep b=rectangle(B,plane,height,2,1,5,4);
   OrthogonalPrismBRepUnion.Result joined=OrthogonalPrismBRepUnion.union(a,b);
   close(joined.area,15,"L-profile union section area");
   close(joined.volume,45,"L-profile union volume");
   close(joined.distance,height,"Signed distance retained");
   ok(joined.solid.vertices().size()==16,
      "Union creates analytic concave polygon, not two glued boxes");
   ok(joined.solid.faces().size()==10,"8 side faces plus two caps");
   ok(joined.solid.edges().size()==24,"Watertight halfedges shared");
   TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(joined.triangles());
   ok(report.isWatertight()&&report.shells==1,
      "Concave union has exactly one closed manifold shell");
   close(report.signedVolume,45,"Positive mesh signed volume");
   OrthogonalPrismBRepUnion.Result swapped=OrthogonalPrismBRepUnion.union(b,a);
   close(swapped.volume,joined.volume,"Commutative volume");
   ok(Arrays.equals(swapped.triangles(),joined.triangles()),
      "Deterministic and operand-order independent output");
   ok(joined.solid.faces().size()>a.faces().size(),
      "Union is a new analytic solid");
   ok(a.faces().size()==6&&b.faces().size()==6,"Input bodies unchanged");
   PolygonPrismBRep crossA=rectangle(A,plane,height,0,1,6,3);
   PolygonPrismBRep crossB=rectangle(B,plane,height,2,0,4,4);
   OrthogonalPrismBRepUnion.Result cross=OrthogonalPrismBRepUnion.union(crossA,crossB);
   close(cross.area,16,"Cross shape union area");
   close(cross.volume,48,"Cross volume");
   ok(cross.solid.faces().size()==14,"Cross has 12 analytic side faces");
   ok(TriangleMeshHealth.inspect(cross.triangles()).isWatertight(),
      "Cross profile tessellates");
   PolygonPrismBRep outer=rectangle(A,plane,height,0,0,4,4);
   PolygonPrismBRep inner=rectangle(B,plane,height,1,1,3,3);
   OrthogonalPrismBRepUnion.Result contained=OrthogonalPrismBRepUnion.union(outer,inner);
   close(contained.area,16,"Containment returns outer rectangle");
   close(contained.volume,48,"Containment volume");
   ok(contained.solid.faces().size()==6,"Containment analytic box");
   PolygonPrismBRep disjoint=rectangle(B,plane,height,6,6,9,9);
   fail(()->OrthogonalPrismBRepUnion.union(a,disjoint),
      "Disjoint union would need multiple shells");
   PolygonPrismBRep touching=rectangle(B,plane,height,4,0,8,2);
   fail(()->OrthogonalPrismBRepUnion.union(a,touching),"No volumetric overlap");
   PolygonPrismBRep differentHeight=rectangle(B,plane,sign*4,2,1,5,4);
   fail(()->OrthogonalPrismBRepUnion.union(a,differentHeight),
      "Unsupported stacked elevations");
   PolygonPrismBRep inverted=rectangle(B,plane,-height,2,1,5,4);
   fail(()->OrthogonalPrismBRepUnion.union(a,inverted),"Opposite signed heights");
   PolygonPrismBRep slanted=profile(B,plane,height,new double[][]{
    {2,1},{5,1},{4,3},{2,3}
   });
   fail(()->OrthogonalPrismBRepUnion.union(a,slanted),"Nonrectangle polygon");
   PolygonPrismBRep concave=profile(B,plane,height,new double[][]{
    {0,0},{3,0},{3,1},{1,1},{1,3},{0,3}
   });
   fail(()->OrthogonalPrismBRepUnion.union(a,concave),"Concave operand");
   ok(joined.plane==fixedPlane,"Plane unchanged");
  }
  fail(()->OrthogonalPrismBRepUnion.union(
       rectangle(A,0,2,0,0,4,4),rectangle(B,1,2,2,2,5,5)),
       "Different base planes");
  fail(()->OrthogonalPrismBRepUnion.union(null,
       rectangle(B,0,2,0,0,4,4)),"Null operand");
  System.out.println("OrthogonalPrismBRepUnionTest: OK");
 }
}
