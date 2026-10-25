package es.josxtm.mycad.cad;
/** Twelve persistent logical edge slots of an axis-aligned local box under rigid XYZ pose.
 * Local box is centered on X/Y and extends in local Z from 0 to height.
 */
public final class PrimitiveEdges {
 private PrimitiveEdges(){}
 public static final class Edge {
  public final String key,axis;
  public final double[] start,end;
  public final double length;
  Edge(String key,String axis,double[] start,double[] end,double length){
   this.key=key;this.axis=axis;this.start=start;this.end=end;this.length=length;
  }
 }
 public static Edge edge(String bodyId,int slot,double width,double depth,double height,RigidTransform pose){
  if(bodyId==null||bodyId.isEmpty()||slot<0||slot>=12||pose==null||
     !Double.isFinite(width)||!Double.isFinite(depth)||!Double.isFinite(height)||
     width<=0||depth<=0||height<=0)
   throw new IllegalArgumentException("Invalid edge");
  double[] a,b;
  String axis;
  int local=slot%4;
  if(slot<4){
   axis="X";double y=(local%2==0?-depth/2:depth/2),z=local<2?0:height;
   a=pose.point(-width/2,y,z);b=pose.point(width/2,y,z);
  }else if(slot<8){
   axis="Y";double x=(local%2==0?-width/2:width/2),z=local<2?0:height;
   a=pose.point(x,-depth/2,z);b=pose.point(x,depth/2,z);
  }else{
   axis="Z";double x=local%2==0?-width/2:width/2,y=local<2?-depth/2:depth/2;
   a=pose.point(x,y,0);b=pose.point(x,y,height);
  }
  double length=Math.sqrt(Math.pow(b[0]-a[0],2)+Math.pow(b[1]-a[1],2)+Math.pow(b[2]-a[2],2));
  return new Edge(bodyId+"/edge/"+axis+"_"+local,axis,a,b,length);
 }
}