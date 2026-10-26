import es.josxtm.mycad.cad.*;
import java.util.Arrays;
public final class TriangleMeshHealthTest {
 static void require(boolean yes,String label){if(!yes)throw new AssertionError(label);}
 /** Two tetrahedra can touch only at the origin while all their edges are manifold. */
 static float[] tetra(float sign){
  float[][] p={{0,0,0},{sign,0,0},{0,sign,0},{0,0,sign}};
  int[][] faces={{0,2,1},{0,1,3},{0,3,2},{1,2,3}};
  float[] triangles=new float[36];int at=0;
  for(int[] face:faces){
   for(int k=0;k<3;k++){
    int index=face[sign>0?k:(k==1?2:k==2?1:0)];
    for(float v:p[index])triangles[at++]=v;
   }
  }
  return triangles;
 }
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
  float[] first=tetra(1),second=tetra(-1);
  require(TriangleMeshHealth.inspect(first).isWatertight(),"First tetrahedron should be closed");
  require(TriangleMeshHealth.inspect(second).isWatertight(),"Second tetrahedron should be closed");
  float[] pinched=Arrays.copyOf(first,first.length+second.length);
  System.arraycopy(second,0,pinched,first.length,second.length);
  TriangleMeshHealth.Report pinch=TriangleMeshHealth.inspect(pinched);
  require(pinch.boundaryEdges==0&&pinch.nonManifoldEdges==0&&pinch.reversedEdges==0,
    "Pinched shells should pass edge-only checks");
  require(pinch.nonManifoldVertices==1,"One pinched vertex expected: "+pinch.nonManifoldVertices);
  require(!pinch.isWatertight(),"Two tetrahedra touching at one vertex are not a 2-manifold");
  // Disjoint closed components are allowed; touching at a vertex is not.
  float[] separate=pinched.clone();
  for(int k=first.length;k<separate.length;k+=3)separate[k]-=5;
  TriangleMeshHealth.Report disconnected=TriangleMeshHealth.inspect(separate);
  require(disconnected.isWatertight()&&disconnected.nonManifoldVertices==0,
    "Disjoint closed shells should remain valid");
  System.out.println("TriangleMeshHealthTest: OK");
 }
}
