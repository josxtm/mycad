import es.josxtm.mycad.cad.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public final class ProfileDoubleCoordinatesTest {
 private static String id(String s){return UUID.nameUUIDFromBytes(s.getBytes(StandardCharsets.UTF_8)).toString();}
 private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 private static double[][] profile(int kind){return kind==0?new double[][]{{0,0},{5,0},{4,3},{1,4}}:new double[][]{{0,0},{5,0},{5,1},{2,1},{2,4},{0,4}};}
 private static class Source {
  final List<ClosedSketchExtrusion.Segment> segments=new ArrayList<>();
  final Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  final LoopExtrusionFeature feature;
  Source(String name,int plane,double h,double[][] p){
   List<String> ids=new ArrayList<>();for(int i=0;i<p.length;i++){
    String key=id(name+i);ids.add(key);double[] a=p[i],b=p[(i+1)%p.length];
    ClosedSketchExtrusion.Segment line=new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]);segments.add(line);lines.put(key,line);
   }
   feature=new LoopExtrusionFeature(id(name),ids.get(0),plane,h,ids);
  }
  PolygonPrismBRep body(){return feature.asBRep(lines);}
 }
 private static void matches(double[] world,float[] legacy){
  check(world.length==legacy.length,"Triangle count changed");for(int i=0;i<world.length;i++)check((float)world[i]==legacy[i],"Winding or tessellation changed");
 }
 public static void main(String[] args)throws Exception{
  MessageDigest hash=MessageDigest.getInstance("SHA-256");
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1})for(int kind=0;kind<2;kind++){
   double height=sign*4.123456789;Source source=new Source("profile",plane,height,profile(kind));
   ClosedSketchExtrusion.Result result=ClosedSketchExtrusion.extrude(source.segments,0,height);
   double[] exact=result.triangleCoordinates();matches(exact,result.triangles);matches(source.body().triangleCoordinates(),result.triangles);
   check(Arrays.equals(exact,source.body().triangleCoordinates()),"B-Rep source bridge");
   int normal=plane==0?2:plane==1?1:0;boolean found=false;
   for(int i=normal;i<exact.length;i+=3)if(exact[i]==height)found=true;
   check(found&&height!=(double)(float)height,"Height precision fixture");
   check(TriangleMeshHealth.inspect(RelativeSceneCoordinates.from(Collections.singletonList(exact)).triangles(0)).isWatertight(),"Closed local extrusion");
   for(float f:result.triangles){int bits=Float.floatToIntBits(f);for(int b=0;b<4;b++)hash.update((byte)(bits>>>(8*b)));}
   exact[0]++;check(!Arrays.equals(exact,result.triangleCoordinates()),"Leaked analytic source");
   double keep=result.triangleCoordinates()[0];result.triangles[0]++;check(result.triangleCoordinates()[0]==keep,"World float buffer mutated exact source");
   Source a=new Source("a",plane,height,new double[][]{{0,0},{8,0},{8,8},{0,8}});
   Source b=new Source("b",plane,height,new double[][]{{4,2},{10,2},{10,6},{4,6}});
   for(LinkedConvexIntersection.Operation op:LinkedConvexIntersection.Operation.values())if(NamedPlanarBoolean.supports(op)){
    NamedPlanarBoolean body=NamedPlanarBoolean.apply(id("owner"),a.body(),b.body(),op);
    PlanarBooleanView view=PlanarBooleanView.from(body);matches(view.worldCoordinates(),body.triangles());
    int at=0;double[] world=view.worldCoordinates();
    for(PolygonPrismBRep shell:body.solids())for(double p:shell.triangleCoordinates())check(p==world[at++],"Multi-shell source order");
   }
   Source triangle=new Source("triangle",plane,sign*3.123456789,new double[][]{{-1,1},{7,1},{3,7}});
   NamedConvexIntersection convex=NamedConvexIntersection.apply(id("convex"),a.body(),triangle.body());
   check(Arrays.equals(convex.shell.triangleCoordinates(),PlanarBooleanView.from(convex).worldCoordinates()),"Inclined intersection source");
  }
  StringBuilder digest=new StringBuilder();for(byte b:hash.digest())digest.append(String.format(Locale.ROOT,"%02x",b));
  check(digest.toString().equals("937c317949dcba19df7127dcbd54c0c387411c527aa8ff4f0c8785bca46010d0"),"Published profile meshes changed");
  Source far=new Source("far",0,4.123456789,new double[][]{{9000.123456789,8000.123456789},{9005.123456789,8000.123456789},{9004.123456789,8003.123456789},{9001.123456789,8004.123456789}});
  double[] world=far.body().triangleCoordinates();boolean retained=false;for(int i=0;i<world.length;i+=3)if(world[i]==9000.123456789)retained=true;
  check(retained,"World profile coordinates rounded early");
  System.out.println("ProfileDoubleCoordinatesTest: OK");
 }
}
