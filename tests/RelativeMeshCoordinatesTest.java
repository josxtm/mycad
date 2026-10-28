import es.josxtm.mycad.cad.RelativeMeshCoordinates;
public class RelativeMeshCoordinatesTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static void fails(double[] data,double x,double y,double z){
  try{RelativeMeshCoordinates.relative(data,x,y,z);throw new AssertionError("Expected rejection");}
  catch(IllegalArgumentException expected){}
 }
 public static void main(String[] args){
  double origin=1000000000.0;
  double[] triangle={origin,origin,origin,origin+.01,origin,origin,origin,origin+.01,origin};
  double[] copy=triangle.clone();
  float[] local=RelativeMeshCoordinates.relative(triangle,origin,origin,origin);
  check(local.length==9,"triangle size");
  check(local[3]>.009f&&local[3]<.011f,"x detail retained");
  check(local[7]>.009f&&local[7]<.011f,"y detail retained");
  check(java.util.Arrays.equals(triangle,copy),"source unchanged");
  check((float)(origin+.01)==(float)origin,"regression fixture must lose world float precision");
  fails(triangle,Double.NaN,0,0);
  fails(new double[]{Double.POSITIVE_INFINITY,0,0,0,0,0,0,0,0},0,0,0);
  fails(new double[]{1,2,3},0,0,0);
  fails(new double[]{Double.MAX_VALUE,0,0,0,0,0,0,0,0},-Double.MAX_VALUE,0,0);
  System.out.println("RelativeMeshCoordinatesTest OK");
 }
}
