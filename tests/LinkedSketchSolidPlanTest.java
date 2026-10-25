import es.josxtm.mycad.cad.*;
import java.util.*;
public final class LinkedSketchSolidPlanTest {
 static void eq(double actual,double want,String context){
  if(Math.abs(actual-want)>0.0001)throw new AssertionError(context+": "+actual+" != "+want);
 }
 static void ok(boolean value,String context){if(!value)throw new AssertionError(context);}
 static void bad(Runnable operation,String message){
  try{operation.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError(message);
 }
 public static void main(String[] args){
  for(int plane=0;plane<3;plane++){
   LinkedSketchSolidPlan.Body rectangle=LinkedSketchSolidPlan.build(2,plane,1,4,21,14,5);
   ok(!rectangle.circle,"rectangle shape");
   eq(rectangle.w,20,"rectangle width");eq(rectangle.d,10,"rectangle depth");
   eq(rectangle.volume(),1000,"rectangle volume");
   if(plane==0){eq(rectangle.x,11,"XY X");eq(rectangle.y,9,"XY Y");eq(rectangle.z,0,"XY Z");}
   if(plane==1){eq(rectangle.x,11,"XZ X");eq(rectangle.y,2.5,"XZ extrusion");
                eq(rectangle.z,6.5,"XZ Z");eq(rectangle.rx,-90,"XZ rotation");}
   if(plane==2){eq(rectangle.x,2.5,"YZ X");eq(rectangle.y,11,"YZ Y");
                eq(rectangle.z,6.5,"YZ Z");eq(rectangle.rx,90,"YZ rx");eq(rectangle.rz,90,"YZ rz");}
   LinkedSketchSolidPlan.Body circle=LinkedSketchSolidPlan.build(3,plane,3,7,8,7,12);
   ok(circle.circle,"circle shape");
   eq(circle.w,10,"circle diameter");eq(circle.d,10,"circle depth");
   eq(circle.volume(),Math.PI*25*12,"circle volume");
   eq(circle.y,plane==1?6:plane==2?3:7,"circle position");
   List<LinkedSketchSolidPlan.Body> batch=LinkedSketchSolidPlan.plan(
    2,plane,1,4,21,14,new double[]{5,11,14});
   ok(batch.size()==3,"three dependent extrusions");
   eq(batch.get(0).w,batch.get(2).w,"same profile across dependents");
   eq(batch.get(1).h,11,"per body height");
   boolean immutable=false;try{batch.clear();}catch(UnsupportedOperationException expected){immutable=true;}
   ok(immutable,"Mutable plan");
  }
  bad(()->LinkedSketchSolidPlan.build(2,0,1,1,1,5,10),"zero width rectangle");
  bad(()->LinkedSketchSolidPlan.build(3,0,1,1,1,1,10),"zero circle radius");
  bad(()->LinkedSketchSolidPlan.build(1,0,1,1,10,10,10),"open line extrusion");
  bad(()->LinkedSketchSolidPlan.build(2,3,1,1,10,10,10),"invalid plane");
  bad(()->LinkedSketchSolidPlan.build(2,0,1,1,10,10,0),"zero height");
  bad(()->LinkedSketchSolidPlan.build(2,0,1,1,10,10,Double.NaN),"non-finite height");
  bad(()->LinkedSketchSolidPlan.build(2,0,1,1,10001,10,10),"over-limit geometry");
  bad(()->LinkedSketchSolidPlan.plan(2,0,1,1,10,10,new double[]{5,-2}),
      "one invalid dependent must reject entire plan");
  bad(()->LinkedSketchSolidPlan.plan(3,0,1,1,3,1,new double[]{5,Double.POSITIVE_INFINITY}),
      "invalid circle dependent");
  ok(LinkedSketchSolidPlan.plan(3,0,1,1,3,1,new double[0]).isEmpty(),"valid unused profile");
  System.out.println("LinkedSketchSolidPlanTest: OK");
 }
}