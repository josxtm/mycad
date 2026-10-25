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
  check(doc.extrude(sketch,0,0).code==CadKernel.Code.INVALID_GEOMETRY,"Reject zero extrusion height");
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
  check(!loaded.setExtrusionHeight(featureId,0),"Reject zero height edit");
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
  ParametricSketchDocument graph=new ParametricSketchDocument(new PrimitiveKernel());
  String base=graph.addRectangleOnPlane(PlaneExtrusion.Plane.XZ,1,2,10,4);
  CadKernel.Result<CadKernel.Box> a=graph.extrude(base,5,3);
  CadKernel.Result<CadKernel.Box> b=graph.extrude(base,8,-2);
  check(a.isSuccess()&&b.isSuccess(),"Multiple dependent extrusions");
  check(graph.dependentsOf(base).size()==2,"Dependency lookup");
  check(graph.updateRectangle(base,1,2,20,4),"Rebuild shared sketch");
  check(graph.bodies().get(a.value.id).volume()==240,"First dependent rebuilt");
  check(graph.bodies().get(b.value.id).volume()==160,"Second dependent rebuilt");
  check(graph.removeExtrusion(graph.dependentsOf(base).get(0).id),"Remove one dependent");
  check(graph.dependentsOf(base).size()==1,"Dependency removal");
  check(graph.undo()&&graph.dependentsOf(base).size()==2,"Undo dependencies");
  String encodedGraph=graph.exportSnapshot();
  String oldFormat=encodedGraph.replace("|XZ\n","\n");
  ParametricSketchDocument legacy=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),oldFormat);
  check(legacy.sketches().get(base).plane==PlaneExtrusion.Plane.XY,"Legacy plane defaults XY");
  check(legacy.bodies().size()==2,"Legacy bodies loaded");
  boolean brokenRef=false;
  try{ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),encodedGraph.replace("|"+base+"|"+a.value.id+"|", "|00000000-0000-0000-0000-000000000001|"+a.value.id+"|"));}catch(IllegalArgumentException expected){brokenRef=true;}
  check(brokenRef,"Dangling feature reference rejected");
  boolean duplicateRejected=false;
  try{ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),encodedGraph+encodedGraph.split("\n")[1]+"\n");}catch(IllegalArgumentException expected){duplicateRejected=true;}
  check(duplicateRejected,"Duplicated sketch rejected");
  ParametricSketchDocument transforms=new ParametricSketchDocument(new PrimitiveKernel());
  String moving=transforms.addRectangle(1,2,4,5);
  CadKernel.Result<CadKernel.Box> movingBody=transforms.extrude(moving,7,3);
  check(movingBody.isSuccess(),"Create movable body");
  String movingId=movingBody.value.id;
  String movingFeature=transforms.dependentsOf(moving).get(0).id;
  check(transforms.setExtrusionOffset(movingFeature,11),"Move extrusion offset");
  check(transforms.bodies().get(movingId).z==11,"Offset rebuilt");
  check(transforms.setSketchPlane(moving,PlaneExtrusion.Plane.YZ),"Change sketch plane");
  check(transforms.bodies().get(movingId).x==11,"YZ origin follows offset");
  check(transforms.bodies().get(movingId).depth==4,"YZ sketch U maps to Y");
  check(!transforms.setExtrusionOffset(movingFeature,Double.NaN),"Reject invalid offset");
  check(!transforms.setSketchPlane(moving,null),"Reject null plane");
  check(transforms.bodies().get(movingId).x==11,"Invalid operations atomic");
  check(transforms.undo()&&transforms.bodies().get(movingId).z==11,"Undo sketch plane change");
  check(transforms.redo()&&transforms.bodies().get(movingId).x==11,"Redo sketch plane change");
  ParametricSketchDocument reopenedTransforms=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),transforms.exportSnapshot());
  check(reopenedTransforms.bodies().get(movingId).x==11,"Persist moved sketch plane");
  check(transforms.validateIntegrity().isEmpty(),"Edited parametric model integrity");
  check(graph.validateIntegrity().isEmpty(),"Shared sketch graph integrity");
  check(reopenedTransforms.validateIntegrity().isEmpty(),"Imported document integrity");
  boolean integrityImmutable=false;
  try{transforms.validateIntegrity().add("corrupted");}catch(UnsupportedOperationException expected){integrityImmutable=true;}
  check(integrityImmutable,"Integrity result immutable");
  System.out.println("ParametricSketchDocumentTest: OK");
 }
}
