import es.josxtm.mycad.cad.*;
import es.josxtm.mycad.MultiBoxUnion;
import java.util.*;
public final class ChainBooleanFeatureTest {
 public static void main(String[] args){
  FaceExtrusionChain chain=new FaceExtrusionChain(10,10,10,0,0,0);
  chain.append(BoxFaceSelection.Face.X_MAX,10);
  ChainBooleanFeature f=new ChainBooleanFeature(0,MultiBoxUnion.Operation.CHAIN_DIFFERENCE,5,0,0,4,4,10);
  int before=f.regenerate(Collections.singletonList(chain)).length;
  if(before==0)throw new AssertionError("Missing mesh");
  chain.update(0,20);
  int after=f.regenerate(Collections.singletonList(chain)).length;
  if(after==0)throw new AssertionError("Missing regenerated mesh");
  boolean missing=false;try{f.regenerate(Collections.emptyList());}catch(IllegalArgumentException ok){missing=true;}
  if(!missing)throw new AssertionError("Missing dependency accepted");
 }
}