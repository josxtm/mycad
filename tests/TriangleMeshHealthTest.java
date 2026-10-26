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
  // Positive-volume microscopic solids are legitimate geometry: the
  // previous absolute normal^2 < 1e-16 threshold falsely rejected them.
  for(float side:new float[]{1e-3f,1e-5f,1e-6f}){
   float[] tiny=tetra(side);
   TriangleMeshHealth.Report report=TriangleMeshHealth.inspect(tiny);
   require(report.isWatertight()&&report.degenerateTriangles==0,
     "Thin but valid tetrahedron rejected for side="+side+
       ", degenerates="+report.degenerateTriangles);
   double expected=Math.pow((double)side,3)/6.0;
   require(Math.abs(report.signedVolume-expected)<=expected*2e-6,
     "Micro-solid volume wrong for side="+side+": "+report.signedVolume);
  }
  // A genuinely collinear micro-triangle remains degenerate at ANY scale.
  float micro=1e-7f;
  float[] line={0,0,0, micro,micro,0, 2*micro,2*micro,0};
  TriangleMeshHealth.Report collapsedLine=TriangleMeshHealth.inspect(line);
  require(collapsedLine.degenerateTriangles==1&&!collapsedLine.isWatertight(),
    "Small collinear triangle must still be flagged");
  // Long + ultra-thin shape should also pass orientation/volume checks.
  float sx=1000f,sy=1e-6f,sz=1e-6f;
  float[] longThin=tetra(1);
  for(int i=0;i<longThin.length;i+=3){
   longThin[i]*=sx;longThin[i+1]*=sy;longThin[i+2]*=sz;
  }
  TriangleMeshHealth.Report slender=TriangleMeshHealth.inspect(longThin);
  require(slender.isWatertight()&&slender.degenerateTriangles==0,
    "Valid high-aspect-ratio tetrahedron marked degenerate");
  require(Math.abs(slender.signedVolume-(double)sx*sy*sz/6.0)<1e-13,
    "Long-thin solid volume must remain accurate");

  float[] first=tetra(1),second=tetra(-1);
  require(TriangleMeshHealth.inspect(first).isWatertight(),"First tetrahedron should be closed");
  require(TriangleMeshHealth.inspect(second).isWatertight(),"Second tetrahedron should be closed");
  // A one-millimetre tetrahedron at 10,000,000 mm is exactly representable
  // as float. Its volume must not suffer cancellation from world coordinates.
  for(float offset:new float[]{10000000f,-10000000f}){
   float[] far=first.clone();
   for(int k=0;k<far.length;k+=3){
    far[k]+=offset;far[k+1]+=offset;far[k+2]+=offset;
   }
   TriangleMeshHealth.Report translated=TriangleMeshHealth.inspect(far);
   require(translated.isWatertight(),"Translated tetrahedron remains manifold");
   require(Math.abs(translated.signedVolume-1.0/6.0)<1e-12,
     "Translated tetrahedron volume inaccurate: "+translated.signedVolume);
  }
  TriangleMeshHealth.Report original=TriangleMeshHealth.inspect(first);
  require(Math.abs(original.signedVolume-1.0/6.0)<1e-12,
    "Original tetrahedron volume inaccurate");
  float[] insideOut=first.clone();
  for(int i=0;i<insideOut.length;i+=9)
   for(int k=0;k<3;k++){
    float tmp=insideOut[i+3+k];
    insideOut[i+3+k]=insideOut[i+6+k];
    insideOut[i+6+k]=tmp;
   }
  TriangleMeshHealth.Report inward=TriangleMeshHealth.inspect(insideOut);
  require(inward.isWatertight()&&Math.abs(inward.signedVolume+1.0/6.0)<1e-12,
    "Signed volume must preserve orientation: "+inward.signedVolume);

  // Disconnected closed shells can be separated by 20 million millimetres.
  // Each shell needs its own local volume origin, not the first mesh vertex.
  float[] distant=new float[first.length*3];
  System.arraycopy(first,0,distant,0,first.length);
  for(int shell=1;shell<3;shell++){
   int start=shell*first.length;
   System.arraycopy(first,0,distant,start,first.length);
   float offset=shell==1?10000000f:-10000000f;
   for(int k=start;k<start+first.length;k+=3){
    distant[k]+=offset;distant[k+1]+=offset;distant[k+2]+=offset;
   }
  }
  TriangleMeshHealth.Report shells=TriangleMeshHealth.inspect(distant);
  require(shells.isWatertight(),"Three disconnected tetrahedra should be closed");
  require(Math.abs(shells.signedVolume-0.5)<1e-12,
    "Disconnected shells must sum to 1/2 mm3: "+shells.signedVolume);
  float[] mixed=distant.clone();
  for(int i=first.length;i<2*first.length;i+=9)
   for(int k=0;k<3;k++){
    float tmp=mixed[i+3+k];
    mixed[i+3+k]=mixed[i+6+k];
    mixed[i+6+k]=tmp;
   }
  TriangleMeshHealth.Report mixedReport=TriangleMeshHealth.inspect(mixed);
  require(mixedReport.isWatertight()&&Math.abs(mixedReport.signedVolume-1.0/6.0)<1e-12,
    "Oppositely oriented distant shell must cancel exactly: "+mixedReport.signedVolume);

  // A doubled planar triangle has exactly two incident faces per edge,
  // opposite edge directions and no degenerate triangles, but no 3D interior.
  float[] flat={0,0,0, 1,0,0, 0,1,0, 0,1,0, 1,0,0, 0,0,0};
  TriangleMeshHealth.Report collapsed=TriangleMeshHealth.inspect(flat);
  require(collapsed.boundaryEdges==0&&collapsed.nonManifoldEdges==0&&
    collapsed.reversedEdges==0&&collapsed.degenerateTriangles==0,
    "Flat double-sided sheet should pass traditional edge checks");
  require(collapsed.shells==1&&collapsed.zeroVolumeShells==1&&!collapsed.isWatertight(),
    "Zero-volume closed sheet must not pass solid validation");
  // One valid shell cannot mask a separate collapsed shell.
  float[] separatedFlat=flat.clone();
  for(int k=0;k<separatedFlat.length;k+=3)separatedFlat[k]+=10;
  float[] withFlat=Arrays.copyOf(first,first.length+separatedFlat.length);
  System.arraycopy(separatedFlat,0,withFlat,first.length,separatedFlat.length);
  TriangleMeshHealth.Report mixedValidity=TriangleMeshHealth.inspect(withFlat);
  require(mixedValidity.shells==2&&mixedValidity.zeroVolumeShells==1&&
    !mixedValidity.isWatertight()&&Math.abs(mixedValidity.signedVolume-1.0/6.0)<1e-12,
    "Collapsed shell must be detected even alongside a valid tetrahedron");
  require(shells.shells==3&&shells.zeroVolumeShells==0,
    "Separated solid tetrahedra should retain three nonzero shells");

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
