import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class TopologyReferenceTest {
 private static String id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)).toString();}
 private static void check(boolean condition){if(!condition)throw new AssertionError();}
 private static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid reference accepted");
 }
 private static PolygonPrismBRep polygon(String name,int plane,double height,double[][] p){
  List<String> ids=new ArrayList<>();Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<p.length;i++){
   String edge=id(name+i);ids.add(edge);double[] a=p[i],b=p[(i+1)%p.length];
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id(name),ids.get(0),plane,height,ids).asBRep(lines);
 }
 private static PolygonPrismBRep box(String name,int plane,double height,double right){
  return polygon(name,plane,height,new double[][]{{0,0},{right,0},{right,8},{0,8}});
 }
 private static NamedGridHoleBody ring(int plane,int sign,double right){
  return NamedGridHoleBody.apply(id("ring"),box("outer",plane,6*sign,8),
   polygon("cut",plane,6*sign,new double[][]{{2,2},{right,2},{right,6},{2,6}}),
   OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE);
 }
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(0,0,0,0,0,0);
  PrimitiveBRepBody cylinder=PrimitiveBRepBody.cylinder(id("cylinder"),10,20,pose);
  TopologySnapshot oldCylinder=TopologySnapshot.from(cylinder);
  TopologySnapshot nextCylinder=TopologySnapshot.from(PrimitiveBRepBody.cylinder(id("cylinder"),12,25,
   new RigidTransform(3,4,5,20,30,40)));
  check(oldCylinder.ids(TopologySnapshot.Kind.VERTEX).isEmpty());
  check(oldCylinder.ids(TopologySnapshot.Kind.EDGE).size()==2&&oldCylinder.ids(TopologySnapshot.Kind.FACE).size()==3);
  for(TopologySnapshot.Kind kind:TopologySnapshot.Kind.values())for(String entity:oldCylinder.ids(kind)){
   TopologySnapshot.Reference ref=oldCylinder.reference(kind,entity);
   check(nextCylinder.resolvePersistent(ref)!=null&&nextCylinder.resolveExact(ref)==null);
   check(ref.equals(TopologySnapshot.restore(ref.bodyId,ref.kind,ref.entityId,ref.geometryVersion)));
  }
  PrimitiveBRepBody tinyEdit=PrimitiveBRepBody.cylinder(id("cylinder"),10+1e-12,20,pose);
  check(Arrays.equals(cylinder.triangles(48),tinyEdit.triangles(48)));
  check(!oldCylinder.geometryVersion.equals(TopologySnapshot.from(tinyEdit).geometryVersion));
  TopologySnapshot box=TopologySnapshot.from(PrimitiveBRepBody.box(id("cylinder"),10,10,20,pose));
  check(box.ids(TopologySnapshot.Kind.VERTEX).size()==8&&box.ids(TopologySnapshot.Kind.EDGE).size()==12);
  TopologyReconciliation changedKind=TopologyReconciliation.between(box,oldCylinder);
  check(changedKind.change(TopologySnapshot.Kind.VERTEX).removed.size()==8);
  String rim=oldCylinder.ids(TopologySnapshot.Kind.EDGE).iterator().next();
  check(oldCylinder.resolvePersistent(TopologySnapshot.restore(oldCylinder.bodyId,TopologySnapshot.Kind.FACE,rim,oldCylinder.geometryVersion))==null);
  try{oldCylinder.ids(TopologySnapshot.Kind.FACE).clear();throw new AssertionError();}catch(UnsupportedOperationException expected){}
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   TopologySnapshot a=TopologySnapshot.from(ring(plane,sign,6)),b=TopologySnapshot.from(ring(plane,sign,10));
   TopologyReconciliation delta=TopologyReconciliation.between(a,b);
   TopologyReconciliation.Change faces=delta.change(TopologySnapshot.Kind.FACE);
   check(!faces.retained.isEmpty()&&!faces.removed.isEmpty()&&!faces.created.isEmpty());
   String keep=faces.retained.iterator().next(),drop=faces.removed.iterator().next();
   TopologySnapshot.Reference retained=a.reference(TopologySnapshot.Kind.FACE,keep);
   check(delta.rebind(retained)!=null&&b.resolveExact(retained)==null);
   check(delta.rebind(a.reference(TopologySnapshot.Kind.FACE,drop))==null);
   check(delta.rebind(b.reference(TopologySnapshot.Kind.FACE,faces.created.iterator().next()))==null);
   check(delta.rebind(TopologySnapshot.restore(id("other"),retained.kind,retained.entityId,retained.geometryVersion))==null);
   check(delta.rebind(TopologySnapshot.restore(retained.bodyId,retained.kind,retained.entityId,"unknown-version"))==null);
   for(TopologySnapshot.Kind kind:TopologySnapshot.Kind.values()){
    TopologyReconciliation.Change c=delta.change(kind);
    check(c.retained.size()+c.removed.size()==a.ids(kind).size());
    check(c.retained.size()+c.created.size()==b.ids(kind).size());
   }
   TopologySnapshot polygonA=TopologySnapshot.from(polygon("triangle",plane,4*sign,new double[][]{{0,0},{4,0},{1,3}}));
   TopologySnapshot polygonB=TopologySnapshot.from(polygon("triangle",plane,7*sign,new double[][]{{0,0},{5,0},{1,4}}));
   check(polygonA.ids(TopologySnapshot.Kind.VERTEX).size()==6&&polygonA.ids(TopologySnapshot.Kind.FACE).size()==5);
   for(String face:polygonA.ids(TopologySnapshot.Kind.FACE))check(polygonB.resolvePersistent(polygonA.reference(TopologySnapshot.Kind.FACE,face))!=null);
   NamedSteppedBody step=NamedSteppedBody.apply(id("step"),box("tall",plane,6*sign,4),box("short",plane,3*sign,6),VariableHeightOrthogonalMesh.Operation.UNION);
   TopologySnapshot stepped=TopologySnapshot.from(step);
   check(stepped.ids(TopologySnapshot.Kind.FACE).size()==step.shell.faces().size());
   check(stepped.geometryVersion.equals(TopologySnapshot.from(step).geometryVersion));
  }
  reject(()->TopologyReconciliation.between(box,TopologySnapshot.from(PrimitiveBRepBody.box(id("other"),2,2,2,pose))));
  reject(()->TopologySnapshot.restore("",TopologySnapshot.Kind.FACE,"face","version"));
  reject(()->oldCylinder.reference(TopologySnapshot.Kind.FACE,"unknown"));
  System.out.println("TopologyReferenceTest: OK");
 }
}
