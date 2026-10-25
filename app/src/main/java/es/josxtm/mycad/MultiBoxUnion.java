package es.josxtm.mycad;
/** Exact axis-aligned union boundary mesh of a bounded list of boxes. */
public final class MultiBoxUnion {
 private MultiBoxUnion(){}
 public enum Operation { UNION, DIFFERENCE, INTERSECTION, CHAIN_DIFFERENCE, CHAIN_INTERSECTION }
 /** Use the first box as the main body, all others as cutters or operands. */
 public static float[] build(java.util.List<BoxBoolean.Box> boxes,Operation op){
  if(op==null)throw new IllegalArgumentException("Missing operation");
  if(op==Operation.UNION)return build(boxes);
  if(boxes==null||boxes.isEmpty())return new float[0];
  if(boxes.size()>52)throw new IllegalArgumentException("Too many solids");
  java.util.TreeSet<Double> xx=new java.util.TreeSet<>(),yy=new java.util.TreeSet<>(),zz=new java.util.TreeSet<>();
  for(BoxBoolean.Box b:boxes){if(b==null)throw new IllegalArgumentException("Missing solid");xx.add(b.x0);xx.add(b.x1);yy.add(b.y0);yy.add(b.y1);zz.add(b.z0);zz.add(b.z1);}
  double[] x=values(xx),y=values(yy),z=values(zz);
  java.util.ArrayList<Float> mesh=new java.util.ArrayList<>();
  for(int i=0;i<x.length-1;i++)for(int j=0;j<y.length-1;j++)for(int k=0;k<z.length-1;k++){
   if(!occupies(boxes,op,(x[i]+x[i+1])/2,(y[j]+y[j+1])/2,(z[k]+z[k+1])/2))continue;
   double a=x[i],b=x[i+1],c=y[j],d=y[j+1],e=z[k],f=z[k+1];
   if(i==0||!occupies(boxes,op,(x[i-1]+x[i])/2,(c+d)/2,(e+f)/2))face(mesh,new double[]{a,c,e},new double[]{a,c,f},new double[]{a,d,f},new double[]{a,d,e});
   if(i==x.length-2||!occupies(boxes,op,(x[i+1]+x[i+2])/2,(c+d)/2,(e+f)/2))face(mesh,new double[]{b,c,e},new double[]{b,d,e},new double[]{b,d,f},new double[]{b,c,f});
   if(j==0||!occupies(boxes,op,(a+b)/2,(y[j-1]+y[j])/2,(e+f)/2))face(mesh,new double[]{a,c,e},new double[]{b,c,e},new double[]{b,c,f},new double[]{a,c,f});
   if(j==y.length-2||!occupies(boxes,op,(a+b)/2,(y[j+1]+y[j+2])/2,(e+f)/2))face(mesh,new double[]{a,d,e},new double[]{a,d,f},new double[]{b,d,f},new double[]{b,d,e});
   if(k==0||!occupies(boxes,op,(a+b)/2,(c+d)/2,(z[k-1]+z[k])/2))face(mesh,new double[]{a,c,e},new double[]{a,d,e},new double[]{b,d,e},new double[]{b,c,e});
   if(k==z.length-2||!occupies(boxes,op,(a+b)/2,(c+d)/2,(z[k+1]+z[k+2])/2))face(mesh,new double[]{a,c,f},new double[]{b,c,f},new double[]{b,d,f},new double[]{a,d,f});
  }
  float[] result=new float[mesh.size()];for(int i=0;i<result.length;i++)result[i]=mesh.get(i);return result;
 }
 private static boolean occupies(java.util.List<BoxBoolean.Box> boxes,Operation op,double x,double y,double z){
  if(op==Operation.CHAIN_DIFFERENCE||op==Operation.CHAIN_INTERSECTION){
   if(boxes.size()<2)throw new IllegalArgumentException("Need chain and tool");
   boolean chain=false;
   for(int n=0;n<boxes.size()-1;n++){
    BoxBoolean.Box c=boxes.get(n);
    if(x>c.x0&&x<c.x1&&y>c.y0&&y<c.y1&&z>c.z0&&z<c.z1){chain=true;break;}
   }
   BoxBoolean.Box tool=boxes.get(boxes.size()-1);
   boolean insideTool=x>tool.x0&&x<tool.x1&&y>tool.y0&&y<tool.y1&&z>tool.z0&&z<tool.z1;
   return chain&&(op==Operation.CHAIN_INTERSECTION?insideTool:!insideTool);
  }
  BoxBoolean.Box root=boxes.get(0);
  boolean initial=x>root.x0&&x<root.x1&&y>root.y0&&y<root.y1&&z>root.z0&&z<root.z1;
  if(!initial)return false;
  boolean any=false,all=true;
  for(int i=1;i<boxes.size();i++){
   BoxBoolean.Box b=boxes.get(i);
   boolean inside=x>b.x0&&x<b.x1&&y>b.y0&&y<b.y1&&z>b.z0&&z<b.z1;
   any|=inside;all&=inside;
  }
  return op==Operation.DIFFERENCE?!any:all;
 }
 public static float[] build(java.util.List<BoxBoolean.Box> boxes){
  if(boxes==null||boxes.isEmpty())return new float[0];
  if(boxes.size()>52)throw new IllegalArgumentException("Too many solids");
  java.util.TreeSet<Double> xx=new java.util.TreeSet<>(),yy=new java.util.TreeSet<>(),zz=new java.util.TreeSet<>();
  for(BoxBoolean.Box b:boxes){if(b==null)throw new IllegalArgumentException("Missing solid");xx.add(b.x0);xx.add(b.x1);yy.add(b.y0);yy.add(b.y1);zz.add(b.z0);zz.add(b.z1);}
  double[] x=values(xx),y=values(yy),z=values(zz);
  java.util.ArrayList<Float> mesh=new java.util.ArrayList<>();
  for(int i=0;i<x.length-1;i++)for(int j=0;j<y.length-1;j++)for(int k=0;k<z.length-1;k++){
   if(!contains(boxes,(x[i]+x[i+1])/2,(y[j]+y[j+1])/2,(z[k]+z[k+1])/2))continue;
   double a=x[i],b=x[i+1],c=y[j],d=y[j+1],e=z[k],f=z[k+1];
   if(i==0||!contains(boxes,(x[i-1]+x[i])/2,(c+d)/2,(e+f)/2))face(mesh,new double[]{a,c,e},new double[]{a,c,f},new double[]{a,d,f},new double[]{a,d,e});
   if(i==x.length-2||!contains(boxes,(x[i+1]+x[i+2])/2,(c+d)/2,(e+f)/2))face(mesh,new double[]{b,c,e},new double[]{b,d,e},new double[]{b,d,f},new double[]{b,c,f});
   if(j==0||!contains(boxes,(a+b)/2,(y[j-1]+y[j])/2,(e+f)/2))face(mesh,new double[]{a,c,e},new double[]{b,c,e},new double[]{b,c,f},new double[]{a,c,f});
   if(j==y.length-2||!contains(boxes,(a+b)/2,(y[j+1]+y[j+2])/2,(e+f)/2))face(mesh,new double[]{a,d,e},new double[]{a,d,f},new double[]{b,d,f},new double[]{b,d,e});
   if(k==0||!contains(boxes,(a+b)/2,(c+d)/2,(z[k-1]+z[k])/2))face(mesh,new double[]{a,c,e},new double[]{a,d,e},new double[]{b,d,e},new double[]{b,c,e});
   if(k==z.length-2||!contains(boxes,(a+b)/2,(c+d)/2,(z[k+1]+z[k+2])/2))face(mesh,new double[]{a,c,f},new double[]{b,c,f},new double[]{b,d,f},new double[]{a,d,f});
  }
  float[] result=new float[mesh.size()];for(int i=0;i<result.length;i++)result[i]=mesh.get(i);return result;
 }
 private static double[] values(java.util.TreeSet<Double> set){double[] v=new double[set.size()];int i=0;for(double x:set)v[i++]=x;return v;}
 private static boolean contains(java.util.List<BoxBoolean.Box> boxes,double x,double y,double z){
  for(BoxBoolean.Box b:boxes)if(x>b.x0&&x<b.x1&&y>b.y0&&y<b.y1&&z>b.z0&&z<b.z1)return true;
  return false;
 }
 private static void face(java.util.ArrayList<Float> output,double[] a,double[] b,double[] c,double[] d){tri(output,a,b,c);tri(output,a,c,d);}
 private static void tri(java.util.ArrayList<Float> output,double[] a,double[] b,double[] c){for(double v:a)output.add((float)v);for(double v:b)output.add((float)v);for(double v:c)output.add((float)v);}
}