import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class VariableHeightOrthogonalMeshTest {
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
  if(Math.abs(actual-expected)>1e-4*Math.max(1,expected))
   throw new AssertionError(label+": "+actual+" != "+expected);
 }
 private static void reject(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid mesh accepted: "+label);
 }
 private static void verify(VariableHeightOrthogonalMesh.Result result,double expected){
  near(result.volume,expected,"result volume");
  TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(result.triangles());
  if(!report.isWatertight()||report.shells!=1||report.negativeVolumeShells!=0)
   throw new AssertionError("Stepped Boolean mesh not a closed positive single shell");
  near(report.signedVolume,expected,"mesh signed volume");
  if(!Arrays.equals(result.triangles(),result.triangles()))
   throw new AssertionError("Non-deterministic mesh access");
 }
 public static void main(String[] args){
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  double[][] nested={{1,1},{3,1},{3,3},{1,3}};
  double[][] distant={{10,0},{12,0},{12,2},{10,2}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep first=prism("first"+plane+sign,plane,3*sign,a);
   PolygonPrismBRep second=prism("second"+plane+sign,plane,2*sign,b);
   float[] beforeFirst=first.triangles(),beforeSecond=second.triangles();
   verify(VariableHeightOrthogonalMesh.apply(first,second,
       VariableHeightOrthogonalMesh.Operation.UNION),64);
   VariableHeightOrthogonalMesh.Result diff=VariableHeightOrthogonalMesh.apply(
       first,second,VariableHeightOrthogonalMesh.Operation.DIFFERENCE);
   verify(diff,32);
   if(!Arrays.equals(diff.triangles(),VariableHeightOrthogonalMesh.apply(
       first,second,VariableHeightOrthogonalMesh.Operation.DIFFERENCE).triangles()))
    throw new AssertionError("Mesh regeneration is not deterministic");
   verify(VariableHeightOrthogonalMesh.apply(second,first,
       VariableHeightOrthogonalMesh.Operation.DIFFERENCE),16);
   PolygonPrismBRep shorterFirst=prism("shorter"+plane+sign,plane,2*sign,a);
   PolygonPrismBRep tallerSecond=prism("taller"+plane+sign,plane,3*sign,b);
   verify(VariableHeightOrthogonalMesh.apply(shorterFirst,tallerSecond,
       VariableHeightOrthogonalMesh.Operation.UNION),64);
   verify(VariableHeightOrthogonalMesh.apply(shorterFirst,tallerSecond,
       VariableHeightOrthogonalMesh.Operation.DIFFERENCE),16);
   PolygonPrismBRep pocket=prism("pocket"+plane+sign,plane,2*sign,nested);
   verify(VariableHeightOrthogonalMesh.apply(first,pocket,
       VariableHeightOrthogonalMesh.Operation.DIFFERENCE),40);
   PolygonPrismBRep far=prism("far"+plane+sign,plane,2*sign,distant);
   reject(()->VariableHeightOrthogonalMesh.apply(first,far,
       VariableHeightOrthogonalMesh.Operation.UNION),"disconnected union");
   PolygonPrismBRep opposite=prism("opposite"+plane+sign,plane,-2*sign,b);
   reject(()->VariableHeightOrthogonalMesh.apply(first,opposite,
       VariableHeightOrthogonalMesh.Operation.DIFFERENCE),"opposite direction");
   reject(()->VariableHeightOrthogonalMesh.apply(first,second,null),"missing operation");
   if(!Arrays.equals(beforeFirst,first.triangles())||
      !Arrays.equals(beforeSecond,second.triangles()))
    throw new AssertionError("Boolean mesh changed source B-Rep");
  }
  System.out.println("VariableHeightOrthogonalMeshTest: OK");
 }
}
