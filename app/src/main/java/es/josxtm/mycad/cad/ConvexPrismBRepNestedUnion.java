package es.josxtm.mycad.cad;

/**
 * Safe, intentionally limited analytic B-Rep union for nested convex prisms.
 * Both bodies must share a work plane and extrusion direction. The containing
 * section must also have at least the height of the contained prism.
 * A true nested union has exactly the containing body's geometry; no mesh
 * stitching, topology approximation or mutation of either operand is needed.
 * Overlaps requiring boundary reconstruction are rejected.
 */
public final class ConvexPrismBRepNestedUnion {
 private static final double EPS=1e-5;
 private ConvexPrismBRepNestedUnion(){}
 private static double[] uv(PolygonPrismBRep.Vertex v,int plane){
  return plane==0?new double[]{v.x,v.y}:plane==1?new double[]{v.x,v.z}:new double[]{v.y,v.z};
 }
 private static boolean containsSection(PolygonPrismBRep outer,PolygonPrismBRep inner){
  int n=outer.vertices().size()/2,m=inner.vertices().size()/2;
  if(n<3||m<3)return false;
  double orientation=0;
  for(int i=0;i<n;i++){
   double[] a=uv(outer.vertices().get(i),outer.plane());
   double[] b=uv(outer.vertices().get((i+1)%n),outer.plane());
   orientation+=a[0]*b[1]-b[0]*a[1];
  }
  if(Math.abs(orientation)<1e-10)return false;
  double sign=Math.signum(orientation);
  for(int j=0;j<m;j++){
   double[] p=uv(inner.vertices().get(j),inner.plane());
   for(int i=0;i<n;i++){
    double[] a=uv(outer.vertices().get(i),outer.plane());
    double[] b=uv(outer.vertices().get((i+1)%n),outer.plane());
    double dx=b[0]-a[0],dy=b[1]-a[1];
    double cross=sign*(dx*(p[1]-a[1])-dy*(p[0]-a[0]));
    if(cross < -1e-7*Math.max(1,Math.hypot(dx,dy)))return false;
   }
  }
  return true;
 }

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
  boolean aInsideB=Math.abs(common.area-areaA)<=tolerance&&containsSection(b,a);
  boolean bInsideA=Math.abs(common.area-areaB)<=tolerance&&containsSection(a,b);
  if(!aInsideB&&!bInsideA)
   throw new IllegalArgumentException("Unión B-Rep: solo contención completa; solape parcial no soportado");
  // Do not treat a protruding top cap as contained just because area tolerance is loose.
  double heightTolerance=1e-9*Math.max(1,Math.max(Math.abs(da),Math.abs(db)));
  boolean aContained=aInsideB&&Math.abs(da)<=Math.abs(db)+heightTolerance;
  boolean bContained=bInsideA&&Math.abs(db)<=Math.abs(da)+heightTolerance;
  if(!aContained&&!bContained)
   throw new IllegalArgumentException("Unión B-Rep: contención espacial incompleta; requiere reconstrucción 3D");
  // Return the existing containing solid: its shell is the exact union.
  // When both containment checks pass (coincident sections), choosing the
  // first operand made the result's UUID depend on operand order. Prefer the
  // genuinely taller shell, even when heights differ only within tolerance;
  // for exactly equal heights use a stable ID tie-break without rebuilding.
  if(aContained&&bContained){
   int heightOrder=Double.compare(Math.abs(da),Math.abs(db));
   if(heightOrder>0)return a;
   if(heightOrder<0)return b;
   return a.id().compareTo(b.id())<=0?a:b;
  }
  return aContained?b:a;
 }
}
