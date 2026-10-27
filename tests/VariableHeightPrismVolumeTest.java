import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class VariableHeightPrismVolumeTest {
 private static PolygonPrismBRep prism(String key,int plane,double h,double[][] poly){
  String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
  List<String> ids=new ArrayList<>();
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<poly.length;i++){
   String edge=UUID.nameUUIDFromBytes((key+"/edge/"+i).getBytes(StandardCharsets.UTF_8)).toString();
   double[] a=poly[i],b=poly[(i+1)%poly.length];
   ids.add(edge);
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,a[0],a[1],b[0],b[1]));
  }
  return new LoopExtrusionFeature(id,ids.get(0),plane,h,ids).asBRep(lines);
 }
 private static void near(double actual,double expected){
  if(Math.abs(actual-expected)>1e-5)throw new AssertionError(actual+" != "+expected);
 }
 private static void reject(Runnable run){
  try{run.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Expected invalid inputs to be rejected");
 }
 public static void main(String[] args){
  double[][] a={{0,0},{4,0},{4,4},{0,4}};
  double[][] b={{2,0},{6,0},{6,4},{2,4}};
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   PolygonPrismBRep first=prism("first"+plane+sign,plane,3*sign,a);
   PolygonPrismBRep second=prism("second"+plane+sign,plane,2*sign,b);
   float[] original=first.triangles();
   VariableHeightPrismVolume.Result v=VariableHeightPrismVolume.calculate(first,second);
   near(v.firstVolume,48);near(v.secondVolume,32);
   near(v.intersectionVolume,16);near(v.differenceVolume,32);near(v.unionVolume,64);
   VariableHeightPrismVolume.Result swapped=VariableHeightPrismVolume.calculate(second,first);
   near(swapped.intersectionVolume,16);near(swapped.unionVolume,64);
   near(swapped.differenceVolume,16);
   if(!Arrays.equals(original,first.triangles()))throw new AssertionError("Source mutated");
   PolygonPrismBRep opposite=prism("opposite"+plane+sign,plane,-sign*2,b);
   reject(()->VariableHeightPrismVolume.calculate(first,opposite));
  }
  System.out.println("VariableHeightPrismVolumeTest: OK");
 }
}
