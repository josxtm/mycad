import es.josxtm.mycad.cad.*;
import java.util.*;

public final class PolygonPrismSideOffsetTest {
 private static final String ID="bcaa76b5-aaaa-4aaa-8aaa-000000000099";
 private static final String[] E={
  "bcaa76b5-aaaa-4aaa-8aaa-000000000001",
  "bcaa76b5-aaaa-4aaa-8aaa-000000000002",
  "bcaa76b5-aaaa-4aaa-8aaa-000000000003",
  "bcaa76b5-aaaa-4aaa-8aaa-000000000004"
 };
 static void yes(boolean test,String msg){if(!test)throw new AssertionError(msg);}
 static void near(double actual,double expected,String name){
  if(Math.abs(actual-expected)>1e-4*Math.max(1,Math.abs(expected)))
   throw new AssertionError(name+" "+actual+" != "+expected);
 }
 static void invalid(Runnable action,String msg){
  try{action.run();}catch(IllegalArgumentException good){return;}
  throw new AssertionError("Unsafe B-Rep side edit accepted: "+msg);
 }
 static LoopExtrusionFeature feature(int plane,double distance){
  return new LoopExtrusionFeature(ID,E[0],plane,distance,Arrays.asList(E));
 }
 static Map<String,ClosedSketchExtrusion.Segment> square(int plane){
  Map<String,ClosedSketchExtrusion.Segment> v=new LinkedHashMap<>();
  v.put(E[2],new ClosedSketchExtrusion.Segment(plane,4,4,0,4));
  v.put(E[0],new ClosedSketchExtrusion.Segment(plane,0,0,4,0));
  v.put(E[3],new ClosedSketchExtrusion.Segment(plane,0,4,0,0));
  v.put(E[1],new ClosedSketchExtrusion.Segment(plane,4,0,4,4));
  return v;
 }
 public static void main(String[] args){
  for(int p=0;p<3;p++)for(double h:new double[]{2,-2}){
   LoopExtrusionFeature f=feature(p,h);
   Map<String,ClosedSketchExtrusion.Segment> source=square(p);
   PolygonPrismBRep solid=f.asBRep(source);
   String side=ID+"/face/side/"+E[0];
   PolygonPrismSideOffset.Plan out=PolygonPrismSideOffset.offset(f,source,side,1);
   yes(out.modifiedLineIds().size()==3,"Move selected line and two neighbors");
   yes(out.modifiedLineIds().contains(E[0]),"Source face line updated");
   near(out.solid.sectionArea(),20,"Outward offset +1");
   near(out.solid.volume(),40,"Outward volume +1");
   yes(out.solid.findFace(side)!=null,"Stable analytic side face identity");
   yes(f.asBRep(source).sectionArea()==16,"No mutation of input polygon");
   PolygonPrismSideOffset.Plan inside=PolygonPrismSideOffset.offset(f,source,side,-1);
   near(inside.solid.sectionArea(),12,"Inward offset -1");
   near(inside.solid.volume(),24,"Inward volume -1");
   // The four endpoints at two shared corners move together.
   ClosedSketchExtrusion.Segment edited=out.segments().get(E[0]);
   near(edited.ay,-1,"Side y outward");
   near(edited.by,-1,"Side y outward end");
   near(out.segments().get(E[1]).ay,-1,"Right neighbor remains connected");
   near(out.segments().get(E[3]).by,-1,"Left neighbor remains connected");
   TriangleMeshHealth.Report mesh=TriangleMeshHealth.inspect(out.solid.triangles());
   yes(mesh.isWatertight(),"Offset reconstructs a closed solid");
   invalid(()->PolygonPrismSideOffset.offset(f,source,side,-4),
    "Collapsed inward profile");
   invalid(()->PolygonPrismSideOffset.offset(f,source,ID+"/face/top",1),
    "Cap is not a lateral planar offset");
   invalid(()->PolygonPrismSideOffset.offset(f,source,ID+"/face/unknown",1),
    "Stale analytic face ID");
   invalid(()->PolygonPrismSideOffset.offset(f,source,side,0),"Zero distance");
   invalid(()->PolygonPrismSideOffset.offset(f,source,side,Double.NaN),
    "Nonfinite distance");
   boolean immutable=false;
   try{out.segments().clear();}catch(UnsupportedOperationException expected){immutable=true;}
   yes(immutable,"Immutable proposed result");
  }
  System.out.println("PolygonPrismSideOffsetTest: OK");
 }
}
