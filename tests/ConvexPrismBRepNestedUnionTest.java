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
 // Regression: the old world-coordinate shoelace lost precision at
 // +/-9000 mm even though the B-Rep source segments are stored as doubles.
 static void translatedContainmentPrecision(){
  try{
   java.lang.reflect.Method area=ConvexPrismBRepNestedUnion.class
       .getDeclaredMethod("signedTwiceArea",PolygonPrismBRep.class);
   area.setAccessible(true);
   for(double origin:new double[]{9000,-9000}){
    double width=(origin+0.6)-origin;
    double[][] outer={{origin,origin},{origin+0.6,origin},
        {origin+0.6,origin+0.6},{origin,origin+0.6}};
    double[][] inner={{origin+0.2,origin+0.2},
        {origin+0.4,origin+0.2},{origin+0.4,origin+0.4},
        {origin+0.2,origin+0.4}};
    double[][] protruding={{origin+0.2,origin+0.2},
        {origin+0.61,origin+0.2},{origin+0.61,origin+0.4},
        {origin+0.2,origin+0.4}};
    for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
     String suffix=origin+"/"+plane+"/"+sign;
     PolygonPrismBRep a=prism("translatedOuter"+suffix,plane,sign*2,outer);
     PolygonPrismBRep b=prism("translatedInner"+suffix,plane,sign*2,inner);
     double signed=((Double)area.invoke(null,a)).doubleValue();
     if(Math.abs(signed-2*width*width)>1e-10)
      throw new AssertionError("Translated nested orientation lost precision: "+signed);
     if(ConvexPrismBRepNestedUnion.union(a,b)!=a||
        ConvexPrismBRepNestedUnion.union(b,a)!=a)
      throw new AssertionError("Translated nested union did not preserve the source shell");
     final int p=plane,s=sign;
     reject(()->ConvexPrismBRepNestedUnion.union(a,
         prism("translatedProtrusion"+suffix,p,s*2,protruding)));
    }
   }
  }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
 }
 public static void main(String[] args){
  translatedContainmentPrecision();
  double[][] outer={{0,0},{6,0},{6,6},{0,6}};
  double[][] inner={{1,1},{3,1},{1,3}};
  double[][] overlap={{4,0},{8,0},{8,4},{4,4}};
  double[][] concave={{0,0},{6,0},{6,6},{3,3},{0,6}};
  double[][] tinyProtrusion={{1,1},{6.00001,1},{6.00001,5},{1,5}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   final int p=plane,s=sign;
   PolygonPrismBRep a=prism("outer"+p+s,p,s*3,outer);
   PolygonPrismBRep b=prism("inner"+p+s,p,s*3,inner);
   PolygonPrismBRep concaveBody=prism("concave"+p+s,p,s*3,concave);
   reject(()->ConvexPrismBRepNestedUnion.union(concaveBody,b));
   reject(()->ConvexPrismBRepNestedUnion.union(a,concaveBody));
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
