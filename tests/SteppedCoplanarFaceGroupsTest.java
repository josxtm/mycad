import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SteppedCoplanarFaceGroupsTest {
 private static PolygonPrismBRep prism(String key,int plane,double h,double[][] poly){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<poly.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/edge/"+i).getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=poly[i],b=poly[(i+1)%poly.length];
   ids.add(edge);lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,h,ids).asBRep(lines);
 }
 public static void main(String[] args){
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep first=prism("a"+plane+sign,plane,3*sign,a);
   PolygonPrismBRep second=prism("b"+plane+sign,plane,2*sign,b);
   for(VariableHeightOrthogonalMesh.Operation op:VariableHeightOrthogonalMesh.Operation.values()){
    VariableHeightOrthogonalBRep shell=VariableHeightOrthogonalBRep.apply(first,second,op);
    int before=shell.faces().size();
    List<List<String>> groups=SteppedCoplanarFaceGroups.find(shell);
    if(groups.isEmpty()||groups.size()>before)
     throw new AssertionError("Invalid grouping");
    Set<String> ids=new HashSet<>();
    for(List<String> group:groups)for(String id:group)
     if(!ids.add(id))throw new AssertionError("Face assigned twice");
    if(ids.size()!=before)throw new AssertionError("Lost faces");
    if(!groups.equals(SteppedCoplanarFaceGroups.find(shell)))
     throw new AssertionError("Nondeterministic groups");
    if(shell.faces().size()!=before)throw new AssertionError("Source mutated");
   }
  }
  try{SteppedCoplanarFaceGroups.find(null);throw new AssertionError("Null accepted");}
  catch(IllegalArgumentException expected){}
  System.out.println("SteppedCoplanarFaceGroupsTest: OK");
 }
}
