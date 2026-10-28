package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Immutable typed entity index. Persistent lookup is an explicit rebind;
 * exact lookup additionally requires the same analytic geometry version.
 * Neither lookup authorizes reuse of an old GPU triangle or viewport ray. */
public final class TopologySnapshot {
 public enum Kind { VERTEX, EDGE, FACE }
 public static final class Reference {
  public final String bodyId,entityId,geometryVersion;
  public final Kind kind;
  private Reference(String body,Kind kind,String id,String version){
   if(body==null||body.trim().isEmpty()||kind==null||id==null||id.isEmpty()||version==null||version.isEmpty())
    throw new IllegalArgumentException("Invalid topology reference");
   bodyId=body;this.kind=kind;entityId=id;geometryVersion=version;
  }
  @Override public boolean equals(Object other){
   if(!(other instanceof Reference))return false;
   Reference r=(Reference)other;
   return bodyId.equals(r.bodyId)&&kind==r.kind&&entityId.equals(r.entityId)&&geometryVersion.equals(r.geometryVersion);
  }
  @Override public int hashCode(){return Objects.hash(bodyId,kind,entityId,geometryVersion);}
 }
 public final String bodyId,geometryVersion,family;
 private final Map<Kind,Set<String>> entities;
 private TopologySnapshot(Builder b){
  bodyId=b.body;family=b.family;
  Map<Kind,Set<String>> copy=new EnumMap<>(Kind.class);
  for(Kind kind:Kind.values())copy.put(kind,Collections.unmodifiableSet(new TreeSet<>(b.ids.get(kind))));
  entities=Collections.unmodifiableMap(copy);
  try{
   byte[] hash=MessageDigest.getInstance("SHA-256").digest(b.geometry.toString().getBytes(StandardCharsets.UTF_8));
   StringBuilder version=new StringBuilder();for(byte value:hash)version.append(String.format(Locale.ROOT,"%02x",value));
   geometryVersion=version.toString();
  }catch(NoSuchAlgorithmException unavailable){throw new AssertionError(unavailable);}
 }
 private static final class Builder {
  final String body,family;
  final Map<Kind,Set<String>> ids=new EnumMap<>(Kind.class);
  final StringBuilder geometry=new StringBuilder();
  Builder(String body,String family){
   if(body==null||body.isEmpty())throw new IllegalArgumentException("Missing topology owner");
   this.body=body;this.family=family;field(body);field(family);
   for(Kind kind:Kind.values())ids.put(kind,new TreeSet<>());
  }
  void field(String value){geometry.append(value.length()).append(':').append(value);}
  void number(double value){
   if(!Double.isFinite(value))throw new IllegalArgumentException("Nonfinite analytic geometry");
   field(Double.toHexString(value==0?0:value));
  }
  void add(Kind kind,String persistent,String snapshot){
   if(persistent==null||persistent.isEmpty()||!ids.get(kind).add(persistent))
    throw new IllegalArgumentException("Ambiguous topology entity");
   field(kind.name());field(persistent);field(snapshot);
  }
  void point(double x,double y,double z){number(x);number(y);number(z);}
 }
 public Set<String> ids(Kind kind){
  if(kind==null)throw new IllegalArgumentException("Missing entity kind");
  return entities.get(kind);
 }
 public Reference reference(Kind kind,String persistentId){
  if(!ids(kind).contains(persistentId))throw new IllegalArgumentException("Unknown topology entity");
  return new Reference(bodyId,kind,persistentId,geometryVersion);
 }
 /** Decode a descriptor; existence is only established by a later lookup. */
 public static Reference restore(String body,Kind kind,String id,String version){
  return new Reference(body,kind,id,version);
 }
 public Reference resolvePersistent(Reference reference){
  if(reference==null||!bodyId.equals(reference.bodyId)||!ids(reference.kind).contains(reference.entityId))return null;
  return new Reference(bodyId,reference.kind,reference.entityId,geometryVersion);
 }
 public Reference resolveExact(Reference reference){
  return reference!=null&&geometryVersion.equals(reference.geometryVersion)?resolvePersistent(reference):null;
 }
 public static TopologySnapshot from(NamedSteppedBody body){
  if(body==null)throw new IllegalArgumentException("Missing named body");
  Builder b=new Builder(body.bodyId,"STEPPED");b.field(body.shell.id());
  for(VariableHeightOrthogonalBRep.Vertex v:body.shell.vertices()){
   b.add(Kind.VERTEX,body.vertexId(v.id),v.id);b.point(v.x,v.y,v.z);
  }
  for(VariableHeightOrthogonalBRep.Edge e:body.shell.edges())b.add(Kind.EDGE,body.edgeId(e.id),e.id);
  for(VariableHeightOrthogonalBRep.Face f:body.shell.faces())b.add(Kind.FACE,body.faceId(f.id),f.id);
  return new TopologySnapshot(b);
 }
 public static TopologySnapshot from(NamedGridHoleBody body){
  if(body==null)throw new IllegalArgumentException("Missing named grid");
  Builder b=new Builder(body.bodyId,"GRID_HOLES");b.field(body.shell.id);
  for(OrthogonalGridHoleBRep.Vertex v:body.shell.vertices()){
   b.add(Kind.VERTEX,body.vertexId(v.id),v.id);b.point(v.x,v.y,v.z);
  }
  for(OrthogonalGridHoleBRep.Edge e:body.shell.edges())b.add(Kind.EDGE,body.edgeId(e.id),e.id);
  for(OrthogonalGridHoleBRep.Face f:body.shell.faces())b.add(Kind.FACE,body.faceId(f.id),f.id);
  return new TopologySnapshot(b);
 }
 public static TopologySnapshot from(NamedConvexIntersection body){
  if(body==null)throw new IllegalArgumentException("Missing named intersection");
  Builder b=new Builder(body.bodyId,"CONVEX_INTERSECTION");b.field(body.shell.id());
  for(PolygonPrismBRep.Vertex v:body.shell.vertices()){
   b.add(Kind.VERTEX,body.vertexId(v.id),v.id);b.point(v.x,v.y,v.z);
  }
  for(PolygonPrismBRep.Edge e:body.shell.edges())b.add(Kind.EDGE,body.edgeId(e.id),e.id);
  for(PolygonPrismBRep.Face f:body.shell.faces()){b.add(Kind.FACE,body.faceId(f.id),f.id);b.number(f.area);}
  return new TopologySnapshot(b);
 }
 public static TopologySnapshot from(NamedPlanarBoolean body){
  if(body==null)throw new IllegalArgumentException("Missing named planar Boolean");
  Builder b=new Builder(body.bodyId,"PLANAR_"+body.operation);
  for(PolygonPrismBRep shell:body.solids()){
   b.field(shell.id());b.number(shell.distance());b.number(shell.plane());
   for(PolygonPrismBRep.Vertex v:shell.vertices()){
    b.add(Kind.VERTEX,body.vertexId(v.id),v.id);b.point(v.x,v.y,v.z);
   }
   for(PolygonPrismBRep.Edge e:shell.edges())b.add(Kind.EDGE,body.edgeId(e.id),e.id);
   for(PolygonPrismBRep.Face f:shell.faces()){b.add(Kind.FACE,body.faceId(f.id),f.id);b.number(f.area);}
  }
  return new TopologySnapshot(b);
 }
 public static TopologySnapshot from(PolygonPrismBRep body){
  if(body==null)throw new IllegalArgumentException("Missing polygon prism");
  Builder b=new Builder(body.id(),"POLYGON_PRISM");b.number(body.distance());b.number(body.plane());
  for(PolygonPrismBRep.Vertex v:body.vertices()){b.add(Kind.VERTEX,v.id,v.id);b.point(v.x,v.y,v.z);}
  for(PolygonPrismBRep.Edge e:body.edges())b.add(Kind.EDGE,e.id,e.id);
  for(PolygonPrismBRep.Face f:body.faces()){b.add(Kind.FACE,f.id,f.id);b.number(f.area);}
  return new TopologySnapshot(b);
 }
 public static TopologySnapshot from(PrimitiveBRepBody body){
  if(body==null)throw new IllegalArgumentException("Missing analytic primitive");
  Builder b=new Builder(body.bodyId,"PRIMITIVE_"+body.kind);
  for(double value:new double[]{body.width,body.depth,body.height,body.x,body.y,body.z,body.rx,body.ry,body.rz})b.number(value);
  if(body.kind==PrimitiveBRepBody.Kind.BOX){
   BoxBRep box=BoxBRep.from(new TransformedBody(
    new CadKernel.Box(body.bodyId,-body.width/2,-body.depth/2,0,body.width,body.depth,body.height),body.pose()));
   for(BoxBRep.Vertex v:box.vertices()){b.add(Kind.VERTEX,v.id,v.id);b.point(v.x,v.y,v.z);}
  }
  // A cylinder's circular rims are closed: no arbitrary seam vertices.
  for(PrimitiveBRepBody.Edge e:body.edges()){b.add(Kind.EDGE,e.id,e.id);b.number(e.length);}
  for(PrimitiveBRepBody.Face f:body.faces()){b.add(Kind.FACE,f.id,f.id);b.number(f.area);}
  return new TopologySnapshot(b);
 }
}
