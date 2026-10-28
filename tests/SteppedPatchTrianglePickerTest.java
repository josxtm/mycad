import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Regression: preview triangle -> immutable coplanar B-Rep patch ownership. */
public final class SteppedPatchTrianglePickerTest {
 private static PolygonPrismBRep prism(String key,int plane,double height,double[][] poly){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
  List<String> edges=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> segments=new LinkedHashMap<>();
  for(int i=0;i<poly.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/edge/"+i).getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=poly[i],b=poly[(i+1)%poly.length];
   edges.add(edge);
   segments.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,edges.get(0),plane,height,edges).asBRep(segments);
 }
 private static String position(float x,float y,float z){
  return Float.floatToIntBits(x==0?0:x)+"/"+
         Float.floatToIntBits(y==0?0:y)+"/"+
         Float.floatToIntBits(z==0?0:z);
 }
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid shell/topology accepted");
 }
 public static void main(String[] args){
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep first=prism("picker-a"+plane+sign,plane,3*sign,a);
   PolygonPrismBRep second=prism("picker-b"+plane+sign,plane,2*sign,b);
   for(VariableHeightOrthogonalMesh.Operation operation:VariableHeightOrthogonalMesh.Operation.values()){
    VariableHeightOrthogonalBRep shell=VariableHeightOrthogonalBRep.apply(first,second,operation);
    SteppedCoplanarPatchTopology.Topology topology=SteppedCoplanarPatchTopology.build(shell);
    if(!topology.shellId.equals(shell.id()))throw new AssertionError("Wrong shell identity");
    float[] triangles=shell.triangles();
    if(triangles.length!=shell.faces().size()*18)
     throw new AssertionError("Preview does not contain two triangles per source quad");
    Map<String,VariableHeightOrthogonalBRep.Vertex> vertices=new HashMap<>();
    for(VariableHeightOrthogonalBRep.Vertex v:shell.vertices())vertices.put(v.id,v);
    for(int faceIndex=0;faceIndex<shell.faces().size();faceIndex++){
     VariableHeightOrthogonalBRep.Face source=shell.faces().get(faceIndex);
     Set<String> corners=new HashSet<>();
     for(String vertexId:source.vertexIds()){
      VariableHeightOrthogonalBRep.Vertex v=vertices.get(vertexId);
      if(v==null)throw new AssertionError("Missing analytic source vertex");
      corners.add(position((float)v.x,(float)v.y,(float)v.z));
     }
     int expectedPatch=topology.patchIndexForSourceFace(source.id);
     for(int triangle=faceIndex*2;triangle<faceIndex*2+2;triangle++){
      SteppedCoplanarPatchTopology.PatchFace picked=
          SteppedPatchTrianglePicker.resolve(shell,topology,triangle);
      if(picked!=topology.patches.get(expectedPatch)||
         !picked.sourceFaceIds.contains(source.id))
       throw new AssertionError("Triangle resolved to wrong analytic patch");
      for(int corner=0;corner<3;corner++){
       int at=triangle*9+corner*3;
       if(!corners.contains(position(triangles[at],triangles[at+1],triangles[at+2])))
        throw new AssertionError("Preview/source face ordering changed");
      }
     }
    }
    if(SteppedPatchTrianglePicker.resolve(shell,topology,-1)!=null||
       SteppedPatchTrianglePicker.resolve(shell,topology,triangles.length/9)!=null)
     throw new AssertionError("Out-of-range triangle selected");
    reject(()->SteppedPatchTrianglePicker.resolve(null,topology,0));
    reject(()->SteppedPatchTrianglePicker.resolve(shell,null,0));
    VariableHeightOrthogonalBRep other=VariableHeightOrthogonalBRep.apply(
       first,second,operation==VariableHeightOrthogonalMesh.Operation.UNION?
       VariableHeightOrthogonalMesh.Operation.DIFFERENCE:
       VariableHeightOrthogonalMesh.Operation.UNION);
    if(shell.id().equals(other.id()))throw new AssertionError("Test shells are identical");
    reject(()->SteppedPatchTrianglePicker.resolve(other,topology,0));
    if(!Arrays.equals(triangles,shell.triangles()))
     throw new AssertionError("Picking mutated source geometry");
   }
  }
  System.out.println("SteppedPatchTrianglePickerTest: OK");
 }
}
