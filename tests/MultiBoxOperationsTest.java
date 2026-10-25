import es.josxtm.mycad.*;
import java.util.*;
public final class MultiBoxOperationsTest {
 static double volume(float[] tris){
  double signed=0;
  for(int i=0;i<tris.length;i+=9){
   double ax=tris[i],ay=tris[i+1],az=tris[i+2],bx=tris[i+3],by=tris[i+4],bz=tris[i+5],cx=tris[i+6],cy=tris[i+7],cz=tris[i+8];
   signed+=ax*(by*cz-bz*cy)+ay*(bz*cx-bx*cz)+az*(bx*cy-by*cx);
  }
  return Math.abs(signed/6);
 }
 static void near(double a,double b){if(Math.abs(a-b)>0.00001)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  BoxBoolean.Box outer=new BoxBoolean.Box(0,0,0,10,10,10);
  BoxBoolean.Box cutter=new BoxBoolean.Box(0,0,0,4,4,10);
  List<BoxBoolean.Box> boxes=Arrays.asList(outer,cutter);
  near(volume(MultiBoxUnion.build(boxes,MultiBoxUnion.Operation.UNION)),1000);
  near(volume(MultiBoxUnion.build(boxes,MultiBoxUnion.Operation.DIFFERENCE)),840);
  near(volume(MultiBoxUnion.build(boxes,MultiBoxUnion.Operation.INTERSECTION)),160);
  BoxBoolean.Box outside=new BoxBoolean.Box(50,50,50,2,2,2);
  near(volume(MultiBoxUnion.build(Arrays.asList(outer,outside),MultiBoxUnion.Operation.DIFFERENCE)),1000);
  if(MultiBoxUnion.build(Arrays.asList(outer,outside),MultiBoxUnion.Operation.INTERSECTION).length!=0)throw new AssertionError("Disjoint");
  System.out.println("MultiBoxOperationsTest: OK");
 }
}