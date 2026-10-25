import es.josxtm.mycad.cad.*;
import java.util.*;
public final class LinkedSketchAuditTest {
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  LinkedSketchAudit.Sketch rect=new LinkedSketchAudit.Sketch("s-rect",2,0,0,0,20,10);
  LinkedSketchAudit.Sketch round=new LinkedSketchAudit.Sketch("s-circle",3,1,3,7,8,7);
  LinkedSketchSolidPlan.Body a=LinkedSketchSolidPlan.build(2,0,0,0,20,10,5);
  LinkedSketchSolidPlan.Body b=LinkedSketchSolidPlan.build(3,1,3,7,8,7,10);
  LinkedSketchAudit.Body linkedA=new LinkedSketchAudit.Body("p-rect","s-rect",a.circle,
    a.w,a.d,a.h,a.x,a.y,a.z,a.rx,a.ry,a.rz);
  LinkedSketchAudit.Body linkedB=new LinkedSketchAudit.Body("p-circle","s-circle",b.circle,
    b.w,b.d,b.h,b.x,b.y,b.z,b.rx,b.ry,b.rz);
  LinkedSketchAudit.Body free=new LinkedSketchAudit.Body("p-free","",false,10,10,10,0,0,0,0,0,0);
  List<LinkedSketchAudit.Sketch> sketches=Arrays.asList(rect,round);
  LinkedSketchAudit.Report good=LinkedSketchAudit.inspect(sketches,Arrays.asList(linkedA,linkedB,free));
  ok(good.isValid()&&good.errors==0&&good.warnings==0,"Linked circle/rectangle and independent body");
  ok(good.sketches==2&&good.linkedBodies==2&&good.independentBodies==1,"Count links");
  LinkedSketchAudit.Body moved=new LinkedSketchAudit.Body("p-moved","s-rect",false,
    a.w,a.d,a.h,a.x+2,a.y,a.z,a.rx,a.ry,a.rz);
  LinkedSketchAudit.Report warning=LinkedSketchAudit.inspect(sketches,Collections.singletonList(moved));
  ok(warning.isValid()&&warning.warnings==1,"Moved dependent geometry warning");
  LinkedSketchAudit.Body orphan=new LinkedSketchAudit.Body("p-orphan","missing",false,
    20,10,5,10,5,0,0,0,0);
  ok(!LinkedSketchAudit.inspect(sketches,Collections.singletonList(orphan)).isValid(),
    "Dangling source must fail");
  ok(!LinkedSketchAudit.inspect(Arrays.asList(rect,rect),Collections.emptyList()).isValid(),
    "Duplicate sketch ID");
  ok(!LinkedSketchAudit.inspect(sketches,Arrays.asList(linkedA,linkedA)).isValid(),
    "Duplicate body ID");
  LinkedSketchAudit.Body badHeight=new LinkedSketchAudit.Body("p-bad","s-rect",false,
    20,10,-5,10,5,0,0,0,0);
  ok(!LinkedSketchAudit.inspect(sketches,Collections.singletonList(badHeight)).isValid(),
    "Invalid linked extrusion height");
  System.out.println("LinkedSketchAuditTest: OK");
 }
}