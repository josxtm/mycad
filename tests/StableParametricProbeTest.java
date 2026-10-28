import es.josxtm.mycad.cad.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class StableParametricProbeTest {
 static String id(String name){return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();}
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 static CadIntegrationProbe.PartData part(String name,double height){return new CadIntegrationProbe.PartData(id(name),id("sketch"),height,10,20,30,20,30,40);}
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1}){
   List<CadIntegrationProbe.SketchData> sketches=Collections.singletonList(new CadIntegrationProbe.SketchData(id("sketch"),2,plane,0,0,40,30));
   List<CadIntegrationProbe.PartData> bodies=Arrays.asList(part("a",sign*10),part("b",sign*20));
   CadIntegrationProbe.Report first=CadIntegrationProbe.inspect(sketches,bodies),same=CadIntegrationProbe.inspect(sketches,bodies);
   check(first.parametricSnapshot.equals(same.parametricSnapshot),"Volatile serialized UUID");check(first.generatedBodyIds.equals(Arrays.asList(id("a"),id("b"))),"Lost actual owners");
   for(int i=0;i<first.worldBounds.size();i++)check(Arrays.equals(first.worldBounds.get(i),same.worldBounds.get(i)),"Geometry drift");
   ParametricSketchDocument model=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),first.parametricSnapshot);
   check(model.sketches().containsKey(id("sketch")),"Lost source sketch UUID");
   CadIntegrationProbe.Report edited=CadIntegrationProbe.inspect(sketches,Arrays.asList(part("a",sign*12),part("b",sign*22)));
   ParametricSketchDocument next=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),edited.parametricSnapshot);
   check(model.extrusions().keySet().equals(next.extrusions().keySet()),"Feature identities changed after dimension edit");
   check(model.bodies().keySet().equals(next.bodies().keySet()),"Body identities changed after dimension edit");
   CadIntegrationProbe.Report reordered=CadIntegrationProbe.inspect(sketches,Arrays.asList(part("b",sign*20),part("a",sign*10)));
   check(reordered.generatedBodyIds.equals(Arrays.asList(id("b"),id("a"))),"Reordering reassigned identities");
   check(ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),reordered.parametricSnapshot).extrusions().keySet().equals(model.extrusions().keySet()),"Order-based feature identity");
   CadIntegrationProbe.Report duplicate=CadIntegrationProbe.inspect(sketches,Arrays.asList(part("a",sign*10),part("a",sign*20)));
   check(duplicate.compatibleExtrusions==1&&duplicate.invalidExtrusions==1&&!duplicate.readyForMigration(),"Duplicate owner not rejected");
  }
  List<CadIntegrationProbe.SketchData> legacy=Collections.singletonList(new CadIntegrationProbe.SketchData("old-sketch",2,0,0,0,20,10));
  List<CadIntegrationProbe.PartData> oldBodies=Arrays.asList(new CadIntegrationProbe.PartData("old-sketch",5),new CadIntegrationProbe.PartData("old-sketch",5));
  CadIntegrationProbe.Report old=CadIntegrationProbe.inspect(legacy,oldBodies);
  check(old.parametricSnapshot.equals(CadIntegrationProbe.inspect(legacy,oldBodies).parametricSnapshot),"Legacy probe not reproducible");
  check(!old.generatedBodyIds.get(0).equals(old.generatedBodyIds.get(1)),"Legacy rows aliased");
  ParametricSketchDocument doc=new ParametricSketchDocument(new PrimitiveKernel());
  check(doc.addRectangleWithId(id("s"),PlaneExtrusion.Plane.XY,0,0,40,30).equals(id("s")),"Explicit sketch");
  check(doc.extrudeWithIds(id("s"),id("f"),id("body"),0,10).isSuccess(),"Explicit extrusion");String before=doc.exportSnapshot();
  check(doc.addRectangleWithId(id("s"),PlaneExtrusion.Plane.XY,0,0,50,50)==null,"Overwritten sketch");
  check(!doc.extrudeWithIds(id("s"),id("f2"),id("body"),0,20).isSuccess(),"Overwritten body");
  check(!doc.extrudeWithIds(id("s"),id("f"),id("body2"),0,20).isSuccess(),"Overwritten feature");
  check(!doc.extrudeWithIds(id("s"),"bad",id("body2"),0,20).isSuccess(),"Malformed feature UUID");
  check(!doc.extrudeWithIds(id("s"),id("f2"),id("body2"),0,0).isSuccess(),"Invalid geometry accepted");
  check(doc.exportSnapshot().equals(before),"Failed reconstruction committed a revision");
  check(doc.undo()&&doc.bodies().isEmpty()&&doc.redo()&&doc.exportSnapshot().equals(before),"Undo/redo identities");
  check(doc.setExtrusionHeight(id("f"),12)&&doc.bodies().containsKey(id("body")),"Explicit feature cannot be edited");
  check(ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),doc.exportSnapshot()).validateIntegrity().isEmpty(),"Roundtrip");
  System.out.println("StableParametricProbeTest: OK");
 }
}
