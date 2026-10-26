package es.josxtm.mycad.cad;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Restricted ASCII DXF bridge for the Android sketch editor (XY, millimetres).
 * Supported entities: LINE, CIRCLE and closed LWPOLYLINE contours.
 * Axis-aligned four-corner rectangles remain native rectangle sketches;
 * other closed straight contours become connected LINE sketches. Unsupported
 * geometry fails instead of silently disappearing on import. This is NOT a general DXF or 3D CAD parser.
 */
public final class SketchDxfIO {
 private SketchDxfIO(){}
 private static final int MAX_BYTES=2*1024*1024, MAX_SHAPES=2000;
 public static final class Shape {
  /** 1=line, 2=axis-aligned rectangle, 3=circle; circle x1/y1=center. */
  public final int type;
  public final double x1,y1,x2,y2;
  public Shape(int type,double x1,double y1,double x2,double y2){
   if(type<1||type>3||!finite(x1)||!finite(y1)||!finite(x2)||!finite(y2)||
      Math.max(Math.max(Math.abs(x1),Math.abs(y1)),Math.max(Math.abs(x2),Math.abs(y2)))>100000)
    throw new IllegalArgumentException("Coordenadas de boceto fuera de rango");
   double span=Math.hypot(x2-x1,y2-y1);
   if((type==1&&span<1e-6)||(type==2&&(Math.abs(x2-x1)<1e-6||
        Math.abs(y2-y1)<1e-6))||(type==3&&(span<1e-6||span>10000)))
    throw new IllegalArgumentException("Boceto DXF degenerado");
   this.type=type;this.x1=x1;this.y1=y1;this.x2=x2;this.y2=y2;
  }
 }
 private static boolean finite(double value){return Double.isFinite(value);}
 private static final class Pair {
  final String code,value;
  Pair(String code,String value){this.code=code;this.value=value;}
 }
 private static void pair(StringBuilder out,int code,Object value){
  out.append(code).append("\r\n").append(value).append("\r\n");
 }
 private static void coordinate(StringBuilder out,int code,double value){
  pair(out,code,Double.toString(value));
 }
 /** Only supplied XY sketches are exported; never silently flatten XZ/YZ. */
 public static void write(List<Shape> shapes,OutputStream destination)throws IOException{
  if(shapes==null||destination==null||shapes.isEmpty()||shapes.size()>MAX_SHAPES)
   throw new IllegalArgumentException("Lista DXF vacía o demasiado grande");
  StringBuilder out=new StringBuilder();
  pair(out,0,"SECTION");pair(out,2,"HEADER");
  pair(out,9,"$INSUNITS");pair(out,70,4); // millimetres
  pair(out,0,"ENDSEC");pair(out,0,"SECTION");pair(out,2,"ENTITIES");
  for(Shape s:shapes){
   if(s==null)throw new IllegalArgumentException("Boceto DXF nulo");
   // Recheck immutable input via constructor for symmetry with the parser.
   new Shape(s.type,s.x1,s.y1,s.x2,s.y2);
   if(s.type==1){
    pair(out,0,"LINE");pair(out,8,"MYCAD");
    coordinate(out,10,s.x1);coordinate(out,20,s.y1);pair(out,30,0);
    coordinate(out,11,s.x2);coordinate(out,21,s.y2);pair(out,31,0);
   }else if(s.type==3){
    pair(out,0,"CIRCLE");pair(out,8,"MYCAD");
    coordinate(out,10,s.x1);coordinate(out,20,s.y1);pair(out,30,0);
    coordinate(out,40,Math.hypot(s.x2-s.x1,s.y2-s.y1));
   }else{
    double loX=Math.min(s.x1,s.x2),hiX=Math.max(s.x1,s.x2);
    double loY=Math.min(s.y1,s.y2),hiY=Math.max(s.y1,s.y2);
    pair(out,0,"LWPOLYLINE");pair(out,8,"MYCAD");pair(out,90,4);pair(out,70,1);
    double[][] corners={{loX,loY},{hiX,loY},{hiX,hiY},{loX,hiY}};
    for(double[] p:corners){coordinate(out,10,p[0]);coordinate(out,20,p[1]);}
   }
  }
  pair(out,0,"ENDSEC");pair(out,0,"EOF");
  byte[] encoded=out.toString().getBytes(StandardCharsets.UTF_8);
  if(encoded.length>MAX_BYTES)throw new IOException("DXF supera el límite de 2 MB");
  destination.write(encoded);
 }
 private static double num(String value,String name){
  if(value==null)throw new IllegalArgumentException("Falta coordenada DXF: "+name);
  try{
   double n=Double.parseDouble(value);
   if(!finite(n))throw new NumberFormatException();
   return n;
  }catch(NumberFormatException problem){
   throw new IllegalArgumentException("Número DXF incorrecto: "+name,problem);
  }
 }
 private static String field(List<Pair> records,int code){
  for(Pair p:records)if(p.code.equals(Integer.toString(code)))return p.value;
  return null;
 }
 private static double optional(List<Pair> records,int code,double fallback){
  String value=field(records,code);return value==null?fallback:num(value,""+code);
 }
 private static void requireXY(List<Pair> attributes){
  if(optional(attributes,30,0)!=0||optional(attributes,31,0)!=0||
     optional(attributes,38,0)!=0||
     optional(attributes,210,0)!=0||optional(attributes,220,0)!=0||
     optional(attributes,230,1)!=1)
   throw new IllegalArgumentException("Solo se admite geometría DXF en el plano XY");
 }
 /**
  * Reject self-intersecting closed DXF contours before the Android editor
  * stages any sketches. Adjacent edges may share their common endpoint;
  * nonadjacent edges may neither cross nor touch (including overlaps).
  */
 private static double turn(double[] a,double[] b,double[] c){
  return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
 }
 private static boolean onEdge(double[] a,double[] b,double[] p){
  return turn(a,b,p)==0&&p[0]>=Math.min(a[0],b[0])&&
    p[0]<=Math.max(a[0],b[0])&&p[1]>=Math.min(a[1],b[1])&&
    p[1]<=Math.max(a[1],b[1]);
 }
 private static boolean edgesMeet(double[] a,double[] b,double[] c,double[] d){
  double abC=turn(a,b,c),abD=turn(a,b,d),
         cdA=turn(c,d,a),cdB=turn(c,d,b);
  if((abC==0&&onEdge(a,b,c))||(abD==0&&onEdge(a,b,d))||
     (cdA==0&&onEdge(c,d,a))||(cdB==0&&onEdge(c,d,b)))
   return true;
  return ((abC>0&&abD<0)||(abC<0&&abD>0))&&
         ((cdA>0&&cdB<0)||(cdA<0&&cdB>0));
 }
 private static void validateSimpleContour(List<double[]> corners){
  int n=corners.size();
  double[] origin=corners.get(0);
  double twiceArea=0,minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY;
  double minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
  for(int i=0;i<n;i++){
   double[] a=corners.get(i),b=corners.get((i+1)%n);
   if(Math.abs(a[0])>100000||Math.abs(a[1])>100000)
    throw new IllegalArgumentException("Vértice DXF fuera de rango");
   if(Math.hypot(b[0]-a[0],b[1]-a[1])<1e-6)
    throw new IllegalArgumentException("Arista DXF degenerada");
   twiceArea+=turn(origin,a,b);
   minX=Math.min(minX,a[0]);maxX=Math.max(maxX,a[0]);
   minY=Math.min(minY,a[1]);maxY=Math.max(maxY,a[1]);
  }
  double extent=Math.max(maxX-minX,maxY-minY);
  if(Math.abs(twiceArea)<=1e-12*extent*extent)
   throw new IllegalArgumentException("Polilínea DXF sin superficie");
  for(int i=0;i<n;i++){
   double[] a=corners.get(i),b=corners.get((i+1)%n);
   for(int j=i+1;j<n;j++){
    if(j==i+1||(i==0&&j==n-1))continue;
    if(edgesMeet(a,b,corners.get(j),corners.get((j+1)%n)))
     throw new IllegalArgumentException("Polilínea DXF autointersectada");
   }
  }
 }
 private static void add(List<Shape> output,Shape shape){
  if(output.size()>=MAX_SHAPES)throw new IllegalArgumentException("Demasiados bocetos DXF");
  output.add(shape);
 }
 private static void parseEntity(String name,List<Pair> a,List<Shape> output){
  requireXY(a);
  if("LINE".equals(name)){
   add(output,new Shape(1,num(field(a,10),"10"),num(field(a,20),"20"),
       num(field(a,11),"11"),num(field(a,21),"21")));
  }else if("CIRCLE".equals(name)){
   double cx=num(field(a,10),"10"),cy=num(field(a,20),"20"),
          r=num(field(a,40),"40");
   if(r<=0)throw new IllegalArgumentException("Radio DXF no positivo");
   add(output,new Shape(3,cx,cy,cx+r,cy));
  }else if("LWPOLYLINE".equals(name)){
   double declared=num(field(a,90),"90"),flags=optional(a,70,0);
   if(declared<3||declared>MAX_SHAPES||declared!=(int)declared||
      flags!=(int)flags||(((int)flags)&1)==0)
    throw new IllegalArgumentException("Solo se importan polilíneas cerradas de 3 a 2000 vértices");
   List<double[]> corners=new ArrayList<>();
   Double waitingX=null;
   for(Pair p:a){
    if("42".equals(p.code)&&num(p.value,"42")!=0)
     throw new IllegalArgumentException("Arcos DXF dentro de polilínea no soportados");
    // Nonzero thickness/width is not a 1D sketch line: never discard it.
    if(("39".equals(p.code)||"40".equals(p.code)||
        "41".equals(p.code)||"43".equals(p.code))&&
       num(p.value,"Ancho/grosor")!=0)
     throw new IllegalArgumentException("Grosor de polilínea DXF no soportado");
    if("10".equals(p.code)){
     if(waitingX!=null)throw new IllegalArgumentException("Falta Y de vértice DXF");
     waitingX=num(p.value,"10");
    }else if("20".equals(p.code)){
     if(waitingX==null)throw new IllegalArgumentException("Falta X de vértice DXF");
     corners.add(new double[]{waitingX,num(p.value,"20")});waitingX=null;
    }
   }
   if(waitingX!=null||corners.size()!=(int)declared)
    throw new IllegalArgumentException("Polilínea DXF con vértices incompletos");
   validateSimpleContour(corners);
   double loX=Double.POSITIVE_INFINITY,loY=Double.POSITIVE_INFINITY;
   double hiX=Double.NEGATIVE_INFINITY,hiY=Double.NEGATIVE_INFINITY;
   for(double[] point:corners){
    loX=Math.min(loX,point[0]);hiX=Math.max(hiX,point[0]);
    loY=Math.min(loY,point[1]);hiY=Math.max(hiY,point[1]);
   }
   double eps=1e-8*Math.max(1,Math.max(hiX-loX,hiY-loY));
   boolean rectangle=corners.size()==4&&hiX-loX>eps&&hiY-loY>eps;
   boolean[] seen=new boolean[4];
   if(rectangle)for(int i=0;i<4;i++){
    double[] v=corners.get(i),w=corners.get((i+1)%4);
    boolean xMin=Math.abs(v[0]-loX)<=eps,xMax=Math.abs(v[0]-hiX)<=eps;
    boolean yMin=Math.abs(v[1]-loY)<=eps,yMax=Math.abs(v[1]-hiY)<=eps;
    if(!(xMin||xMax)||!(yMin||yMax)||
       !(Math.abs(v[0]-w[0])<=eps ^ Math.abs(v[1]-w[1])<=eps)){
     rectangle=false;break;
    }
    int quadrant=(xMax?1:0)+(yMax?2:0);
    if(seen[quadrant]){rectangle=false;break;}
    seen[quadrant]=true;
   }
   if(rectangle){
    add(output,new Shape(2,loX,loY,hiX,hiY));
   }else{
    // Stage every segment before appending; a malformed final edge leaves no
    // partial entity behind. read() builds a fresh document until full success.
    List<Shape> segments=new ArrayList<>();
    for(int i=0;i<corners.size();i++){
     double[] v=corners.get(i),w=corners.get((i+1)%corners.size());
     segments.add(new Shape(1,v[0],v[1],w[0],w[1]));
    }
    if(output.size()+segments.size()>MAX_SHAPES)
     throw new IllegalArgumentException("Demasiados bocetos DXF");
    output.addAll(segments);
   }
  }else throw new IllegalArgumentException("Entidad DXF no compatible: "+name);
 }
 public static List<Shape> read(InputStream source)throws IOException{
  if(source==null)throw new IllegalArgumentException("Archivo DXF ausente");
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();
  byte[] block=new byte[8192];int n,total=0;
  while((n=source.read(block))!=-1){
   total+=n;if(total>MAX_BYTES)throw new IOException("DXF demasiado grande");
   buffer.write(block,0,n);
  }
  String contents=new String(buffer.toByteArray(),StandardCharsets.UTF_8);
  String[] lines=contents.split("\r?\n",-1);
  int end=lines.length;
  while(end>0&&lines[end-1].isEmpty())end--;
  if(end==0||end%2!=0)throw new IllegalArgumentException("Pares de códigos DXF incompletos");
  List<Pair> pairs=new ArrayList<>(end/2);
  for(int i=0;i<end;i+=2){
   String code=lines[i].trim(),value=lines[i+1].trim();
   try{Integer.parseInt(code);}catch(NumberFormatException bad){
    throw new IllegalArgumentException("Código DXF incorrecto en línea "+(i+1));
   }
   pairs.add(new Pair(code,value));
  }
  String section="";boolean entitiesSeen=false,unitsPending=false,unitsSeen=false,headerSeen=false;
  List<Shape> output=new ArrayList<>();
  for(int i=0;i<pairs.size();){
   Pair tag=pairs.get(i);
   if("HEADER".equals(section)){
    if(unitsPending){
     if("70".equals(tag.code)){
      if(!"4".equals(tag.value))
       throw new IllegalArgumentException("Unidades DXF incompatibles: se requieren mm");
      unitsSeen=true;unitsPending=false;
     }else if("9".equals(tag.code)||"0".equals(tag.code))
      throw new IllegalArgumentException("Valor de $INSUNITS ausente");
    }
    if("9".equals(tag.code)&&"$INSUNITS".equals(tag.value)){
     if(unitsSeen||unitsPending)
      throw new IllegalArgumentException("Unidades DXF duplicadas");
     unitsPending=true;
    }
   }
   if("0".equals(tag.code)){
    if("SECTION".equals(tag.value)){
     if(i+1>=pairs.size()||!"2".equals(pairs.get(i+1).code))
      throw new IllegalArgumentException("Sección DXF incorrecta");
     section=pairs.get(i+1).value;
     if("HEADER".equals(section))headerSeen=true;
     if("ENTITIES".equals(section))entitiesSeen=true;
     i+=2;continue;
    }
    if("ENDSEC".equals(tag.value)){section="";i++;continue;}
    if("EOF".equals(tag.value))break;
    if("ENTITIES".equals(section)){
     int until=i+1;
     while(until<pairs.size()&&!"0".equals(pairs.get(until).code))until++;
     parseEntity(tag.value,pairs.subList(i+1,until),output);
     i=until;continue;
    }
   }
   i++;
  }
  if(unitsPending)throw new IllegalArgumentException("Valor de $INSUNITS ausente");
  if(!headerSeen||!unitsSeen)
   throw new IllegalArgumentException("DXF sin unidades explícitas en milímetros ($INSUNITS=4)");
  if(!entitiesSeen||output.isEmpty())
   throw new IllegalArgumentException("DXF sin bocetos XY compatibles");
  return Collections.unmodifiableList(output);
 }
}
