package es.josxtm.mycad.cad;

/**
 * Unified analytic Boolean entry point for supported single-shell prisms.
 *
 * UNION uses the established containment, convex-hull and orthogonal routes.
 * INTERSECTION clips convex profiles (including unequal signed heights) or
 * falls back to the equal-height orthogonal arrangement for concave profiles.
 * DIFFERENCE currently supports only equal-height orthogonal profiles.
 *
 * A disconnected result, hole or unsupported freeform profile is rejected;
 * this API never approximates a Boolean by concatenating triangle meshes.
 */
public final class PrismBRepBoolean {
 public enum Operation { UNION, INTERSECTION, DIFFERENCE }
 private PrismBRepBoolean(){}

 public static final class Result {
  public final PolygonPrismBRep solid;
  public final Operation operation;
  public final double area,volume,distance;
  public final int plane;
  private Result(PolygonPrismBRep solid,Operation operation){
   this.solid=solid;this.operation=operation;
   area=solid.sectionArea();volume=solid.volume();
   distance=solid.distance();plane=solid.plane();
  }
  public float[] triangles(){return solid.triangles();}
 }

 public static Result apply(PolygonPrismBRep first,PolygonPrismBRep second,
                            Operation operation){
  if(first==null||second==null||operation==null||
     first.plane()!=second.plane())
   throw new IllegalArgumentException("Booleana B-Rep: sólidos, planos u operación incompatibles");
  double a=first.distance(),b=second.distance();
  if(!Double.isFinite(a)||!Double.isFinite(b)||a*b<=0)
   throw new IllegalArgumentException("Booleana B-Rep: extrusiones incompatibles");

  PolygonPrismBRep body;
  switch(operation){
   case UNION:
    body=PrismBRepUnion.union(first,second);
    break;
   case INTERSECTION:
    try{
     body=ConvexPrismBRepIntersection.intersect(first,second).solid;
    }catch(IllegalArgumentException unsupportedConvex){
     try{
      body=OrthogonalPolygonBRepBoolean.apply(first,second,
          OrthogonalPolygonBRepBoolean.Operation.INTERSECTION).solid;
     }catch(IllegalArgumentException unsupportedOrthogonal){
      throw new IllegalArgumentException(
          "Intersección B-Rep: perfil o topología todavía no admitidos",
          unsupportedOrthogonal);
     }
    }
    break;
   case DIFFERENCE:
    // A variable-height subtraction needs a 3D shell reconstruction.
    body=OrthogonalPolygonBRepBoolean.apply(first,second,
        OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE).solid;
    break;
   default:
    throw new IllegalArgumentException("Operación B-Rep desconocida");
  }
  return new Result(body,operation);
 }
}
