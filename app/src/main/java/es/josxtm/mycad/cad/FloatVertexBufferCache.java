package es.josxtm.mycad.cad;

import java.nio.*;
import java.util.*;

/**
 * Direct GPU-ready triangle buffers cached by scene mesh identity.
 *
 * A Mesh's XYZ float array is treated as immutable while it is displayed.
 * Renderer obtains a new view (independent position/limit) over the same
 * direct allocation for each frame, avoiding repeated native allocations.
 * Call clear() when preview/scene contents change.
 *
 * Pure Java, no Android or GL dependency: behavior is unit-testable in CI.
 */
public final class FloatVertexBufferCache {
 private static final int MAX_FLOATS=9_000_000;
 private static final class Record {
  final float[] source;
  final FloatBuffer buffer;
  Record(float[] array,FloatBuffer buffer){this.source=array;this.buffer=buffer;}
 }
 private final IdentityHashMap<Object,Record> records=new IdentityHashMap<>();
 private long bytes=0;

 public FloatBuffer triangles(Object owner,float[] coords){
  if(owner==null||coords==null||coords.length%9!=0||coords.length>MAX_FLOATS)
   throw new IllegalArgumentException("Invalid GPU triangle buffer");
  Record existing=records.get(owner);
  if(existing==null||existing.source!=coords){
   for(float f:coords)if(!Float.isFinite(f))
    throw new IllegalArgumentException("Nonfinite GPU geometry");
   long newBytes=4L*coords.length;
   long oldBytes=existing==null?0:4L*existing.source.length;
   if(bytes-oldBytes+newBytes>4L*MAX_FLOATS)
    throw new IllegalArgumentException("Scene buffer cache limit exceeded");
   FloatBuffer buffer=ByteBuffer.allocateDirect(coords.length*4)
     .order(ByteOrder.nativeOrder()).asFloatBuffer();
   buffer.put(coords);
   buffer.position(0);
   records.put(owner,new Record(coords,buffer));
   bytes=bytes-oldBytes+newBytes;
   existing=records.get(owner);
  }
  FloatBuffer view=existing.buffer.duplicate();
  view.position(0);
  return view;
 }
 public long residentBytes(){return bytes;}
 public int entries(){return records.size();}
 public void clear(){records.clear();bytes=0;}
}
