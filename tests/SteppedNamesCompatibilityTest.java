import es.josxtm.mycad.cad.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Frozen digest of every vertex/edge/face/patch name from the 12 fixtures
 * published in b74c4e7. Refactoring naming must preserve the v1 format. */
public final class SteppedNamesCompatibilityTest {
 public static void main(String[] args)throws Exception{
  var fixture=NamedSteppedBodyTest.class.getDeclaredMethod("body",String.class,int.class,int.class,int.class,double.class);
  fixture.setAccessible(true);List<String> names=new ArrayList<>();
  for(int plane=0;plane<3;plane++)for(int sign:new int[]{-1,1})for(int shape=0;shape<2;shape++){
   NamedSteppedBody body=(NamedSteppedBody)fixture.invoke(null,"owner",plane,sign,6+shape,4.0+shape);
   for(var v:body.shell.vertices())names.add(body.vertexId(v.id));
   for(var e:body.shell.edges())names.add(body.edgeId(e.id));
   for(var f:body.shell.faces())names.add(body.faceId(f.id));
   for(var p:SteppedCoplanarPatchTopology.build(body.shell).patches)names.add(body.patchId(p));
  }
  Collections.sort(names);byte[] hash=MessageDigest.getInstance("SHA-256").digest(String.join("\n",names).getBytes(StandardCharsets.UTF_8));
  StringBuilder digest=new StringBuilder();for(byte b:hash)digest.append(String.format(Locale.ROOT,"%02x",b));
  if(!digest.toString().equals("00bb629b7298b2fa0ea3b4584f6d691ccd9f2fda8e3189f7b5778c9f0d6629c5"))
   throw new AssertionError("Published stepped v1 identities changed: "+digest);
  System.out.println("SteppedNamesCompatibilityTest: OK");
 }
}
