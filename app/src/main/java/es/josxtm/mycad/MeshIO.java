package es.josxtm.mycad;
import java.io.*;import java.nio.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** STL/OBJ triangular mesh interchange. Millimetres assumed; OBJ indices may be negative. */
public final class MeshIO {
 private MeshIO(){}
 public static MeshEngine.Mesh read(String name,InputStream stream)throws IOException{
  byte[] bytes=readLimited(stream,48*1024*1024);String lower=name.toLowerCase(Locale.ROOT);
  if(lower.endsWith(".obj"))return obj(new String(bytes,StandardCharsets.UTF_8));
  if(lower.endsWith(".stl"))return stl(bytes);
  throw new IOException("Formato no compatible: "+name);
 }
 private static byte[] readLimited(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Archivo supera 48 MB");out.write(b,0,n);}return out.toByteArray();}
 private static MeshEngine.Mesh stl(byte[] data)throws IOException{
  if(data.length>=84){ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);long count=Integer.toUnsignedLong(b.getInt(80));if(count<=500000&&84+count*50==data.length){float[] vertices=new float[(int)count*9];b.position(84);for(int t=0;t<count;t++){b.position(b.position()+12);for(int j=0;j<9;j++)vertices[t*9+j]=b.getFloat();b.position(b.position()+2);}return new MeshEngine.Mesh(vertices);}}
  String text=new String(data,StandardCharsets.US_ASCII);ArrayList<Float> vertices=new ArrayList<>();String[] lines=text.split("\\R");for(String line:lines){String s=line.trim();if(s.startsWith("vertex ")){String[] a=s.split("\\s+");if(a.length<4)throw new IOException("Vértice STL incompleto");for(int i=1;i<=3;i++)vertices.add(parse(a[i]));}}if(vertices.isEmpty()||vertices.size()%9!=0)throw new IOException("STL inválido");return new MeshEngine.Mesh(toArray(vertices));
 }
 private static MeshEngine.Mesh obj(String text)throws IOException{
  ArrayList<float[]> positions=new ArrayList<>();ArrayList<Float> triangles=new ArrayList<>();for(String line:text.split("\\R")){String s=line.trim();if(s.startsWith("v ")){String[] a=s.split("\\s+");if(a.length<4)throw new IOException("Vértice OBJ inválido");positions.add(new float[]{parse(a[1]),parse(a[2]),parse(a[3])});}else if(s.startsWith("f ")){String[] a=s.split("\\s+");if(a.length<4)continue;int[] ids=new int[a.length-1];for(int i=1;i<a.length;i++){String n=a[i].split("/",-1)[0];int v;try{v=Integer.parseInt(n);}catch(NumberFormatException e){throw new IOException("Índice OBJ inválido");}ids[i-1]=v>0?v-1:positions.size()+v;if(ids[i-1]<0||ids[i-1]>=positions.size())throw new IOException("Índice OBJ fuera de rango");}for(int i=1;i<ids.length-1;i++){append(triangles,positions.get(ids[0]));append(triangles,positions.get(ids[i]));append(triangles,positions.get(ids[i+1]));}}}
  if(triangles.isEmpty())throw new IOException("OBJ sin caras");return new MeshEngine.Mesh(toArray(triangles));
 }
 private static float parse(String s)throws IOException{try{float v=Float.parseFloat(s);if(!Float.isFinite(v))throw new NumberFormatException();return v;}catch(NumberFormatException e){throw new IOException("Coordenada inválida: "+s);}}
 private static void append(ArrayList<Float> out,float[] p){for(float v:p)out.add(v);}
 private static float[] toArray(ArrayList<Float> a){float[] r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}
 public static void writeObj(MeshEngine.Mesh mesh,OutputStream output)throws IOException{BufferedWriter w=new BufferedWriter(new OutputStreamWriter(output,StandardCharsets.UTF_8));w.write("# MyCAD OBJ - millimetres\\n");for(int i=0;i<mesh.vertices.length;i+=3)w.write("v "+mesh.vertices[i]+" "+mesh.vertices[i+1]+" "+mesh.vertices[i+2]+"\\n");for(int i=0;i<mesh.vertices.length/3;i+=3)w.write("f "+(i+1)+" "+(i+2)+" "+(i+3)+"\\n");w.flush();}
 public static void writeStl(MeshEngine.Mesh mesh,OutputStream output)throws IOException{int count=mesh.triangles();ByteBuffer b=ByteBuffer.allocate(84).order(ByteOrder.LITTLE_ENDIAN);b.position(80);b.putInt(count);output.write(b.array());ByteBuffer face=ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN);for(int t=0;t<count;t++){face.clear();int p=t*9;float[] a=mesh.vertices;float ux=a[p+3]-a[p],uy=a[p+4]-a[p+1],uz=a[p+5]-a[p+2],vx=a[p+6]-a[p],vy=a[p+7]-a[p+1],vz=a[p+8]-a[p+2];float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(len>0){nx/=len;ny/=len;nz/=len;}face.putFloat(nx).putFloat(ny).putFloat(nz);for(int i=0;i<9;i++)face.putFloat(a[p+i]);face.putShort((short)0);output.write(face.array());}}
}
