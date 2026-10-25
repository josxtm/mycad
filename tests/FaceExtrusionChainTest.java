import es.josxtm.mycad.cad.*;
import java.util.*;
public final class FaceExtrusionChainTest {
 static void near(double a,double b){if(Math.abs(a-b)>1e-8)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  FaceExtrusionChain c=new FaceExtrusionChain(10,20,30,0,0,0);
  c.append(BoxFaceSelection.Face.X_MAX,5);c.append(BoxFaceSelection.Face.Z_MAX,7);
  List<FaceExtrusionChain.Solid> r=c.regenerate();
  if(r.size()!=3)throw new AssertionError("History");
  near(r.get(1).x,7.5);near(r.get(2).z,30);
  c.update(0,9);r=c.regenerate();near(r.get(1).x,9.5);near(r.get(2).x,9.5);
  if(c.size()!=2)throw new AssertionError("Steps lost");
  c.removeLast();if(c.regenerate().size()!=2)throw new AssertionError("Remove last");
  boolean bad=false;try{c.append(BoxFaceSelection.Face.Y_MIN,-1);}catch(IllegalArgumentException ok){bad=true;}
  if(!bad)throw new AssertionError("Negative extrusion");
  System.out.println("FaceExtrusionChainTest: OK");
 }
}