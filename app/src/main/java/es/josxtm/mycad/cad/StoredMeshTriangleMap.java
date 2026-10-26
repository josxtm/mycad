package es.josxtm.mycad.cad;

/**
 * Maps the exact world-scene triangle order to an imported mesh or a stored
 * boolean / linked-loop mesh. Meshes precede analytic primitives and chain
 * results in AnalyticSceneMesh; only the prefix is classified here.
 */
public final class StoredMeshTriangleMap {
 private StoredMeshTriangleMap(){}
 public enum Kind { IMPORTED, STORED }
 public static final class Hit {
  public final Kind kind;
  /** -1 for imported geometry, otherwise its index in booleanMeshes. */
  public final int meshIndex;
  public final int localTriangle,firstTriangle,count;
  private Hit(Kind kind,int index,int local,int first,int count){
   this.kind=kind;meshIndex=index;localTriangle=local;
   firstTriangle=first;this.count=count;
  }
 }
 public static Hit resolve(int triangle,int importedTriangles,int[] storedCounts){
  if(importedTriangles<0||storedCounts==null)
   throw new IllegalArgumentException("Escena de mallas inválida");
  long total=importedTriangles;
  for(int count:storedCounts){
   if(count<0)throw new IllegalArgumentException("Recuento de triángulos negativo");
   total+=count;
   if(total>Integer.MAX_VALUE)
    throw new IllegalArgumentException("Escena demasiado grande");
  }
  if(triangle<0||triangle>=total)return null;
  if(triangle<importedTriangles)
   return new Hit(Kind.IMPORTED,-1,triangle,0,importedTriangles);
  int first=importedTriangles;
  for(int index=0;index<storedCounts.length;index++){
   int n=storedCounts[index];
   if(triangle<first+n)
    return new Hit(Kind.STORED,index,triangle-first,first,n);
   first+=n;
  }
  return null;
 }
}
