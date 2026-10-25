import es.josxtm.mycad.cad.*;
import java.util.Arrays;
public final class TriangleMeshHealthTest {
 static void require(boolean yes,String label){if(!yes)throw new AssertionError(label);}
 public static void main(String[] args){
  RigidTransform pose=new RigidTransform(10,20,-4,30,-25,15);
  float[] cylinder=CylinderSurface.triangles(8,20,pose,64);
  TriangleMeshHealth.Report a=TriangleMeshHealth.inspect(cylinder);
  require(a.isWatertight(),"Closed transformed cylinder: edges "+a.boundaryEdges+" reversed "+a.reversedEdges);
  for(OrientedThroughHole.Axis axis:OrientedThroughHole.Axis.values()){
   float[] mesh=OrientedThroughHole.triangles(30,25,20,2,-2,7,axis,pose);
   TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(mesh);
   require(report.isWatertight(),"Watertight oriented bore "+axis+" edges="+report.boundaryEdges+
     " reversed="+report.reversedEdges+" nonmanifold="+report.nonManifoldEdges+
     " degenerate="+report.degenerateTriangles);
   require(report.signedVolume>0,"Positive orientation "+axis);
  }
  float[] open=Arrays.copyOf(cylinder,cylinder.length-9);
  require(!TriangleMeshHealth.inspect(open).isWatertight(),"Open shell accepted");
  float[] reversed=cylinder.clone();
  for(int k=0;k<3;k++){float t=reversed[3+k];reversed[3+k]=reversed[6+k];reversed[6+k]=t;}
  require(!TriangleMeshHealth.inspect(reversed).isWatertight(),"Reversed face accepted");
  float[] duplicated=Arrays.copyOf(cylinder,cylinder.length+9);
  System.arraycopy(cylinder,0,duplicated,cylinder.length,9);
  require(TriangleMeshHealth.inspect(duplicated).nonManifoldEdges>0,"Duplicated triangle accepted");
  System.out.println("TriangleMeshHealthTest: OK");
 }
}
