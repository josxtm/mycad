import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static es.josxtm.mycad.cad.LinkedConvexIntersection.Operation.*;

public final class NamedPlanarBooleanTest {
 private static String id(String s){return UUID.nameUUIDFromBytes(s.getBytes(StandardCharsets.UTF_8)).toString();}
 private static double[][] rect(double x,double y,double xx,double yy){return new double[][]{{x,y},{xx,y},{xx,yy},{x,yy}};}
 private static class Source {
  final LoopExtrusionFeature feature;final Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  Source(String key,int plane,double height,double[][] p){
   List<String> ids=new ArrayList<>();
   for(int i=0;i<p.length;i++){
    String edge=id(key+i);ids.add(edge);double[] a=p[i],b=p[(i+1)%p.length];
    lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
   }
   feature=new LoopExtrusionFeature(id(key),ids.get(0),plane,height,ids);
  }
  PolygonPrismBRep body(){return feature.asBRep(lines);}
 }
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Invalid naming accepted");}
 private static NamedPlanarBoolean named(Source a,Source b,LinkedConvexIntersection.Operation op){
  LinkedConvexIntersection recipe=new LinkedConvexIntersection(id("owner"),a.feature.id,b.feature.id,op);
  Map<String,LoopExtrusionFeature> features=new LinkedHashMap<>();features.put(a.feature.id,a.feature);features.put(b.feature.id,b.feature);
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>(a.lines);lines.putAll(b.lines);
  NamedPlanarBoolean result=recipe.regenerateNamedPlanar(features,lines);
  check(Arrays.equals(recipe.regenerateTriangles(features,lines),result.triangles()),"Historical tessellation changed: "+op);
  PlanarBooleanView view=PlanarBooleanView.from(result);int triangle=0;
  for(PolygonPrismBRep shell:result.solids()){
   check(shell.vertices().size()-shell.edges().size()+shell.faces().size()==2,"Euler");
   for(int i=0;i<shell.triangles().length/9;i++){
    String expected=result.faceId(PolygonPrismFacePicker.resolve(shell,i).id);
    check(expected.equals(view.faceAtTriangle(triangle++).id),"Global triangle ownership");
   }
   for(PolygonPrismBRep.Face face:shell.faces())check(view.trianglesForFace(result.faceId(face.id),19).length>=2,"Whole face highlight");
  }
  check(triangle*9==result.triangles().length,"Exact mesh length");
  check(TriangleMeshHealth.inspect(result.triangles()).isWatertight(),"Closed output");
  return result;
 }
 private static void sameEntities(NamedPlanarBoolean a,NamedPlanarBoolean b){
  TopologySnapshot x=TopologySnapshot.from(a),y=TopologySnapshot.from(b);
  check(!x.geometryVersion.equals(y.geometryVersion),"Geometry version must change");
  for(TopologySnapshot.Kind kind:TopologySnapshot.Kind.values()){
   check(x.ids(kind).equals(y.ids(kind)),"Names changed without topological change: "+kind+"/"+a.operation);
   for(String entity:x.ids(kind)){
    TopologySnapshot.Reference old=x.reference(kind,entity);
    check(y.resolveExact(old)==null&&y.resolvePersistent(old)!=null,"Explicit rebind only");
   }
  }
 }
 private static Set<String> caps(NamedPlanarBoolean body){
  Set<String> ids=new HashSet<>();for(PolygonPrismBRep shell:body.solids())for(PolygonPrismBRep.Face face:shell.faces())
   if(face.sourceLineId==null)ids.add(body.faceId(face.id));return ids;
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   for(LinkedConvexIntersection.Operation op:Arrays.asList(UNION_ANALYTIC,INTERSECTION_ANALYTIC,DIFFERENCE_ANALYTIC,
       UNION_ORTHOGONAL,DIFFERENCE_ORTHOGONAL,UNION_POLYGONAL,INTERSECTION_POLYGONAL,DIFFERENCE_POLYGONAL,
       UNION_MULTISHELL,DIFFERENCE_MULTISHELL,INTERSECTION_MULTISHELL)){
    Source a=new Source("a",plane,sign*4,rect(0,0,8,8)),b=new Source("b",plane,sign*4,rect(4,2,10,6));
    NamedPlanarBoolean before=named(a,b,op);
    NamedPlanarBoolean after=named(new Source("a",plane,sign*5,rect(0,0,9,9)),new Source("b",plane,sign*5,rect(4.5,2.5,11,6.5)),op);
    sameEntities(before,after);
    reject(()->after.vertexId(before.solids().get(0).vertices().get(0).id));
    NamedPlanarBoolean other=NamedPlanarBoolean.apply(id("other"),a.body(),b.body(),op);
    check(TopologySnapshot.from(other).resolvePersistent(TopologySnapshot.from(before).reference(TopologySnapshot.Kind.FACE,caps(before).iterator().next()))==null,"Owner isolation");
   }
   // Inclined shared-edge union, including a corner removed by the hull.
   NamedPlanarBoolean diamond=named(new Source("upper",plane,sign*4,new double[][]{{0,0},{4,0},{2,2}}),
    new Source("lower",plane,sign*4,new double[][]{{0,0},{2,-2},{4,0}}),UNION_ANALYTIC);
   NamedPlanarBoolean bigger=named(new Source("upper",plane,sign*5,new double[][]{{0,0},{5,0},{2.5,3}}),
    new Source("lower",plane,sign*5,new double[][]{{0,0},{2.5,-3},{5,0}}),UNION_ANALYTIC);
   sameEntities(diamond,bigger);
   // Nested union reuses a source shell but ownership remains the recipe UUID.
   NamedPlanarBoolean nested=named(new Source("outer",plane,sign*8,rect(0,0,10,10)),new Source("inner",plane,sign*2,new double[][]{{2,2},{4,2},{3,4}}),UNION_ANALYTIC);
   check(nested.bodyId.equals(id("owner")),"Nested owner");
   // Concave unequal-height intersection keeps original source-line UUIDs.
   double[][] l={{0,0},{4,0},{4,1},{1,1},{1,4},{0,4}};
   named(new Source("l",plane,sign*6,l),new Source("r",plane,sign*3,rect(2,0,5,3)),INTERSECTION_ANALYTIC);
   // U-shaped subtraction: two different faces can lie on a single cutter support.
   Source target=new Source("target",plane,sign*4,rect(0,0,10,10));
   NamedPlanarBoolean notch=named(target,new Source("cut",plane,sign*4,rect(3,3,7,12)),DIFFERENCE_MULTISHELL);
   NamedPlanarBoolean split=named(target,new Source("cut",plane,sign*4,rect(3,-2,7,12)),DIFFERENCE_MULTISHELL);
   check(notch.solids().size()==1&&split.solids().size()==2,"Split topology");
   Set<String> oldCaps=caps(notch),newCaps=caps(split);check(Collections.disjoint(oldCaps,newCaps),"Split must retire parent caps");
   TopologyReconciliation delta=TopologyReconciliation.between(TopologySnapshot.from(notch),TopologySnapshot.from(split));
   check(delta.change(TopologySnapshot.Kind.FACE).removed.containsAll(oldCaps),"Removed parent caps");
   check(delta.change(TopologySnapshot.Kind.FACE).created.containsAll(newCaps),"New child caps");
   NamedPlanarBoolean restored=named(target,new Source("cut",plane,sign*4,rect(3,3,7,12)),DIFFERENCE_MULTISHELL);
   check(caps(restored).equals(oldCaps),"Deterministic undo/reopen");
   // Physical order reverses, source identities do not: no index-based component labels.
   NamedPlanarBoolean disjoint=named(new Source("left",plane,sign*4,rect(0,0,2,2)),new Source("right",plane,sign*4,rect(5,0,7,2)),UNION_MULTISHELL);
   NamedPlanarBoolean reordered=named(new Source("left",plane,sign*4,rect(5,0,7,2)),new Source("right",plane,sign*4,rect(0,0,2,2)),UNION_MULTISHELL);
   sameEntities(disjoint,reordered);
   // Removing/reintroducing a corner invalidates affected sides, preserving single-shell caps.
   NamedPlanarBoolean notched=named(target,new Source("cut",plane,sign*4,rect(3,3,7,12)),DIFFERENCE_ANALYTIC);
   NamedPlanarBoolean corner=named(target,new Source("cut",plane,sign*4,rect(3,3,12,12)),DIFFERENCE_ANALYTIC);
   check(caps(notched).equals(caps(corner)),"Semantic single-shell caps");
   check(!TopologyReconciliation.between(TopologySnapshot.from(notched),TopologySnapshot.from(corner)).change(TopologySnapshot.Kind.FACE).removed.isEmpty(),"Retire changed sides");
   float[] copy=split.triangles();copy[0]++;check(!Arrays.equals(copy,split.triangles()),"Immutable mesh");
   try{split.solids().clear();throw new AssertionError("Mutable shells");}catch(UnsupportedOperationException expected){}
  }
  check(!NamedPlanarBoolean.supports(INTERSECTION)&&!NamedPlanarBoolean.supports(UNION_STEPPED),"Keep published families separate");
  reject(()->NamedPlanarBoolean.apply("bad",null,null,UNION_ANALYTIC));
  System.out.println("NamedPlanarBooleanTest: OK");
 }
}
