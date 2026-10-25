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
  try{ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),snapshot.replace("|30.0|10.0","|-30.0|10.0"));}catch(IllegalArgumentException expected){rejected=true;}
  check(rejected,"Invalid dimensions accepted");
  String featureId=loaded.extrusions().values().iterator().next().id;
  check(loaded.setExtrusionHeight(featureId,8),"Edit extrusion height");
  check(loaded.bodies().get(bodyId).volume()==2400,"Height recalculates volume");
  check(loaded.extrusions().get(featureId).bodyId.equals(bodyId),"Stable body after edit");
  check(!loaded.setExtrusionHeight(featureId,-3),"Reject negative height edit");
  check(loaded.bodies().get(bodyId).volume()==2400,"Rejected edit must be atomic");
  check(loaded.undo()&&loaded.bodies().get(bodyId).volume()==1500,"Undo extrusion height");
  check(loaded.redo()&&loaded.bodies().get(bodyId).volume()==2400,"Redo extrusion height");
  check(loaded.removeExtrusion(featureId),"Remove feature");
  check(loaded.bodies().isEmpty()&&loaded.sketches().containsKey(sketch),"Preserve source sketch");
  check(loaded.undo()&&loaded.bodies().containsKey(bodyId),"Restore removed extrusion");
  for(PlaneExtrusion.Plane plane:PlaneExtrusion.Plane.values()){
   ParametricSketchDocument threeD=new ParametricSketchDocument(new PrimitiveKernel());
   String profile=threeD.addRectangleOnPlane(plane,2,3,7,20);
   check(profile!=null,"Plane sketch "+plane);
   CadKernel.Result<CadKernel.Box> extruded=threeD.extrude(profile,5,-4);
   check(extruded.isSuccess()&&extruded.value.volume()==560,"Signed extrusion "+plane);
   String id3=extruded.value.id;
   check(threeD.updateRectangle(profile,2,3,10,20),"Update plane sketch "+plane);
   check(threeD.bodies().get(id3).volume()==800,"Rebuild "+plane);
   String roundTrip=threeD.exportSnapshot();
   ParametricSketchDocument reopened=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),roundTrip);
   check(reopened.sketches().get(profile).plane==plane,"Restore plane "+plane);
   check(reopened.bodies().get(id3).volume()==800,"Restore volume "+plane);
   check(reopened.undo()&&reopened.bodies().isEmpty(),"Undo after import returns empty initial state");
  }
  System.out.println("ParametricSketchDocumentTest: OK");
 }
}
