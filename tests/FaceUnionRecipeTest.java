import es.josxtm.mycad.cad.*;
public final class FaceUnionRecipeTest {
 public static void main(String[] args){
  for(BoxFaceSelection.Face f:BoxFaceSelection.Face.values()){
   FaceUnionRecipe r=new FaceUnionRecipe(10,20,30,4,5,6,f,7);
   FaceUnionRecipe updated=r.withDistance(9);
   if(updated.distance!=9||!updated.id.equals(r.id))throw new AssertionError("Reparametrization lost identity");
   if(!new FaceUnionRecipe(r.id,10,20,30,4,5,6,f,7).id.equals(r.id))
    throw new AssertionError("Restored face union lost UUID");
   if(r.unionVolume()<=6000)throw new AssertionError("Union volume");
  }
  boolean bad=false;
  try{new FaceUnionRecipe("not-a-uuid",10,20,30,4,5,6,BoxFaceSelection.Face.X_MAX,7);}
  catch(IllegalArgumentException expected){bad=true;}
  if(!bad)throw new AssertionError("Invalid face union UUID accepted");
  System.out.println("FaceUnionRecipeTest: OK");
 }
}