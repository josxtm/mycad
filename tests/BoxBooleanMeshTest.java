import es.josxtm.mycad.BoxBoolean;
import es.josxtm.mycad.BoxBooleanMesh;
public class BoxBooleanMeshTest {
 static double signedVolume(float[] v){
  double total=0;
  for(int i=0;i<v.length;i+=9){
   double ax=v[i],ay=v[i+1],az=v[i+2],bx=v[i+3],by=v[i+4],bz=v[i+5],cx=v[i+6],cy=v[i+7],cz=v[i+8];
   total+=ax*(by*cz-bz*cy)+ay*(bz*cx-bx*cz)+az*(bx*cy-by*cx);
  }
  return total/6;
 }
 static void near(double actual,double expected){
  if(Math.abs(actual-expected)>0.0001)throw new AssertionError("Expected "+expected+" got "+actual);
 }
 public static void main(String[] args){
  BoxBoolean.Box a=new BoxBoolean.Box(0,0,0,10,10,10);
  BoxBoolean.Box b=new BoxBoolean.Box(5,0,0,10,10,10);
  near(signedVolume(BoxBooleanMesh.build(a,b,BoxBooleanMesh.Operation.UNION)),1500);
  near(signedVolume(BoxBooleanMesh.build(a,b,BoxBooleanMesh.Operation.INTERSECTION)),500);
  near(signedVolume(BoxBooleanMesh.build(a,b,BoxBooleanMesh.Operation.DIFFERENCE)),500);
  BoxBoolean.Box far=new BoxBoolean.Box(30,0,0,10,10,10);
  near(BoxBooleanMesh.build(a,far,BoxBooleanMesh.Operation.INTERSECTION).length,0);
  near(signedVolume(BoxBooleanMesh.build(a,far,BoxBooleanMesh.Operation.UNION)),2000);
  System.out.println("Boolean mesh volumes and triangle winding OK");
 }
}
