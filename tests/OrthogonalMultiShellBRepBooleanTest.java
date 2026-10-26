import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class OrthogonalMultiShellBRepBooleanTest {
 private static final String A="1a1b1c1d-0000-4000-8000-000000000001",
                             B="1a1b1c1d-0000-4000-8000-000000000002";
 private static void check(boolean value,String why){
  if(!value)throw new AssertionError(why);
 }
 private static void close(double got,double expected,String why){
  if(Math.abs(got-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(why+": "+got+" != "+expected);
 }
 private static void reject(Runnable task,String why){
  try{task.run();}catch(IllegalArgumentException good){return;}
  throw new AssertionError("Expected rejection: "+why);
 }
 static PolygonPrismBRep solid(String id,int plane,double height,double[][] p){
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<p.length;i++){
   String key=UUID.nameUUIDFromBytes((id+"/line/"+i).getBytes(StandardCharsets.UTF_8))
      .toString();
   double[] v=p[i],w=p[(i+1)%p.length];
   lines.put(key,new ClosedSketchExtrusion.Segment(
     plane,v[0],v[1],w[0],w[1]));
   ids.add(key);
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids).asBRep(lines);
 }
 static double[][] box(double x1,double y1,double x2,double y2){
  return new double[][]{{x1,y1},{x2,y1},{x2,y2},{x1,y2}};
 }
 static void inspect(OrthogonalPolygonBRepBoolean.MultiResult result,int count,
                     double area,double height,String label){
  check(result.shellCount==count,label+" B-Rep component count");
  check(result.solids().size()==count,label+" B-Rep solids");
  close(result.area,area,label+" area");
  close(result.volume,area*Math.abs(height),label+" analytic volume");
  close(result.distance,height,label+" signed extrusion");
  TriangleMeshHealth.Report mesh=TriangleMeshHealth.inspect(result.triangles());
  check(mesh.isWatertight(),label+" watertight combined mesh");
  check(mesh.shells==count,label+" exact mesh connected shells");
  close(mesh.signedVolume,area*Math.abs(height),label+" signed mesh volume");
  for(PolygonPrismBRep body:result.solids()){
   check(body.vertices().size()-body.edges().size()+body.faces().size()==2,
      label+" Euler characteristic per solid");
   check(TriangleMeshHealth.inspect(body.triangles()).isWatertight(),
      label+" each component is individually closed");
  }
  float[] first=result.triangles();
  check(first!=result.triangles(),label+" returns defensive mesh clone");
  check(Arrays.equals(first,result.triangles()),label+" stable mesh floats");
  boolean immutable=false;
  try{result.solids().clear();}catch(UnsupportedOperationException expected){immutable=true;}
  check(immutable,label+" shell collection immutable");
 }
 public static void main(String[] args){
  for(int p=0;p<3;p++)for(double height:new double[]{3,-3}){
   final int plane=p;final double h=height;
   final int fixedPlane=p;
   PolygonPrismBRep left=solid(A,p,h,box(0,0,2,2));
   PolygonPrismBRep right=solid(B,p,h,box(5,0,7,2));
   OrthogonalPolygonBRepBoolean.MultiResult union=
    OrthogonalPolygonBRepBoolean.applyMulti(left,right,
     OrthogonalPolygonBRepBoolean.Operation.UNION);
   inspect(union,2,8,h,"Two disjoint rectangles, p="+p);
   check(Arrays.equals(union.triangles(),
    OrthogonalPolygonBRepBoolean.applyMulti(left,right,
     OrthogonalPolygonBRepBoolean.Operation.UNION).triangles()),"Reproducible multi union");
   check(Arrays.equals(union.triangles(),
    OrthogonalPolygonBRepBoolean.applyMulti(right,left,
     OrthogonalPolygonBRepBoolean.Operation.UNION).triangles()),"Commutative multi union");
   reject(()->OrthogonalPolygonBRepBoolean.apply(left,right,
     OrthogonalPolygonBRepBoolean.Operation.UNION),"Legacy single result rejects disjoint shells");
   PolygonPrismBRep base=solid(A,p,h,box(0,0,8,4));
   PolygonPrismBRep stripe=solid(B,p,h,box(3,-1,5,5));
   OrthogonalPolygonBRepBoolean.MultiResult split=
    OrthogonalPolygonBRepBoolean.applyMulti(base,stripe,
     OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE);
   inspect(split,2,24,h,"Stripe splits one solid into two");
   reject(()->OrthogonalPolygonBRepBoolean.apply(base,stripe,
     OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),"Legacy difference rejects disconnected");
   PolygonPrismBRep touching=solid(B,p,h,box(8,4,10,6));
   reject(()->OrthogonalPolygonBRepBoolean.applyMulti(base,touching,
     OrthogonalPolygonBRepBoolean.Operation.UNION),"Vertex-only contact not manifold");
   PolygonPrismBRep hole=solid(B,p,h,box(2,1,4,3));
   reject(()->OrthogonalPolygonBRepBoolean.applyMulti(base,hole,
     OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),"An internal hole requires inner wires");
   PolygonPrismBRep overlap=solid(B,p,h,box(2,1,6,5));
   for(OrthogonalPolygonBRepBoolean.Operation op:
       OrthogonalPolygonBRepBoolean.Operation.values()){
    float[] old=OrthogonalPolygonBRepBoolean.apply(base,overlap,op).triangles();
    OrthogonalPolygonBRepBoolean.MultiResult upgraded=
     OrthogonalPolygonBRepBoolean.applyMulti(base,overlap,op);
    check(upgraded.shellCount==1,"Single-body compatibility");
    check(Arrays.equals(old,upgraded.triangles()),"Original JSON mesh byte-equivalence");
   }
   reject(()->OrthogonalPolygonBRepBoolean.applyMulti(base,right,
     OrthogonalPolygonBRepBoolean.Operation.INTERSECTION),
      "No volume in intersection");
   reject(()->OrthogonalPolygonBRepBoolean.applyMulti(base,
     solid(B,fixedPlane,-h,box(2,1,6,5)),OrthogonalPolygonBRepBoolean.Operation.UNION),
      "Incompatible signed heights");
  }
  reject(()->OrthogonalPolygonBRepBoolean.applyMulti(
    solid(A,0,3,box(0,0,2,2)),
    solid(B,1,3,box(5,0,7,2)),
    OrthogonalPolygonBRepBoolean.Operation.UNION),"Different planes");
  reject(()->OrthogonalPolygonBRepBoolean.applyMulti(null,
    solid(B,0,3,box(5,0,7,2)),
    OrthogonalPolygonBRepBoolean.Operation.UNION),"Missing operand");
  System.out.println("OrthogonalMultiShellBRepBooleanTest: OK");
 }
}
