import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class OrthogonalPrismBRepDifferenceTest {
 static final String A="abdde117-2222-4222-8222-000000000001",
                     B="abdde117-2222-4222-8222-000000000002",
                     RESULT="abdde117-2222-4222-8222-000000000099";
 static void ok(boolean value,String label){
  if(!value)throw new AssertionError(label);
 }
 static void close(double actual,double expected,String label){
  if(Math.abs(actual-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(label+": "+actual+" vs "+expected);
 }
 static void invalid(Runnable op,String label){
  try{op.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid subtraction: "+label);
 }
 static LoopExtrusionFeature source(String id,int p,double h,double[][] corners,
    Map<String,ClosedSketchExtrusion.Segment> lines){
  List<String> ids=new ArrayList<>();
  for(int i=0;i<corners.length;i++){
   String key=UUID.nameUUIDFromBytes((id+"/line/"+i)
     .getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=corners[i],b=corners[(i+1)%corners.length];
   ids.add(key);
   lines.put(key,new ClosedSketchExtrusion.Segment(p,
      a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),p,h,ids);
 }
 static double[][] rect(double x1,double y1,double x2,double y2){
  return new double[][]{{x1,y1},{x2,y1},{x2,y2},{x1,y2}};
 }
 static PolygonPrismBRep prism(String id,int p,double h,double[][] corners){
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  LoopExtrusionFeature f=source(id,p,h,corners,lines);
  return f.asBRep(lines);
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(double sign:new double[]{1,-1}){
   final int p=plane;
   double h=sign*3;
   PolygonPrismBRep a=prism(A,p,h,rect(0,0,4,4));
   PolygonPrismBRep b=prism(B,p,h,rect(2,2,5,5));
   OrthogonalPrismBRepUnion.Result cut=OrthogonalPrismBRepUnion.difference(a,b);
   close(cut.area,12,"L cut area");
   close(cut.volume,36,"L cut signed height volume");
   ok(cut.solid.vertices().size()==12&&cut.solid.faces().size()==8,
      "Concave difference is a new half-edge B-Rep with 6 side faces");
   TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(cut.triangles());
   ok(report.isWatertight()&&report.shells==1,
      "Difference has exactly one closed manifold");
   close(report.signedVolume,36,"Positive preview volume");
   ok(a.faces().size()==6&&b.faces().size()==6,"No mutation to operands");
   OrthogonalPrismBRepUnion.Result rev=OrthogonalPrismBRepUnion.difference(b,a);
   close(rev.area,5,"Subtraction order matters");
   close(rev.volume,15,"Reverse B minus A volume");
   OrthogonalPrismBRepUnion.Result notch=OrthogonalPrismBRepUnion.difference(
    prism(A,p,h,rect(0,0,6,4)),prism(B,p,h,rect(2,2,4,5)));
   close(notch.area,20,"Edge notch difference");
   ok(notch.solid.vertices().size()==16,"Notched cut has eight vertices");
   ok(TriangleMeshHealth.inspect(notch.triangles()).isWatertight(),
      "Notch watertight");
   OrthogonalPrismBRepUnion.Result side=OrthogonalPrismBRepUnion.difference(
    a,prism(B,p,h,rect(2,-1,5,5)));
   close(side.area,8,"Remove entire right stripe gives a rectangle");
   ok(side.solid.faces().size()==6,"Simple rectangle difference");
   invalid(()->OrthogonalPrismBRepUnion.difference(
    a,prism(B,p,h,rect(1,1,2,2))),"Interior hole unsupported");
   invalid(()->OrthogonalPrismBRepUnion.difference(
    a,prism(B,p,h,rect(1,-1,3,5))),"Disconnected results unsupported");
   invalid(()->OrthogonalPrismBRepUnion.difference(
    a,prism(B,p,h,rect(-1,-1,5,5))),"Cut destroys the entire body");
   invalid(()->OrthogonalPrismBRepUnion.difference(
    a,prism(B,p,h,rect(4,0,6,4))),"Tangential contact without volume");
   invalid(()->OrthogonalPrismBRepUnion.difference(
    a,prism(B,p,-h,rect(2,2,5,5))),"Different signed height");
   invalid(()->OrthogonalPrismBRepUnion.difference(
    a,prism(B,p,h*2,rect(2,2,5,5))),"Different absolute height");
   Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
   LoopExtrusionFeature first=source(A,p,h,rect(0,0,4,4),lines);
   LoopExtrusionFeature second=source(B,p,h,rect(2,2,5,5),lines);
   Map<String,LoopExtrusionFeature> features=new LinkedHashMap<>();
   features.put(A,first);features.put(B,second);
   LinkedConvexIntersection recipe=new LinkedConvexIntersection(RESULT,A,B,
      LinkedConvexIntersection.Operation.DIFFERENCE_ORTHOGONAL);
   float[] triangles=recipe.regenerateTriangles(features,lines);
   close(TriangleMeshHealth.inspect(triangles).signedVolume,36,
     "Linked Boolean reconstructs exact difference");
   ok(Arrays.equals(triangles,recipe.regenerateTriangles(features,lines)),
      "Deterministic linked reload");
   Map<String,ClosedSketchExtrusion.Segment> edited=new LinkedHashMap<>(lines);
   double[][] cutter=rect(1,2,5,5);
   for(int i=0;i<4;i++){
    double[] p0=cutter[i],p1=cutter[(i+1)%4];
    edited.put(second.lineIds().get(i),
      new ClosedSketchExtrusion.Segment(p,p0[0],p0[1],p1[0],p1[1]));
   }
   close(TriangleMeshHealth.inspect(recipe.regenerateTriangles(features,edited))
     .signedVolume,30,"Edit cutter profile updates linked difference");
   close(TriangleMeshHealth.inspect(triangles).signedVolume,36,
     "Original difference unchanged");
   Map<String,LoopExtrusionFeature> mismatch=new LinkedHashMap<>(features);
   mismatch.put(B,second.withDistance(sign*2));
   invalid(()->recipe.regenerateTriangles(mismatch,lines),
      "Invalid dependency height edit blocked");
   mismatch.put(A,first.withDistance(sign*2));
   close(TriangleMeshHealth.inspect(recipe.regenerateTriangles(mismatch,lines))
     .signedVolume,24,"Both heights edited coherently");
   Map<String,ClosedSketchExtrusion.Segment> missing=new LinkedHashMap<>(lines);
   missing.remove(first.lineIds().get(0));
   invalid(()->recipe.regenerateTriangles(features,missing),
      "Orphan sketch source blocked");
  }
  invalid(()->OrthogonalPrismBRepUnion.difference(
    prism(A,0,2,rect(0,0,4,4)),prism(B,1,2,rect(2,2,5,5))),
    "Different base planes");
  invalid(()->OrthogonalPrismBRepUnion.difference(
    null,prism(B,0,2,rect(2,2,5,5))),"Null source");
  System.out.println("OrthogonalPrismBRepDifferenceTest: OK");
 }
}
