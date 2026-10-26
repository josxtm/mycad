import es.josxtm.mycad.cad.FloatVertexBufferCache;
import java.nio.FloatBuffer;

public final class FloatVertexBufferCacheTest {
 static void check(boolean condition,String reason){
  if(!condition)throw new AssertionError(reason);
 }
 static void throwsInvalid(Runnable r,String reason){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Accepted invalid geometry "+reason);
 }
 public static void main(String[] args){
  FloatVertexBufferCache cache=new FloatVertexBufferCache();
  float[] tri={0,0,0, 1,0,0, 0,1,0};
  Object mesh=new Object();
  FloatBuffer first=cache.triangles(mesh,tri);
  check(first.isDirect(),"GPU needs direct memory");
  check(first.capacity()==9&&first.remaining()==9,"Correct initial position");
  check(cache.entries()==1&&cache.residentBytes()==36,"One 9-float native allocation");
  first.position(7);
  FloatBuffer second=cache.triangles(mesh,tri);
  check(second.position()==0&&second.remaining()==9,
    "Each frame receives an independent buffer cursor");
  check(first.position()==7,"Cursor is not shared with another frame");
  check(second.get(3)==1f,"Preserves XYZ contents");
  tri[3]=8f;
  check(cache.triangles(mesh,tri).get(3)==1f,
    "Stored native data not corrupted by external source writes");
  // Array identity changes when mesh is replaced, even with the same owner.
  float[] replacement={0,0,0, 8,0,0, 0,1,0};
  check(cache.triangles(mesh,replacement).get(3)==8f,
    "New source identity refreshes prepared buffer");
  check(cache.entries()==1&&cache.residentBytes()==36,
    "Replacing scene mesh does not leak buffers");
  Object other=new Object();
  cache.triangles(other,new float[]{0,0,0, 1,0,0, 0,0,1});
  check(cache.entries()==2&&cache.residentBytes()==72,
    "Distinct simultaneous meshes have distinct buffers");
  cache.clear();
  check(cache.entries()==0&&cache.residentBytes()==0,
    "Scene replacement drops all direct buffer references");
  check(cache.triangles(mesh,replacement).get(3)==8f,
    "Cache reusable after scene reset");
  throwsInvalid(()->cache.triangles(null,tri),"Null owner");
  throwsInvalid(()->cache.triangles(new Object(),null),"Null floats");
  throwsInvalid(()->cache.triangles(new Object(),new float[10]),"Not triangle aligned");
  throwsInvalid(()->cache.triangles(new Object(),
    new float[]{0,0,0,1,0,0,Float.NaN,1,0}),"Nonfinite vertices");
  check(cache.entries()==1&&cache.residentBytes()==36,
    "Rejected geometry never changes cache");
  System.out.println("FloatVertexBufferCacheTest: OK");
 }
}
