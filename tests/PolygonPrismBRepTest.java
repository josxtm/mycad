import es.josxtm.mycad.cad.*;
import java.util.*;

/** Analytic half-edge manifold, oriented faces and stable UUID topology tests. */
public final class PolygonPrismBRepTest {
 static final String ID="daffaABB-0000-4000-8000-000000000090".toLowerCase(Locale.US);
 static final String[] IDS={
  "daffaabb-0000-4000-8000-000000000001",
  "daffaabb-0000-4000-8000-000000000002",
  "daffaabb-0000-4000-8000-000000000003",
  "daffaabb-0000-4000-8000-000000000004",
  "daffaabb-0000-4000-8000-000000000005",
  "daffaabb-0000-4000-8000-000000000006"
 };
 static void check(boolean condition,String reason){
  if(!condition)throw new AssertionError(reason);
 }
 static void close(double actual,double expected,String reason){
  if(Math.abs(actual-expected)>Math.max(1e-4,Math.abs(expected)*1e-5))
   throw new AssertionError(reason+": "+actual+" != "+expected);
 }
 static void invalid(Runnable op,String reason){
  try{op.run();}catch(IllegalArgumentException good){return;}
  throw new AssertionError("Accepted invalid topology: "+reason);
 }
 static Map<String,ClosedSketchExtrusion.Segment> polygon(
       int plane,double[][] pts,boolean reverse,boolean reversedInputs){
  Map<String,ClosedSketchExtrusion.Segment> out=new LinkedHashMap<>();
  int n=pts.length;
  // Shuffle map iteration intentionally; source slots do not depend on it.
  for(int i=n-1;i>=0;i--){
   int a=reverse?((n-i)%n):i;
   int b=reverse?((n-i-1+n)%n):(i+1)%n;
   if(reversedInputs){int swap=a;a=b;b=swap;}
   out.put(IDS[i],new ClosedSketchExtrusion.Segment(plane,
      pts[a][0],pts[a][1],pts[b][0],pts[b][1]));
  }
  return out;
 }
 static LoopExtrusionFeature feature(int plane,double height,int edges){
  return new LoopExtrusionFeature(ID,IDS[0],plane,height,
    Arrays.asList(Arrays.copyOf(IDS,edges)));
 }
 static Map<String,String> vertexPositions(PolygonPrismBRep model){
  Map<String,String> result=new HashMap<>();
  for(PolygonPrismBRep.Vertex v:model.vertices())
   result.put(v.id,v.x+","+v.y+","+v.z);
  return result;
 }
 static void verify(PolygonPrismBRep model,int n,double volume,String label){
  check(model.vertices().size()==2*n,label+" V=2n");
  check(model.edges().size()==3*n,label+" E=3n");
  check(model.faces().size()==n+2,label+" F=n+2");
  check(model.halfEdges().size()==6*n,label+" HE=6n");
  check(model.vertices().size()-model.edges().size()+model.faces().size()==2,
        label+" Euler");
  close(model.volume(),volume,label+" volume");
  close(TriangleMeshHealth.inspect(model.triangles()).signedVolume,volume,
        label+" signed mesh volume");
  check(TriangleMeshHealth.inspect(model.triangles()).isWatertight(),
        label+" watertight preview");
  HashMap<String,PolygonPrismBRep.HalfEdge> uses=new HashMap<>();
  for(PolygonPrismBRep.HalfEdge e:model.halfEdges()){
   check(uses.put(e.id,e)==null,label+" unique halfedge");
   check(model.findFace(e.faceId)!=null,label+" face reference");
   check(model.findVertex(e.fromId)!=null&&model.findVertex(e.toId)!=null,
         label+" vertex reference");
   check(model.findEdge(e.edgeId)!=null,label+" edge reference");
  }
  for(PolygonPrismBRep.HalfEdge he:model.halfEdges()){
   PolygonPrismBRep.HalfEdge twin=uses.get(he.twinId),
     next=uses.get(he.nextId),prev=uses.get(he.previousId);
   check(twin!=null&&next!=null&&prev!=null,label+" all halfedge links exist");
   check(twin.twinId.equals(he.id),label+" reciprocal twin");
   check(twin.fromId.equals(he.toId)&&twin.toId.equals(he.fromId),
         label+" opposite winding");
   check(next.fromId.equals(he.toId)&&prev.toId.equals(he.fromId),
         label+" halfedge cyclic order");
   check(next.previousId.equals(he.id)&&prev.nextId.equals(he.id),
         label+" next/previous reciprocity");
  }
  for(PolygonPrismBRep.Face f:model.faces()){
   check(f.halfEdgeIds().size()==f.vertexIds().size(),label+" face loop edges");
   check(f.area>0&&Double.isFinite(f.area),label+" positive face area");
   double[] normal=f.outwardNormal();
   close(Math.sqrt(normal[0]*normal[0]+normal[1]*normal[1]+normal[2]*normal[2]),
         1,label+" unit face normal");
  }
  for(PolygonPrismBRep.Edge e:model.edges()){
   check(e.adjacentFaceIds().size()==2&&e.halfEdgeIds().size()==2,
         label+" shared analytic edge");
   check(e.length>0,label+" positive edge length");
  }
  check(model.findFace(ID+"/face/top")!=null&&model.findFace(ID+"/face/base")!=null,
        label+" stable caps");
 }
 public static void main(String[] args){
  double[][] triangle={{0,0},{3,0},{0,4}};
  double[][] concave={{0,0},{3,0},{3,1},{1,1},{1,3},{0,3}};
  for(int p=0;p<3;p++)for(double h:new double[]{7,-7}){
   PolygonPrismBRep tri=PolygonPrismBRep.from(feature(p,h,3),
     polygon(p,triangle,false,false));
   verify(tri,3,42,"triangle plane "+p+" distance "+h);
   PolygonPrismBRep l=PolygonPrismBRep.from(feature(p,h,6),
     polygon(p,concave,false,false));
   verify(l,6,35,"concave plane "+p+" distance "+h);
   close(l.sectionArea(),5,"L shape area");
   close(l.surfaceArea(),10+12*Math.abs(h),"L surface area");
   // Topology slots remain stable across coordinate and height edits.
   PolygonPrismBRep resized=PolygonPrismBRep.from(feature(p,h*0.5,6),
     polygon(p,concave,false,false));
   for(PolygonPrismBRep.Face f:l.faces())check(resized.findFace(f.id)!=null,"stable face ID");
   for(PolygonPrismBRep.Edge e:l.edges())check(resized.findEdge(e.id)!=null,"stable edge ID");
   for(PolygonPrismBRep.Vertex v:l.vertices())check(resized.findVertex(v.id)!=null,"stable vertex ID");
  }
  // Vertex positions differ but logical topology still derives from incident
  // sketch UUIDs, never from tessellation indices or input drawing direction.
  PolygonPrismBRep a=PolygonPrismBRep.from(feature(0,5,6),polygon(0,concave,false,false));
  PolygonPrismBRep backward=PolygonPrismBRep.from(feature(0,5,6),polygon(0,concave,false,true));
  for(PolygonPrismBRep.Edge edge:a.edges())
   check(backward.findEdge(edge.id)!=null,"stable ID under reversed line directions");
  Map<String,ClosedSketchExtrusion.Segment> changed=polygon(0,concave,false,false);
  changed.put(IDS[0],new ClosedSketchExtrusion.Segment(0,0,0,4,0));
  changed.put(IDS[1],new ClosedSketchExtrusion.Segment(0,4,0,4,1));
  changed.put(IDS[2],new ClosedSketchExtrusion.Segment(0,4,1,1,1));
  PolygonPrismBRep moved=PolygonPrismBRep.from(feature(0,5,6),changed);
  close(moved.volume(),30,"edited profile volume");
  for(PolygonPrismBRep.Edge edge:a.edges())
   check(moved.findEdge(edge.id)!=null,"edit retains edge ID");
  invalid(()->PolygonPrismBRep.from(feature(0,5,6),Collections.emptyMap()),
          "lost sketch lines");
  Map<String,ClosedSketchExtrusion.Segment> invalid=polygon(0,concave,false,false);
  invalid.remove(IDS[0]);
  invalid(()->PolygonPrismBRep.from(feature(0,5,6),invalid),"deleted edge");
  check(a.triangles()!=a.triangles(),"preview defensive copying");
  boolean immutable=false;
  try{a.faces().clear();}catch(UnsupportedOperationException good){immutable=true;}
  check(immutable,"immutable face collection");
  System.out.println("PolygonPrismBRepTest: OK");
 }
}
