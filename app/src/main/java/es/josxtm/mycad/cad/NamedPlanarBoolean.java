package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Source-incidence names for the remaining persisted planar Boolean routes.
 * Existing kernels, output shell order and triangle buffers remain unchanged.
 * Side faces include both endpoint identities: disjoint concave segments on
 * one supporting plane are distinct faces. Multi-shell caps use a boundary
 * provenance signature, so a split/merge never silently inherits a parent's cap.
 * This conservative component policy is independent of shell enumeration. */
public final class NamedPlanarBoolean {
 public final String bodyId;
 public final LinkedConvexIntersection.Operation operation;
 private final int plane;
 private final String namespace;
 private final List<PolygonPrismBRep> shells;
 private final List<Line> sources=new ArrayList<>();
 private final Map<String,Set<String>> sourceCorners=new HashMap<>();
 private final Map<String,String> vertexNames=new LinkedHashMap<>(),edgeNames=new LinkedHashMap<>(),faceNames=new LinkedHashMap<>();
 private final Map<String,PolygonPrismBRep.Vertex> vertices=new LinkedHashMap<>();
 private final Map<String,PolygonPrismBRep.Edge> edges=new LinkedHashMap<>();
 private final Map<String,PolygonPrismBRep.Face> faces=new LinkedHashMap<>();
 private final float[] triangles;
 private static final class Line {
  final String id;final double ax,ay,bx,by,length;
  Line(String id,double[] a,double[] b){this.id=id;ax=a[0];ay=a[1];bx=b[0];by=b[1];length=Math.hypot(bx-ax,by-ay);}
  boolean contains(double[] p,double tolerance){
   double dx=bx-ax,dy=by-ay;
   double distance=Math.abs((p[0]-ax)*dy-(p[1]-ay)*dx)/length;
   // Reconstructed boundaries may simplify several collinear source segments.
   return distance<=tolerance;
  }
 }
 private double[] uv(PolygonPrismBRep.Vertex v){
  return new double[]{plane==2?v.y:v.x,plane==0?v.y:v.z};
 }
 private String name(String kind,Collection<String> keys){
  List<String> sorted=new ArrayList<>(keys);Collections.sort(sorted);
  return bodyId+"/"+kind+"/"+UUID.nameUUIDFromBytes((namespace+String.join("|",sorted)).getBytes(StandardCharsets.UTF_8));
 }
 private Set<String> supports(PolygonPrismBRep.Vertex vertex){
  double[] p=uv(vertex);
  Set<String> corner=sourceCorners.get(pointKey(p));
  if(corner!=null)return new TreeSet<>(corner);
  double tolerance=Math.max(1e-7,2*Math.hypot(Math.ulp((float)p[0]),Math.ulp((float)p[1])));
  Set<String> result=new TreeSet<>();for(Line line:sources)if(line.contains(p,tolerance))result.add(line.id);
  return result;
 }
 private static String pointKey(double[] p){
  return Float.floatToIntBits((float)(p[0]==0?0:p[0]))+":"+Float.floatToIntBits((float)(p[1]==0?0:p[1]));
 }
 private Set<String> sideSupports(PolygonPrismBRep shell,PolygonPrismBRep.Face face){
  Set<String> exact=new TreeSet<>();
  for(Line line:sources){
   boolean matches=true;
   for(String vertex:face.vertexIds())if(!line.contains(uv(shell.findVertex(vertex)),1e-9)){matches=false;break;}
   if(matches)exact.add(line.id);
  }
  if(!exact.isEmpty())return exact;
  Set<String> shared=null;
  for(String vertex:face.vertexIds()){
   double[] p=uv(shell.findVertex(vertex));
   double tolerance=Math.max(1e-7,2*Math.hypot(Math.ulp((float)p[0]),Math.ulp((float)p[1])));
   Set<String> incident=new TreeSet<>();for(Line line:sources)if(line.contains(p,tolerance))incident.add(line.id);
   if(shared==null)shared=incident;else shared.retainAll(incident);
  }
  return shared;
 }
 private static <T> void unique(Map<String,T> values,String id,T entity){
  if(values.put(id,entity)!=null)throw new IllegalArgumentException("Ambiguous planar Boolean identity");
 }
 public static boolean supports(LinkedConvexIntersection.Operation operation){
  if(operation==null)return false;
  switch(operation){
   case UNION_ANALYTIC:case INTERSECTION_ANALYTIC:case DIFFERENCE_ANALYTIC:
   case UNION_ORTHOGONAL:case DIFFERENCE_ORTHOGONAL:
   case UNION_POLYGONAL:case DIFFERENCE_POLYGONAL:case INTERSECTION_POLYGONAL:
   case UNION_MULTISHELL:case DIFFERENCE_MULTISHELL:case INTERSECTION_MULTISHELL:return true;
   default:return false;
  }
 }
 private static boolean multi(LinkedConvexIntersection.Operation op){
  return op==LinkedConvexIntersection.Operation.UNION_MULTISHELL||op==LinkedConvexIntersection.Operation.DIFFERENCE_MULTISHELL||
   op==LinkedConvexIntersection.Operation.INTERSECTION_MULTISHELL;
 }
 private NamedPlanarBoolean(String owner,PolygonPrismBRep first,PolygonPrismBRep second,LinkedConvexIntersection.Operation operation){
  bodyId=owner;this.operation=operation;plane=first.plane();
  namespace=first.id()+"|"+second.id()+"/PLANAR_V1/"+operation+"/"+plane+"/";
  OrthogonalPolygonBRepBoolean.Operation op=operation.name().startsWith("UNION")?OrthogonalPolygonBRepBoolean.Operation.UNION:
   operation.name().startsWith("DIFFERENCE")?OrthogonalPolygonBRepBoolean.Operation.DIFFERENCE:OrthogonalPolygonBRepBoolean.Operation.INTERSECTION;
  List<PolygonPrismBRep> result;
  if(multi(operation))result=OrthogonalPolygonBRepBoolean.applyMulti(first,second,op).solids();
  else{
   PolygonPrismBRep shell;
   if(operation.name().endsWith("_ANALYTIC"))shell=PrismBRepBoolean.apply(first,second,PrismBRepBoolean.Operation.valueOf(op.name())).solid;
   else if(operation.name().endsWith("_ORTHOGONAL"))shell=(op==OrthogonalPolygonBRepBoolean.Operation.UNION?
    OrthogonalPrismBRepUnion.union(first,second):OrthogonalPrismBRepUnion.difference(first,second)).solid;
   else shell=OrthogonalPolygonBRepBoolean.apply(first,second,op).solid;
   result=Collections.singletonList(shell);
  }
  shells=Collections.unmodifiableList(new ArrayList<>(result));
  for(PolygonPrismBRep source:Arrays.asList(first,second)){
   for(PolygonPrismBRep.Face face:source.faces())if(face.sourceLineId!=null){
    double[] a=null,b=null;
    for(String id:face.vertexIds()){
     double[] p=uv(source.findVertex(id));
     if(a==null)a=p;else if(Math.hypot(p[0]-a[0],p[1]-a[1])>1e-10){b=p;break;}
    }
    if(a==null||b==null)throw new IllegalArgumentException("Invalid source boundary");
    String lineId=source.id()+"/line/"+face.sourceLineId;
    sources.add(new Line(lineId,a,b));
    for(String vertex:face.vertexIds())sourceCorners.computeIfAbsent(pointKey(uv(source.findVertex(vertex))),key->new TreeSet<>()).add(lineId);
   }
  }
  int length=0;
  for(PolygonPrismBRep shell:shells){
   List<String> heights=new ArrayList<>();
   for(PolygonPrismBRep source:Arrays.asList(first,second))if(Double.compare(source.distance(),shell.distance())==0)heights.add(source.id());
   if(heights.isEmpty())throw new IllegalArgumentException("Unknown Boolean height provenance");
   Collections.sort(heights);String topSources=String.join(",",heights);
   List<String> componentVertices=new ArrayList<>();
   for(PolygonPrismBRep.Vertex vertex:shell.vertices()){
    Set<String> incident=supports(vertex);
    if(incident.size()<2)throw new IllegalArgumentException("Unresolved planar vertex provenance");
    boolean base=vertex.id.contains("/vertex/base/");
    if(base)componentVertices.add(name("corner",incident));
    incident.add(base?"layer/BASE":"layer/TOP/"+topSources);
    String id=name("vertex",incident);unique(vertexNames,vertex.id,id);unique(vertices,id,vertex);
   }
   String component=multi(operation)?name("component",componentVertices):"single";
   for(PolygonPrismBRep.Edge edge:shell.edges()){
    String id=name("edge",Arrays.asList(vertexId(edge.firstVertexId),vertexId(edge.secondVertexId)));
    unique(edgeNames,edge.id,id);unique(edges,id,edge);
   }
   for(PolygonPrismBRep.Face face:shell.faces()){
    String id;
    if(face.sourceLineId==null)id=name("face",Arrays.asList("component/"+component,
     face.id.endsWith("/base")?"cap/BASE":"cap/TOP/"+topSources));
    else{
     Set<String> shared=sideSupports(shell,face);
     if(shared==null||shared.isEmpty())throw new IllegalArgumentException("Unresolved planar side provenance");
     for(String vertex:face.vertexIds())shared.add("endpoint/"+vertexId(vertex));
     shared.add("height/"+topSources);id=name("face",shared);
    }
    unique(faceNames,face.id,id);unique(faces,id,face);
   }
   length+=shell.triangles().length;
  }
  if(length>18000)throw new IllegalArgumentException("Planar Boolean exceeds mesh size limit");
  triangles=new float[length];int at=0;
  for(PolygonPrismBRep shell:shells){float[] mesh=shell.triangles();System.arraycopy(mesh,0,triangles,at,mesh.length);at+=mesh.length;}
 }
 public static NamedPlanarBoolean apply(String owner,PolygonPrismBRep first,PolygonPrismBRep second,LinkedConvexIntersection.Operation operation){
  if(owner==null||!UUID.fromString(owner).toString().equals(owner)||first==null||second==null||!supports(operation))
   throw new IllegalArgumentException("Invalid planar Boolean owner, operation or sources");
  return new NamedPlanarBoolean(owner,first,second,operation);
 }
 public List<PolygonPrismBRep> solids(){return shells;}
 public float[] triangles(){return triangles.clone();}
 private static String require(Map<String,String> names,String snapshot){
  String id=names.get(snapshot);if(id==null)throw new IllegalArgumentException("Entity belongs to another planar snapshot");return id;
 }
 public String vertexId(String snapshot){return require(vertexNames,snapshot);}
 public String edgeId(String snapshot){return require(edgeNames,snapshot);}
 public String faceId(String snapshot){return require(faceNames,snapshot);}
 public PolygonPrismBRep.Vertex findVertex(String id){return vertices.get(id);}
 public PolygonPrismBRep.Edge findEdge(String id){return edges.get(id);}
 public PolygonPrismBRep.Face findFace(String id){return faces.get(id);}
}
