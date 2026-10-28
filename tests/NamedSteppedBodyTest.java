import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NamedSteppedBodyTest {
 private static String id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)).toString();}
 private static PolygonPrismBRep box(String name,int plane,double h,double x1,double x2){
  double[][] p={{x1,0},{x2,0},{x2,4},{x1,4}};
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();List<String> ids=new ArrayList<>();
  for(int i=0;i<4;i++){
   String line=id(name+i);ids.add(line);double[] a=p[i],b=p[(i+1)%4];
   lines.put(line,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id(name),ids.get(0),plane,h,ids).asBRep(lines);
 }
 private static NamedSteppedBody body(String owner,int plane,int sign,int height,double width){
  return NamedSteppedBody.apply(id(owner),box("a",plane,height*sign,0,width),box("b",plane,3*sign,2,6),
   VariableHeightOrthogonalMesh.Operation.UNION);
 }
 private static void check(boolean condition){if(!condition)throw new AssertionError();}
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Wrong snapshot/name accepted");
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   NamedSteppedBody old=body("owner",plane,sign,6,4),next=body("owner",plane,sign,7,5);
   check(old.bodyId.equals(next.bodyId));check(!old.shell.id().equals(next.shell.id()));
   check(old.shell.vertices().size()==next.shell.vertices().size());
   for(VariableHeightOrthogonalBRep.Vertex vertex:old.shell.vertices())
    check(next.findVertex(old.vertexId(vertex.id))!=null);
   for(VariableHeightOrthogonalBRep.Edge edge:old.shell.edges()){
    VariableHeightOrthogonalBRep.Edge found=next.findEdge(old.edgeId(edge.id));check(found!=null);
    Set<String> before=new HashSet<>(Arrays.asList(old.vertexId(edge.firstVertexId),old.vertexId(edge.secondVertexId)));
    Set<String> after=new HashSet<>(Arrays.asList(next.vertexId(found.firstVertexId),next.vertexId(found.secondVertexId)));
    check(before.equals(after));
   }
   for(VariableHeightOrthogonalBRep.Face face:old.shell.faces())
    check(next.findFace(old.faceId(face.id))!=null);
   Set<String> beforePatches=new HashSet<>(),afterPatches=new HashSet<>();
   for(SteppedCoplanarPatchTopology.PatchFace patch:SteppedCoplanarPatchTopology.build(old.shell).patches)
    check(beforePatches.add(old.patchId(patch)));
   for(SteppedCoplanarPatchTopology.PatchFace patch:SteppedCoplanarPatchTopology.build(next.shell).patches)
    check(afterPatches.add(next.patchId(patch)));
   check(beforePatches.equals(afterPatches));
   NamedSteppedBody other=body("other",plane,sign,6,4);
   for(VariableHeightOrthogonalBRep.Edge edge:old.shell.edges())
    check(other.findEdge(old.edgeId(edge.id))==null);
   VariableHeightOrthogonalBRep.Vertex top=null;int heightAxis=plane==0?2:plane==1?1:0;
   for(VariableHeightOrthogonalBRep.Vertex vertex:old.shell.vertices()){
    double[] xyz={vertex.x,vertex.y,vertex.z};if(Math.abs(xyz[heightAxis]-6*sign)<1e-6){top=vertex;break;}
   }
   check(top!=null);
   NamedSteppedBody mergedLevels=body("owner",plane,sign,3,4);
   check(mergedLevels.findVertex(old.vertexId(top.id))==null);
   int uAxis=plane==2?1:0;
   VariableHeightOrthogonalBRep.Vertex outer=null;
   for(VariableHeightOrthogonalBRep.Vertex vertex:old.shell.vertices()){
    double[] xyz={vertex.x,vertex.y,vertex.z};
    if(Math.abs(xyz[uAxis]-4)<1e-6&&Math.abs(xyz[heightAxis]-6*sign)<1e-6){outer=vertex;break;}
   }
   check(outer!=null);
   NamedSteppedBody mergedCoordinates=body("owner",plane,sign,6,6);
   check(mergedCoordinates.findVertex(old.vertexId(outer.id))==null);
   NamedSteppedBody difference=NamedSteppedBody.apply(id("owner"),box("a",plane,6*sign,0,4),
    box("b",plane,3*sign,2,6),VariableHeightOrthogonalMesh.Operation.DIFFERENCE);
   for(VariableHeightOrthogonalBRep.Vertex vertex:old.shell.vertices())
    check(difference.findVertex(old.vertexId(vertex.id))==null);
   NamedSteppedBody reversed=NamedSteppedBody.apply(id("owner"),box("b",plane,3*sign,2,6),
    box("a",plane,6*sign,0,4),VariableHeightOrthogonalMesh.Operation.UNION);
   for(VariableHeightOrthogonalBRep.Vertex vertex:old.shell.vertices())
    check(reversed.findVertex(old.vertexId(vertex.id))==null);
   reject(()->next.vertexId(old.shell.vertices().get(0).id));
   reject(()->next.patchId(SteppedCoplanarPatchTopology.build(old.shell).patches.get(0)));
  }
  reject(()->NamedSteppedBody.apply("bad UUID",null,null,VariableHeightOrthogonalMesh.Operation.UNION));
  System.out.println("NamedSteppedBodyTest: OK");
 }
}
