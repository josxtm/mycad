import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class VariableHeightOrthogonalBRepTest {
 private static PolygonPrismBRep prism(String key,int plane,double h,double[][] poly){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<poly.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/edge/"+i).getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=poly[i],b=poly[(i+1)%poly.length];
   ids.add(edge);
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,h,ids).asBRep(lines);
 }
 private static void near(double actual,double expected,String label){
  if(Math.abs(actual-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(label+": "+actual+" != "+expected);
 }
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Unsupported Boolean accepted");
 }
 private static void check(VariableHeightOrthogonalBRep body,double expected){
  near(body.volume(),expected,"analytic shell volume");
  if(body.vertices().size()-body.edges().size()+body.faces().size()!=2)
   throw new AssertionError("Euler topology invalid");
  if(body.halfEdges().size()!=body.edges().size()*2||
      body.halfEdges().size()!=body.faces().size()*4)
   throw new AssertionError("Half-edge count invalid");
  Map<String,VariableHeightOrthogonalBRep.HalfEdge> byId=new HashMap<>();
  for(VariableHeightOrthogonalBRep.HalfEdge edge:body.halfEdges())
   if(byId.put(edge.id,edge)!=null)throw new AssertionError("Duplicate half-edge ID");
  for(VariableHeightOrthogonalBRep.HalfEdge edge:body.halfEdges()){
   VariableHeightOrthogonalBRep.HalfEdge twin=byId.get(edge.twinId);
   if(twin==null||!edge.id.equals(twin.twinId)||
      !edge.fromId.equals(twin.toId)||!edge.toId.equals(twin.fromId)||
      !edge.faceId.equals(byId.get(edge.nextId).faceId)||
      !edge.faceId.equals(byId.get(edge.previousId).faceId)||
      !edge.id.equals(byId.get(edge.nextId).previousId)||
      !edge.id.equals(byId.get(edge.previousId).nextId))
    throw new AssertionError("Broken analytic half-edge links");
  }
  double area=0;
  for(VariableHeightOrthogonalBRep.Face face:body.faces()){
   if(!"PLANE".equals(face.kind)||face.vertexIds().size()!=4||
       face.halfEdgeIds().size()!=4||face.area<=0)
    throw new AssertionError("Invalid planar quad face");
   double[] normal=face.outwardNormal();
   near(Math.sqrt(normal[0]*normal[0]+normal[1]*normal[1]+normal[2]*normal[2]),
       1,"unit normal");
   area+=face.area;
  }
  near(body.surfaceArea(),area,"surface area");
  TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(body.triangles());
  if(!report.isWatertight()||report.shells!=1||report.negativeVolumeShells!=0)
   throw new AssertionError("Invalid analytic shell preview");
  near(report.signedVolume,expected,"preview volume");
  float[] original=body.triangles();
  float[] changed=body.triangles();changed[0]+=100;
  if(!Arrays.equals(original,body.triangles()))
   throw new AssertionError("Mutable preview escaped");
 }
 public static void main(String[] args){
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  double[][] nested={{1,1},{3,1},{3,3},{1,3}};
  double[][] distant={{10,0},{12,0},{12,2},{10,2}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep first=prism("first"+plane+sign,plane,3*sign,a);
   PolygonPrismBRep second=prism("second"+plane+sign,plane,2*sign,b);
   float[] firstBefore=first.triangles(),secondBefore=second.triangles();
   VariableHeightOrthogonalBRep union=VariableHeightOrthogonalBRep.apply(
       first,second,VariableHeightOrthogonalMesh.Operation.UNION);
   check(union,64);
   VariableHeightOrthogonalBRep diff=VariableHeightOrthogonalBRep.apply(
       first,second,VariableHeightOrthogonalMesh.Operation.DIFFERENCE);
   check(diff,32);
   VariableHeightOrthogonalBRep repeated=VariableHeightOrthogonalBRep.apply(
       first,second,VariableHeightOrthogonalMesh.Operation.DIFFERENCE);
   if(!diff.id().equals(repeated.id())||
       !Arrays.equals(diff.triangles(),repeated.triangles()))
    throw new AssertionError("Analytic topology regeneration is not deterministic");
   check(VariableHeightOrthogonalBRep.apply(second,first,
       VariableHeightOrthogonalMesh.Operation.DIFFERENCE),16);
   PolygonPrismBRep pocket=prism("pocket"+plane+sign,plane,2*sign,nested);
   check(VariableHeightOrthogonalBRep.apply(first,pocket,
       VariableHeightOrthogonalMesh.Operation.DIFFERENCE),40);
   PolygonPrismBRep far=prism("far"+plane+sign,plane,2*sign,distant);
   reject(()->VariableHeightOrthogonalBRep.apply(first,far,
       VariableHeightOrthogonalMesh.Operation.UNION));
   if(!Arrays.equals(firstBefore,first.triangles())||
      !Arrays.equals(secondBefore,second.triangles()))
    throw new AssertionError("Source prism mutated");
  }
  System.out.println("VariableHeightOrthogonalBRepTest: OK");
 }
}
