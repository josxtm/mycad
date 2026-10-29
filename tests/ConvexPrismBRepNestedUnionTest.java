import es.josxtm.mycad.cad.*;
import java.util.*;
public final class ConvexPrismBRepNestedUnionTest {
 static PolygonPrismBRep prism(String key,int plane,double height,double[][] points){
  String id=UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<points.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/edge/"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
   ids.add(edge);double[] a=points[i],b=points[(i+1)%points.length];
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids).asBRep(lines);
 }
 static void near(double a,double b){if(Math.abs(a-b)>1e-4)throw new AssertionError(a+" != "+b);}
 static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Unsupported union accepted");}
 public static void main(String[] args){
  double[][] outer={{0,0},{6,0},{6,6},{0,6}};
  double[][] inner={{1,1},{3,1},{1,3}};
  double[][] overlap={{4,0},{8,0},{8,4},{4,4}};
  double[][] tinyProtrusion={{1,1},{6.00001,1},{6.00001,5},{1,5}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   final int p=plane,s=sign;
   PolygonPrismBRep a=prism("outer"+p+s,p,s*3,outer);
   PolygonPrismBRep b=prism("inner"+p+s,p,s*3,inner);
   PolygonPrismBRep result=ConvexPrismBRepNestedUnion.union(a,b);
   near(result.volume(),108);
   near(ConvexPrismBRepNestedUnion.union(b,a).volume(),108);
   if(!TriangleMeshHealth.inspect(result.triangles()).isWatertight())
    throw new AssertionError("Nested union must be watertight");
   // Identical sections are nested both ways: operand order must not
   // select a different persisted shell or change its UUID.
   PolygonPrismBRep equal=prism("equal"+p+s,p,s*3,outer);
   PolygonPrismBRep same=ConvexPrismBRepNestedUnion.union(a,equal);
   PolygonPrismBRep swapped=ConvexPrismBRepNestedUnion.union(equal,a);
   if(same!=swapped||!same.id().equals(swapped.id())||
      !Arrays.equals(same.triangles(),swapped.triangles()))
    throw new AssertionError("Equal nested union depends on operand order");
   // A tolerance-level height difference still requires the actual taller
   // source shell; choosing the shorter one would truncate the top cap.
   PolygonPrismBRep slightlyTaller=prism("nearHeight"+p+s,p,
       s*(3+1e-10),outer);
   if(ConvexPrismBRepNestedUnion.union(a,slightlyTaller)!=slightlyTaller||
      ConvexPrismBRepNestedUnion.union(slightlyTaller,a)!=slightlyTaller)
    throw new AssertionError("Nested union lost the taller source shell");
   if(!TriangleMeshHealth.inspect(same.triangles()).isWatertight()||
      !TriangleMeshHealth.inspect(slightlyTaller.triangles()).isWatertight())
    throw new AssertionError("Nested union source shells must remain watertight");
   reject(()->ConvexPrismBRepNestedUnion.union(a,prism("partial"+p+s,p,s*3,overlap)));
   reject(()->ConvexPrismBRepNestedUnion.union(a,prism("tinyProtrusion"+p+s,p,s*3,tinyProtrusion)));
   near(ConvexPrismBRepNestedUnion.union(a,prism("shorter"+p+s,p,s*2,inner)).volume(),108);
   near(ConvexPrismBRepNestedUnion.union(prism("shorterReversed"+p+s,p,s*2,inner),a).volume(),108);
   reject(()->ConvexPrismBRepNestedUnion.union(a,prism("taller"+p+s,p,s*4,inner)));
   reject(()->ConvexPrismBRepNestedUnion.union(a,prism("tinyTaller"+p+s,p,s*3.00001,inner)));
   reject(()->ConvexPrismBRepNestedUnion.union(a,prism("direction"+p+s,p,-s*3,inner)));
  }
  System.out.println("ConvexPrismBRepNestedUnionTest: OK");
 }
}
