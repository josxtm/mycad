import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Regression: scene-level occlusion and deterministic B-Rep patch selection. */
public final class SteppedSceneRayPickerTest {
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
 private static SteppedSceneRayPicker.Candidate candidate(int plane,int sign,int scale){
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  String key="scene-"+plane+"-"+sign+"-"+scale;
  PolygonPrismBRep first=prism(key+"-a",plane,sign*(scale==1?3:6),a);
  PolygonPrismBRep second=prism(key+"-b",plane,sign*(scale==1?2:5),b);
  VariableHeightOrthogonalBRep shell=VariableHeightOrthogonalBRep.apply(
      first,second,VariableHeightOrthogonalMesh.Operation.UNION);
  return new SteppedSceneRayPicker.Candidate(
      shell,SteppedCoplanarPatchTopology.build(shell));
 }
 private static void near(double actual,double expected){
  if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-5)
   throw new AssertionError("Expected "+expected+" got "+actual);
 }
 private static void reject(Runnable operation){
  try{operation.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid scene accepted");
 }
 private static double[] vector(int plane,double u,double v,double height){
  if(plane==0)return new double[]{u,v,height};
  if(plane==1)return new double[]{u,height,v};
  return new double[]{height,u,v};
 }
 public static void main(String[] args){
  if(SteppedSceneRayPicker.pick(Collections.emptyList(),
      new double[]{0,0,0},new double[]{1,0,0})!=null)
   throw new AssertionError("Empty scene should miss");
  reject(()->SteppedSceneRayPicker.pick(null,new double[]{0,0,0},new double[]{1,0,0}));
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   SteppedSceneRayPicker.Candidate low=candidate(plane,sign,1);
   SteppedSceneRayPicker.Candidate high=candidate(plane,sign,2);
   List<SteppedSceneRayPicker.Candidate> scene=Arrays.asList(low,high);
   float[] lowBefore=low.shell.triangles(),highBefore=high.shell.triangles();
   double[] origin=vector(plane,1,1,10*sign);
   double[] direction=vector(plane,0,0,-sign);
   SteppedSceneRayPicker.Hit hit=SteppedSceneRayPicker.pick(scene,origin,direction);
   if(hit==null||hit.candidateIndex!=1||hit.shell!=high.shell)
    throw new AssertionError("Farther body incorrectly occludes front body");
   near(hit.patchHit.distance,4);
   double[] expected=vector(plane,1,1,6*sign);
   for(int axis=0;axis<3;axis++)near(hit.patchHit.point()[axis],expected[axis]);
   if(hit.patchHit.patch!=SteppedPatchTrianglePicker.resolve(
       high.shell,high.topology,hit.patchHit.triangleIndex))
    throw new AssertionError("Scene hit lost analytic patch identity");
   SteppedSceneRayPicker.Hit reversed=SteppedSceneRayPicker.pick(
       Arrays.asList(high,low),origin,direction);
   if(reversed==null||reversed.candidateIndex!=0||reversed.shell!=high.shell)
    throw new AssertionError("Nearest hit depends on candidate ordering");
   SteppedSceneRayPicker.Hit single=SteppedSceneRayPicker.pick(
       Collections.singletonList(low),origin,direction);
   if(single==null||single.shell!=low.shell)throw new AssertionError("Single body missed");
   near(single.patchHit.distance,7);
   double[] below=vector(plane,1,1,-10*sign);
   double[] toward=vector(plane,0,0,sign);
   SteppedSceneRayPicker.Hit tie=SteppedSceneRayPicker.pick(scene,below,toward);
   if(tie==null||tie.candidateIndex!=0)
    throw new AssertionError("Equal-distance tie must keep first candidate");
   near(tie.patchHit.distance,10);
   SteppedSceneRayPicker.Hit tieReversed=SteppedSceneRayPicker.pick(
       Arrays.asList(high,low),below,toward);
   if(tieReversed==null||tieReversed.candidateIndex!=0)
    throw new AssertionError("Reversed equal-distance tie not deterministic");
   if(SteppedSceneRayPicker.pick(scene,vector(plane,100,100,10*sign),direction)!=null)
    throw new AssertionError("Ray outside both bodies must miss");
   reject(()->SteppedSceneRayPicker.pick(Arrays.asList(low,low),origin,direction));
   reject(()->SteppedSceneRayPicker.pick(Arrays.asList(low,null),origin,direction));
   reject(()->SteppedSceneRayPicker.pick(scene,origin,new double[]{0,0,0}));
   reject(()->SteppedSceneRayPicker.pick(scene,origin,new double[]{Double.NaN,0,0}));
   reject(()->new SteppedSceneRayPicker.Candidate(low.shell,high.topology));
   if(!Arrays.equals(origin,vector(plane,1,1,10*sign))||
      !Arrays.equals(direction,vector(plane,0,0,-sign))||
      !Arrays.equals(lowBefore,low.shell.triangles())||
      !Arrays.equals(highBefore,high.shell.triangles()))
    throw new AssertionError("Scene picking mutated caller or geometry");
  }
  System.out.println("SteppedSceneRayPickerTest: OK");
 }
}
