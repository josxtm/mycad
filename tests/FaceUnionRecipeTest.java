import es.josxtm.mycad.cad.*;
public final class FaceUnionRecipeTest {
 public static void main(String[] args){
  for(BoxFaceSelection.Face f:BoxFaceSelection.Face.values()){
   FaceUnionRecipe r=new FaceUnionRecipe(10,20,30,4,5,6,f,7);
   if(r.withDistance(9).distance!=9)throw new AssertionError("Reparametrization");
   if(r.unionVolume()<=6000)throw new AssertionError("Union volume");
  }
  System.out.println("FaceUnionRecipeTest: OK");
 }
}