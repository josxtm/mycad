import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class LinkedConcaveBooleanTest {
 static final String A="7dacd301-2222-4222-8222-000000000001",
                     B="7dacd301-2222-4222-8222-000000000002",
                     R="7dacd301-2222-4222-8222-000000000099";
 static void eq(double a,double b,String msg){
  if(Math.abs(a-b)>1e-4*Math.max(1,Math.abs(b)))
   throw new AssertionError(msg+": "+a+" != "+b);
 }
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static void bad(Runnable r,String msg){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Expected invalid Boolean history: "+msg);
 }
 static LoopExtrusionFeature feature(String id,int p,double h,double[][] ring,
     Map<String,ClosedSketchExtrusion.Segment> lines){
  List<String> ids=new ArrayList<>();
  for(int i=0;i<ring.length;i++){
   String k=UUID.nameUUIDFromBytes((id+"/segment/"+i)
     .getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=ring[i],b=ring[(i+1)%ring.length];
   lines.put(k,new ClosedSketchExtrusion.Segment(p,a[0],a[1],b[0],b[1]));
   ids.add(k);
  }
  return new LoopExtrusionFeature(id,ids.get(0),p,h,ids);
 }
 public static void main(String[] args){
  double[][] L={{0,0},{4,0},{4,1},{1,1},{1,4},{0,4}};
  double[][] BRECT={{2,0},{5,0},{5,3},{2,3}};
  for(int plane=0;plane<3;plane++)for(double signed:new double[]{5,-5}){
   Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
   LoopExtrusionFeature a=feature(A,plane,signed,L,lines),
                        b=feature(B,plane,signed,BRECT,lines);
   Map<String,LoopExtrusionFeature> sources=new HashMap<>();
   sources.put(A,a);sources.put(B,b);
   for(LinkedConvexIntersection.Operation op:new LinkedConvexIntersection.Operation[]{
    LinkedConvexIntersection.Operation.UNION_POLYGONAL,
    LinkedConvexIntersection.Operation.DIFFERENCE_POLYGONAL,
    LinkedConvexIntersection.Operation.INTERSECTION_POLYGONAL
   }){
    LinkedConvexIntersection recipe=new LinkedConvexIntersection(R,A,B,op);
    double area=op==LinkedConvexIntersection.Operation.UNION_POLYGONAL?14:
                op==LinkedConvexIntersection.Operation.DIFFERENCE_POLYGONAL?5:2;
    float[] first=recipe.regenerateTriangles(sources,lines);
    eq(TriangleMeshHealth.inspect(first).signedVolume,area*5,op+" volume");
    check(Arrays.equals(first,recipe.regenerateTriangles(sources,lines)),
          op+" saved JSON compares exact floats");
    Map<String,LoopExtrusionFeature> changed=new HashMap<>(sources);
    changed.put(A,a.withDistance(signed*0.5));
    bad(()->recipe.regenerateTriangles(changed,lines),
        op+" disallow incompatible source height");
    changed.put(B,b.withDistance(signed*0.5));
    eq(TriangleMeshHealth.inspect(recipe.regenerateTriangles(changed,lines))
       .signedVolume,area*2.5,op+" coherent height rebuild");
    // Reposition the linked rectangle while preserving an orthogonal profile.
    double[][] moved={{1,0},{5,0},{5,3},{1,3}};
    Map<String,ClosedSketchExtrusion.Segment> edited=new LinkedHashMap<>(lines);
    for(int i=0;i<4;i++){
     double[] p=moved[i],q=moved[(i+1)%4];
     edited.put(b.lineIds().get(i),
       new ClosedSketchExtrusion.Segment(plane,p[0],p[1],q[0],q[1]));
    }
    double editedArea=op==LinkedConvexIntersection.Operation.UNION_POLYGONAL?16:
                      op==LinkedConvexIntersection.Operation.DIFFERENCE_POLYGONAL?4:3;
    eq(TriangleMeshHealth.inspect(recipe.regenerateTriangles(sources,edited))
      .signedVolume,editedArea*5,op+" source geometry propagation");
    check(Arrays.equals(first,recipe.regenerateTriangles(sources,lines)),
         op+" original unchanged by prevalidation");
    Map<String,ClosedSketchExtrusion.Segment> missing=new HashMap<>(lines);
    missing.remove(a.lineIds().get(0));
    bad(()->recipe.regenerateTriangles(sources,missing),
        op+" missing source ID must fail");
   }
  }
  check(new LinkedConvexIntersection(R,A,B).operation==
        LinkedConvexIntersection.Operation.INTERSECTION,
        "Legacy default remains unchanged");
  System.out.println("LinkedConcaveBooleanTest: OK");
 }
}
