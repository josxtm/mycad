import es.josxtm.mycad.cad.*;
import java.util.*;

public final class LinkedConvexIntersectionTest {
 static final String A="ac901224-0000-4000-8000-000000000001";
 static final String B="ac901224-0000-4000-8000-000000000002";
 static final String C="ac901224-0000-4000-8000-000000000003";
 static final String R="ac901224-0000-4000-8000-000000000099";
 static void ok(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
 static void close(double actual,double expected,String msg){
  if(Math.abs(actual-expected)>1e-4*Math.max(1,expected))
   throw new AssertionError(msg+": "+actual+" != "+expected);
 }
 static void fails(Runnable op,String msg){
  try{op.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError(msg);
 }
 static LoopExtrusionFeature feature(String id,int plane,double height,
    double[][] corners,Map<String,ClosedSketchExtrusion.Segment> lines){
  ArrayList<String> ids=new ArrayList<>();
  for(int i=0;i<corners.length;i++){
   String line=UUID.nameUUIDFromBytes((id+"/line/"+i).getBytes(
     java.nio.charset.StandardCharsets.UTF_8)).toString();
   int n=(i+1)%corners.length;
   lines.put(line,new ClosedSketchExtrusion.Segment(plane,
    corners[i][0],corners[i][1],corners[n][0],corners[n][1]));
   ids.add(line);
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids);
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(double sign:new double[]{1,-1}){
   Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
   LoopExtrusionFeature a=feature(A,plane,sign*4,
    new double[][]{{0,0},{4,0},{4,4},{0,4}},lines);
   LoopExtrusionFeature b=feature(B,plane,sign*2,
    new double[][]{{2,1},{5,1},{5,3},{2,3}},lines);
   Map<String,LoopExtrusionFeature> features=new LinkedHashMap<>();
   features.put(A,a);features.put(B,b);
   LinkedConvexIntersection recipe=new LinkedConvexIntersection(R,A,B);
   ok(recipe.dependsOnFeature(A)&&recipe.dependsOnFeature(B)&&
      !recipe.dependsOnFeature(C),"Two exact UUID dependencies");
   ConvexPrismBRepIntersection.Result initial=recipe.regenerate(features,lines);
   close(initial.volume,8,"Original intersection volume");
   ok(TriangleMeshHealth.inspect(initial.triangles()).isWatertight(),
      "Original BRep intersection closed");
   // Height changed on a source, ID preserved and dependent regenerated.
   features.put(A,a.withDistance(sign));
   ConvexPrismBRepIntersection.Result height=recipe.regenerate(features,lines);
   close(height.volume,4,"Rebuild after source height edit");
   features.put(A,a);
   // Move two coherent source points to enlarge the overlap.
   Map<String,ClosedSketchExtrusion.Segment> moved=new LinkedHashMap<>(lines);
   double[][] p={{1,1},{5,1},{5,3},{1,3}};
   for(int i=0;i<4;i++){
    String name=b.lineIds().get(i);int j=(i+1)%4;
    moved.put(name,new ClosedSketchExtrusion.Segment(plane,
      p[i][0],p[i][1],p[j][0],p[j][1]));
   }
   close(recipe.regenerate(features,moved).volume,12,
         "Regenerated after source sketch geometry edit");
   close(recipe.regenerate(features,lines).volume,8,"Source unaffected");
   fails(()->recipe.regenerate(Collections.singletonMap(A,a),lines),
         "Missing second source");
   Map<String,ClosedSketchExtrusion.Segment> missing=new LinkedHashMap<>(lines);
   missing.remove(b.lineIds().get(0));
   fails(()->recipe.regenerate(features,missing),"Missing sketch source");
   fails(()->recipe.regenerate(features,new LinkedHashMap<>()),"Orphan geometry");
   Map<String,LoopExtrusionFeature> opposite=new HashMap<>(features);
   opposite.put(B,b.withDistance(-sign*2));
   fails(()->recipe.regenerate(opposite,lines),"Opposite extrusions");
  }
  fails(()->new LinkedConvexIntersection(R,A,A),"Repeated source UUID");
  fails(()->new LinkedConvexIntersection(A,A,B),"Result reused source UUID");
  fails(()->new LinkedConvexIntersection("bad",A,B),"Invalid result UUID");
  System.out.println("LinkedConvexIntersectionTest: OK");
 }
}
