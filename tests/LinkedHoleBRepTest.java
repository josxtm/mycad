import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class LinkedHoleBRepTest {
 private static final String A="ddcbaa99-0000-4000-8000-000000000001",
                             B="ddcbaa99-0000-4000-8000-000000000002",
                             R="ddcbaa99-0000-4000-8000-000000000099";
 private static void check(boolean x,String message){if(!x)throw new AssertionError(message);}
 private static void close(double got,double expected,String name){
  if(Math.abs(got-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(name+": "+got+" vs "+expected);
 }
 private static void invalid(Runnable action,String name){
  try{action.run();}catch(IllegalArgumentException okay){return;}
  throw new AssertionError("Invalid history was accepted: "+name);
 }
 static LoopExtrusionFeature source(String id,int plane,double height,
    double[][] points,Map<String,ClosedSketchExtrusion.Segment> lines){
  List<String> ids=new ArrayList<>();
  for(int i=0;i<points.length;i++){
   String edge=UUID.nameUUIDFromBytes((id+"/edge/"+i)
       .getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=points[i],b=points[(i+1)%points.length];
   ids.add(edge);lines.put(edge,
    new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids);
 }
 static double[][] box(double x1,double y1,double x2,double y2){
  return new double[][]{{x1,y1},{x2,y1},{x2,y2},{x1,y2}};
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(double signed:new double[]{3,-3}){
   final int p=plane;
   Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
   LoopExtrusionFeature first=source(A,p,signed,box(0,0,6,6),lines),
                        second=source(B,p,signed,box(2,2,4,4),lines);
   Map<String,LoopExtrusionFeature> features=new LinkedHashMap<>();
   features.put(A,first);features.put(B,second);
   LinkedConvexIntersection linked=new LinkedConvexIntersection(R,A,B,
      LinkedConvexIntersection.Operation.DIFFERENCE_WITH_HOLES);
   check(linked.dependsOnFeature(A)&&linked.dependsOnFeature(B),
     "Original source links preserved");
   float[] saved=linked.regenerateTriangles(features,lines);
   TriangleMeshHealth.Report mesh=TriangleMeshHealth.inspect(saved);
   check(mesh.isWatertight()&&mesh.shells==1,"Linked through-hole watertight");
   close(mesh.signedVolume,96,"Through-hole signed volume");
   check(Arrays.equals(saved,linked.regenerateTriangles(features,lines)),
     "Saved JSON mesh reconstructs with identical floats");
   Map<String,ClosedSketchExtrusion.Segment> edited=new LinkedHashMap<>(lines);
   double[][] bigger=box(1,1,4,4);
   for(int i=0;i<4;i++){
    double[] a=bigger[i],b=bigger[(i+1)%4];
    edited.put(second.lineIds().get(i),
       new ClosedSketchExtrusion.Segment(p,a[0],a[1],b[0],b[1]));
   }
   close(TriangleMeshHealth.inspect(linked.regenerateTriangles(features,edited))
     .signedVolume,81,"New sketch expands through-hole, regenerate linked result");
   check(Arrays.equals(saved,linked.regenerateTriangles(features,lines)),
     "Original saved result immutable before commit");
   Map<String,LoopExtrusionFeature> heights=new LinkedHashMap<>(features);
   heights.put(A,first.withDistance(signed*2));
   invalid(()->linked.regenerateTriangles(heights,lines),
     "Incompatible extrusion height blocks editing");
   heights.put(B,second.withDistance(signed*2));
   close(TriangleMeshHealth.inspect(linked.regenerateTriangles(heights,lines))
     .signedVolume,192,"Compatible height edits regenerate hole");
   Map<String,ClosedSketchExtrusion.Segment> missing=new LinkedHashMap<>(lines);
   missing.remove(second.lineIds().get(0));
   invalid(()->linked.regenerateTriangles(features,missing),
     "Deleted source cannot silently corrupt the derived mesh");
   Map<String,LoopExtrusionFeature> noSource=new LinkedHashMap<>(features);
   noSource.remove(B);
   invalid(()->linked.regenerateTriangles(noSource,lines),
     "Deleted operand blocks linked history rebuild");
   LinkedConvexIntersection union=new LinkedConvexIntersection(R,A,B,
     LinkedConvexIntersection.Operation.UNION_WITH_HOLES);
   close(TriangleMeshHealth.inspect(union.regenerateTriangles(features,lines))
     .signedVolume,108,"Union contains cutter remains valid");
  }
  check(new LinkedConvexIntersection(R,A,B).operation==
      LinkedConvexIntersection.Operation.INTERSECTION,
    "Historical JSON without operation still defaults to INTERSECTION");
  System.out.println("LinkedHoleBRepTest: OK");
 }
}
