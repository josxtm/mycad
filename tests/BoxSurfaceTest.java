import es.josxtm.mycad.cad.*;
public final class BoxSurfaceTest {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);}
 public static void main(String[] args){
  CadKernel.Box cube=new PrimitiveKernel().createBox(0,0,0,2,4,6).value;
  TransformedBody body=new TransformedBody(cube,new RigidTransform(10,20,30,0,0,90));
  float[] trianglePositions=BoxSurface.triangles(body);
  check(BoxSurface.triangleCount()==12,"Twelve triangles");
  check(trianglePositions.length==108,"36 vertices");
  double[] bounds=body.worldBounds();
  for(int i=0;i<trianglePositions.length;i+=3)
   for(int axis=0;axis<3;axis++)
    check(trianglePositions[i+axis]>=bounds[axis]-1e-5 &&
          trianglePositions[i+axis]<=bounds[axis+3]+1e-5,"Inside transformed bounds");
  // Verify face winding points away from the transformed solid center.
  double[] center=body.center();
  for(int i=0;i<trianglePositions.length;i+=9){
   double ax=trianglePositions[i],ay=trianglePositions[i+1],az=trianglePositions[i+2];
   double ux=trianglePositions[i+3]-ax,uy=trianglePositions[i+4]-ay,uz=trianglePositions[i+5]-az;
   double vx=trianglePositions[i+6]-ax,vy=trianglePositions[i+7]-ay,vz=trianglePositions[i+8]-az;
   double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
   double mx=(ax+trianglePositions[i+3]+trianglePositions[i+6])/3;
   double my=(ay+trianglePositions[i+4]+trianglePositions[i+7])/3;
   double mz=(az+trianglePositions[i+5]+trianglePositions[i+8])/3;
   check(nx*(mx-center[0])+ny*(my-center[1])+nz*(mz-center[2])>0,"Outward face winding");
  }
  boolean invalid=false;
  try{BoxSurface.triangles(null);}catch(IllegalArgumentException ok){invalid=true;}
  check(invalid,"Reject missing solid");
  System.out.println("BoxSurfaceTest: OK");
 }
}
