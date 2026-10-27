package es.josxtm.mycad.cad;

/**
 * Safe, intentionally limited analytic B-Rep union for nested convex prisms.
 * Both bodies must share a work plane and extrusion direction. The containing\n * section must also have at least the height of the contained prism.
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
  if(da*db<=0)
   throw new IllegalArgumentException("Unión B-Rep: las direcciones deben coincidir");
  // Also validates convexity and nondegenerate section topology.
  ConvexPrismBRepIntersection.Result common=ConvexPrismBRepIntersection.intersect(a,b);
  double areaA=a.sectionArea(),areaB=b.sectionArea();
  double tolerance=EPS*Math.max(1,Math.max(areaA,areaB));
  boolean aInsideB=Math.abs(common.area-areaA)<=tolerance;
  boolean bInsideA=Math.abs(common.area-areaB)<=tolerance;
  if(!aInsideB&&!bInsideA)
   throw new IllegalArgumentException("Unión B-Rep: solo contención completa; solape parcial no soportado");
  double heightTolerance=EPS*Math.max(1,Math.max(Math.abs(da),Math.abs(db)));
  boolean aContained=aInsideB&&Math.abs(da)<=Math.abs(db)+heightTolerance;
  boolean bContained=bInsideA&&Math.abs(db)<=Math.abs(da)+heightTolerance;
  if(!aContained&&!bContained)
   throw new IllegalArgumentException("Unión B-Rep: contención espacial incompleta; requiere reconstrucción 3D");
  // Return the existing containing solid: its shell is the exact union.
  return aContained&&!bContained?b:a;
 }
}
