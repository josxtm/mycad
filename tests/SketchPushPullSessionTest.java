import es.josxtm.mycad.cad.*;

/** Gesture distance -> detached proposal -> solid and parametric document roundtrip.
 * This does not simulate Android MotionEvents or certify physical touch behavior.
 */
public final class SketchPushPullSessionTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void rejects(Runnable operation){
  try{operation.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Invalid extrusion accepted");
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++){
   ParametricSketchDocument doc=new ParametricSketchDocument(new PrimitiveKernel());
   String profile=doc.addRectangleOnPlane(PlaneExtrusion.Plane.values()[plane],0,0,40,30);
   String before=doc.exportSnapshot();
   SketchPushPullSession drag=new SketchPushPullSession(profile,2,plane,0,0,40,30);
   double height=drag.heightForTravel(80);
   LinkedSketchSolidPlan.Body preview=drag.propose(height);
   check(preview.volume()==12000,"40 x 30 x 10 mm preview");
   check(before.equals(doc.exportSnapshot()),"Preview must not persist a body");
   check(drag.heightForTravel(0)==0&&drag.heightForTravel(-80)==0,"Return to origin cancels");
   rejects(()->drag.propose(0));
   check(drag.matches(profile,2,plane,0,0,40,30),"Current source");
   check(!drag.matches(profile,2,plane,0,0,50,30),"Reject changed dimensions");
   check(!drag.matches("other",2,plane,0,0,40,30),"Reject replaced source");
   CadKernel.Result<CadKernel.Box> committed=doc.extrude(profile,0,height);
   check(committed.isSuccess(),"Commit extrusion");
   String bodyId=committed.value.id;
   BoxBRep brep=BoxBRep.from(new TransformedBody(committed.value,new RigidTransform(0,0,0,0,0,0)));
   check(brep.volume()==12000&&TriangleMeshHealth.inspect(brep.triangles()).isWatertight(),"Closed B-Rep");
   check(doc.updateRectangle(profile,0,0,50,30),"Edit dimensions");
   check(doc.bodies().get(bodyId).volume()==15000,"Regenerate dependent solid");
   String saved=doc.exportSnapshot();
   ParametricSketchDocument reopened=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),saved);
   check(reopened.bodies().get(bodyId).volume()==15000,"Reopen geometry");
   check(saved.equals(reopened.exportSnapshot()),"Preserve IDs, dimensions and dependencies");
   check(doc.undo()&&doc.bodies().get(bodyId).volume()==12000,"Undo dimension edit");
   check(doc.undo()&&doc.bodies().isEmpty(),"Undo extrusion, no preview revision");
  }
  SketchPushPullSession circle=new SketchPushPullSession("circle",3,0,0,0,5,0);
  check(Math.abs(circle.propose(10).volume()-250*Math.PI)<1e-6,"Circle preview");
  rejects(()->new SketchPushPullSession("open",1,0,0,0,40,30));
  rejects(()->circle.heightForTravel(Double.NaN));
  rejects(()->circle.propose(10001));
  System.out.println("SketchPushPullSessionTest: OK");
 }
}
