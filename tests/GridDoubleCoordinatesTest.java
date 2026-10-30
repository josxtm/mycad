import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** The grid preview keeps its legacy float order while rendering the analytic coordinates. */
public final class GridDoubleCoordinatesTest {
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private static String id(String value){return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();}
 private static PolygonPrismBRep rectangle(String name,int plane,double height,double x,double y,double w,double d){
  double[][] p={{x,y},{x+w,y},{x+w,y+d},{x,y+d}};
  List<String> ids=new ArrayList<>();Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<4;i++){
   String key=id(name+"/"+i);ids.add(key);double[] a=p[i],b=p[(i+1)%4];
   lines.put(key,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id(name),ids.get(0),plane,height,ids).asBRep(lines);
 }
 private static void near(double actual,double expected,double tolerance,String message){
  check(Double.isFinite(actual)&&Math.abs(actual-expected)<=tolerance,
      message+": "+actual+" != "+expected);
 }
 private static void topology(PolygonPrismBRep body){
  Map<String,PolygonPrismBRep.Vertex> vertices=new HashMap<>();
  Map<String,PolygonPrismBRep.Edge> edges=new HashMap<>();
  Map<String,PolygonPrismBRep.HalfEdge> uses=new HashMap<>();
  Set<String> faces=new HashSet<>(),visited=new HashSet<>();
  for(PolygonPrismBRep.Vertex v:body.vertices())check(vertices.put(v.id,v)==null,"Duplicate vertex");
  for(PolygonPrismBRep.Edge e:body.edges())check(edges.put(e.id,e)==null,"Duplicate edge");
  for(PolygonPrismBRep.HalfEdge u:body.halfEdges())check(uses.put(u.id,u)==null,"Duplicate half-edge");
  check(vertices.size()-edges.size()+body.faces().size()==2,"Euler characteristic");
  check(uses.size()==2*edges.size(),"Two uses per edge");
  for(PolygonPrismBRep.Face face:body.faces()){
   check(faces.add(face.id)&&face.area>0,"Unique nondegenerate face");
   List<String> loop=face.halfEdgeIds(),points=face.vertexIds();
   check(loop.size()==points.size()&&loop.size()>=3,"Face loop size");
   for(int i=0;i<loop.size();i++){
    PolygonPrismBRep.HalfEdge u=uses.get(loop.get(i));
    check(u!=null&&visited.add(u.id)&&u.faceId.equals(face.id),"Face owns each use exactly once");
    check(u.fromId.equals(points.get(i))&&u.toId.equals(points.get((i+1)%points.size())),"Face vertices follow half-edges");
    check(vertices.containsKey(u.fromId)&&vertices.containsKey(u.toId),"No dangling vertices");
    check(u.nextId.equals(loop.get((i+1)%loop.size()))&&u.previousId.equals(loop.get((i+loop.size()-1)%loop.size())),"Closed ordered face loop");
    PolygonPrismBRep.HalfEdge twin=uses.get(u.twinId);
    check(twin!=null&&twin.twinId.equals(u.id)&&twin.edgeId.equals(u.edgeId)&&
      twin.fromId.equals(u.toId)&&twin.toId.equals(u.fromId),"Reciprocal opposite twin");
    PolygonPrismBRep.Edge edge=edges.get(u.edgeId);
    check(edge!=null&&edge.halfEdgeIds().contains(u.id)&&edge.adjacentFaceIds().contains(face.id),"Edge incidence");
   }
  }
  check(visited.size()==uses.size(),"No orphan half-edges");
  for(PolygonPrismBRep.Edge edge:edges.values()){
   check(edge.halfEdgeIds().size()==2&&new HashSet<>(edge.adjacentFaceIds()).size()==2,"Manifold edge");
   PolygonPrismBRep.Vertex a=vertices.get(edge.firstVertexId),b=vertices.get(edge.secondVertexId);
   check(a!=null&&b!=null,"Edge endpoints exist");
   near(edge.length,Math.sqrt(Math.pow(a.x-b.x,2)+Math.pow(a.y-b.y,2)+Math.pow(a.z-b.z,2)),1e-12,"Analytic edge length");
  }
  double[] world=body.triangleCoordinates();
  for(int triangle=0;triangle<world.length/9;triangle++){
   double[] normal=PolygonPrismFacePicker.resolve(body,triangle).outwardNormal();
   int i=triangle*9;
   double ax=world[i+3]-world[i],ay=world[i+4]-world[i+1],az=world[i+5]-world[i+2];
   double bx=world[i+6]-world[i],by=world[i+7]-world[i+1],bz=world[i+8]-world[i+2];
   check((ay*bz-az*by)*normal[0]+(az*bx-ax*bz)*normal[1]+(ax*by-ay*bx)*normal[2]>0,"Outward triangle winding");
  }
  TriangleMeshHealth.Report health=TriangleMeshHealth.inspect(
      RelativeSceneCoordinates.from(Collections.singletonList(world)).triangles(0));
  check(health.isWatertight()&&health.shells==1&&health.negativeVolumeShells==0,"One oriented watertight shell");
  // The render buffer is float; analytic area/volume is checked separately at 1e-12.
  near(health.signedVolume,body.volume(),2e-7*Math.max(1,body.volume()),"Relative GPU preview volume");
 }
 private static void sameTopology(PolygonPrismBRep a,PolygonPrismBRep b){
  TopologySnapshot x=TopologySnapshot.from(a),y=TopologySnapshot.from(b);
  check(a.id().equals(b.id())&&x.geometryVersion.equals(y.geometryVersion),"Deterministic analytic identity");
  check(Arrays.equals(a.triangleCoordinates(),b.triangleCoordinates()),"Deterministic double tessellation");
  for(TopologySnapshot.Kind kind:TopologySnapshot.Kind.values()){
   check(x.ids(kind).equals(y.ids(kind)),"Deterministic topology IDs");
   for(String entity:x.ids(kind))check(y.resolveExact(x.reference(kind,entity))!=null,"Exact references survive regeneration");
  }
 }
 private static void orthogonalPrecision(){
  for(double origin:new double[]{0,9000,-9000})for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   double h=sign*2,split=origin+0.4,end=origin+0.8;
   PolygonPrismBRep a=rectangle("target",plane,h,origin,origin,0.8,0.8);
   PolygonPrismBRep b=rectangle("cutter",plane,h,split,split,0.8,0.8);
   double[] beforeA=a.triangleCoordinates(),beforeB=b.triangleCoordinates();
   double overlap=(end-split)*(end-split);
   PolygonPrismBRep intersection=ConvexPrismBRepIntersection.intersect(a,b).solid;
   PolygonPrismBRep union=OrthogonalPrismBRepUnion.union(a,b).solid;
   PolygonPrismBRep difference=OrthogonalPrismBRepUnion.difference(a,b).solid;
   near(intersection.sectionArea(),overlap,1e-12,"Translated intersection area");
   near(union.sectionArea(),a.sectionArea()+b.sectionArea()-overlap,1e-12,"Translated union area");
   near(difference.sectionArea(),a.sectionArea()-overlap,1e-12,"Translated difference area");
   near(union.volume()+intersection.volume(),a.volume()+b.volume(),2e-12,"Union inclusion-exclusion");
   near(difference.volume()+intersection.volume(),a.volume(),2e-12,"Difference volume conservation");
   for(PolygonPrismBRep result:Arrays.asList(union,intersection,difference))topology(result);
   sameTopology(union,OrthogonalPrismBRepUnion.union(b,a).solid);
   sameTopology(intersection,ConvexPrismBRepIntersection.intersect(b,a).solid);
   sameTopology(difference,OrthogonalPrismBRepUnion.difference(a,b).solid);
   check(Arrays.equals(beforeA,a.triangleCoordinates())&&Arrays.equals(beforeB,b.triangleCoordinates()),"Source shells remain immutable");
  }
 }
 private static void orthogonalSubFloatIdentity(){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep a=rectangle("target",plane,sign*2,0,0,4,4);
   PolygonPrismBRep b=rectangle("cutter",plane,sign*2,2,2,4,4);
   PolygonPrismBRep edited=rectangle("cutter",plane,sign*2,2+1e-7,2,4,4);
   for(boolean subtract:new boolean[]{false,true}){
    PolygonPrismBRep old=subtract?OrthogonalPrismBRepUnion.difference(a,b).solid:OrthogonalPrismBRepUnion.union(a,b).solid;
    PolygonPrismBRep changed=subtract?OrthogonalPrismBRepUnion.difference(a,edited).solid:OrthogonalPrismBRepUnion.union(a,edited).solid;
    check(!old.id().equals(changed.id()),"Sub-float edit must change orthogonal Boolean UUID: subtract="+subtract);
    near(changed.sectionArea()-old.sectionArea(),2e-7,1e-12,"Sub-float geometry edit retained");
    topology(changed);
    TopologySnapshot previous=TopologySnapshot.from(old),next=TopologySnapshot.from(changed);
    for(TopologySnapshot.Kind kind:TopologySnapshot.Kind.values())for(String entity:previous.ids(kind))
     check(next.resolveExact(previous.reference(kind,entity))==null,"Stale references rejected after edit");
   }
  }
 }
 private static void orthogonalNearEqualHeights(){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep a=rectangle("height-target",plane,sign*2,9000,9000,.8,.8);
   PolygonPrismBRep b=rectangle("height-cutter",plane,sign*(2+1e-10),9000.4,9000.4,.8,.8);
   PolygonPrismBRep forward=OrthogonalPrismBRepUnion.union(a,b).solid;
   PolygonPrismBRep reverse=OrthogonalPrismBRepUnion.union(b,a).solid;
   sameTopology(forward,reverse);
   near(forward.distance(),sign*2,0,"Canonical common signed height");
   near(forward.volume(),forward.sectionArea()*2,1e-12,"Canonical volume");
   topology(forward);
  }
 }
 public static void main(String[] args)throws Exception{
  orthogonalNearEqualHeights();
  orthogonalPrecision();
  orthogonalSubFloatIdentity();
  MessageDigest digest=MessageDigest.getInstance("SHA-256");
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1})for(int shape=0;shape<4;shape++){
   double x=9000.123456789,y=8000.987654321,h=sign*4.123456789;
   PolygonPrismBRep a=rectangle("a",plane,h,x,y,8,8);
   PolygonPrismBRep b=shape==3?rectangle("b",plane,h,x+3,y-1,2,10):
     rectangle("b",plane,h,x+2,y+2,4,4);
   OrthogonalPolygonBRepBoolean.Operation op=shape==1?OrthogonalPolygonBRepBoolean.Operation.UNION:
     shape==2?OrthogonalPolygonBRepBoolean.Operation.INTERSECTION:OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE;
   NamedGridHoleBody named=NamedGridHoleBody.apply(id("owner"),a,b,op);
   float[] legacy=named.shell.triangles();
   double[] world=named.shell.triangleCoordinates();
   PlanarBooleanView view=PlanarBooleanView.from(named);
   check(Arrays.equals(world,view.worldCoordinates()),"Precise grid source lost at viewer bridge");
   check(world.length==legacy.length,"Triangle count changed");
   boolean subFloat=false;for(int i=0;i<world.length;i++){
    check((float)world[i]==legacy[i],"Triangle order or orientation changed");
    subFloat|=world[i]!=(double)legacy[i];
   }
   check(subFloat,"Fixture lacks detail below world-float precision");
   // Match each rendered corner back to its analytic face, without coordinate tolerance.
   Map<String,OrthogonalGridHoleBRep.Vertex> vertices=new HashMap<>();
   for(OrthogonalGridHoleBRep.Vertex v:named.shell.vertices())vertices.put(v.id,v);
   int at=0;for(OrthogonalGridHoleBRep.Face f:named.shell.faces()){
    for(int corner:new int[]{0,1,2,0,2,3}){
     OrthogonalGridHoleBRep.Vertex v=vertices.get(f.vertexIds().get(corner));
     check(world[at++]==v.x&&world[at++]==v.y&&world[at++]==v.z,"Analytic face tessellation mismatch");
    }
   }
   RelativeSceneCoordinates frame=RelativeSceneCoordinates.from(Collections.singletonList(world));
   TriangleMeshHealth.Report local=TriangleMeshHealth.inspect(frame.triangles(0));
   check(local.isWatertight()&&local.shells==named.shell.solidCount,"Relative grid is not a closed shell");
   check(Math.abs(local.signedVolume-named.shell.volume)<1e-5*Math.max(1,named.shell.volume),"Relative volume mismatch");
   for(int t=0;t<legacy.length/9;t++){
    check(view.faceAtTriangle(t).id.equals(named.faceId(named.faceAtTriangle(t).id)),"Triangle ownership changed");
    check(Arrays.binarySearch(view.trianglesForFace(view.faceAtTriangle(t).id,7),t+7)>=0,"Selection indices changed");
   }
   world[0]++;check(!Arrays.equals(world,named.shell.triangleCoordinates()),"Mutable source leaked");
   double keep=view.worldCoordinates()[0];double[] copy=view.worldCoordinates();copy[0]++;
   check(view.worldCoordinates()[0]==keep,"Mutable viewer source leaked");
   for(float value:legacy){int bits=Float.floatToIntBits(value);for(int k=0;k<4;k++)digest.update((byte)(bits>>>(8*k)));}
   check(named.shell.holeCount==(shape==0?1:0),"Fixture holes");
   check(named.shell.solidCount==(shape==3?2:1),"Fixture solids");
  }
  StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b));
  check(hash.toString().equals("c73027254614a75d9e2b60786c0a84aa1dcdb123fdd2a3f17ccc0e9385210b50"),"Published grid meshes changed");
  System.out.println("GridDoubleCoordinatesTest: OK");
 }
}
