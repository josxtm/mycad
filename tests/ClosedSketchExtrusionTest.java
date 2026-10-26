import es.josxtm.mycad.cad.*;
import java.util.*;

public final class ClosedSketchExtrusionTest {
 private static ClosedSketchExtrusion.Segment line(int p,double a,double b,double c,double d){
  return new ClosedSketchExtrusion.Segment(p,a,b,c,d);
 }
 private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
 private static void near(double actual,double expected,String name){
  if(Math.abs(actual-expected)>Math.max(1e-4,Math.abs(expected)*1e-5))
   throw new AssertionError(name+": "+actual+" != "+expected);
 }
 private static void invalid(List<ClosedSketchExtrusion.Segment> input,int index,double h,String name){
  try{ClosedSketchExtrusion.extrude(input,index,h);}
  catch(IllegalArgumentException ok){return;}
  throw new AssertionError("Accepted invalid profile: "+name);
 }
 private static void valid(List<ClosedSketchExtrusion.Segment> input,int index,double h,
                           double expectedArea,int expectedLines,String name){
  ClosedSketchExtrusion.Result r=ClosedSketchExtrusion.extrude(input,index,h);
  near(r.area,expectedArea,name+" area");
  near(r.volume,expectedArea*h,name+" volume");
  check(r.usedLines==expectedLines,name+" line count");
  check(r.triangles.length==(4*expectedLines-4)*9,name+" triangle count");
  TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(r.triangles);
  check(report.isWatertight()&&report.shells==1,name+" watertight");
  near(report.signedVolume,r.volume,name+" oriented volume");
 }
 public static void main(String[] args){
  // Concave L-shaped profile (area 5 mm2), segments intentionally unordered
  // and some oriented backwards; algorithm must reconstruct actual loop.
  List<ClosedSketchExtrusion.Segment> l=Arrays.asList(
   line(0,0,0,3,0),line(0,1,1,1,3),line(0,0,3,0,0),
   line(0,3,0,3,1),line(0,3,1,1,1),line(0,1,3,0,3));
  for(int p=0;p<3;p++){
   List<ClosedSketchExtrusion.Segment> plane=new ArrayList<>();
   for(ClosedSketchExtrusion.Segment e:l)plane.add(line(p,e.ax,e.ay,e.bx,e.by));
   valid(plane,2,7,5,6,"Concave L plane "+p);
  }
  // Component-based selection: ignore a disconnected closed triangle, do
  // not accidentally fuse two separate solids into one body.
  List<ClosedSketchExtrusion.Segment> two=new ArrayList<>(l);
  two.add(line(0,10,0,12,0));two.add(line(0,12,0,11,2));
  two.add(line(0,11,2,10,0));
  valid(two,0,2,5,6,"Select L component");
  valid(two,7,2,2,3,"Select triangle component");
  // Slightly different drawn endpoints are snapped to form ONE manifold mesh.
  List<ClosedSketchExtrusion.Segment> snap=Arrays.asList(
   line(0,0,0,5,0),line(0,5.05,0,5,3),
   line(0,5,3.04,0,3),line(0,0,3.05,0.03,0));
  valid(snap,0,3,15,4,"Snap imperfect closed rectangle");
  List<ClosedSketchExtrusion.Segment> open=Arrays.asList(
   line(0,0,0,3,0),line(0,3,0,3,3),line(0,3,3,0,3));
  invalid(open,0,4,"Open polyline");
  List<ClosedSketchExtrusion.Segment> branched=new ArrayList<>(l);
  branched.add(line(0,3,0,4,0));
  invalid(branched,0,2,"Branching line");
  List<ClosedSketchExtrusion.Segment> bow=Arrays.asList(
   line(0,0,0,3,3),line(0,3,3,0,3),
   line(0,0,3,3,0),line(0,3,0,0,0));
  invalid(bow,0,3,"Self-crossing bow tie");
  List<ClosedSketchExtrusion.Segment> spur=Arrays.asList(
   line(0,0,0,4,0),line(0,4,0,4,4),
   line(0,4,4,0,4),line(0,0,4,0,0),line(0,0,0,4,4));
  invalid(spur,0,2,"Branch diagonal");
  invalid(l,0,0,"Zero height");
  invalid(l,0,-1,"Negative height");
  invalid(l,0,20000,"Too tall");
  invalid(l,-1,1,"Missing selection");
  invalid(l,100,1,"Selection outside scene");
  try{line(0,0,0,0.01,0);}catch(IllegalArgumentException expected){
   System.out.println("ClosedSketchExtrusionTest: OK");return;
  }
  throw new AssertionError("Accepted collapsed segment");
 }
}
