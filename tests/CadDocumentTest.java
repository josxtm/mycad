import es.josxtm.mycad.cad.*;
public final class CadDocumentTest {
 static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 public static void main(String[] args){
  CadDocument doc=new CadDocument(new PrimitiveKernel());
  CadKernel.Result<CadKernel.Box> first=doc.addBox(0,0,0,20,10,5);
  check(first.isSuccess(),"Add box");String id=first.value.id;
  check(doc.bodies().size()==1&&doc.features().size()==1,"Feature registration");
  String featureId=doc.features().get(0).id;
  check(doc.setBoxHeight(id,8).isSuccess(),"Resize");
  check(doc.body(id).volume()==1600,"Volume changed");
  check(doc.features().get(0).id.equals(featureId),"Feature ID preserved");
  int revision=doc.revision();
  check(!doc.setBoxHeight(id,-1).isSuccess(),"Invalid height rejected");
  check(doc.revision()==revision&&doc.body(id).volume()==1600,"Atomic failure");
  check(doc.undo()&&doc.body(id).volume()==1000,"Undo");
  check(doc.redo()&&doc.body(id).volume()==1600,"Redo");
  check(doc.undo(),"Undo before branch");
  CadKernel.Result<CadKernel.Box> second=doc.addBox(30,0,0,3,4,5);
  check(second.isSuccess()&&doc.bodies().size()==2,"Add second body");
  check(!doc.canRedo(),"Redo branch must be cleared");
  check(doc.removeBody(id),"Remove first");
  check(doc.bodies().size()==1&&!doc.bodies().containsKey(id),"Removal reflected");
  check(doc.undo()&&doc.bodies().size()==2,"Undo removal");
  check(!doc.removeBody("invalid"),"Unknown body");
  System.out.println("CadDocumentTest: OK");
 }
}
