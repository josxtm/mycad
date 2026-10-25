import es.josxtm.mycad.cad.*;
public final class PlaneExtrusionTest {
 static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
 static void eq(double actual,double expected,String msg){check(Math.abs(actual-expected)<1e-8,msg+": "+actual);}
 public static void main(String[] args){
  CadKernel kernel=new PrimitiveKernel();
  for(PlaneExtrusion.Plane plane:PlaneExtrusion.Plane.values()){
   CadKernel.Result<CadKernel.Box> r=PlaneExtrusion.extrude(kernel,plane,2,3,7,20,10,5);
   check(r.isSuccess(),plane+" positive");
   eq(r.value.volume(),1000,plane+" volume");
   CadKernel.Result<CadKernel.Box> negative=PlaneExtrusion.extrude(kernel,plane,2,3,7,20,10,-5);
   check(negative.isSuccess(),plane+" negative");
   eq(negative.value.volume(),1000,plane+" negative volume");
   CadKernel.Box b=r.value,n=negative.value;
   if(plane==PlaneExtrusion.Plane.XY){eq(b.z,7,"XY z");eq(n.z,2,"XY negative z");eq(b.width,20,"XY w");eq(b.depth,10,"XY d");}
   if(plane==PlaneExtrusion.Plane.XZ){eq(b.y,7,"XZ y");eq(n.y,2,"XZ negative y");eq(b.depth,5,"XZ thickness");eq(b.height,10,"XZ height");}
   if(plane==PlaneExtrusion.Plane.YZ){eq(b.x,7,"YZ x");eq(n.x,2,"YZ negative x");eq(b.width,5,"YZ thickness");eq(b.depth,20,"YZ depth");}
  }
  check(PlaneExtrusion.extrude(kernel,PlaneExtrusion.Plane.XY,0,0,0,20,10,0).code==CadKernel.Code.INVALID_GEOMETRY,"Zero height");
  check(PlaneExtrusion.extrude(kernel,PlaneExtrusion.Plane.XY,0,0,0,-20,10,5).code==CadKernel.Code.INVALID_GEOMETRY,"Negative size");
  check(PlaneExtrusion.extrude(kernel,PlaneExtrusion.Plane.XY,Double.NaN,0,0,20,10,5).code==CadKernel.Code.INVALID_GEOMETRY,"NaN");
  System.out.println("PlaneExtrusionTest: OK");
 }
}
