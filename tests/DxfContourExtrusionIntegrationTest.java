import es.josxtm.mycad.cad.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** End-to-end offline regression for a DXF nonrectangular sketch to 3D mesh. */
public final class DxfContourExtrusionIntegrationTest {
 static void ok(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  String text="0\nSECTION\n2\nHEADER\n9\n$INSUNITS\n70\n4\n"+
    "0\nENDSEC\n0\nSECTION\n2\nENTITIES\n0\nLWPOLYLINE\n"+
    "90\n6\n70\n1\n"+
    "10\n0\n20\n0\n10\n3\n20\n0\n10\n3\n20\n1\n"+
    "10\n1\n20\n1\n10\n1\n20\n3\n10\n0\n20\n3\n"+
    "0\nENDSEC\n0\nEOF\n";
  List<SketchDxfIO.Shape> imported=SketchDxfIO.read(
      new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
  ok(imported.size()==6,"DXF LWPOLYLINE must preserve six line edges");
  List<ClosedSketchExtrusion.Segment> loop=new ArrayList<>();
  for(SketchDxfIO.Shape shape:imported){
   ok(shape.type==1,"Nonrectangular DXF loop needs independent sketch lines");
   loop.add(new ClosedSketchExtrusion.Segment(0,shape.x1,shape.y1,shape.x2,shape.y2));
  }
  ClosedSketchExtrusion.Result extruded=ClosedSketchExtrusion.extrude(loop,3,4);
  ok(Math.abs(extruded.area-5)<1e-6,"L polygon area");
  ok(Math.abs(extruded.volume-20)<1e-6,"Extruded DXF solid volume");
  TriangleMeshHealth.Report surface=TriangleMeshHealth.inspect(extruded.triangles);
  ok(surface.isWatertight()&&surface.boundaryEdges==0&&surface.shells==1,
     "DXF loop 3D solid is not watertight");
  ok(Math.abs(surface.signedVolume-20)<1e-4,
     "DXF loop 3D solid lost outward normals");
  System.out.println("DxfContourExtrusionIntegrationTest: OK");
 }
}
