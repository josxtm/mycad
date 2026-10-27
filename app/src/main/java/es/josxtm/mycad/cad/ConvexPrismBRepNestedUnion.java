package es.josxtm.mycad.cad;

/**
 * Safe, intentionally limited analytic B-Rep union for nested convex prisms.
 * Both bodies must share a work plane, extrusion direction and signed height.
 * A true nested union has exactly the containing body's geometry; no mesh
 * stitching, topology approximation or mutation of either operand is needed.
 * Overlaps requiring boundary reconstruction are rejected.
 */
public final class ConvexPrismBRepNestedUnion {
 private static final double EPS=1e-5;
 private ConvexPrismBRepNestedUnion(){}

 public static PolygonPrismBRep union(PolygonPrismBRep a,PolygonPrismBRep b){
  if(a==null||b==null||a.plane()!=b.plane())
   throw new IllegalArgumentException("Unión B-Rep: se requiere el mismo plano");
  double da=a.distance(),db=b.distance();
  if(da*db<=0||Math.abs(da-db)>EPS*Math.max(1,Math.max(Math.abs(da),Math.abs(db))))
   throw new IllegalArgumentException("Unión B-Rep: alturas y direcciones deben coincidir");
  // Also validates convexity and nondegenerate section topology.
  ConvexPrismBRepIntersection.Result common=ConvexPrismBRepIntersection.intersect(a,b);
  double areaA=a.sectionArea(),areaB=b.sectionArea();
  double tolerance=EPS*Math.max(1,Math.max(areaA,areaB));
  boolean aInsideB=Math.abs(common.area-areaA)<=tolerance;
  boolean bInsideA=Math.abs(common.area-areaB)<=tolerance;
  if(!aInsideB&&!bInsideA)
   throw new IllegalArgumentException("Unión B-Rep: solo contención completa; solape parcial no soportado");
  // The larger analytic B-Rep already represents the exact union.
  return aInsideB&&!bInsideA?b:a;
 }
}
