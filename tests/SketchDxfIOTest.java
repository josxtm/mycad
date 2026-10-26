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
  String unix=exported.replace("\r\n","\n");
  ok(read(unix).size()==3,"DXF con saltos LF");
  String inches=exported.replace("$INSUNITS\r\n70\r\n4","$INSUNITS\r\n70\r\n1");
  fail(()->read(inches),"Se aceptaron pulgadas como milímetros");
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
