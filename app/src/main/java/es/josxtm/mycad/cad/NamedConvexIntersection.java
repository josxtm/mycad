package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Names a convex prism intersection by incidence with current source lines.
 * New crossing vertices are identified by their supporting line UUIDs, not by
 * matching old positions. Caps have semantic base/top roles. A change of the
 * active top-height source conservatively invalidates top-dependent names. */
public final class NamedConvexIntersection {
 public final String bodyId;
 public final PolygonPrismBRep shell;
 private final String namespace,topSources;
 private final List<Line> sources=new ArrayList<>();
 private final Map<String,Set<String>> sourceCorners=new HashMap<>();
 private final Map<String,String> vertexNames=new LinkedHashMap<>(),edgeNames=new LinkedHashMap<>(),faceNames=new LinkedHashMap<>();
 private final Map<String,PolygonPrismBRep.Vertex> vertices=new LinkedHashMap<>();
 private final Map<String,PolygonPrismBRep.Edge> edges=new LinkedHashMap<>();
 private final Map<String,PolygonPrismBRep.Face> faces=new LinkedHashMap<>();
 private static final class Line {
  final String id;final double ax,ay,bx,by,length;
  Line(String id,double[] a,double[] b){this.id=id;ax=a[0];ay=a[1];bx=b[0];by=b[1];length=Math.hypot(bx-ax,by-ay);}
  boolean contains(double[] p,double tolerance){
   double dx=bx-ax,dy=by-ay;
   double distance=Math.abs((p[0]-ax)*dy-(p[1]-ay)*dx)/length;
   // Convex clipping uses supporting half-planes, including collinear chains.
   return distance<=tolerance;
  }
 }
 private double[] uv(PolygonPrismBRep.Vertex v){
  return new double[]{shell.plane()==2?v.y:v.x,shell.plane()==0?v.y:v.z};
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
 private Set<String> sideSupports(PolygonPrismBRep.Face face){
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
  if(values.put(id,entity)!=null)throw new IllegalArgumentException("Ambiguous intersection identity");
 }
 private NamedConvexIntersection(String owner,PolygonPrismBRep first,PolygonPrismBRep second){
  bodyId=owner;shell=ConvexPrismBRepIntersection.intersect(first,second).solid;
  namespace=first.id()+"|"+second.id()+"/CONVEX_INTERSECTION/"+shell.plane()+"/";
  List<String> heights=new ArrayList<>();
  for(PolygonPrismBRep source:Arrays.asList(first,second)){
   if(Double.compare(source.distance(),shell.distance())==0)heights.add(source.id());
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
  Collections.sort(heights);topSources=String.join(",",heights);
  for(PolygonPrismBRep.Vertex vertex:shell.vertices()){
   Set<String> incident=supports(vertex);
   if(incident.size()<2)throw new IllegalArgumentException("Unresolved intersection vertex provenance");
   incident.add(vertex.id.contains("/vertex/base/")?"layer/BASE":"layer/TOP/"+topSources);
   String id=name("vertex",incident);vertexNames.put(vertex.id,id);unique(vertices,id,vertex);
  }
  for(PolygonPrismBRep.Edge edge:shell.edges()){
   String id=name("edge",Arrays.asList(vertexId(edge.firstVertexId),vertexId(edge.secondVertexId)));
   edgeNames.put(edge.id,id);unique(edges,id,edge);
  }
  for(PolygonPrismBRep.Face face:shell.faces()){
   String id;
   if(face.sourceLineId==null)id=name("face",Collections.singletonList(
    face.id.endsWith("/base")?"cap/BASE":"cap/TOP/"+topSources));
   else{
    Set<String> shared=sideSupports(face);
    if(shared==null||shared.isEmpty())throw new IllegalArgumentException("Unresolved side-face provenance");
    shared.add("height/"+topSources);id=name("face",shared);
   }
   faceNames.put(face.id,id);unique(faces,id,face);
  }
 }
 public static NamedConvexIntersection apply(String owner,PolygonPrismBRep first,PolygonPrismBRep second){
  if(owner==null||!UUID.fromString(owner).toString().equals(owner)||first==null||second==null)
   throw new IllegalArgumentException("Invalid intersection owner or sources");
  return new NamedConvexIntersection(owner,first,second);
 }
 private static String require(Map<String,String> names,String snapshot){
  String id=names.get(snapshot);if(id==null)throw new IllegalArgumentException("Entity belongs to another intersection snapshot");return id;
 }
 public String vertexId(String snapshot){return require(vertexNames,snapshot);}
 public String edgeId(String snapshot){return require(edgeNames,snapshot);}
 public String faceId(String snapshot){return require(faceNames,snapshot);}
 public PolygonPrismBRep.Vertex findVertex(String id){return vertices.get(id);}
 public PolygonPrismBRep.Edge findEdge(String id){return edges.get(id);}
 public PolygonPrismBRep.Face findFace(String id){return faces.get(id);}
}
