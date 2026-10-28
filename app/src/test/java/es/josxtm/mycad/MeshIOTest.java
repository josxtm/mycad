package es.josxtm.mycad;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
public class MeshIOTest {
 private MeshEngine.Mesh mesh(){return new MeshEngine.Mesh(new float[]{0,0,0,40,0,0,0,30,0,0,0,10,0,30,10,40,0,10});}
 @Test public void objWritesRealLinesAndReopensWithSameGeometry() throws Exception {
  ByteArrayOutputStream out=new ByteArrayOutputStream();MeshIO.writeObj(mesh(),out);
  String text=new String(out.toByteArray(),StandardCharsets.UTF_8);
  assertFalse(text.contains("\\n"));assertTrue(text.startsWith("# MyCAD OBJ - millimetres\nv "));
  assertArrayEquals(mesh().vertices,MeshIO.read("part.obj",new ByteArrayInputStream(out.toByteArray())).vertices,0);
 }
 @Test public void binaryStlRoundtripPreservesCoordinates() throws Exception {
  ByteArrayOutputStream out=new ByteArrayOutputStream();MeshIO.writeStl(mesh(),out);
  assertEquals(84+mesh().triangles()*50,out.size());
  assertArrayEquals(mesh().vertices,MeshIO.read("part.stl",new ByteArrayInputStream(out.toByteArray())).vertices,0);
 }
 @Test public void externalAsciiStlAndNegativeObjIndicesAreAccepted() throws Exception {
  String stl="solid a\nfacet normal 0 0 1\nouter loop\nvertex 0 0 0\nvertex 40 0 0\nvertex 0 30 0\nendloop\nendfacet\nendsolid a\n";
  assertEquals(1,MeshIO.read("a.stl",new ByteArrayInputStream(stl.getBytes(StandardCharsets.UTF_8))).triangles());
  String obj="v\t0 0 0 # origin\nv 40 0 0\nv 0 30 0\nf\t-3//1 -2//1 -1//1 # triangle\n";
  assertEquals(1,MeshIO.read("a.obj",new ByteArrayInputStream(obj.getBytes(StandardCharsets.UTF_8))).triangles());
 }
 @Test public void nonfiniteBinaryCoordinatesAndEmptyExportsAreRejected() throws Exception {
  ByteArrayOutputStream out=new ByteArrayOutputStream();MeshIO.writeStl(mesh(),out);
  byte[] data=out.toByteArray();ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putFloat(96,Float.NaN);
  try{MeshIO.read("a.stl",new ByteArrayInputStream(data));fail("NaN accepted");}catch(IOException correct){}
  try{MeshIO.writeObj(new MeshEngine.Mesh(new float[0]),new ByteArrayOutputStream());fail("Empty export accepted");}catch(IOException correct){}
 }
}
