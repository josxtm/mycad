import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Visible GPU hit -> analytic stepped patch, without bypassing occlusion. */
public final class SteppedVisiblePatchBridgeTest {
 private static void check(boolean ok,String message){
  if(!ok)throw new AssertionError(message);
 }
 private static void near(double actual,double expected){
  if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-5)
   throw new AssertionError("Expected "+expected+" got "+actual);
 }
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid scene registration accepted");
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
  PolygonPrismBRep first=prism("bridge-a"+plane+sign,plane,6*sign,a);
  PolygonPrismBRep second=prism("bridge-b"+plane+sign,plane,5*sign,b);
  return VariableHeightOrthogonalBRep.apply(
      first,second,VariableHeightOrthogonalMesh.Operation.UNION);
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
   SteppedCoplanarPatchTopology.Topology topology=
       SteppedCoplanarPatchTopology.build(shell);
   float[] triangles=shell.triangles(),unchanged=triangles.clone();
   float[] missed={100,100,100,101,100,100,100,101,100};
   List<ViewportMeshRayPicker.Candidate> scene=Arrays.asList(
       mesh(missed),mesh(triangles));
   SteppedVisiblePatchBridge.Candidate candidate=
       new SteppedVisiblePatchBridge.Candidate(1,triangles,shell,topology);
   ViewportMeshRayPicker.Hit visible=ViewportMeshRayPicker.pick(
       scene,projection(plane,sign),200,100,110,45);
   check(visible!=null&&visible.triangle>=1,"GPU did not select shell");
   SteppedVisiblePatchBridge.Hit selected=
       SteppedVisiblePatchBridge.resolve(visible,Collections.singletonList(candidate));
   check(selected!=null,"Visible stepped patch not resolved");
   check(selected.globalTriangle==visible.triangle&&
       selected.localTriangle==visible.triangle-1,"Global/local triangle mismatch");
   check(selected.patch==SteppedPatchTrianglePicker.resolve(
       shell,topology,selected.localTriangle),"Wrong B-Rep patch");
   check(selected.shellId.equals(shell.id()),"Wrong shell identity");
   double[] point=selected.point();
   near(point[0],visible.x);near(point[1],visible.y);near(point[2],visible.z);
   point[0]=999;
   near(selected.point()[0],visible.x);
   check(Arrays.equals(triangles,unchanged),"Preview was mutated");
   // An imported triangle in front occludes the analytic shell.
   ViewportMeshRayPicker.Hit blocked=ViewportMeshRayPicker.pick(
       Arrays.asList(mesh(frontTriangle(plane,sign)),mesh(triangles)),
       projection(plane,sign),200,100,110,45);
   check(blocked!=null&&blocked.triangle==0,"Occluder was not frontmost");
   check(SteppedVisiblePatchBridge.resolve(
       blocked,Collections.singletonList(candidate))==null,
       "Hidden stepped patch bypassed GPU visibility");
   check(SteppedVisiblePatchBridge.resolve(null,
       Collections.singletonList(candidate))==null,"Null hit should miss");
   float[] stale=triangles.clone();stale[0]+=1;
   reject(()->new SteppedVisiblePatchBridge.Candidate(
       1,stale,shell,topology));
   reject(()->new SteppedVisiblePatchBridge.Candidate(
       -1,triangles,shell,topology));
   reject(()->new SteppedVisiblePatchBridge.Candidate(
       Integer.MAX_VALUE,triangles,shell,topology));
   reject(()->SteppedVisiblePatchBridge.resolve(
       visible,Arrays.asList(candidate,candidate)));
   reject(()->SteppedVisiblePatchBridge.resolve(visible,null));
  }
  System.out.println("SteppedVisiblePatchBridgeTest: OK");
 }
}
