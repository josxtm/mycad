import es.josxtm.mycad.cad.ChainTriangleMap;
public final class ChainTriangleMapTest {
 static void hit(ChainTriangleMap.Hit h,int c,int s,int t){if(h==null||h.chainIndex!=c||h.stepIndex!=s||h.localTriangle!=t)throw new AssertionError("Wrong hit");}
 public static void main(String[] args){
  int[] counts={2,0,3};
  if(ChainTriangleMap.resolve(99,100,counts)!=null)throw new AssertionError("Before");
  hit(ChainTriangleMap.resolve(100,100,counts),0,0,0);
  hit(ChainTriangleMap.resolve(123,100,counts),0,1,11);
  hit(ChainTriangleMap.resolve(124,100,counts),2,0,0);
  hit(ChainTriangleMap.resolve(159,100,counts),2,2,11);
  if(ChainTriangleMap.resolve(160,100,counts)!=null)throw new AssertionError("After");
  boolean invalid=false;try{ChainTriangleMap.resolve(0,0,new int[]{-1});}catch(IllegalArgumentException e){invalid=true;}
  if(!invalid)throw new AssertionError("Bad steps accepted");
  System.out.println("ChainTriangleMapTest: OK");
 }
}