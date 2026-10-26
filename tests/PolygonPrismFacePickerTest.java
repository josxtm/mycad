import es.josxtm.mycad.cad.*;
import java.util.*;

public final class PolygonPrismFacePickerTest {
 private static final String FEATURE="a0b0c0d0-1111-4111-8111-000000000099";
 private static final String[] LINES={
  "a0b0c0d0-1111-4111-8111-000000000001",
  "a0b0c0d0-1111-4111-8111-000000000002",
  "a0b0c0d0-1111-4111-8111-000000000003",
  "a0b0c0d0-1111-4111-8111-000000000004",
  "a0b0c0d0-1111-4111-8111-000000000005",
  "a0b0c0d0-1111-4111-8111-000000000006"
 };
 private static void check(boolean okay,String label){if(!okay)throw new AssertionError(label);}
 private static PolygonPrismBRep polygon(int p,double h,boolean changed){
  double[][] coords={{0,0},{3,0},{3,1},{1,1},{1,3},{0,3}};
  if(changed){coords[1][0]=4;coords[2][0]=4;}
  Map<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  // Reversing every input segment must not change UUID-based face identity.
  for(int i=5;i>=0;i--){
   double[] a=coords[i],b=coords[(i+1)%6];
   lines.put(LINES[i],new ClosedSketchExtrusion.Segment(p,b[0],b[1],a[0],a[1]));
  }
  return PolygonPrismBRep.from(new LoopExtrusionFeature(
    FEATURE,LINES[2],p,h,Arrays.asList(LINES)),lines);
 }
 public static void main(String[] args){
  for(int p=0;p<3;p++)for(double h:new double[]{7,-7}){
   PolygonPrismBRep model=polygon(p,h,false);
   int count=model.triangles().length/9;
   Map<String,Integer> usage=new HashMap<>();
   for(int i=0;i<count;i++){
    PolygonPrismBRep.Face f=PolygonPrismFacePicker.resolve(model,i);
    check(f!=null&&model.findFace(f.id)==f,"Stable analytic face for triangle "+i);
    usage.put(f.id,usage.getOrDefault(f.id,0)+1);
    if(i<2*(6-2))
     check((i%2==0?f.id.endsWith("/top"):f.id.endsWith("/base")),
       "Cap face order "+i+" on plane "+p);
    else check(f.sourceLineId!=null,
       "Side face must map to original sketch line UUID");
   }
   check(usage.size()==8,"All cap and six lateral faces recognized");
   check(usage.get(FEATURE+"/face/top")==4,"Top has n-2 triangles");
   check(usage.get(FEATURE+"/face/base")==4,"Base has n-2 triangles");
   for(String id:LINES)check(usage.get(FEATURE+"/face/side/"+id)==2,
      "Each side uses two mesh triangles");
   PolygonPrismBRep edited=polygon(p,h*0.5,true);
   for(int i=0;i<count;i++){
    String old=PolygonPrismFacePicker.resolve(model,i).id;
    String now=PolygonPrismFacePicker.resolve(edited,i).id;
    check(old.equals(now),"Face id changed after profile/height edit");
   }
   check(PolygonPrismFacePicker.resolve(model,-1)==null,"Negative triangle");
   check(PolygonPrismFacePicker.resolve(model,count)==null,"Outside result");
  }
  check(PolygonPrismFacePicker.resolve(null,0)==null,"Null model");
  System.out.println("PolygonPrismFacePickerTest: OK");
 }
}
