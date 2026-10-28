package es.josxtm.mycad.cad;

import java.util.*;

/** Persistent owner and conservative source-derived names, separate from the
 * geometry-specific shell ID used by ray/triangle snapshot guards.
 * Names survive parameter changes while the source-coordinate arrangement
 * stays the same. Coincident coordinates splitting/merging change their names;
 * no nearest-point heuristic silently transfers an identity to another entity.
 */
public final class NamedSteppedBody {
 public final String bodyId;
 public final VariableHeightOrthogonalBRep shell;
 private final SourceTopologyNames names;
 private final Map<String,String> vertexNames=new LinkedHashMap<>(),
  edgeNames=new LinkedHashMap<>(),faceNames=new LinkedHashMap<>();
 private final Map<String,VariableHeightOrthogonalBRep.Vertex> vertices=new LinkedHashMap<>();
 private final Map<String,VariableHeightOrthogonalBRep.Edge> edges=new LinkedHashMap<>();
 private final Map<String,VariableHeightOrthogonalBRep.Face> faces=new LinkedHashMap<>();

 private static <T> void unique(Map<String,T> map,String key,T value){
  if(map.put(key,value)!=null)throw new IllegalArgumentException("Ambiguous persistent topology name");
 }
 private NamedSteppedBody(String owner,VariableHeightOrthogonalBRep shell,
     PolygonPrismBRep first,PolygonPrismBRep second){
  bodyId=owner;this.shell=shell;
  names=new SourceTopologyNames(owner,first,second,shell.plane(),shell.operation().name());
  for(VariableHeightOrthogonalBRep.Vertex vertex:shell.vertices()){
   String stable=names.vertexName(vertex.x,vertex.y,vertex.z);
   vertexNames.put(vertex.id,stable);unique(vertices,stable,vertex);
  }
  for(VariableHeightOrthogonalBRep.Edge edge:shell.edges()){
   String stable=names.edgeName(vertexId(edge.firstVertexId),vertexId(edge.secondVertexId));
   edgeNames.put(edge.id,stable);unique(edges,stable,edge);
  }
  for(VariableHeightOrthogonalBRep.Face face:shell.faces()){
   List<String> keys=new ArrayList<>();
   for(String vertex:face.vertexIds())keys.add(vertexId(vertex));
   String stable=names.faceName(keys,face.outwardNormal());faceNames.put(face.id,stable);unique(faces,stable,face);
  }
 }
 public static NamedSteppedBody apply(String owner,PolygonPrismBRep first,
     PolygonPrismBRep second,VariableHeightOrthogonalMesh.Operation operation){
  if(owner==null||!UUID.fromString(owner).toString().equals(owner))
   throw new IllegalArgumentException("Invalid persistent body UUID");
  return new NamedSteppedBody(owner,VariableHeightOrthogonalBRep.apply(first,second,operation),first,second);
 }
 private static String require(Map<String,String> names,String key){
  String value=names.get(key);
  if(value==null)throw new IllegalArgumentException("Entity belongs to another geometry snapshot");
  return value;
 }
 public String vertexId(String snapshotId){return require(vertexNames,snapshotId);}
 public String edgeId(String snapshotId){return require(edgeNames,snapshotId);}
 public String faceId(String snapshotId){return require(faceNames,snapshotId);}
 public VariableHeightOrthogonalBRep.Vertex findVertex(String persistentId){return vertices.get(persistentId);}
 public VariableHeightOrthogonalBRep.Edge findEdge(String persistentId){return edges.get(persistentId);}
 public VariableHeightOrthogonalBRep.Face findFace(String persistentId){return faces.get(persistentId);}
 /** A patch keeps its name only when its source-derived set of quad faces does. */
 public String patchId(SteppedCoplanarPatchTopology.PatchFace patch){
  if(patch==null||patch.sourceFaceIds.isEmpty())throw new IllegalArgumentException("Missing patch");
  List<String> keys=new ArrayList<>();
  for(String source:patch.sourceFaceIds)keys.add(faceId(source));
  return names.name("patch",keys);
 }
}
