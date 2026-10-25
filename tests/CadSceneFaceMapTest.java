import es.josxtm.mycad.cad.*;
public final class CadSceneFaceMapTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args){
  CadSceneFaceMap map=new CadSceneFaceMap();
  map.addBox(1,7);map.addBox(4,19);
  check(map.resolve(0)==null&&map.resolve(6)==null,"Preceding mesh");
  check(map.resolve(7).partIndex==1,"First body");
  check(map.resolve(8).face==BoxFaceSelection.Face.Z_MIN,"First face second triangle");
  check(map.resolve(18).face==BoxFaceSelection.Face.X_MAX,"First body end");
  check(map.resolve(19).partIndex==4,"Second body");
  check(map.resolve(30).face==BoxFaceSelection.Face.X_MAX,"Second last face");
  check(map.resolve(31)==null,"Out of range");
  System.out.println("CadSceneFaceMapTest: OK");
 }
}
