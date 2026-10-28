import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NamedGridHoleBodyTest {
 private static String id(String seed){return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();}
 private static PolygonPrismBRep box(String seed,int plane,double height,double x1,double y1,double x2,double y2){
  double[][] p={{x1,y1},{x2,y1},{x2,y2},{x1,y2}};
  List<String> ids=new ArrayList<>();Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<4;i++){
   String line=id(seed+i);ids.add(line);double[] a=p[i],b=p[(i+1)%4];
   lines.put(line,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id(seed),ids.get(0),plane,height,ids).asBRep(lines);
 }
 private static void check(boolean condition){if(!condition)throw new AssertionError();}
 private static void reject(Runnable operation){
  try{operation.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid topology accepted");
 }
 private static void sameNames(NamedGridHoleBody a,NamedGridHoleBody b){
  for(var v:a.shell.vertices())check(b.findVertex(a.vertexId(v.id))!=null);
  for(var e:a.shell.edges())check(b.findEdge(a.edgeId(e.id))!=null);
  for(var f:a.shell.faces())check(b.findFace(a.faceId(f.id))!=null);
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep a=box("a",plane,6*sign,0,0,8,8),b=box("b",plane,6*sign,2,2,6,6);
   NamedGridHoleBody ring=NamedGridHoleBody.apply(id("owner"),a,b,OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE);
   check(ring.shell.holeCount==1&&ring.shell.solidCount==1&&Math.abs(ring.shell.volume-288)<1e-6);
   check(ring.shell.vertices().size()-ring.shell.edges().size()+ring.shell.faces().size()==0);
   NamedGridHoleBody edited=NamedGridHoleBody.apply(id("owner"),box("a",plane,7*sign,0,0,9,8),
    box("b",plane,7*sign,2,2,6,6),OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE);
   sameNames(ring,edited);check(!ring.shell.id.equals(edited.shell.id));
   for(int triangle=0;triangle<ring.shell.triangles().length/9;triangle++)
    check(ring.faceAtTriangle(triangle)==ring.shell.faces().get(triangle/2));
   check(ring.faceAtTriangle(-1)==null&&ring.faceAtTriangle(Integer.MAX_VALUE)==null);
   NamedGridHoleBody other=NamedGridHoleBody.apply(id("other"),a,b,OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE);
   check(other.findFace(ring.faceId(ring.shell.faces().get(0).id))==null);
   NamedGridHoleBody opened=NamedGridHoleBody.apply(id("owner"),a,box("b",plane,6*sign,2,2,10,6),
    OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE);
   check(opened.shell.holeCount==0&&opened.shell.solidCount==1);
   int retained=0,removed=0;
   for(var face:ring.shell.faces())if(opened.findFace(ring.faceId(face.id))==null)removed++;else retained++;
   check(retained>0&&removed>0);
   reject(()->edited.faceId(ring.shell.faces().get(0).id));
   NamedGridHoleBody split=NamedGridHoleBody.apply(id("split"),box("left",plane,6*sign,0,0,2,2),
    box("right",plane,6*sign,4,0,6,2),OrthogonalPolygonBRepBoolean.Operation.UNION);
   NamedGridHoleBody splitEdit=NamedGridHoleBody.apply(id("split"),box("left",plane,7*sign,0,0,2.5,2),
    box("right",plane,7*sign,4,0,6,2),OrthogonalPolygonBRepBoolean.Operation.UNION);
   check(split.shell.solidCount==2);sameNames(split,splitEdit);
   reject(()->NamedGridHoleBody.apply(id("owner"),a,b,null));
  }
  System.out.println("NamedGridHoleBodyTest: OK");
 }
}
