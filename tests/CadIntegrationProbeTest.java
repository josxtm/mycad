import es.josxtm.mycad.cad.*;
import java.util.*;
public final class CadIntegrationProbeTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args){
  List<CadIntegrationProbe.SketchData> sketches=Arrays.asList(
   new CadIntegrationProbe.SketchData("s1",2,1,0,0,20,10),
   new CadIntegrationProbe.SketchData("circle",3,0,0,0,10,10));
  List<CadIntegrationProbe.PartData> parts=Arrays.asList(
   new CadIntegrationProbe.PartData("s1",5),
   new CadIntegrationProbe.PartData("s1",-3),
   new CadIntegrationProbe.PartData("missing",2),
   new CadIntegrationProbe.PartData("",10));
  CadIntegrationProbe.Report r=CadIntegrationProbe.inspect(sketches,parts);
  check(r.compatibleSketches==1&&r.compatibleExtrusions==2,"Linked rectangle conversion");
  check(r.unsupportedSketches==1&&r.invalidExtrusions==1&&r.unlinkedParts==1,"Incompatible counts");
  check(!r.readyForMigration(),"Migration cannot be automatic");
  CadIntegrationProbe.Report clean=CadIntegrationProbe.inspect(
   Arrays.asList(new CadIntegrationProbe.SketchData("s",2,2,0,0,2,3)),
   Arrays.asList(new CadIntegrationProbe.PartData("s",4)));
  check(clean.readyForMigration()&&clean.compatibleExtrusions==1,"Clean probe");
  ParametricSketchDocument preview=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),r.parametricSnapshot);
  check(preview.bodies().size()==2&&preview.validateIntegrity().isEmpty(),"Partial preview roundtrip");
  check(ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),clean.parametricSnapshot).bodies().size()==1,"Full preview roundtrip");
  check(clean.parametricSnapshot.startsWith("MYCAD-PARAMETRIC|1"),"Versioned preview");
  check(r.compatiblePartIndices.size()==2&&r.compatiblePartIndices.get(0)==0&&r.compatiblePartIndices.get(1)==1,"Source indices");
  check(r.generatedBodyIds.size()==2&&preview.bodies().containsKey(r.generatedBodyIds.get(0)),"Source to solid binding");
  check(!r.generatedBodyIds.get(0).equals(r.generatedBodyIds.get(1)),"Unique generated solid IDs");
  check(clean.compatiblePartIndices.size()==1&&clean.compatiblePartIndices.get(0)==0,"Clean source index");
  check(r.worldBounds.size()==2,"World bounds aligned");
  CadIntegrationProbe.Report moved=CadIntegrationProbe.inspect(
   Arrays.asList(new CadIntegrationProbe.SketchData("m",2,0,0,0,2,4)),
   Arrays.asList(new CadIntegrationProbe.PartData("m",6,10,20,30,0,0,90)));
  check(moved.worldBounds.size()==1,"Transformed preview");
  double[] bounds=moved.worldBounds.get(0);
  check(Math.abs(bounds[0]-6)<1e-8&&Math.abs(bounds[3]-10)<1e-8,"Rotated X bounds");
  check(Math.abs(bounds[1]-20)<1e-8&&Math.abs(bounds[4]-22)<1e-8,"Rotated Y bounds");
  System.out.println("CadIntegrationProbeTest: OK");
 }
}
