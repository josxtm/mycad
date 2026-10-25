package es.josxtm.mycad.cad;
/** Pure Java mapping between flattened GPU triangles and chained extrusion steps. */
public final class ChainTriangleMap {
 private ChainTriangleMap(){}
 public static final class Hit {
  public final int chainIndex,stepIndex,localTriangle;
  Hit(int c,int s,int t){chainIndex=c;stepIndex=s;localTriangle=t;}
 }
 public static Hit resolve(int triangle,int precedingTriangles,int[] stepCounts){
  if(triangle<0||precedingTriangles<0||stepCounts==null)return null;
  long first=precedingTriangles;
  for(int i=0;i<stepCounts.length;i++){
   if(stepCounts[i]<0||stepCounts[i]>50)throw new IllegalArgumentException("Invalid chain");
   long count=stepCounts[i]*12L;
   if(triangle>=first&&triangle<first+count){
    int local=(int)(triangle-first);
    return new Hit(i,local/12,local%12);
   }
   first+=count;
  }
  return null;
 }
}