import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SteppedBoundaryEdgePickerTest {
 private static String id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)).toString();}
 private static PolygonPrismBRep box(String name,int plane,double h,double x1,double x2){
  double[][] p={{x1,0},{x2,0},{x2,4},{x1,4}};
  List<String> ids=new ArrayList<>();Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<4;i++){
   String line=id(name+i);ids.add(line);double[] a=p[i],b=p[(i+1)%4];
   lines.put(line,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id(name),ids.get(0),plane,h,ids).asBRep(lines);
 }
 private static VariableHeightOrthogonalBRep shell(int plane,int sign,int height){
  return VariableHeightOrthogonalBRep.apply(box("a",plane,height*sign,0,4),
   box("b",plane,3*sign,2,6),VariableHeightOrthogonalMesh.Operation.UNION);
 }
 private static SteppedVisiblePatchBridge.Hit hit(VariableHeightOrthogonalBRep shell,int sign,double u,double v){
  double[] matrix=new double[16];matrix[15]=1;
  int plane=shell.plane();
  matrix[(plane==2?1:0)*4]=.1;matrix[(plane==0?1:2)*4+1]=.1;
  matrix[(plane==0?2:plane==1?1:0)*4+2]=-.1*sign;
  float[] triangles=shell.triangles();
  ViewportMeshRayPicker.Hit visible=ViewportMeshRayPicker.pick(
   Collections.singletonList(new ViewportMeshRayPicker.Candidate(new MeshRayIndex(triangles),triangles.length/9)),
   matrix,100,100,50+u*5,50-v*5);
  return SteppedScenePatchRegistry.register(Collections.singletonList(triangles),
   Collections.singletonList(new SteppedScenePatchRegistry.Binding(0,shell))).resolve(visible);
 }
 private static void check(boolean value){if(!value)throw new AssertionError();}
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid boundary pick accepted");
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   VariableHeightOrthogonalBRep shell=shell(plane,sign,6);float[] before=shell.triangles();
   SteppedVisiblePatchBridge.Hit near=hit(shell,sign,.02,1.7);check(near!=null);
   SteppedBoundaryEdgePicker.Hit edge=SteppedBoundaryEdgePicker.nearest(shell,near,.05);
   check(edge!=null&&Math.abs(edge.distance-.02)<1e-5&&edge.length>0);
   check(edge.patchIndex==near.patch.index&&edge.adjacentPatchIndex>=0&&edge.adjacentPatchIndex!=edge.patchIndex);
   check(shell.edges().stream().anyMatch(e->e.id.equals(edge.edgeId)));
   float[] endpoints=edge.endpoints();endpoints[0]=999;check(edge.endpoints()[0]!=999);
   check(SteppedBoundaryEdgePicker.nearest(shell,near,.001)==null);
   SteppedVisiblePatchBridge.Hit seam=hit(shell,sign,2,1.7);check(seam!=null);
   check(SteppedBoundaryEdgePicker.nearest(shell,seam,1e-4)==null);
   final int p=plane,s=sign;
   reject(()->SteppedBoundaryEdgePicker.nearest(shell(p,s,7),near,.05));
   reject(()->SteppedBoundaryEdgePicker.nearest(shell,near,Double.NaN));
   reject(()->SteppedBoundaryEdgePicker.nearest(shell,near,-1));
   double[] point=near.point();double ox=point[0],oy=point[1],oz=point[2]+8;
   double radius=SteppedBoundaryEdgePicker.touchTolerance(shell,ox,oy,oz,point,900,1);
   check(Math.abs(radius-EdgeScreenTolerance.compute(4,8,900,1))<1e-10);
   reject(()->SteppedBoundaryEdgePicker.touchTolerance(shell,ox,oy,oz,point,0,1));
   check(Arrays.equals(before,shell.triangles()));
  }
  System.out.println("SteppedBoundaryEdgePickerTest: OK");
 }
}
