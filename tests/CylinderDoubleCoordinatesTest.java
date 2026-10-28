import es.josxtm.mycad.cad.*;
import java.security.MessageDigest;
import java.util.*;

public final class CylinderDoubleCoordinatesTest {
 private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
 private static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Invalid cylinder accepted");}
 public static void main(String[] args)throws Exception{
  // Frozen from the parent implementation before introducing the double path.
  MessageDigest hash=MessageDigest.getInstance("SHA-256");
  for(int n:new int[]{12,48,128})for(int sign:new int[]{-1,1}){
   RigidTransform pose=new RigidTransform(sign*8123.25,-7041.125,34.5,23,47,91);
   float[] legacy=CylinderSurface.triangles(13.25,7.125,pose,n);
   double[] exact=CylinderSurface.triangleCoordinates(13.25,7.125,pose,n);
   check(legacy.length==exact.length&&legacy.length==n*36,"Tessellation count");
   for(int i=0;i<legacy.length;i++){
    check((float)exact[i]==legacy[i],"Triangle order/rounding");
    int bits=Float.floatToIntBits(legacy[i]);for(int b=0;b<4;b++)hash.update((byte)(bits>>>(8*b)));
   }
   PrimitiveBRepBody primitive=PrimitiveBRepBody.cylinder("cylinder",13.25,7.125,pose);
   check(Arrays.equals(exact,primitive.triangleCoordinates(n)),"Primitive bridge changed coordinates");
   check(Arrays.equals(exact,CylinderBRep.from("cylinder",13.25,7.125,pose).triangleCoordinates(n)),"B-Rep bridge changed coordinates");
  }
  StringBuilder digest=new StringBuilder();for(byte b:hash.digest())digest.append(String.format(Locale.ROOT,"%02x",b));
  check(digest.toString().equals("efa2199fc0211246566d29720d693f0835f387152b8497962a524bedba428c73"),"Legacy mesh compatibility");
  for(double far:new double[]{-1e9,1e9}){
   double[] world=CylinderSurface.triangleCoordinates(.01,.02,new RigidTransform(far,far,far,0,0,0),48);
   float[] local=RelativeSceneCoordinates.from(Collections.singletonList(world)).triangles(0);
   double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;
   for(int i=0;i<local.length;i+=3){lo=Math.min(lo,local[i]);hi=Math.max(hi,local[i]);}
   check(Math.abs((hi-lo)-.01)<1e-6,"Lost circular detail at distant origin");
   check(TriangleMeshHealth.inspect(local).isWatertight(),"Local cylinder collapsed");
   double keep=world[0];local[0]++;check(world[0]==keep,"Mutated analytic geometry");
  }
  reject(()->CylinderSurface.triangleCoordinates(0,2,new RigidTransform(0,0,0,0,0,0),48));
  reject(()->CylinderSurface.triangleCoordinates(2,2,null,48));
  reject(()->CylinderSurface.triangleCoordinates(2,2,new RigidTransform(0,0,0,0,0,0),3));
  System.out.println("CylinderDoubleCoordinatesTest: OK");
 }
}
