import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NamedConvexIntersectionTest {
 private static String id(String s){return UUID.nameUUIDFromBytes(s.getBytes(StandardCharsets.UTF_8)).toString();}
 private static PolygonPrismBRep prism(String name,int plane,double height,double[][] p){
  List<String> ids=new ArrayList<>();Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<p.length;i++){
   String edge=id(name+i);ids.add(edge);double[] a=p[i],b=p[(i+1)%p.length];
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id(name),ids.get(0),plane,height,ids).asBRep(lines);
 }
 private static NamedConvexIntersection result(String owner,int plane,int sign,double width,double peak,double height,double dx,double dy){
  return NamedConvexIntersection.apply(id(owner),
   prism("square",plane,6*sign,new double[][]{{dx,dy},{dx+width,dy},{dx+width,dy+6},{dx,dy+6}}),
   prism("triangle",plane,height*sign,new double[][]{{dx-1,dy+1},{dx+7,dy+1},{dx+3,dy+peak}}));
 }
 private static void check(boolean condition){if(!condition)throw new AssertionError();}
 private static void reject(Runnable operation){
  try{operation.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid intersection accepted");
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   NamedConvexIntersection before=result("owner",plane,sign,6,7,4,0,0),after=result("owner",plane,sign,6.5,7.5,5,0,0);
   check(before.shell.vertices().size()==12);check(!before.shell.id().equals(after.shell.id()));
   for(PolygonPrismBRep.Vertex v:before.shell.vertices())check(after.findVertex(before.vertexId(v.id))!=null);
   for(PolygonPrismBRep.Edge e:before.shell.edges())check(after.findEdge(before.edgeId(e.id))!=null);
   for(PolygonPrismBRep.Face f:before.shell.faces())check(after.findFace(before.faceId(f.id))!=null);
   NamedConvexIntersection translated=result("owner",plane,sign,6,7,4,132.5,-754.25);
   for(PolygonPrismBRep.Vertex v:before.shell.vertices())check(translated.findVertex(before.vertexId(v.id))!=null);
   NamedConvexIntersection changed=result("owner",plane,sign,6,3,4,0,0);
   TopologyReconciliation delta=TopologyReconciliation.between(TopologySnapshot.from(before),TopologySnapshot.from(changed));
   check(!delta.change(TopologySnapshot.Kind.VERTEX).created.isEmpty());
   check(!delta.change(TopologySnapshot.Kind.FACE).removed.isEmpty());
   for(PolygonPrismBRep.Face f:before.shell.faces())if(f.sourceLineId==null)check(changed.findFace(before.faceId(f.id))!=null);
   PlanarBooleanView view=PlanarBooleanView.from(before);int capCount=0;
   for(PolygonPrismBRep.Face f:before.shell.faces())if(f.id.endsWith("/top")){
    int[] triangles=view.trianglesForFace(before.faceId(f.id),37);capCount=triangles.length;check(capCount==4);
    for(int triangle:triangles)check(view.faceAtTriangle(triangle-37).id.equals(before.faceId(f.id)));
   }
   check(capCount>2&&view.faceAtTriangle(-1)==null);
   float[] copy=view.triangles();copy[0]+=99;check(!Arrays.equals(copy,view.triangles()));
   NamedConvexIntersection other=result("other",plane,sign,6,7,4,0,0);
   check(other.findFace(before.faceId(before.shell.faces().get(0).id))==null);
   NamedConvexIntersection switchedHeight=result("owner",plane,sign,6,7,7,0,0);
   for(PolygonPrismBRep.Face f:before.shell.faces())if(f.id.endsWith("/top"))check(switchedHeight.findFace(before.faceId(f.id))==null);
   reject(()->after.vertexId(before.shell.vertices().get(0).id));
   // A convex profile may have several collinear source segments per support line.
   PolygonPrismBRep collinear=prism("chain",plane,6*sign,new double[][]{{0,0},{3,0},{6,0},{6,6},{0,6}});
   NamedConvexIntersection chained=NamedConvexIntersection.apply(id("chain-result"),collinear,
    prism("cover",plane,4*sign,new double[][]{{-1,-1},{7,-1},{7,7},{-1,7}}));
   check(chained.shell.volume()>0);
  }
  reject(()->NamedConvexIntersection.apply("bad",null,null));
  // Preserve a valid legacy shape at high coordinates: do not confuse its
  // exact source corners with nearby supporting planes within a float ULP.
  PolygonPrismBRep acute=prism("acute",0,4,new double[][]{{9000,9000},{9100,9000},{9200,9000.0009765625}});
  PolygonPrismBRep cover=prism("acute-cover",0,6,new double[][]{{8999,8999},{9201,8999},{9201,9001},{8999,9001}});
  NamedConvexIntersection thin=NamedConvexIntersection.apply(id("acute-result"),acute,cover);
  check(Arrays.equals(thin.shell.triangles(),ConvexPrismBRepIntersection.intersect(acute,cover).triangles()));
  check(thin.shell.vertices().size()==6);
  System.out.println("NamedConvexIntersectionTest: OK");
 }
}
