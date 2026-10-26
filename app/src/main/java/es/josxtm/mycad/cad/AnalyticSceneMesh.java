package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Single, ordered triangle buffer for display, export and live CAD previews.
 * Order: imported/Boolean meshes, analytic primitive bodies, feature meshes.
 *
 * Only the primitive bodies are analytic B-Rep; imported and feature meshes
 * remain triangles. The result is a new, defensively assembled float array.
 */
public final class AnalyticSceneMesh {
 private static final long MAX_FLOATS=9000000L; // 36 MB max raw GPU buffer
 private AnalyticSceneMesh(){}

 private static void accept(List<float[]> chunks,float[] vertices,long[] count){
  if(vertices==null||vertices.length%9!=0)
   throw new IllegalArgumentException("Invalid triangle buffer");
  if(count[0]+vertices.length>MAX_FLOATS)
   throw new IllegalArgumentException("Scene exceeds GPU buffer limit");
  for(float value:vertices)
   if(!Float.isFinite(value))
    throw new IllegalArgumentException("Non-finite scene geometry");
  chunks.add(vertices);
  count[0]+=vertices.length;
 }
 public static float[] compose(List<float[]> before,
                               List<PrimitiveBRepBody> primitives,
                               List<float[]> after,int cylinderSegments){
  if(before==null||primitives==null||after==null||
     cylinderSegments<12||cylinderSegments>128)
   throw new IllegalArgumentException("Invalid scene or tessellation");
  List<float[]> chunks=new ArrayList<>();
  long[] count={0};
  for(float[] vertices:before)accept(chunks,vertices,count);
  Set<String> identifiers=new HashSet<>();
  for(PrimitiveBRepBody body:primitives){
   if(body==null||!identifiers.add(body.bodyId))
    throw new IllegalArgumentException("Missing or duplicated B-Rep identity");
   accept(chunks,body.triangles(cylinderSegments),count);
  }
  for(float[] vertices:after)accept(chunks,vertices,count);
  float[] result=new float[(int)count[0]];
  int offset=0;
  for(float[] chunk:chunks){
   System.arraycopy(chunk,0,result,offset,chunk.length);
   offset+=chunk.length;
  }
  return result;
 }
}
