package es.josxtm.mycad.cad;

import java.util.*;

/** Source-derived identities for same-height orthogonal shells, including
 * through-holes and disconnected components. All names belong to one recipe. */
public final class NamedGridHoleBody {
 public final String bodyId;
 public final OrthogonalGridHoleBRep shell;
 private final Map<String,String> vertexNames=new LinkedHashMap<>(),edgeNames=new LinkedHashMap<>(),faceNames=new LinkedHashMap<>();
 private final Map<String,OrthogonalGridHoleBRep.Vertex> vertices=new LinkedHashMap<>();
 private final Map<String,OrthogonalGridHoleBRep.Edge> edges=new LinkedHashMap<>();
 private final Map<String,OrthogonalGridHoleBRep.Face> faces=new LinkedHashMap<>();
 private NamedGridHoleBody(String owner,PolygonPrismBRep first,PolygonPrismBRep second,
                          OrthogonalPolygonBRepBoolean.Operation operation){
  SourceTopologyNames names=new SourceTopologyNames(owner,first,second,first.plane(),"GRID_HOLES/"+operation);
  bodyId=owner;shell=OrthogonalGridHoleBRep.apply(first,second,operation);
  for(OrthogonalGridHoleBRep.Vertex vertex:shell.vertices()){
   String stable=names.vertexName(vertex.x,vertex.y,vertex.z);
   vertexNames.put(vertex.id,stable);unique(vertices,stable,vertex);
  }
  for(OrthogonalGridHoleBRep.Edge edge:shell.edges()){
   String stable=names.edgeName(vertexId(edge.firstVertexId),vertexId(edge.secondVertexId));
   edgeNames.put(edge.id,stable);unique(edges,stable,edge);
  }
  for(OrthogonalGridHoleBRep.Face face:shell.faces()){
   List<String> corners=new ArrayList<>();for(String vertex:face.vertexIds())corners.add(vertexId(vertex));
   String stable=names.faceName(corners,face.outwardNormal());
   faceNames.put(face.id,stable);unique(faces,stable,face);
  }
 }
 private static <T> void unique(Map<String,T> values,String id,T entity){
  if(values.put(id,entity)!=null)throw new IllegalArgumentException("Ambiguous grid topology name");
 }
 public static NamedGridHoleBody apply(String owner,PolygonPrismBRep first,PolygonPrismBRep second,
                                      OrthogonalPolygonBRepBoolean.Operation operation){
  if(first==null||second==null||operation==null)throw new IllegalArgumentException("Missing grid operands");
  return new NamedGridHoleBody(owner,first,second,operation);
 }
 private static String require(Map<String,String> names,String snapshotId){
  String stable=names.get(snapshotId);
  if(stable==null)throw new IllegalArgumentException("Entity belongs to another geometry snapshot");
  return stable;
 }
 public String vertexId(String snapshotId){return require(vertexNames,snapshotId);}
 public String edgeId(String snapshotId){return require(edgeNames,snapshotId);}
 public String faceId(String snapshotId){return require(faceNames,snapshotId);}
 public OrthogonalGridHoleBRep.Vertex findVertex(String id){return vertices.get(id);}
 public OrthogonalGridHoleBRep.Edge findEdge(String id){return edges.get(id);}
 public OrthogonalGridHoleBRep.Face findFace(String id){return faces.get(id);}
 /** Exact quad order used by this shell's two-triangle face tessellation. */
 public OrthogonalGridHoleBRep.Face faceAtTriangle(int triangle){
  return triangle<0||triangle/2>=shell.faces().size()?null:shell.faces().get(triangle/2);
 }
}
