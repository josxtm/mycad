import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Regression: nearest ray hit -> immutable coplanar patch, without UI dependencies. */
public final class SteppedPatchRayPickerTest {
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
 private static void near(double actual,double expected){
  if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-5)
   throw new AssertionError("Expected "+expected+" but got "+actual);
 }
 private static void reject(Runnable operation){
  try{operation.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid ray or topology accepted");
 }
 public static void main(String[] args){
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep first=prism("ray-a"+plane+sign,plane,3*sign,a);
   PolygonPrismBRep second=prism("ray-b"+plane+sign,plane,2*sign,b);
   for(VariableHeightOrthogonalMesh.Operation operation:VariableHeightOrthogonalMesh.Operation.values()){
    VariableHeightOrthogonalBRep shell=VariableHeightOrthogonalBRep.apply(first,second,operation);
    SteppedCoplanarPatchTopology.Topology topology=SteppedCoplanarPatchTopology.build(shell);
    float[] original=shell.triangles();
    for(int triangle=0;triangle<original.length/9;triangle++){
     int at=triangle*9;
     double[] center={
       (original[at]+original[at+3]+original[at+6])/3.0,
       (original[at+1]+original[at+4]+original[at+7])/3.0,
       (original[at+2]+original[at+5]+original[at+8])/3.0
     };
     double[] normal=shell.faces().get(triangle/2).outwardNormal();
     SteppedCoplanarPatchTopology.PatchFace expected=
         SteppedPatchTrianglePicker.resolve(shell,topology,triangle);
     for(int side:new int[]{-1,1}){
      double[] origin=new double[3],direction=new double[3];
      for(int axis=0;axis<3;axis++){
       origin[axis]=center[axis]+side*normal[axis]*0.125;
       direction[axis]=-side*normal[axis]*7;
      }
      SteppedPatchRayPicker.Hit hit=SteppedPatchRayPicker.pick(
          shell,topology,origin,direction);
      if(hit==null||hit.patch!=expected)
       throw new AssertionError("Wrong nearest patch for triangle "+triangle+
           ", plane "+plane+", sign "+sign+", operation "+operation);
      near(hit.distance,0.125);
      double[] point=hit.point();
      for(int axis=0;axis<3;axis++)near(point[axis],center[axis]);
      point[0]+=100;
      near(hit.point()[0],center[0]); // defensive copy
     }
    }
    double maxX=Double.NEGATIVE_INFINITY;
    for(VariableHeightOrthogonalBRep.Vertex vertex:shell.vertices())
     maxX=Math.max(maxX,vertex.x);
    if(SteppedPatchRayPicker.pick(shell,topology,
        new double[]{maxX+100,0,0},new double[]{1,0,0})!=null)
     throw new AssertionError("Ray escaping the shell must miss");
    reject(()->SteppedPatchRayPicker.pick(shell,topology,null,new double[]{1,0,0}));
    reject(()->SteppedPatchRayPicker.pick(shell,topology,new double[]{0,0,0},new double[]{0,0,0}));
    reject(()->SteppedPatchRayPicker.pick(shell,topology,new double[]{0,0,0},new double[]{Double.NaN,0,0}));
    reject(()->SteppedPatchRayPicker.pick(shell,topology,new double[]{0,0},new double[]{1,0,0}));
    VariableHeightOrthogonalBRep other=VariableHeightOrthogonalBRep.apply(
        first,second,operation==VariableHeightOrthogonalMesh.Operation.UNION?
        VariableHeightOrthogonalMesh.Operation.DIFFERENCE:
        VariableHeightOrthogonalMesh.Operation.UNION);
    reject(()->SteppedPatchRayPicker.pick(other,topology,
        new double[]{0,0,0},new double[]{1,0,0}));
    if(!Arrays.equals(original,shell.triangles()))
     throw new AssertionError("Ray picking mutated B-Rep preview");
   }
  }
  System.out.println("SteppedPatchRayPickerTest: OK");
 }
}
