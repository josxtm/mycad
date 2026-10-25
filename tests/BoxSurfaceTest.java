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
  boolean invalid=false;
  try{BoxSurface.triangles(null);}catch(IllegalArgumentException ok){invalid=true;}
  check(invalid,"Reject missing solid");
  System.out.println("BoxSurfaceTest: OK");
 }
}
