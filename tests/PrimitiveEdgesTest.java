import es.josxtm.mycad.cad.*;
import java.util.*;
public final class PrimitiveEdgesTest {
 static void near(double x,double expected,String label){
  if(Math.abs(x-expected)>0.000001)throw new AssertionError(label+" "+x+" vs "+expected);
 }
 static void yes(boolean ok,String label){if(!ok)throw new AssertionError(label);}
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(12,18,-7,35,17,-45);
  HashSet<String> keys=new HashSet<>();
  for(int i=0;i<12;i++){
   PrimitiveEdges.Edge e=PrimitiveEdges.edge("body-123",i,20,10,6,pose);
   near(e.length,i<4?20:i<8?10:6,"Edge length "+i);
   yes(keys.add(e.key),"Duplicate edge key");
   yes(e.axis.equals(i<4?"X":i<8?"Y":"Z"),"Axis "+i);
   for(double v:e.start)yes(Double.isFinite(v),"Finite edge endpoint");
  }
  PrimitiveEdges.Edge e=PrimitiveEdges.edge("body-123",0,20,10,6,
    new RigidTransform(0,0,0,0,0,0));
  near(e.start[0],-10,"Lower X");near(e.end[0],10,"Upper X");
  near(e.start[1],-5,"Front Y");near(e.start[2],0,"Bottom Z");
  yes(e.key.equals("body-123/edge/X_0"),"Stable edge slot");
  boolean invalid=false;
  try{PrimitiveEdges.edge("body-123",12,20,10,6,pose);}
  catch(IllegalArgumentException x){invalid=true;}
  yes(invalid,"Reject invalid slot");
  System.out.println("PrimitiveEdgesTest: OK");
 }
}
