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
 public static void main(String[] args)throws Exception{
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
