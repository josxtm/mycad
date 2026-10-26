import es.josxtm.mycad.cad.SketchDxfIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SketchDxfIOTest {
 static void ok(boolean c,String name){if(!c)throw new AssertionError(name);}
 static void fail(Runnable r,String name){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Se aceptó DXF inválido: "+name);
 }
 static SketchDxfIO.Shape s(int t,double x,double y,double a,double b){
  return new SketchDxfIO.Shape(t,x,y,a,b);
 }
 static List<SketchDxfIO.Shape> read(String dxf){
  try{return SketchDxfIO.read(new ByteArrayInputStream(dxf.getBytes(StandardCharsets.UTF_8)));}
  catch(IOException e){throw new RuntimeException(e);}
 }
 static String write(List<SketchDxfIO.Shape> shapes)throws IOException{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  SketchDxfIO.write(shapes,bytes);
  return bytes.toString("UTF-8");
 }
 static String polygon(double[][] points){
  StringBuilder dxf=new StringBuilder("0\nSECTION\n2\nHEADER\n9\n$INSUNITS\n70\n4\n"+
   "0\nENDSEC\n0\nSECTION\n2\nENTITIES\n0\nLWPOLYLINE\n"+
   "90\n"+points.length+"\n70\n1\n");
  for(double[] p:points)dxf.append("10\n").append(p[0]).append("\n20\n").append(p[1]).append('\n');
  return dxf.append("0\nENDSEC\n0\nEOF\n").toString();
 }
 public static void main(String[] args)throws Exception{
  List<SketchDxfIO.Shape> original=Arrays.asList(
   s(1,-5,7,80,19),s(2,32,15,-12,-21),s(3,10,-5,10,3));
  String exported=write(original);
  ok(exported.contains("$INSUNITS\r\n70\r\n4"),"Unidades DXF milímetros");
  ok(exported.contains("LWPOLYLINE"),"Rectángulo como polilínea DXF");
  List<SketchDxfIO.Shape> recovered=read(exported);
  ok(recovered.size()==3,"Número de figuras importadas");
  ok(recovered.get(0).type==1&&recovered.get(0).x1==-5&&recovered.get(0).y2==19,
     "Línea recuperada");
  ok(recovered.get(1).type==2&&recovered.get(1).x1==-12&&
     recovered.get(1).x2==32&&recovered.get(1).y1==-21,
     "Rectángulo normalizado");
  ok(recovered.get(2).type==3&&
     Math.abs(Math.hypot(recovered.get(2).x2-recovered.get(2).x1,
               recovered.get(2).y2-recovered.get(2).y1)-8)<1e-9,
     "Radio círculo recuperado");
  // A concave six-corner DXF loop is imported as six connected line
  // sketches, suitable for ClosedSketchExtrusion; not discarded or
  // incorrectly normalized into a rectangle.
  String concave="0\nSECTION\n2\nHEADER\n9\n$INSUNITS\n70\n4\n"+
   "0\nENDSEC\n0\nSECTION\n2\nENTITIES\n0\nLWPOLYLINE\n"+
   "90\n6\n70\n1\n"+
   "10\n0\n20\n0\n10\n3\n20\n0\n10\n3\n20\n1\n"+
   "10\n1\n20\n1\n10\n1\n20\n3\n10\n0\n20\n3\n"+
   "0\nENDSEC\n0\nEOF\n";
  List<SketchDxfIO.Shape> loop=read(concave);
  ok(loop.size()==6,"Contorno cóncavo: seis segmentos");
  for(int i=0;i<loop.size();i++){
   SketchDxfIO.Shape edge=loop.get(i),next=loop.get((i+1)%loop.size());
   ok(edge.type==1,"Arista de polilínea cóncava no es LINE");
   ok(edge.x2==next.x1&&edge.y2==next.y1,
      "Extremos de polilínea deben conservar continuidad");
  }
  ok(read(write(loop)).size()==6,"Exportar segmentos importados a DXF");
  String triangle=concave.replace("90\n6\n70\n1\n"+
   "10\n0\n20\n0\n10\n3\n20\n0\n10\n3\n20\n1\n"+
   "10\n1\n20\n1\n10\n1\n20\n3\n10\n0\n20\n3\n",
   "90\n3\n70\n1\n10\n0\n20\n0\n10\n3\n20\n0\n10\n0\n20\n4\n");
  ok(read(triangle).size()==3,"Importación de triángulo cerrado");
  // An otherwise well-formed, nonzero-area five-vertex DXF loop crosses
  // itself. It must fail at import rather than create unextrudable sketches.
  fail(()->read(polygon(new double[][]{
   {0,0},{4,4},{0,4},{4,0},{2,5}
  })),"Polilínea DXF cruzada con área no nula");
  fail(()->read(polygon(new double[][]{
   {0,0},{4,0},{4,4},{0,4},{2,0},{2,2}
  })),"Polilínea DXF que toca una arista no adyacente");
  fail(()->read(polygon(new double[][]{
   {0,0},{4,0},{4,4},{0,4},{4,0}
  })),"Vértice DXF repetido en posición no adyacente");
  ok(read(polygon(new double[][]{
   {0,0},{0,3},{1,3},{1,1},{3,1},{3,0}
  })).size()==6,"Contorno cóncavo horario válido");

  fail(()->read(concave.replace("90\n6\n70\n1","90\n6\n70\n0")),
       "Polilínea abierta no soportada");
  fail(()->read(concave.replace("90\n6\n70\n1","90\n5\n70\n1")),
       "Recuento de vértices incompatible");
  fail(()->read(concave.replace("10\n3\n20\n1\n10\n1",
       "10\n3\n20\n1\n42\n1\n10\n1")),
       "Arco bulge en contorno no soportado");
  fail(()->read(concave.replace("10\n3\n20\n1\n10\n1",
       "10\n3\n20\n1\n43\n2\n10\n1")),
       "Anchura variable no soportada");
  fail(()->read(concave.replace("10\n1\n20\n3\n10\n0\n20\n3",
       "10\n1\n20\n3\n10\n1\n20\n3")),
       "Lado de longitud nula");
  String unix=exported.replace("\r\n","\n");
  ok(read(unix).size()==3,"DXF con saltos LF");
  String inches=exported.replace("$INSUNITS\r\n70\r\n4","$INSUNITS\r\n70\r\n1");
  fail(()->read(inches),"Se aceptaron pulgadas como milímetros");
  String unitless=exported.replace("$INSUNITS\r\n70\r\n4","$INSUNITS\r\n70\r\n0");
  fail(()->read(unitless),"DXF sin unidades no debe asumirse en milímetros");
  String noUnits=exported.replace("9\r\n$INSUNITS\r\n70\r\n4\r\n","");
  fail(()->read(noUnits),"DXF sin $INSUNITS debe rechazarse");
  String duplicateUnits=exported.replace("9\r\n$INSUNITS\r\n70\r\n4",
      "9\r\n$INSUNITS\r\n70\r\n4\r\n9\r\n$INSUNITS\r\n70\r\n4");
  fail(()->read(duplicateUnits),"DXF con $INSUNITS repetido debe rechazarse");
  String missingUnitValue=exported.replace("9\r\n$INSUNITS\r\n70\r\n4\r\n",
      "9\r\n$INSUNITS\r\n");
  fail(()->read(missingUnitValue),"DXF con $INSUNITS incompleto debe rechazarse");
  fail(()->read(exported.replace("0\r\nCIRCLE","0\r\nSPLINE")),
       "Entidad no soportada no debe perderse silenciosamente");
  fail(()->read(exported.replace("30\r\n0","30\r\n10")),
       "No debe proyectar entidades 3D sobre boceto 2D");
  fail(()->read(exported.replace("70\r\n1\r\n10","70\r\n0\r\n10")),
       "No debe aceptar contorno abierto como rectángulo");
  fail(()->read("0\nSECTION\n2\nENTITIES\n0\nENDSEC\n0\nEOF\n"),
       "DXF vacío");
  fail(()->read("0\nSECTION\n2\nENTITIES\n0\nLINE\n10\n5\n20\n"),
       "DXF truncado");
  fail(()->new SketchDxfIO.Shape(3,0,0,0,0),"Radio nulo");
  fail(()->new SketchDxfIO.Shape(2,0,0,0,9),"Rectángulo degenerado");
  System.out.println("SketchDxfIOTest: OK");
 }
}
