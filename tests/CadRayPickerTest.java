import es.josxtm.mycad.cad.*;
public final class CadRayPickerTest {
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
 static void near(double a,double b,String m){
  if(!Double.isFinite(a)||Math.abs(a-b)>1e-6)
   throw new AssertionError(m+": "+a+" != "+b);
 }
 static void rejects(Runnable r,String m){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Expected rejection: "+m);
 }
 public static void main(String[] args){
  CadKernel.Box b=new PrimitiveKernel().createBox(0,0,0,2,4,6).value;
  float[] triangles=BoxSurface.triangles(new TransformedBody(b,new RigidTransform(10,20,30,0,0,90)));
  CadRayPicker.Hit hit=CadRayPicker.pick(triangles,8,21,50,0,0,-1);
  ok(hit!=null&&Math.abs(hit.z-36)<1e-6,"Top surface selection");
  ok(hit.triangle>=0&&hit.triangle<12,"Triangle identity");
  near(hit.distance,14,"Physical hit distance (mm)");
  ok(CadRayPicker.pick(triangles,100,100,50,0,0,-1)==null,"Miss");
  CadRayPicker.Hit far=CadRayPicker.pick(triangles,8,21,50,0,0,-1e300);
  near(far.distance,hit.distance,"Direction scale independent");
  near(far.x,hit.x,"Hit x scale independent");
  near(far.y,hit.y,"Hit y scale independent");
  near(far.z,hit.z,"Hit z scale independent");
  float[] simple={-1,-1,0,1,-1,0,0,1,0};
  float[] damaged={
   Float.NaN,0,0,1,0,0,0,1,0,
   0,0,0,Float.POSITIVE_INFINITY,0,0,0,1,0,
   0,0,0,0,0,0,0,0,0,
   -1,-1,0,1,-1,0,0,1,0
  };
  CadRayPicker.Hit clean=CadRayPicker.pick(simple,0,0,2,0,0,-1);
  near(clean.distance,2,"Simple triangle hit");
  CadRayPicker.Hit recovered=CadRayPicker.pick(damaged,0,0,2,0,0,-1);
  ok(recovered!=null&&recovered.triangle==3,"Skip damaged and degenerate faces");
  near(recovered.distance,2,"Damaged triangle does not corrupt hit distance");
  ok(CadRayPicker.pick(new float[0],0,0,2,0,0,-1)==null,"Empty mesh");
  rejects(()->CadRayPicker.pick(triangles,0,0,0,0,0,0),"Zero ray");
  rejects(()->CadRayPicker.pick(triangles,0,0,0,1e-300,0,0),"Near-zero ray");
  rejects(()->CadRayPicker.pick(triangles,0,0,0,Double.NaN,0,1),"NaN ray");
  rejects(()->CadRayPicker.pick(triangles,0,0,0,Double.POSITIVE_INFINITY,0,1),"Infinite ray");
  rejects(()->CadRayPicker.pick(triangles,Double.NaN,0,0,0,0,1),"NaN origin");
  rejects(()->CadRayPicker.pick(new float[]{0,0,0},0,0,0,0,0,1),"Invalid mesh size");
  System.out.println("CadRayPickerTest: OK");
 }
}
