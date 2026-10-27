package es.josxtm.mycad.cad;

/**
 * Safe entry point for analytic unions of supported prism profiles.
 * Preserves a single closed B-Rep shell and never falls back to mesh stitching.
 * Unsupported topologies (holes, disconnected shells or variable-height
 * partial overlaps) are rejected rather than silently approximated.
 */
public final class PrismBRepUnion {
 private PrismBRepUnion(){}

 public static PolygonPrismBRep union(PolygonPrismBRep first,PolygonPrismBRep second){
  if(first==null||second==null||first.plane()!=second.plane())
   throw new IllegalArgumentException("Unión B-Rep: sólidos o planos incompatibles");
  if(!Double.isFinite(first.distance())||!Double.isFinite(second.distance())||
      first.distance()*second.distance()<=0)
   throw new IllegalArgumentException("Unión B-Rep: extrusiones incompatibles");

  // Nested solids may have different heights; return the exact existing shell.
  try{
   return ConvexPrismBRepNestedUnion.union(first,second);
  }catch(IllegalArgumentException unsupportedNested){
   // A non-nested pair may still admit a reconstructed union.
  }
  // Convex hull union supports oblique edges, including edge-sharing prisms.
  try{
   return ConvexPrismBRepHullUnion.union(first,second).solid;
  }catch(IllegalArgumentException unsupportedConvex){
   // Orthogonal arrangement handles concave but connected results.
  }
  try{
   return OrthogonalPolygonBRepBoolean.apply(first,second,
       OrthogonalPolygonBRepBoolean.Operation.UNION).solid;
  }catch(IllegalArgumentException unsupportedOrthogonal){
   throw new IllegalArgumentException(
       "Unión B-Rep: perfil o topología todavía no admitidos",unsupportedOrthogonal);
  }
 }
}
