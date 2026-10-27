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
    List<List<String>> boundaries=SteppedCoplanarFaceGroups.boundaryEdgeIds(shell);
    List<List<List<String>>> loops=SteppedCoplanarFaceGroups.boundaryLoops(shell);
    List<List<Double>> signedAreas=SteppedCoplanarFaceGroups.boundaryLoopAreas(shell);
    List<List<Integer>> counts=SteppedCoplanarFaceGroups.loopCounts(shell);
    List<SteppedCoplanarFaceGroups.Patch> descriptors=SteppedCoplanarFaceGroups.describe(shell);
    if(descriptors.size()!=groups.size())throw new AssertionError("Missing analytic patches");
    for(int i=0;i<descriptors.size();i++){
     SteppedCoplanarFaceGroups.Patch patch=descriptors.get(i);
     if(!patch.faceIds.equals(groups.get(i))||
        !patch.boundaryHalfEdgeLoops.equals(SteppedCoplanarFaceGroups.boundaryLoops(shell).get(i))||
        !patch.signedLoopAreas.equals(SteppedCoplanarFaceGroups.boundaryLoopAreas(shell).get(i))||
        Math.abs(patch.area-SteppedCoplanarFaceGroups.areas(shell).get(i))>1e-5||
        patch.exteriorCount+patch.holeCount!=patch.boundaryHalfEdgeLoops.size())
      throw new AssertionError("Inconsistent analytic patch descriptor");
    }
    try{descriptors.add(descriptors.get(0));throw new AssertionError("Mutable descriptors");}
    catch(UnsupportedOperationException expected){}
    try{descriptors.get(0).faceIds.clear();throw new AssertionError("Mutable descriptor faces");}
    catch(UnsupportedOperationException expected){}
    if(counts.size()!=groups.size()||
       !counts.equals(SteppedCoplanarFaceGroups.loopCounts(shell)))
     throw new AssertionError("Invalid or nondeterministic loop counts");
    for(int i=0;i<counts.size();i++){
     int exterior=0,holes=0;
     for(double area:SteppedCoplanarFaceGroups.boundaryLoopAreas(shell).get(i)){
      if(area>0)exterior++;else holes++;
     }
     if(counts.get(i).get(0)!=exterior||counts.get(i).get(1)!=holes||exterior<1)
      throw new AssertionError("Incorrect patch loop classification");
    }
    try{counts.get(0).set(0,99);throw new AssertionError("Mutable loop counts");}
    catch(UnsupportedOperationException expected){}
    if(signedAreas.size()!=groups.size()||
       !signedAreas.equals(SteppedCoplanarFaceGroups.boundaryLoopAreas(shell)))
     throw new AssertionError("Invalid or nondeterministic signed loop areas");
    for(int i=0;i<signedAreas.size();i++){
     double sum=0;
     for(double area:signedAreas.get(i)){
      if(!Double.isFinite(area)||Math.abs(area)<1e-9)
       throw new AssertionError("Degenerate signed loop area");
      sum+=area;
     }
     if(Math.abs(sum-SteppedCoplanarFaceGroups.areas(shell).get(i))>1e-5)
      throw new AssertionError("Signed loop area does not conserve patch area");
    }
    if(loops.size()!=groups.size()||
       !loops.equals(SteppedCoplanarFaceGroups.boundaryLoops(shell)))
     throw new AssertionError("Invalid or nondeterministic boundary loops");
    Map<String,VariableHeightOrthogonalBRep.HalfEdge> halfById=new HashMap<>();
    for(VariableHeightOrthogonalBRep.HalfEdge half:shell.halfEdges())
     halfById.put(half.id,half);
    for(int i=0;i<loops.size();i++){
     Set<String> loopEdges=new HashSet<>();
     for(List<String> loop:loops.get(i)){
      if(loop.size()<4)throw new AssertionError("Degenerate loop");
      for(int j=0;j<loop.size();j++){
       VariableHeightOrthogonalBRep.HalfEdge current=halfById.get(loop.get(j));
       VariableHeightOrthogonalBRep.HalfEdge next=halfById.get(loop.get((j+1)%loop.size()));
       if(current==null||next==null||!current.toId.equals(next.fromId))
        throw new AssertionError("Open or misoriented boundary loop");
       if(!loopEdges.add(current.edgeId))throw new AssertionError("Duplicate loop edge");
      }
     }
     if(!loopEdges.equals(new HashSet<>(SteppedCoplanarFaceGroups.boundaryEdgeIds(shell).get(i))))
      throw new AssertionError("Boundary loop coverage mismatch");
    }
    if(boundaries.size()!=groups.size()||
       !boundaries.equals(SteppedCoplanarFaceGroups.boundaryEdgeIds(shell)))
     throw new AssertionError("Invalid or nondeterministic patch boundaries");
    Map<String,Integer> owners=new HashMap<>();
    for(VariableHeightOrthogonalBRep.Edge edge:shell.edges()){
     List<String> f=edge.adjacentFaceIds();
     int ga=-1,gb=-1;
     for(int i=0;i<groups.size();i++){
      if(groups.get(i).contains(f.get(0)))ga=i;
      if(groups.get(i).contains(f.get(1)))gb=i;
     }
     if(ga<0||gb<0)throw new AssertionError("Missing patch owner");
     owners.put(edge.id,ga==gb?0:2);
    }
    for(List<String> boundary:boundaries){
     if(boundary.isEmpty()||new HashSet<>(boundary).size()!=boundary.size())
      throw new AssertionError("Invalid patch boundary edge set");
     for(String edgeId:boundary){
      Integer count=owners.get(edgeId);
      if(count==null||count!=2)throw new AssertionError("Internal edge exposed");
     }
    }
    for(Map.Entry<String,Integer> e:owners.entrySet()){
     int count=0;
     for(List<String> boundary:boundaries)
      if(boundary.contains(e.getKey()))count++;
     if(count!=e.getValue())throw new AssertionError("Incorrect edge boundary incidence");
    }
    List<Double> patchAreas=SteppedCoplanarFaceGroups.areas(shell);
    if(patchAreas.size()!=groups.size())
     throw new AssertionError("Patch area count mismatch");
    double sum=0;
    for(double patchArea:patchAreas){
     if(!Double.isFinite(patchArea)||patchArea<=0)
      throw new AssertionError("Invalid patch area");
     sum+=patchArea;
    }
    if(Math.abs(sum-shell.surfaceArea())>1e-5*Math.max(1,shell.surfaceArea()))
     throw new AssertionError("Coplanar patch areas do not conserve surface area");
    try{patchAreas.add(1.0);throw new AssertionError("Mutable patch areas");}
    catch(UnsupportedOperationException expected){}
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
