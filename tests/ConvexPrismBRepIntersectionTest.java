import es.josxtm.mycad.cad.*;
import java.util.*;

public final class ConvexPrismBRepIntersectionTest {
 private static final String BODY_A="cbe96e18-0001-4001-8001-000000000001";
 private static final String BODY_B="cbe96e18-0001-4001-8001-000000000002";
 private static final String BODY_C="cbe96e18-0001-4001-8001-000000000003";
 private static void ok(boolean yes,String label){if(!yes)throw new AssertionError(label);}
 private static void near(double a,double b,String label){
  if(Math.abs(a-b)>1e-4*Math.max(1,Math.abs(b)))
   throw new AssertionError(label+": "+a+" vs "+b);
 }
 private static void invalid(Runnable f,String label){
  try{f.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Unsafe Boolean accepted: "+label);
 }
 private static PolygonPrismBRep shape(String id,int plane,double distance,
                                      double[][] points,boolean reverse){
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<points.length;i++){
   int a=reverse?(points.length-i)%points.length:i,
       b=reverse?(points.length-i-1+points.length)%points.length:(i+1)%points.length;
   String edge=UUID.nameUUIDFromBytes((id+"/source/"+i).getBytes(
       java.nio.charset.StandardCharsets.UTF_8)).toString();
   ids.add(edge);
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,
    points[a][0],points[a][1],points[b][0],points[b][1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,distance,ids).asBRep(lines);
 }
 /** The winding predicate must preserve sub-mm areas far from zero. */
 private static void translatedWindingPrecision(){
  try{
   java.lang.reflect.Method area=ConvexPrismBRepIntersection.class
       .getDeclaredMethod("twiceArea",List.class);
   area.setAccessible(true);
   for(double origin:new double[]{9000,-9000}){
    double side=(origin+0.2)-origin;
    List<double[]> ring=new ArrayList<>(Arrays.asList(
        new double[]{origin,origin},new double[]{origin+0.2,origin},
        new double[]{origin+0.2,origin+0.2},new double[]{origin,origin+0.2}));
    double expected=2*side*side;
    double ccw=((Double)area.invoke(null,ring)).doubleValue();
    if(Math.abs(ccw-expected)>1e-10)
     throw new AssertionError("Translated CCW intersection area: "+ccw+" != "+expected);
    Collections.reverse(ring);
    double cw=((Double)area.invoke(null,ring)).doubleValue();
    if(Math.abs(cw+expected)>1e-10)
     throw new AssertionError("Translated CW intersection area: "+cw+" != "+(-expected));
   }
  }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
 }
 /** Clipped B-Rep edges must not lose sub-mm overlap precision when
  * the sketch is translated far from the work-plane origin. */
 private static void translatedIntersectionReconstructionPrecision(){
  for(double origin:new double[]{9000,-9000})
   for(int plane=0;plane<3;plane++)
    for(int sign:new int[]{-1,1}){
     double[][] left={{origin,origin},{origin+0.4,origin},
         {origin+0.4,origin+0.4},{origin,origin+0.4}};
     double[][] right={{origin+0.2,origin},{origin+0.6,origin},
         {origin+0.6,origin+0.4},{origin+0.2,origin+0.4}};
     PolygonPrismBRep a=shape(BODY_A,plane,sign*2,left,false);
     PolygonPrismBRep b=shape(BODY_B,plane,sign*2,right,false);
     ConvexPrismBRepIntersection.Result forward=
         ConvexPrismBRepIntersection.intersect(a,b);
     ConvexPrismBRepIntersection.Result reverse=
         ConvexPrismBRepIntersection.intersect(b,a);
     double expected=((origin+0.4)-(origin+0.2))*
                     ((origin+0.4)-origin);
     if(Math.abs(forward.area-expected)>1e-6||
        Math.abs(forward.volume-2*expected)>2e-6)
      throw new AssertionError("Translated intersection lost precision: "+
          forward.area+" / "+forward.volume+" expected "+expected);
     ok(forward.solid.id().equals(reverse.solid.id()),
         "translated intersection UUID independent of operand order");
     ok(Arrays.equals(forward.triangles(),reverse.triangles()),
         "translated intersection tessellation independent of operand order");
     TriangleMeshHealth.Report mesh=TriangleMeshHealth.inspect(forward.triangles());
     ok(mesh.isWatertight()&&mesh.shells==1,
         "translated intersection remains watertight");
     ok(a.vertices().size()==8&&b.vertices().size()==8,
         "translated intersection does not mutate operands");
     // A sub-float-ULP edit must produce a distinct analytic topology ID,
     // even if the same source feature IDs are reused after sketch editing.
     double[][] shifted={{origin+0.2001,origin},{origin+0.6001,origin},
         {origin+0.6001,origin+0.4},{origin+0.2001,origin+0.4}};
     PolygonPrismBRep edited=shape(BODY_B,plane,sign*2,shifted,false);
     ConvexPrismBRepIntersection.Result changed=
         ConvexPrismBRepIntersection.intersect(a,edited);
     ok(!forward.solid.id().equals(changed.solid.id()),
         "sub-mm edit must change intersection topology UUID");
     ok(Math.abs(forward.area-changed.area)>1e-5,
         "edited intersection must have distinct analytic area");
     ok(changed.solid.id().equals(
         ConvexPrismBRepIntersection.intersect(edited,a).solid.id()),
         "edited intersection UUID remains commutative");
    }
 }
 public static void main(String[] args){
  translatedWindingPrecision();
  translatedIntersectionReconstructionPrecision();
  double[][] square={{0,0},{4,0},{4,4},{0,4}};
  double[][] clip={{2,1},{5,1},{5,3},{2,3}};
  double[][] triangle={{1,1},{3,1},{1,3}};
  double[][] far={{7,7},{9,7},{9,9},{7,9}};
  double[][] touch={{4,0},{6,0},{6,4},{4,4}};
  double[][] concave={{0,0},{3,0},{3,1},{1,1},{1,3},{0,3}};
  // A large section with a shallow reflex corner was previously accepted:
  // the global area-scaled turn tolerance hid its 0.9 mm2 notch.
  double[][] shallowConcave={{-9000,-9000},{9000,-9000},
      {9000,9000},{0,8999.9999},{-9000,9000}};
  for(int plane=0;plane<3;plane++)for(double signed:new double[]{1,-1}){
   final int fixedPlane=plane;
   PolygonPrismBRep a=shape(BODY_A,plane,signed*3,square,false);
   PolygonPrismBRep b=shape(BODY_B,plane,signed*2,clip,true);
   ConvexPrismBRepIntersection.Result result=
      ConvexPrismBRepIntersection.intersect(a,b);
   near(result.area,4,"overlap 2x2 area");
   near(result.volume,8,"intersection volume");
   near(result.distance,signed*2,"height clipped by shorter prism");
   ok(result.plane==plane,"preserve sketch plane");
   ok(result.solid.vertices().size()==8&&result.solid.faces().size()==6,
      "new analytic rectangular B-Rep shell");
   TriangleMeshHealth.Report mesh=TriangleMeshHealth.inspect(result.triangles());
   ok(mesh.isWatertight()&&mesh.shells==1,"Boolean watertight");
   near(mesh.signedVolume,8,"Positive signed mesh volume");
   ConvexPrismBRepIntersection.Result swapped=
      ConvexPrismBRepIntersection.intersect(b,a);
   near(swapped.area,result.area,"commutative area");
   near(swapped.volume,result.volume,"commutative volume");
   ok(result.solid.id().equals(swapped.solid.id()),
      "commutative B-Rep UUID across operand order");
   ok(Arrays.equals(result.triangles(),swapped.triangles()),
      "commutative B-Rep tessellation across operand order");
   PolygonPrismBRep contained=shape(BODY_C,fixedPlane,signed,triangle,false);
   ConvexPrismBRepIntersection.Result inner=
      ConvexPrismBRepIntersection.intersect(a,contained);
   near(inner.area,2,"contained triangle area");
   near(inner.volume,2,"contained triangle volume");
   ok(TriangleMeshHealth.inspect(inner.triangles()).isWatertight(),
      "Contained triangle watertight");
   invalid(()->ConvexPrismBRepIntersection.intersect(
       a,shape(BODY_C,fixedPlane,signed,far,false)),"disjoint prisms");
   invalid(()->ConvexPrismBRepIntersection.intersect(
       a,shape(BODY_C,fixedPlane,signed,touch,false)),"touching without volume");
   invalid(()->ConvexPrismBRepIntersection.intersect(
       a,shape(BODY_C,fixedPlane,-signed*2,square,false)),
       "opposite extrusion direction");
   invalid(()->ConvexPrismBRepIntersection.intersect(
       a,shape(BODY_C,fixedPlane,signed*2,concave,false)),
       "concave operand outside supported topology");
   PolygonPrismBRep shallow=shape(BODY_C,fixedPlane,signed*2,
       shallowConcave,false);
   invalid(()->ConvexPrismBRepIntersection.intersect(a,shallow),
       "large near-convex section must not silently fill a shallow notch");
   invalid(()->ConvexPrismBRepIntersection.intersect(shallow,a),
       "shallow concavity rejected in both operand orders");
   ok(a.faces().size()==6&&b.faces().size()==6,
      "operands unchanged after Boolean");
  }
  PolygonPrismBRep a=shape(BODY_A,0,2,square,false);
  PolygonPrismBRep b=shape(BODY_B,1,2,clip,false);
  invalid(()->ConvexPrismBRepIntersection.intersect(a,b),"different planes");
  invalid(()->ConvexPrismBRepIntersection.intersect(null,a),"null");
  System.out.println("ConvexPrismBRepIntersectionTest: OK");
 }
}
