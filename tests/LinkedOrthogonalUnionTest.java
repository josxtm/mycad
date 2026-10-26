import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class LinkedOrthogonalUnionTest {
 private static final String A="df015a66-1111-4111-8111-000000000001",
                             B="df015a66-1111-4111-8111-000000000002",
                             R="df015a66-1111-4111-8111-000000000099";
 static void check(boolean okay,String title){
  if(!okay)throw new AssertionError(title);
 }
 static void close(double actual,double expected,String title){
  if(Math.abs(actual-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(title+" "+actual+" != "+expected);
 }
 static void fails(Runnable op,String title){
  try{op.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid state was accepted: "+title);
 }
 static LoopExtrusionFeature feature(String id,int plane,double height,
    double[][] points,Map<String,ClosedSketchExtrusion.Segment> sources){
  ArrayList<String> ids=new ArrayList<>();
  for(int i=0;i<points.length;i++){
   String segment=UUID.nameUUIDFromBytes((id+"/edge/"+i).getBytes(StandardCharsets.UTF_8)).toString();
   int k=(i+1)%points.length;
   sources.put(segment,new ClosedSketchExtrusion.Segment(plane,
     points[i][0],points[i][1],points[k][0],points[k][1]));
   ids.add(segment);
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids);
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(double sign:new double[]{1,-1}){
   final int p=plane;
   Map<String,ClosedSketchExtrusion.Segment> source=new LinkedHashMap<>();
   LoopExtrusionFeature a=feature(A,p,sign*3,
     new double[][]{{0,0},{4,0},{4,2},{0,2}},source);
   LoopExtrusionFeature b=feature(B,p,sign*3,
     new double[][]{{2,1},{5,1},{5,4},{2,4}},source);
   Map<String,LoopExtrusionFeature> bodies=new HashMap<>();
   bodies.put(A,a);bodies.put(B,b);
   LinkedConvexIntersection recipe=new LinkedConvexIntersection(R,A,B,
     LinkedConvexIntersection.Operation.UNION_ORTHOGONAL);
   check(recipe.dependsOnFeature(A)&&recipe.dependsOnFeature(B),
     "Both original operand UUIDs remain linked");
   float[] original=recipe.regenerateTriangles(bodies,source);
   TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(original);
   check(report.isWatertight()&&report.shells==1,"Joined analytic closed shell");
   close(report.signedVolume,45,"Original linked union volume");
   check(Arrays.equals(recipe.regenerateTriangles(bodies,source),original),
      "Deterministic reconstruction and JSON comparison");
   fails(()->recipe.regenerate(bodies,source),
      "Legacy INTERSECTION-only typed API not used for union");
   // A valid edit to the second RECTANGLE updates the derived union.
   Map<String,ClosedSketchExtrusion.Segment> updated=new LinkedHashMap<>(source);
   double[][] moved={{1,1},{5,1},{5,4},{1,4}};
   for(int i=0;i<4;i++){
    String name=b.lineIds().get(i),next=b.lineIds().get((i+1)%4);
    double[] start=moved[i],end=moved[(i+1)%4];
    updated.put(name,new ClosedSketchExtrusion.Segment(p,
      start[0],start[1],end[0],end[1]));
   }
   close(TriangleMeshHealth.inspect(recipe.regenerateTriangles(bodies,updated))
     .signedVolume,51,"Union regenerates after source corners move");
   close(TriangleMeshHealth.inspect(original).signedVolume,45,
     "Rebuild did not modify source result");
   Map<String,LoopExtrusionFeature> heights=new HashMap<>(bodies);
   heights.put(A,a.withDistance(sign*2));
   fails(()->recipe.regenerateTriangles(heights,source),
     "Height mismatch prevents invalid partial edit");
   heights.put(B,b.withDistance(sign*2));
   close(TriangleMeshHealth.inspect(recipe.regenerateTriangles(heights,source))
     .signedVolume,30,"Coherent heights may rebuild union");
   Map<String,LoopExtrusionFeature> missing=new HashMap<>(bodies);
   missing.remove(A);
   fails(()->recipe.regenerateTriangles(missing,source),"Missing source UUID");
   Map<String,ClosedSketchExtrusion.Segment> missingLine=new HashMap<>(source);
   missingLine.remove(b.lineIds().get(0));
   fails(()->recipe.regenerateTriangles(bodies,missingLine),"Missing source sketch line");
  }
  LinkedConvexIntersection legacy=new LinkedConvexIntersection(R,A,B);
  check(legacy.operation==LinkedConvexIntersection.Operation.INTERSECTION,
        "JSON v1 default operation remains old intersection");
  fails(()->new LinkedConvexIntersection(R,A,B,null),"Null Boolean operation");
  System.out.println("LinkedOrthogonalUnionTest: OK");
 }
}
