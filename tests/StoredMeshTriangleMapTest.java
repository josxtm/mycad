import es.josxtm.mycad.cad.StoredMeshTriangleMap;
public final class StoredMeshTriangleMapTest {
 private static void check(boolean yes,String label){
  if(!yes)throw new AssertionError(label);
 }
 private static void invalid(Runnable action,String label){
  try{action.run();}catch(IllegalArgumentException pass){return;}
  throw new AssertionError("Accepted invalid layout: "+label);
 }
 public static void main(String[] args){
  int[] meshes={12,0,48,4};
  StoredMeshTriangleMap.Hit i=StoredMeshTriangleMap.resolve(2,7,meshes);
  check(i!=null&&i.kind==StoredMeshTriangleMap.Kind.IMPORTED&&
        i.meshIndex==-1&&i.localTriangle==2&&i.count==7,"imported prefix");
  check(StoredMeshTriangleMap.resolve(6,7,meshes).kind==
        StoredMeshTriangleMap.Kind.IMPORTED,"last imported");
  StoredMeshTriangleMap.Hit first=StoredMeshTriangleMap.resolve(7,7,meshes);
  check(first!=null&&first.kind==StoredMeshTriangleMap.Kind.STORED&&
        first.meshIndex==0&&first.localTriangle==0&&first.firstTriangle==7,
        "first stored mesh");
  check(StoredMeshTriangleMap.resolve(18,7,meshes).localTriangle==11,"last of first mesh");
  StoredMeshTriangleMap.Hit second=StoredMeshTriangleMap.resolve(19,7,meshes);
  check(second!=null&&second.meshIndex==2&&second.localTriangle==0,
        "skip empty mesh slot");
  StoredMeshTriangleMap.Hit last=StoredMeshTriangleMap.resolve(70,7,meshes);
  check(last!=null&&last.meshIndex==3&&last.localTriangle==3,
        "last stored triangle");
  check(StoredMeshTriangleMap.resolve(71,7,meshes)==null,
        "primitive triangles must not masquerade as stored mesh");
  check(StoredMeshTriangleMap.resolve(-1,7,meshes)==null,"invalid negative hit");
  check(StoredMeshTriangleMap.resolve(0,0,new int[]{0,0})==null,"empty scene");
  check(StoredMeshTriangleMap.resolve(0,0,new int[]{12}).meshIndex==0,
        "no imported mesh");
  invalid(()->StoredMeshTriangleMap.resolve(1,-1,new int[]{2}),"negative import count");
  invalid(()->StoredMeshTriangleMap.resolve(1,0,new int[]{-2}),"negative mesh count");
  invalid(()->StoredMeshTriangleMap.resolve(1,0,null),"missing stored counts");
  invalid(()->StoredMeshTriangleMap.resolve(1,Integer.MAX_VALUE,new int[]{3}),
          "integer overflow");
  System.out.println("StoredMeshTriangleMapTest: OK");
 }
}
