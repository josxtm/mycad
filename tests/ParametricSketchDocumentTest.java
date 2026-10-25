import es.josxtm.mycad.cad.*;
public final class ParametricSketchDocumentTest {
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args){
  ParametricSketchDocument doc=new ParametricSketchDocument(new PrimitiveKernel());
  String sketch=doc.addRectangle(0,0,20,10);
  check(sketch!=null,"Create sketch");
  CadKernel.Result<CadKernel.Box> r=doc.extrude(sketch,0,5);
  check(r.isSuccess()&&r.value.volume()==1000,"Extrusion");
  String bodyId=r.value.id;
  check(doc.updateRectangle(sketch,0,0,30,10),"Edit linked sketch");
  check(doc.bodies().get(bodyId).volume()==1500,"Rebuild body after sketch change");
  check(doc.bodies().get(bodyId).id.equals(bodyId),"Stable body id");
  check(!doc.updateRectangle(sketch,0,0,-1,10),"Reject invalid sketch");
  check(doc.bodies().get(bodyId).volume()==1500,"Invalid edit atomic");
  check(doc.undo()&&doc.bodies().get(bodyId).volume()==1000,"Undo sketch update");
  check(doc.redo()&&doc.bodies().get(bodyId).volume()==1500,"Redo sketch update");
  check(doc.removeRectangle(sketch)&&doc.bodies().isEmpty(),"Remove sketch and dependent body");
  check(doc.undo()&&doc.bodies().containsKey(bodyId),"Undo dependency removal");
  check(doc.extrude("missing",0,1).code==CadKernel.Code.INVALID_GEOMETRY,"Reject absent sketch");
  check(doc.extrude(sketch,0,-5).code==CadKernel.Code.INVALID_GEOMETRY,"Reject negative height for initial adapter");
  check(doc.bodies().size()==1,"Invalid extrusion atomic");
  String snapshot=doc.exportSnapshot();
  ParametricSketchDocument loaded=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),snapshot);
  check(loaded.bodies().size()==doc.bodies().size(),"Snapshot count");
  check(loaded.bodies().get(bodyId).volume()==doc.bodies().get(bodyId).volume(),"Snapshot volume");
  check(snapshot.equals(loaded.exportSnapshot()),"Stable round trip");
  boolean rejected=false;
  try{ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),snapshot.replace("MYCAD-PARAMETRIC|1","MYCAD-PARAMETRIC|2"));}catch(IllegalArgumentException expected){rejected=true;}
  check(rejected,"Invalid version accepted");
  rejected=false;
  try{ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),snapshot.replace("|20.0|10.0","|-20.0|10.0"));}catch(IllegalArgumentException expected){rejected=true;}
  check(rejected,"Invalid dimensions accepted");
  System.out.println("ParametricSketchDocumentTest: OK");
 }
}
