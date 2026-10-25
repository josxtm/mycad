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
  System.out.println("CadIntegrationProbeTest: OK");
 }
}
