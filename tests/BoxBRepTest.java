import es.josxtm.mycad.cad.*;
import java.util.*;
public final class BoxBRepTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void near(double a,double b,String m){
  check(Math.abs(a-b)<=1e-6*Math.max(1,Math.abs(b)),m+" "+a+" "+b);
 }
 static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException ok){return;}
  throw new AssertionError("Invalid solid accepted");}
 public static void main(String[] args){
  CadKernel.Box box=new CadKernel.Box("uuid-box",5,-6,3,20,10,8);
  RigidTransform pose=new RigidTransform(17,-4,26,33,12,91);
  BoxBRep solid=BoxBRep.from(new TransformedBody(box,pose));
  check(solid.vertices().size()==8&&solid.edges().size()==12&&solid.faces().size()==6,
        "Wrong shell topology");
  near(solid.volume(),1600,"Volume");
  near(solid.surfaceArea(),880,"Surface area");
  Set<String> ids=new HashSet<>();
  for(BoxBRep.Edge e:solid.edges()){
   check(ids.add(e.id)&&e.length>0,"Bad edge");
   check(e.adjacentFaceIds().size()==2,"Edge must belong to two faces");
   for(String id:e.adjacentFaceIds())check(solid.findFace(id)!=null,"Orphan face");
   check(solid.findEdge(e.id)==e,"Edge lookup");
  }
  ids.clear();
  for(BoxBRep.Face f:solid.faces()){
   check(ids.add(f.id)&&f.vertexIndices().length==4&&f.edgeIds().size()==4,
         "Bad face loop");
   double[] n=f.outwardNormal();
   near(Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]),1,"Unit normal");
  }
  check(solid.triangles().length==108,"Expected 12 triangles");
  float[] mesh=solid.triangles();mesh[0]=123456;
  check(solid.triangles()[0]!=123456,"Leaked mutable mesh");
  BoxBRep resized=BoxBRep.from(new TransformedBody(
   new CadKernel.Box(box.id,box.x,box.y,box.z,20,10,12),pose));
  near(resized.volume(),2400,"Updated volume");
  for(int i=0;i<12;i++)check(solid.edges().get(i).id.equals(resized.edges().get(i).id),
                             "Edge ID changed");
  for(int i=0;i<6;i++)check(solid.faces().get(i).id.equals(resized.faces().get(i).id),
                             "Face ID changed");
  rejects(()->BoxBRep.from(null));
  rejects(()->BoxBRep.from(new TransformedBody(
   new CadKernel.Box("bad",0,0,0,1,0,1),new RigidTransform(0,0,0,0,0,0))));
  rejects(()->BoxBRep.from(new TransformedBody(
   new CadKernel.Box("",0,0,0,1,1,1),new RigidTransform(0,0,0,0,0,0))));
  try{solid.edges().clear();throw new AssertionError("Mutable shell");}
  catch(UnsupportedOperationException ok){}
  System.out.println("BoxBRepTest: OK");
 }
}
