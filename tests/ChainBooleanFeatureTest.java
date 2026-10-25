import es.josxtm.mycad.cad.*;
import es.josxtm.mycad.MultiBoxUnion;
import java.util.*;
public final class ChainBooleanFeatureTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  FaceExtrusionChain chain=new FaceExtrusionChain(10,10,10,0,0,0);
  chain.append(BoxFaceSelection.Face.X_MAX,10);
  ChainBooleanFeature cut=new ChainBooleanFeature(0,MultiBoxUnion.Operation.CHAIN_DIFFERENCE,5,0,0,4,4,10);
  int before=cut.regenerate(Collections.singletonList(chain)).length;
  check(before>0,"Missing cut mesh");
  ChainBooleanFeature moved=cut.withCutter(5,0,0,8,6,10);
  check(moved!=cut&&cut.w==4&&cut.d==4&&moved.w==8&&moved.d==6,"Immutable cutter");
  check(moved.regenerate(Collections.singletonList(chain)).length>0,"Modified cut");
  ChainBooleanFeature intersect=moved.withOperation(MultiBoxUnion.Operation.CHAIN_INTERSECTION);
  check(intersect.operation==MultiBoxUnion.Operation.CHAIN_INTERSECTION,"Operation toggle");
  check(intersect.regenerate(Collections.singletonList(chain)).length>0,"Intersection mesh");
  check(cut.operation==MultiBoxUnion.Operation.CHAIN_DIFFERENCE,"Original operation unchanged");
  chain.update(0,20);
  check(cut.regenerate(Collections.singletonList(chain)).length>0,"Missing regenerated mesh");
  check(intersect.regenerate(Collections.singletonList(chain)).length>0,"Missing downstream update");
  boolean invalid=false;try{cut.withCutter(0,0,0,-1,4,4);}catch(IllegalArgumentException ok){invalid=true;}
  check(invalid,"Negative cutter accepted");
  invalid=false;try{cut.withOperation(MultiBoxUnion.Operation.UNION);}catch(IllegalArgumentException ok){invalid=true;}
  check(invalid,"Unsupported operation accepted");
  invalid=false;try{cut.regenerate(Collections.emptyList());}catch(IllegalArgumentException ok){invalid=true;}
  check(invalid,"Missing dependency accepted");
  System.out.println("ChainBooleanFeatureTest: OK");
 }
}