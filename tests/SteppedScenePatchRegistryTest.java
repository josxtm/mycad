import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Renderer-scene registration and visible GPU -> B-Rep patch regression. */
public final class SteppedScenePatchRegistryTest {
 private static void check(boolean ok,String message){
  if(!ok)throw new AssertionError(message);
 }
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid scene accepted");
 }
 private static double[] projection(int plane,int sign){
  double[][] rows=new double[4][4];
  rows[3][3]=1;
  if(plane==0){rows[0][0]=.1;rows[1][1]=.1;rows[2][2]=-.1*sign;}
  else if(plane==1){rows[0][0]=.1;rows[1][2]=.1;rows[2][1]=-.1*sign;}
  else {rows[0][1]=.1;rows[1][2]=.1;rows[2][0]=-.1*sign;}
  double[] matrix=new double[16];
  for(int row=0;row<4;row++)for(int col=0;col<4;col++)
   matrix[col*4+row]=rows[row][col];
  return matrix;
 }
 private static double[] world(int plane,double u,double v,double height){
  if(plane==0)return new double[]{u,v,height};
  if(plane==1)return new double[]{u,height,v};
  return new double[]{height,u,v};
 }
 private static PolygonPrismBRep prism(String key,int plane,double height,double[][] polygon){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
  List<String> edges=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> segments=new LinkedHashMap<>();
  for(int i=0;i<polygon.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/edge/"+i).getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=polygon[i],b=polygon[(i+1)%polygon.length];
   edges.add(edge);
   segments.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,edges.get(0),plane,height,edges).asBRep(segments);
 }
 private static VariableHeightOrthogonalBRep shell(int plane,int sign){
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  return VariableHeightOrthogonalBRep.apply(
      prism("registry-a"+plane+sign,plane,6*sign,a),
      prism("registry-b"+plane+sign,plane,5*sign,b),
      VariableHeightOrthogonalMesh.Operation.UNION);
 }
 private static ViewportMeshRayPicker.Candidate mesh(float[] triangles){
  return new ViewportMeshRayPicker.Candidate(
      new MeshRayIndex(triangles),triangles.length/9);
 }
 private static float[] frontTriangle(int plane,int sign){
  float[] result=new float[9];
  double[][] uv={{0,0},{4,0},{0,4}};
  for(int i=0;i<3;i++){
   double[] point=world(plane,uv[i][0],uv[i][1],8*sign);
   for(int k=0;k<3;k++)result[i*3+k]=(float)point[k];
  }
  return result;
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   VariableHeightOrthogonalBRep shell=shell(plane,sign);
   float[] triangles=shell.triangles(),original=triangles.clone();
   float[] miss={100,100,100,101,100,100,100,101,100};
   List<float[]> scene=Arrays.asList(miss,triangles);
   SteppedScenePatchRegistry.Binding binding=
       new SteppedScenePatchRegistry.Binding(1,shell);
   SteppedScenePatchRegistry registry=SteppedScenePatchRegistry.register(
       scene,Collections.singletonList(binding));
   check(registry.registeredShellCount()==1,"Shell not registered");
   ViewportMeshRayPicker.Hit visible=ViewportMeshRayPicker.pick(
       Arrays.asList(mesh(miss),mesh(triangles)),
       projection(plane,sign),200,100,110,45);
   check(visible!=null&&visible.triangle>=1,"Visible shell not hit");
   SteppedVisiblePatchBridge.Hit patch=registry.resolve(visible);
   check(patch!=null&&patch.globalTriangle==visible.triangle,
       "Wrong global triangle or missing patch");
   check(patch.localTriangle==visible.triangle-1&&
       patch.shellId.equals(shell.id()),"Wrong local triangle or shell");
   check(Arrays.equals(original,triangles),"Scene was mutated");
   check(registry.resolve(null)==null,"Null mesh hit must miss");
   check(SteppedScenePatchRegistry.register(scene,
       Collections.<SteppedScenePatchRegistry.Binding>emptyList())
       .resolve(visible)==null,"Unregistered shell was selected");

   float[] blocker=frontTriangle(plane,sign);
   ViewportMeshRayPicker.Hit blocked=ViewportMeshRayPicker.pick(
       Arrays.asList(mesh(blocker),mesh(triangles)),
       projection(plane,sign),200,100,110,45);
   check(blocked!=null&&blocked.triangle==0,"Occluder not frontmost");
   check(SteppedScenePatchRegistry.register(
       Arrays.asList(blocker,triangles),Collections.singletonList(binding))
       .resolve(blocked)==null,"Occluded patch incorrectly selected");

   float[] stale=triangles.clone();stale[0]+=1;
   reject(()->SteppedScenePatchRegistry.register(
       Arrays.asList(miss,stale),Collections.singletonList(binding)));
   reject(()->SteppedScenePatchRegistry.register(scene,
       Arrays.asList(binding,binding)));
   reject(()->SteppedScenePatchRegistry.register(scene,
       Collections.singletonList(new SteppedScenePatchRegistry.Binding(2,shell))));
   reject(()->new SteppedScenePatchRegistry.Binding(-1,shell));
   reject(()->new SteppedScenePatchRegistry.Binding(0,null));
   reject(()->SteppedScenePatchRegistry.register(null,
       Collections.singletonList(binding)));
   reject(()->SteppedScenePatchRegistry.register(scene,null));
   reject(()->SteppedScenePatchRegistry.register(
       Arrays.asList(new float[]{1,2},triangles),Collections.singletonList(binding)));
  }
  System.out.println("SteppedScenePatchRegistryTest: OK");
 }
}
