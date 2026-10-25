import es.josxtm.mycad.*;
import java.util.*;
public final class ChainBooleanTest {
 static double volume(float[] tris){
  double result=0;
  for(int i=0;i<tris.length;i+=9){
   double ax=tris[i],ay=tris[i+1],az=tris[i+2],bx=tris[i+3],by=tris[i+4],bz=tris[i+5],cx=tris[i+6],cy=tris[i+7],cz=tris[i+8];
   result+=ax*(by*cz-bz*cy)+ay*(bz*cx-bx*cz)+az*(bx*cy-by*cx);
  }
  return Math.abs(result/6);
 }
 static void near(double a,double b){if(Math.abs(a-b)>0.0001)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  BoxBoolean.Box a=new BoxBoolean.Box(0,0,0,10,10,10);
  BoxBoolean.Box b=new BoxBoolean.Box(10,0,0,10,10,10);
  BoxBoolean.Box hole=new BoxBoolean.Box(5,0,0,4,4,10);
  java.util.List<BoxBoolean.Box> boxes=Arrays.asList(a,b,hole);
  near(volume(MultiBoxUnion.build(boxes,MultiBoxUnion.Operation.CHAIN_DIFFERENCE)),1840);
  near(volume(MultiBoxUnion.build(boxes,MultiBoxUnion.Operation.CHAIN_INTERSECTION)),160);
  near(volume(MultiBoxUnion.build(Arrays.asList(a,b),MultiBoxUnion.Operation.UNION)),2000);
  System.out.println("ChainBooleanTest: OK");
 }
}