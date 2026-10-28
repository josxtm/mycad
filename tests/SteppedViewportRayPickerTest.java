import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Regression: screen-to-world B-Rep selection in all work planes. */
public final class SteppedViewportRayPickerTest {
 private static void near(double actual,double expected){
  if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-6)
   throw new AssertionError("Expected "+expected+" got "+actual);
 }
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid viewport input accepted");
 }
 private static double[] matrix(double[][] rows){
  double[] result=new double[16];
  for(int row=0;row<4;row++)for(int col=0;col<4;col++)
   result[col*4+row]=rows[row][col];
  return result;
 }
 private static double[] projection(int plane,int sign){
  double[][] rows=new double[4][4];
  rows[3][3]=1;
  if(plane==0){rows[0][0]=.1;rows[1][1]=.1;rows[2][2]=-.1*sign;}
  else if(plane==1){rows[0][0]=.1;rows[1][2]=.1;rows[2][1]=-.1*sign;}
  else {rows[0][1]=.1;rows[1][2]=.1;rows[2][0]=-.1*sign;}
  return matrix(rows);
 }
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
 private static SteppedSceneRayPicker.Candidate candidate(int plane,int sign,int height){
  String key="viewport-"+plane+"-"+sign+"-"+height;
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  PolygonPrismBRep first=prism(key+"a",plane,sign*height,a);
  PolygonPrismBRep second=prism(key+"b",plane,sign*(height-1),b);
  VariableHeightOrthogonalBRep shell=VariableHeightOrthogonalBRep.apply(
      first,second,VariableHeightOrthogonalMesh.Operation.UNION);
  return new SteppedSceneRayPicker.Candidate(
      shell,SteppedCoplanarPatchTopology.build(shell));
 }
 private static double[] world(int plane,double u,double v,double h){
  if(plane==0)return new double[]{u,v,h};
  if(plane==1)return new double[]{u,h,v};
  return new double[]{h,u,v};
 }
 public static void main(String[] args){
  double[] identity=matrix(new double[][]{{1,0,0,0},{0,1,0,0},
      {0,0,1,0},{0,0,0,1}});
  SteppedViewportRayPicker.Ray center=SteppedViewportRayPicker.ray(identity,100,200,50,100);
  near(center.origin()[2],-1);near(center.direction()[2],1);
  near(center.farDistance(),2);
  double[] copy=center.origin();copy[2]=100;
  near(center.origin()[2],-1);
  double[] dirCopy=center.direction();dirCopy[2]=0;
  near(center.direction()[2],1);
  double nearPlane=1,farPlane=10;
  double[][] perspective={{1,0,0,0},{0,1,0,0},
      {0,0,-(farPlane+nearPlane)/(farPlane-nearPlane),
           -2*farPlane*nearPlane/(farPlane-nearPlane)},
      {0,0,-1,0}};
  double[] perspectiveMatrix=matrix(perspective);
  SteppedViewportRayPicker.Ray p=SteppedViewportRayPicker.ray(
      perspectiveMatrix,100,100,75,50);
  near(p.origin()[0],.5);near(p.origin()[2],-1);
  near(p.direction()[0],1/Math.sqrt(5));
  near(p.direction()[2],-2/Math.sqrt(5));
  reject(()->SteppedViewportRayPicker.ray(null,100,100,50,50));
  reject(()->SteppedViewportRayPicker.ray(new double[16],100,100,50,50));
  reject(()->SteppedViewportRayPicker.ray(identity,0,100,50,50));
  reject(()->SteppedViewportRayPicker.ray(identity,100,100,-1,50));
  reject(()->SteppedViewportRayPicker.ray(identity,100,100,50,101));
  reject(()->SteppedViewportRayPicker.ray(identity,100,100,Double.NaN,50));
  double[] invalid=identity.clone();invalid[4]=Double.POSITIVE_INFINITY;
  reject(()->SteppedViewportRayPicker.ray(invalid,100,100,50,50));
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   double[] vp=projection(plane,sign),before=vp.clone();
   SteppedSceneRayPicker.Candidate low=candidate(plane,sign,3);
   SteppedSceneRayPicker.Candidate high=candidate(plane,sign,6);
   List<SteppedSceneRayPicker.Candidate> scene=Arrays.asList(low,high);
   float[] lowMesh=low.shell.triangles(),highMesh=high.shell.triangles();
   SteppedViewportRayPicker.Ray ray=SteppedViewportRayPicker.ray(vp,200,100,110,45);
   double[] start=world(plane,1,1,10*sign);
   double[] along=world(plane,0,0,-sign);
   for(int axis=0;axis<3;axis++){
    near(ray.origin()[axis],start[axis]);
    near(ray.direction()[axis],along[axis]);
   }
   SteppedSceneRayPicker.Hit hit=SteppedViewportRayPicker.pick(scene,vp,200,100,110,45);
   if(hit==null||hit.candidateIndex!=1||hit.shell!=high.shell)
    throw new AssertionError("Screen ray did not pick front shell");
   near(hit.patchHit.distance,4);
   double[] expected=world(plane,1,1,6*sign);
   for(int axis=0;axis<3;axis++)near(hit.patchHit.point()[axis],expected[axis]);
   if(hit.patchHit.patch!=SteppedPatchTrianglePicker.resolve(
       high.shell,high.topology,hit.patchHit.triangleIndex))
    throw new AssertionError("Viewport hit lost analytic patch");
   if(SteppedViewportRayPicker.pick(scene,vp,200,100,190,5)!=null)
    throw new AssertionError("Ray outside geometry should miss");
   // Near=20, far=10: geometry at 0..6 must not be selectable.
   double[] clipped=vp.clone();
   for(int column=0;column<4;column++)clipped[column*4+2]=vp[column*4+2]*2;
   clipped[14]=3;
   if(SteppedViewportRayPicker.pick(scene,clipped,200,100,110,45)!=null)
    throw new AssertionError("Geometry beyond far clip was selected");
   if(!Arrays.equals(vp,before)||!Arrays.equals(lowMesh,low.shell.triangles())||
       !Arrays.equals(highMesh,high.shell.triangles()))
    throw new AssertionError("Viewport picking modified scene or camera");
  }
  System.out.println("SteppedViewportRayPickerTest: OK");
 }
}
