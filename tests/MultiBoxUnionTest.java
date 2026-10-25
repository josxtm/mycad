import es.josxtm.mycad.*;
import java.util.*;
public final class MultiBoxUnionTest {
 static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
 public static void main(String[] args){
  BoxBoolean.Box a=new BoxBoolean.Box(0,0,0,10,10,10),b=new BoxBoolean.Box(10,0,0,10,10,10),c=new BoxBoolean.Box(20,0,0,10,10,10);
  float[] joined=MultiBoxUnion.build(Arrays.asList(a,b,c));
  check(joined.length>0&&joined.length%9==0,"Valid triangles");
  int internal=0;for(int i=0;i<joined.length;i+=9){
   boolean x5=joined[i]==5&&joined[i+3]==5&&joined[i+6]==5;
   boolean x15=joined[i]==15&&joined[i+3]==15&&joined[i+6]==15;
   if(x5||x15)internal++;
  }
  check(internal==0,"No shared inner planes");
  check(MultiBoxUnion.build(Collections.singletonList(a)).length==108,"Single box 12 triangles");
  System.out.println("MultiBoxUnionTest: OK");
 }
}