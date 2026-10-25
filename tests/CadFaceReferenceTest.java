import es.josxtm.mycad.cad.*;
public final class CadFaceReferenceTest {
 static void check(boolean v,String msg){if(!v)throw new AssertionError(msg);}
 public static void main(String[] args){
  CadFaceReference a=new CadFaceReference(2,BoxFaceSelection.Face.Z_MAX);
  CadFaceReference b=new CadFaceReference(2,BoxFaceSelection.Face.Z_MAX);
  check(a.equals(b)&&a.hashCode()==b.hashCode(),"Equality");
  check(a.firstLocalTriangle()==2,"Face starts at triangle 2");
  check(a.isValidFor(3)&&!a.isValidFor(2),"Deleted part invalidates index");
  boolean failed=false;try{new CadFaceReference(-1,BoxFaceSelection.Face.X_MAX);}catch(IllegalArgumentException ok){failed=true;}
  check(failed,"Reject negative index");
  System.out.println("CadFaceReferenceTest: OK");
 }
}