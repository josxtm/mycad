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
 public static void main(String[] args){
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
   PolygonPrismBRep outer=prism("outer"+p+s,p,s*3,rect(0,0,4,4));
   PolygonPrismBRep inner=prism("inner"+p+s,p,s*3,triangle);
   near(ConvexPrismBRepHullUnion.union(outer,inner).volume,48,
       "nested convex union");
   reject(()->ConvexPrismBRepHullUnion.union(
      prism("offsetA"+p+s,p,s*3,rect(0,0,3,3)),
      prism("offsetB"+p+s,p,s*3,rect(2,2,5,5))),"concave union");
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
