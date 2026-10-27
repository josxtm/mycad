import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class OrthogonalGridHoleBRepTest {
 private static final String A="bbccdd01-0000-4000-8000-000000000001",
                             B="bbccdd01-0000-4000-8000-000000000002";
 static void check(boolean value,String msg){if(!value)throw new AssertionError(msg);}
 static void close(double value,double expected,String msg){
  if(Math.abs(value-expected)>1e-4*Math.max(1,expected))
   throw new AssertionError(msg+" "+value+" expected "+expected);
 }
 static void invalid(Runnable task,String msg){
  try{task.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Expected validation error "+msg);
 }
 static PolygonPrismBRep body(String id,int p,double height,double[][] points){
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<points.length;i++){
   String name=UUID.nameUUIDFromBytes((id+"/line/"+i)
    .getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=points[i],b=points[(i+1)%points.length];
   lines.put(name,new ClosedSketchExtrusion.Segment(p,a[0],a[1],b[0],b[1]));
   ids.add(name);
  }
  return new LoopExtrusionFeature(id,ids.get(0),p,height,ids).asBRep(lines);
 }
 static double[][] rect(double x1,double y1,double x2,double y2){
  return new double[][]{{x1,y1},{x2,y1},{x2,y2},{x1,y2}};
 }
 static void health(OrthogonalGridHoleBRep result,int solids,int holes,
                    double area,double height,String context){
  check(result.solidCount==solids,context+" body count");
  check(result.holeCount==holes,context+" through-hole count");
  close(result.area,area,context+" analytic area");
  close(result.volume,area*Math.abs(height),context+" analytic volume");
  close(result.distance,height,context+" signed height");
  TriangleMeshHealth.Report test=TriangleMeshHealth.inspect(result.triangles());
  check(test.isWatertight(),context+" manifold and oriented mesh");
  check(test.shells==solids,context+" shell count");
  close(test.signedVolume,result.volume,context+" signed mesh volume");
  check(result.vertices().size()-result.edges().size()+result.faces().size()==
    2*solids-2*holes,context+" genus/Euler");
  check(result.halfEdges().size()==2*result.edges().size(),
       context+" two analytic half edges per shared edge");
  Map<String,OrthogonalGridHoleBRep.HalfEdge> uses=new HashMap<>();
  for(OrthogonalGridHoleBRep.HalfEdge h:result.halfEdges())
   check(uses.put(h.id,h)==null,context+" unique halfedge ID");
  for(OrthogonalGridHoleBRep.HalfEdge h:result.halfEdges()){
   OrthogonalGridHoleBRep.HalfEdge twin=uses.get(h.twinId);
   check(twin!=null&&twin.twinId.equals(h.id),context+" symmetric twin");
   check(h.fromId.equals(twin.toId)&&h.toId.equals(twin.fromId),
         context+" opposite orientation on shared edge");
   check(uses.containsKey(h.nextId)&&uses.containsKey(h.previousId),
         context+" closed face loops");
  }
  for(OrthogonalGridHoleBRep.Face face:result.faces()){
   check(face.vertexIds().size()==4&&face.halfEdgeIds().size()==4,
     context+" analytic planar quad face");
   double[] n=face.outwardNormal();
   close(Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]),1,
     context+" normalized outward normal");
  }
  float[] a=result.triangles(),b=result.triangles();
  check(a!=b&&Arrays.equals(a,b),context+" immutable tessellation result");
 }
 public static void main(String[] args){
  for(int p=0;p<3;p++)for(double height:new double[]{3,-3}){
   final int plane=p;final double signed=height;
   PolygonPrismBRep outer=body(A,p,height,rect(0,0,6,6));
   PolygonPrismBRep hole=body(B,p,height,rect(2,2,4,4));
   OrthogonalGridHoleBRep drilled=OrthogonalGridHoleBRep.apply(
      outer,hole,OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE);
   health(drilled,1,1,32,height,"through-cut XY/XZ/YZ signed");
   check(Arrays.equals(drilled.triangles(),
     OrthogonalGridHoleBRep.apply(outer,hole,
       OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE).triangles()),
     "Hole B-Rep deterministic for JSON history");
   PolygonPrismBRep distant=body(B,p,height,rect(9,0,11,2));
   OrthogonalGridHoleBRep joined=OrthogonalGridHoleBRep.apply(
      outer,distant,OrthogonalPolygonBRepBoolean.Operation.UNION);
   health(joined,2,0,40,height,"disconnected union");
   OrthogonalGridHoleBRep retained=OrthogonalGridHoleBRep.apply(
      outer,hole,OrthogonalPolygonBRepBoolean.Operation.UNION);
   health(retained,1,0,36,height,"contained rectangle union");
   invalid(()->OrthogonalGridHoleBRep.apply(
      outer,distant,OrthogonalPolygonBRepBoolean.Operation.INTERSECTION),
      "Empty intersection");
   invalid(()->OrthogonalGridHoleBRep.apply(
      outer,body(B,plane,-signed,rect(2,2,4,4)),
      OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),
      "Opposite signed height");
   invalid(()->OrthogonalGridHoleBRep.apply(
      outer,body(B,plane,signed,new double[][]{
       {1,1},{5,1},{4,4},{1,4}
      }),OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),
      "Oblique cutter");
   PolygonPrismBRep tangent=body(B,p,height,rect(6,6,9,9));
   invalid(()->OrthogonalGridHoleBRep.apply(
      outer,tangent,OrthogonalPolygonBRepBoolean.Operation.UNION),
      "Non-manifold point-only contact");
   check(outer.faces().size()==6&&hole.faces().size()==6,
     "Original analytic bodies unchanged");
  }
  invalid(()->OrthogonalGridHoleBRep.apply(
    body(A,0,3,rect(0,0,6,6)),body(B,1,3,rect(2,2,4,4)),
    OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),"different planes");
  invalid(()->OrthogonalGridHoleBRep.apply(null,
    body(B,0,3,rect(2,2,4,4)),
    OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),"null operand");
  System.out.println("OrthogonalGridHoleBRepTest: OK");
 }
}
