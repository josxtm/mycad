import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class OrthogonalPolygonBRepBooleanTest {
 private static final String A="3b49dd9a-0001-4001-8001-000000000001";
 private static final String B="3b49dd9a-0001-4001-8001-000000000002";
 static void ok(boolean x,String label){if(!x)throw new AssertionError(label);}
 static void near(double x,double y,String label){
  if(Math.abs(x-y)>1e-4*Math.max(1,Math.abs(y)))
   throw new AssertionError(label+": "+x+" != "+y);
 }
 static void invalid(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Expected rejection: "+label);
 }
 static PolygonPrismBRep prism(String id,int plane,double height,double[][] poly){
  ArrayList<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<poly.length;i++){
   String uuid=UUID.nameUUIDFromBytes((id+"/segment/"+i)
      .getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=poly[i],b=poly[(i+1)%poly.length];
   lines.put(uuid,new ClosedSketchExtrusion.Segment(plane,
      a[0],a[1],b[0],b[1]));
   ids.add(uuid);
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids).asBRep(lines);
 }
 static double[][] rectangle(double x1,double y1,double x2,double y2){
  return new double[][]{{x1,y1},{x2,y1},{x2,y2},{x1,y2}};
 }
 static final double[][] L={{0,0},{4,0},{4,1},{1,1},{1,4},{0,4}};
 static final double[][] U={{0,0},{5,0},{5,5},{4,5},{4,1},{1,1},{1,5},{0,5}};
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(double signed:new double[]{3,-3}){
   final int p=plane;final double h=signed;
   PolygonPrismBRep a=prism(A,p,h,L);
   PolygonPrismBRep b=prism(B,p,h,rectangle(2,0,5,3));
   for(OrthogonalPolygonBRepBoolean.Operation op:
       OrthogonalPolygonBRepBoolean.Operation.values()){
    OrthogonalPolygonBRepBoolean.Result actual=
      OrthogonalPolygonBRepBoolean.apply(a,b,op);
    double area=op==OrthogonalPolygonBRepBoolean.Operation.UNION?14:
                op==OrthogonalPolygonBRepBoolean.Operation.INTERSECTION?2:5;
    near(actual.area,area,op+" area");
    near(actual.volume,area*3,op+" volume");
    near(actual.distance,h,op+" signed height");
    ok(actual.solid.vertices().size()-actual.solid.edges().size()+
       actual.solid.faces().size()==2,op+" B-Rep Euler");
    TriangleMeshHealth.Report mesh=TriangleMeshHealth.inspect(actual.triangles());
    ok(mesh.isWatertight()&&mesh.shells==1,op+" watertight single shell");
    near(mesh.signedVolume,area*3,op+" positive winding");
    ok(Arrays.equals(actual.triangles(),
       OrthogonalPolygonBRepBoolean.apply(a,b,op).triangles()),
       op+" deterministic");
    if(op!=OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE){
     OrthogonalPolygonBRepBoolean.Result swapped=
       OrthogonalPolygonBRepBoolean.apply(b,a,op);
     ok(Arrays.equals(actual.triangles(),swapped.triangles()),
       op+" operand order independent");
    }
   }
   OrthogonalPolygonBRepBoolean.Result otherOrder=
     OrthogonalPolygonBRepBoolean.apply(b,a,
       OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE);
   near(otherOrder.area,7,"Ordered difference B minus A");
   PolygonPrismBRep u=prism(B,p,h,U);
   PolygonPrismBRep inner=prism(A,p,h,rectangle(2,0,3,2));
   OrthogonalPolygonBRepBoolean.Result fill=
     OrthogonalPolygonBRepBoolean.apply(u,inner,
       OrthogonalPolygonBRepBoolean.Operation.UNION);
   near(fill.area,15,"U shape with bridge creates concave but single-shell B-Rep");
   ok(TriangleMeshHealth.inspect(fill.triangles()).isWatertight(),
      "U plus bridge watertight");
   invalid(()->OrthogonalPolygonBRepBoolean.apply(
      prism(A,p,h,rectangle(0,0,5,5)),
      prism(B,p,h,rectangle(1,1,2,2)),
      OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),
      "Hole inside profile");
   invalid(()->OrthogonalPolygonBRepBoolean.apply(
      a,prism(B,p,h,rectangle(-1,1,5,2)),
      OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),
      "Disconnected upper part after full horizontal cut");
   invalid(()->OrthogonalPolygonBRepBoolean.apply(
      a,prism(B,p,h,rectangle(5,6,8,9)),
      OrthogonalPolygonBRepBoolean.Operation.INTERSECTION),
      "No overlap");
   invalid(()->OrthogonalPolygonBRepBoolean.apply(
      a,prism(B,p,h,rectangle(5,6,8,9)),
      OrthogonalPolygonBRepBoolean.Operation.UNION),
      "Disconnected union shells");
   invalid(()->OrthogonalPolygonBRepBoolean.apply(
      a,prism(B,p,h,rectangle(4,0,5,1)),
      OrthogonalPolygonBRepBoolean.Operation.UNION),
      "Only touching at an edge is disconnected under supported contract");
   invalid(()->OrthogonalPolygonBRepBoolean.apply(
      a,prism(B,p,-h,rectangle(2,0,5,3)),
      OrthogonalPolygonBRepBoolean.Operation.UNION),
      "Opposite height");
   invalid(()->OrthogonalPolygonBRepBoolean.apply(
      a,prism(B,p,h+1,rectangle(2,0,5,3)),
      OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE),
      "Different heights");
   PolygonPrismBRep diagonal=prism(B,p,h,
      new double[][]{{1,1},{4,1},{3,3},{1,3}});
   invalid(()->OrthogonalPolygonBRepBoolean.apply(a,diagonal,
      OrthogonalPolygonBRepBoolean.Operation.UNION),
      "Nonorthogonal edges");
   ok(a.faces().size()==8&&b.faces().size()==6,
      "Operand topology never modified");
  }
  invalid(()->OrthogonalPolygonBRepBoolean.apply(
    prism(A,0,3,L),prism(B,1,3,rectangle(2,0,5,3)),
    OrthogonalPolygonBRepBoolean.Operation.UNION),"Different work planes");
  invalid(()->OrthogonalPolygonBRepBoolean.apply(
    null,prism(B,0,3,rectangle(2,0,5,3)),
    OrthogonalPolygonBRepBoolean.Operation.UNION),"Null source");
  System.out.println("OrthogonalPolygonBRepBooleanTest: OK");
 }
}
