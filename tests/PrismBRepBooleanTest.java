import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PrismBRepBooleanTest {
 private static PolygonPrismBRep prism(String key,int plane,double height,double[][] poly){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<poly.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/edge/"+i).getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=poly[i],b=poly[(i+1)%poly.length];
   ids.add(edge);
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids).asBRep(lines);
 }
 private static double[][] rect(double x,double y,double xx,double yy){
  return new double[][]{{x,y},{xx,y},{xx,yy},{x,yy}};
 }
 private static void near(double actual,double expected,String label){
  if(Math.abs(actual-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(label+": "+actual+" != "+expected);
 }
 private static void check(PrismBRepBoolean.Result result,double expected,String label){
  near(result.volume,expected,label+" volume");
  near(result.solid.volume(),expected,label+" body volume");
  TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(result.triangles());
  if(!health.isWatertight()||health.shells!=1)
   throw new AssertionError(label+" is not a closed single shell");
  near(health.signedVolume,expected,label+" mesh winding");
  if(result.solid.vertices().size()-result.solid.edges().size()+
     result.solid.faces().size()!=2)
   throw new AssertionError(label+" has invalid Euler topology");
 }
 private static void reject(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Unsafe Boolean accepted: "+label);
 }
 public static void main(String[] args){
  double[][] l={{0,0},{4,0},{4,1},{1,1},{1,4},{0,4}};
  double[][] r=rect(2,0,5,3);
  double[][] triangle={{0,0},{4,0},{2,4}};
  double[][] shifted={{1,0},{5,0},{3,4}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   final int p=plane,s=sign;
   PolygonPrismBRep a=prism("l"+p+s,p,s*3,l);
   PolygonPrismBRep b=prism("r"+p+s,p,s*3,r);
   check(PrismBRepBoolean.apply(a,b,PrismBRepBoolean.Operation.UNION),42,
       "concave orthogonal union");
   check(PrismBRepBoolean.apply(a,b,PrismBRepBoolean.Operation.INTERSECTION),6,
       "concave orthogonal intersection");
   check(PrismBRepBoolean.apply(a,b,PrismBRepBoolean.Operation.DIFFERENCE),15,
       "concave orthogonal difference");
   PolygonPrismBRep c=prism("convexA"+p+s,p,s*3,triangle);
   PolygonPrismBRep d=prism("convexB"+p+s,p,s*2,shifted);
   PrismBRepBoolean.Result clipped=PrismBRepBoolean.apply(c,d,
       PrismBRepBoolean.Operation.INTERSECTION);
   double reference=ConvexPrismBRepIntersection.intersect(c,d).volume;
   check(clipped,reference,"slanted intersection with unequal heights");
   near(clipped.distance,s*2,"minimum signed intersection height");
   near(PrismBRepBoolean.apply(d,c,PrismBRepBoolean.Operation.INTERSECTION).volume,
       reference,"intersection commutativity");
   reject(()->PrismBRepBoolean.apply(c,d,PrismBRepBoolean.Operation.DIFFERENCE),
       "slanted difference unsupported");
   reject(()->PrismBRepBoolean.apply(a,
       prism("opposite"+p+s,p,-s*3,r),PrismBRepBoolean.Operation.UNION),
       "opposite signed heights");
   reject(()->PrismBRepBoolean.apply(a,
       prism("higher"+p+s,p,s*3.00001,r),PrismBRepBoolean.Operation.DIFFERENCE),
       "variable-height difference");
   reject(()->PrismBRepBoolean.apply(
       prism("outer"+p+s,p,s*3,rect(0,0,6,6)),
       prism("inner"+p+s,p,s*3,rect(2,2,4,4)),
       PrismBRepBoolean.Operation.DIFFERENCE),"hole in difference");
   if(a.faces().size()!=8||b.faces().size()!=6)
    throw new AssertionError("Boolean modified input topology");
  }
  reject(()->PrismBRepBoolean.apply(null,null,PrismBRepBoolean.Operation.UNION),
      "missing bodies");
  reject(()->PrismBRepBoolean.apply(
      prism("one",0,3,triangle),prism("two",1,3,shifted),
      PrismBRepBoolean.Operation.INTERSECTION),"different planes");
  reject(()->PrismBRepBoolean.apply(
      prism("three",0,3,triangle),prism("four",0,3,shifted),null),
      "missing operation");
  System.out.println("PrismBRepBooleanTest: OK");
 }
}
