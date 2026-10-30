import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class ConvexPrismBRepHullUnionTest {
 private static void near(double a,double b,String message){
  if(Math.abs(a-b)>1e-4*Math.max(1,Math.abs(b)))
   throw new AssertionError(message+": "+a+" != "+b);
 }
 private static void ok(boolean condition,String message){
  if(!condition)throw new AssertionError(message);
 }
 private static void reject(Runnable action,String message){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Unsafe union accepted: "+message);
 }
 private static double[][] rect(double x,double y,double xx,double yy){
  return new double[][]{{x,y},{xx,y},{xx,yy},{x,yy}};
 }
 private static PolygonPrismBRep prism(String key,int plane,double h,double[][] poly){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
  ArrayList<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<poly.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/edge/"+i)
       .getBytes(StandardCharsets.UTF_8)).toString();
   ids.add(edge);
   double[] a=poly[i],b=poly[(i+1)%poly.length];
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,
       a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,h,ids).asBRep(lines);
 }
 /** Two analytic unions with the same source IDs but a sub-float change
  * in the section must not receive the same deterministic B-Rep UUID. */
 private static void distinctDoubleGeometryHasDistinctUnionId(){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   String key="double-uuid-"+plane+"-"+sign;
   PolygonPrismBRep left=prism(key+"-left",plane,sign*2,rect(0,0,1,1));
   PolygonPrismBRep right=prism(key+"-right",plane,sign*2,rect(1,0,2,1));
   // A 1e-7 mm change at x=2 is lost when converted to float.
   PolygonPrismBRep wider=prism(key+"-right",plane,sign*2,
       rect(1,0,2+1e-7,1));
   ConvexPrismBRepHullUnion.Result original=
       ConvexPrismBRepHullUnion.union(left,right);
   ConvexPrismBRepHullUnion.Result changed=
       ConvexPrismBRepHullUnion.union(left,wider);
   ok(!original.solid.id().equals(changed.solid.id()),
       "distinct double geometry must not reuse a union UUID");
   ok(Math.abs(changed.area-(2+1e-7))<1e-12,
       "sub-float edit must reach the analytic section, not only its UUID");
   ok(Math.abs(changed.volume-(4+2e-7))<1e-12,
       "sub-float edit must retain its analytic volume");
   double maxU=Double.NEGATIVE_INFINITY;
   for(PolygonPrismBRep.Vertex v:changed.solid.vertices())
    maxU=Math.max(maxU,plane==2?v.y:v.x);
   ok(Double.compare(maxU,2+1e-7)==0,
       "reconstructed vertices must retain the edited double coordinate");
   ok(original.solid.id().equals(
       ConvexPrismBRepHullUnion.union(right,left).solid.id()),
       "original union UUID remains commutative");
   ok(changed.solid.id().equals(
       ConvexPrismBRepHullUnion.union(wider,left).solid.id()),
       "perturbed union UUID remains commutative");
  }
 }
 /** A 0.2 mm convex contour translated far from zero must keep its
  * signed area; world-coordinate shoelace loses several 1e-9 mm². */
 private static void translatedAreaPrecision(){
  try{
   java.lang.reflect.Method area=ConvexPrismBRepHullUnion.class
       .getDeclaredMethod("area",List.class);
   area.setAccessible(true);
   for(double origin:new double[]{9000,-9000}){
    double side=(origin+0.2)-origin;
    List<double[]> ring=new ArrayList<>(Arrays.asList(
        new double[]{origin,origin},new double[]{origin+0.2,origin},
        new double[]{origin+0.2,origin+0.2},new double[]{origin,origin+0.2}));
    double expected=side*side;
    double ccw=((Double)area.invoke(null,ring)).doubleValue();
    if(Math.abs(ccw-expected)>1e-10)
     throw new AssertionError("Translated convex CCW area: "+ccw+" != "+expected);
    Collections.reverse(ring);
    double cw=((Double)area.invoke(null,ring)).doubleValue();
    if(Math.abs(cw+expected)>1e-10)
     throw new AssertionError("Translated convex CW area: "+cw+" != "+(-expected));
   }
  }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
 }
 /** Reconstructed B-Rep edges must retain double coordinates. Casting
  * to float at +/-9000 mm changes a 0.4 mm section by more than the
  * analytic Boolean volume tolerance, even though the source is valid. */
 private static void translatedUnionReconstructionPrecision(){
  for(double origin:new double[]{9000,-9000})
   for(double offset:new double[]{0.2,0.4})
   for(int plane=0;plane<3;plane++)
    for(int sign:new int[]{-1,1}){
     String key="translated-"+origin+"-"+offset+"-"+plane+"-"+sign;
     PolygonPrismBRep left=prism(key+"-left",plane,sign*2,
         rect(origin,origin,origin+0.4,origin+0.4));
     PolygonPrismBRep right=prism(key+"-right",plane,sign*2,
         rect(origin+offset,origin,origin+0.8,origin+0.4));
     double[] leftBefore=left.triangleCoordinates(),rightBefore=right.triangleCoordinates();
     ConvexPrismBRepHullUnion.Result forward=
         ConvexPrismBRepHullUnion.union(left,right);
     ConvexPrismBRepHullUnion.Result reverse=
         ConvexPrismBRepHullUnion.union(right,left);
     double expected=((origin+0.8)-origin)*((origin+0.4)-origin);
     if(Math.abs(forward.area-expected)>1e-12||
        Math.abs(forward.volume-2*expected)>2e-12)
      throw new AssertionError("Translated union lost double precision: "+
          forward.area+" / "+forward.volume+" expected "+expected);
     ok(forward.solid.id().equals(reverse.solid.id()),
         "translated union UUID independent of operand order");
     ok(Arrays.equals(forward.triangles(),reverse.triangles()),
         "translated union tessellation independent of operand order");
     ok(Arrays.equals(forward.solid.triangleCoordinates(),reverse.solid.triangleCoordinates()),
         "analytic tessellation independent of operand order");
     if(offset<0.4){
      double common=ConvexPrismBRepIntersection.intersect(left,right).volume;
      ok(Math.abs(forward.volume-(left.volume()+right.volume()-common))<2e-12,
          "overlapping union conserves analytic volume");
     }
     PolygonPrismBRep solid=forward.solid;
     ok(solid.vertices().size()==8&&solid.edges().size()==12&&
         solid.faces().size()==6&&solid.halfEdges().size()==24,
         "translated rectangular union has complete analytic topology");
     Map<String,PolygonPrismBRep.HalfEdge> uses=new HashMap<>();
     for(PolygonPrismBRep.HalfEdge e:solid.halfEdges())uses.put(e.id,e);
     for(PolygonPrismBRep.HalfEdge e:solid.halfEdges()){
      PolygonPrismBRep.HalfEdge twin=uses.get(e.twinId);
      ok(twin!=null&&twin.twinId.equals(e.id)&&twin.edgeId.equals(e.edgeId)&&
          twin.fromId.equals(e.toId)&&twin.toId.equals(e.fromId),
          "each analytic half-edge has a reciprocal oppositely directed twin");
      ok(uses.get(e.nextId).previousId.equals(e.id)&&
          uses.get(e.previousId).nextId.equals(e.id),"closed half-edge face loops");
     }
     TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(forward.triangles());
     ok(health.isWatertight()&&health.shells==1,
         "translated union remains watertight");
     ok(Arrays.equals(leftBefore,left.triangleCoordinates())&&
         Arrays.equals(rightBefore,right.triangleCoordinates()),
         "translated union does not mutate source B-Rep shells");
    }
 }
 public static void main(String[] args){
  translatedAreaPrecision();
  distinctDoubleGeometryHasDistinctUnionId();
  translatedUnionReconstructionPrecision();
  double[][] upper={{0,0},{4,0},{2,2}};
  double[][] lower={{0,0},{2,-2},{4,0}};
  double[][] triangle={{1,1},{2,1},{1,2}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   final int p=plane,s=sign;
   PolygonPrismBRep a=prism("upper"+p+s,p,s*3,upper);
   PolygonPrismBRep b=prism("lower"+p+s,p,s*3,lower);
   ConvexPrismBRepHullUnion.Result diamond=ConvexPrismBRepHullUnion.union(a,b);
   near(diamond.area,8,"slanted shared-edge union area");
   near(diamond.volume,24,"slanted shared-edge union volume");
   near(diamond.distance,s*3,"signed height");
   ok(diamond.solid.vertices().size()==8,"four-corner diamond");
   ok(diamond.solid.vertices().size()-diamond.solid.edges().size()+
      diamond.solid.faces().size()==2,"closed analytic half-edge topology");
   TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(diamond.triangles());
   ok(health.isWatertight()&&health.shells==1,"watertight slanted union");
   near(health.signedVolume,24,"positive mesh winding");
   ok(Arrays.equals(diamond.triangles(),ConvexPrismBRepHullUnion.union(b,a).triangles()),
      "commutative deterministic tessellation");
   PolygonPrismBRep left=prism("left"+p+s,p,s*3,rect(0,0,3,2));
   PolygonPrismBRep right=prism("right"+p+s,p,s*3,rect(2,0,5,2));
   ConvexPrismBRepHullUnion.Result overlap=ConvexPrismBRepHullUnion.union(left,right);
   near(overlap.area,10,"partially overlapping rectangles");
   near(overlap.volume,30,"partially overlapping volume");
   ok(TriangleMeshHealth.inspect(overlap.triangles()).isWatertight(),
      "overlap mesh watertight");
   // Tiny accepted height differences must not make union order-dependent.
   PolygonPrismBRep almostSameHeight=prism("almostSameHeight"+p+s,p,
       s*(3+1e-10),rect(2,0,5,2));
   ConvexPrismBRepHullUnion.Result forward=
       ConvexPrismBRepHullUnion.union(left,almostSameHeight);
   ConvexPrismBRepHullUnion.Result reverse=
       ConvexPrismBRepHullUnion.union(almostSameHeight,left);
   near(forward.distance,s*3,"canonical signed near-equal height");
   ok(Double.compare(forward.distance,reverse.distance)==0,
       "near-equal union distance independent of operand order");
   ok(forward.solid.id().equals(reverse.solid.id()),
       "near-equal union UUID independent of operand order");
   ok(Arrays.equals(forward.triangles(),reverse.triangles()),
       "near-equal union tessellation independent of operand order");
   near(forward.volume,reverse.volume,"near-equal union volume");
   PolygonPrismBRep outer=prism("outer"+p+s,p,s*3,rect(0,0,4,4));
   PolygonPrismBRep inner=prism("inner"+p+s,p,s*3,triangle);
   near(ConvexPrismBRepHullUnion.union(outer,inner).volume,48,
       "nested convex union");
   reject(()->ConvexPrismBRepHullUnion.union(
      prism("offsetA"+p+s,p,s*3,rect(0,0,3,3)),
      prism("offsetB"+p+s,p,s*3,rect(2,2,5,5))),"concave union");
   // A thin step is concave, but its hull-area error is below the
   // historical 1e-5 relative area tolerance. Never fill that gap.
   PolygonPrismBRep slightlyTaller=prism("tinyStep"+p+s,p,s*3,
       rect(2,0,5,2.00001));
   reject(()->ConvexPrismBRepHullUnion.union(left,slightlyTaller),
       "tiny concave step must not be filled by hull");
   reject(()->ConvexPrismBRepHullUnion.union(slightlyTaller,left),
       "tiny concave step also rejected in reverse operand order");
   reject(()->ConvexPrismBRepHullUnion.union(left,
      prism("far"+p+s,p,s*3,rect(6,0,8,2))),"disconnected union");
   reject(()->ConvexPrismBRepHullUnion.union(left,
      prism("point"+p+s,p,s*3,rect(3,2,5,4))),"point-only contact");
   reject(()->ConvexPrismBRepHullUnion.union(left,
      prism("height"+p+s,p,s*3.00001,rect(2,0,5,2))),"unequal height");
   reject(()->ConvexPrismBRepHullUnion.union(left,
      prism("opposite"+p+s,p,-s*3,rect(2,0,5,2))),"opposite extrusion");
   ok(a.faces().size()==5&&b.faces().size()==5,"operands unchanged");
  }
  reject(()->ConvexPrismBRepHullUnion.union(
      prism("different1",0,3,upper),prism("different2",1,3,lower)),
      "different work planes");
  reject(()->ConvexPrismBRepHullUnion.union(
      null,prism("nonnull",0,3,upper)),"null body");
  System.out.println("ConvexPrismBRepHullUnionTest: OK");
 }
}
