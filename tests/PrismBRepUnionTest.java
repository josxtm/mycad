import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class PrismBRepUnionTest {
 static PolygonPrismBRep prism(String key,int plane,double height,double[][] polygon){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> edges=new LinkedHashMap<>();
  for(int i=0;i<polygon.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/e/"+i).getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=polygon[i],b=polygon[(i+1)%polygon.length];
   ids.add(edge);
   edges.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,height,ids).asBRep(edges);
 }
 static void check(PolygonPrismBRep body,double volume,String name){
  if(Math.abs(body.volume()-volume)>1e-4)
   throw new AssertionError(name+" volume "+body.volume()+" expected "+volume);
  if(!TriangleMeshHealth.inspect(body.triangles()).isWatertight())
   throw new AssertionError(name+" must be watertight");
 }
 static void reject(Runnable action){
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Unsupported union accepted");
 }
 public static void main(String[] args){
  double[][] outer={{0,0},{6,0},{6,6},{0,6}};
  double[][] inner={{1,1},{3,1},{1,3}};
  double[][] slantedUpper={{0,0},{4,0},{2,2}};
  double[][] slantedLower={{0,0},{2,-2},{4,0}};
  double[][] lShape={{0,0},{4,0},{4,1},{1,1},{1,4},{0,4}};
  double[][] rectangle={{2,0},{5,0},{5,3},{2,3}};
  double[][] far={{9,9},{11,9},{11,11},{9,11}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   final int p=plane,s=sign;
   PolygonPrismBRep big=prism("big"+p+s,p,s*3,outer);
   PolygonPrismBRep small=prism("small"+p+s,p,s*2,inner);
   if(PrismBRepUnion.union(big,small)!=big||PrismBRepUnion.union(small,big)!=big)
    throw new AssertionError("Nested union must preserve containing shell");
   check(PrismBRepUnion.union(big,small),108,"nested");
   PolygonPrismBRep a=prism("a"+p+s,p,s*3,slantedUpper);
   PolygonPrismBRep b=prism("b"+p+s,p,s*3,slantedLower);
   check(PrismBRepUnion.union(a,b),24,"oblique edge-sharing");
   PolygonPrismBRep l=prism("l"+p+s,p,s*3,lShape);
   PolygonPrismBRep r=prism("r"+p+s,p,s*3,rectangle);
   check(PrismBRepUnion.union(l,r),42,"concave orthogonal union");
   reject(()->PrismBRepUnion.union(big,prism("far"+p+s,p,s*3,far)));
   reject(()->PrismBRepUnion.union(big,prism("reverse"+p+s,p,-s*3,inner)));
  }
  System.out.println("PrismBRepUnionTest: OK");
 }
}
