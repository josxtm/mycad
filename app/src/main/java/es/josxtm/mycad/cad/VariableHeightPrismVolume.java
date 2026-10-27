package es.josxtm.mycad.cad;

import java.util.List;

/**
 * Exact volume accounting for orthogonal prisms extruded from the same plane.
 * This is a preflight calculation, NOT a replacement for a reconstructed
 * watertight 3D B-Rep shell.
 */
public final class VariableHeightPrismVolume {
 private VariableHeightPrismVolume(){}

 public static final class Result {
  public final double firstVolume,secondVolume,intersectionVolume;
  public final double differenceVolume,unionVolume;
  private Result(double first,double second,double intersection){
   firstVolume=first;secondVolume=second;intersectionVolume=intersection;
   differenceVolume=first-intersection;
   unionVolume=first+second-intersection;
  }
 }

 public static Result calculate(PolygonPrismBRep first,PolygonPrismBRep second){
  if(first==null||second==null||first.plane()!=second.plane())
   throw new IllegalArgumentException("Variable-height B-Rep: incompatible planes or bodies");
  double a=first.distance(),b=second.distance();
  if(!Double.isFinite(a)||!Double.isFinite(b)||a*b<=0)
   throw new IllegalArgumentException("Variable-height B-Rep: incompatible extrusion directions");
  List<double[]> pa=OrthogonalPolygonBRepBoolean.points(first);
  List<double[]> pb=OrthogonalPolygonBRepBoolean.points(second);
  double[] xs=OrthogonalPolygonBRepBoolean.coordinates(pa,pb,0);
  double[] ys=OrthogonalPolygonBRepBoolean.coordinates(pa,pb,1);
  double overlap=0;
  for(int i=0;i<xs.length-1;i++)for(int j=0;j<ys.length-1;j++){
   double x=(xs[i]+xs[i+1])/2,y=(ys[j]+ys[j+1])/2;
   if(OrthogonalPolygonBRepBoolean.inside(pa,x,y)&&
      OrthogonalPolygonBRepBoolean.inside(pb,x,y))
    overlap+=(xs[i+1]-xs[i])*(ys[j+1]-ys[j]);
  }
  double intersection=overlap*Math.min(Math.abs(a),Math.abs(b));
  return new Result(first.volume(),second.volume(),intersection);
 }
}
