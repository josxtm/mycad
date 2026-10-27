package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

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

 /**
  * The intersection of same-origin, same-direction extrusions is the planar
  * intersection extruded by the shorter height. Rebuild only the temporary
  * operand shell; never alter the persisted source feature or its mesh.
  */
 private static PolygonPrismBRep clippedHeight(PolygonPrismBRep body,double height){
  if(body.distance()==height)return body;
  int n=body.vertices().size()/2;
  ArrayList<String> ids=new ArrayList<>(n);
  LinkedHashMap<String,ClosedSketchExtrusion.Segment> lines=new LinkedHashMap<>();
  for(int i=0;i<n;i++){
   PolygonPrismBRep.Vertex a=body.vertices().get(i);
   PolygonPrismBRep.Vertex b=body.vertices().get((i+1)%n);
   int plane=body.plane();
   double au=plane==2?a.y:a.x,av=plane==0?a.y:plane==1?a.z:a.z;
   double bu=plane==2?b.y:b.x,bv=plane==0?b.y:plane==1?b.z:b.z;
   String edge=UUID.nameUUIDFromBytes(
       (body.id()+"/intersection-height/edge/"+i).getBytes(StandardCharsets.UTF_8))
       .toString();
   ids.add(edge);
   lines.put(edge,new ClosedSketchExtrusion.Segment(plane,au,av,bu,bv));
  }
  return new LoopExtrusionFeature(body.id(),ids.get(0),body.plane(),height,ids)
      .asBRep(lines);
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
      double commonHeight=Math.copySign(Math.min(Math.abs(a),Math.abs(b)),a);
      body=OrthogonalPolygonBRepBoolean.apply(
          clippedHeight(first,commonHeight),clippedHeight(second,commonHeight),
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
