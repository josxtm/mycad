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
 public static void main(String[] args){
  double[][] square={{0,0},{4,0},{4,4},{0,4}};
  double[][] clip={{2,1},{5,1},{5,3},{2,3}};
  double[][] triangle={{1,1},{3,1},{1,3}};
  double[][] far={{7,7},{9,7},{9,9},{7,9}};
  double[][] touch={{4,0},{6,0},{6,4},{4,4}};
  double[][] concave={{0,0},{3,0},{3,1},{1,1},{1,3},{0,3}};
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
