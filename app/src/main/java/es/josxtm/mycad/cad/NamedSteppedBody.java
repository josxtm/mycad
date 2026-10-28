package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
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
 private final String sourceSignature;
 private final Map<String,String> vertexNames=new LinkedHashMap<>(),
  edgeNames=new LinkedHashMap<>(),faceNames=new LinkedHashMap<>();
 private final Map<String,VariableHeightOrthogonalBRep.Vertex> vertices=new LinkedHashMap<>();
 private final Map<String,VariableHeightOrthogonalBRep.Edge> edges=new LinkedHashMap<>();
 private final Map<String,VariableHeightOrthogonalBRep.Face> faces=new LinkedHashMap<>();

 private static int coordinate(double value){
  float f=(float)value;
  if(!Float.isFinite(f))throw new IllegalArgumentException("Invalid naming coordinate");
  return Float.floatToIntBits(f==0?0:f);
 }
 private String name(String kind,Collection<String> sourceKeys){
  ArrayList<String> keys=new ArrayList<>(sourceKeys);Collections.sort(keys);
  String seed=sourceSignature+"/"+shell.plane()+"/"+shell.operation()+"/"+String.join("|",keys);
  return bodyId+"/"+kind+"/"+UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
 }
 private static <T> void unique(Map<String,T> map,String key,T value){
  if(map.put(key,value)!=null)throw new IllegalArgumentException("Ambiguous persistent topology name");
 }
 private NamedSteppedBody(String owner,VariableHeightOrthogonalBRep shell,
     PolygonPrismBRep first,PolygonPrismBRep second){
  bodyId=owner;this.shell=shell;sourceSignature=first.id()+"|"+second.id();
  List<Map<Integer,SortedSet<String>>> axes=new ArrayList<>();
  for(int i=0;i<3;i++)axes.add(new HashMap<>());
  for(PolygonPrismBRep source:Arrays.asList(first,second))
   for(PolygonPrismBRep.Vertex vertex:source.vertices()){
    double[] xyz={vertex.x,vertex.y,vertex.z};
    for(int k=0;k<3;k++)axes.get(k).computeIfAbsent(coordinate(xyz[k]),v->new TreeSet<>()).add(vertex.id);
   }
  for(VariableHeightOrthogonalBRep.Vertex vertex:shell.vertices()){
   double[] xyz={vertex.x,vertex.y,vertex.z};List<String> keys=new ArrayList<>();
   for(int k=0;k<3;k++){
    SortedSet<String> origins=axes.get(k).get(coordinate(xyz[k]));
    if(origins==null)throw new IllegalArgumentException("Shell coordinate has no source identity");
    keys.add(k+":"+String.join(",",origins));
   }
   String stable=name("vertex",keys);vertexNames.put(vertex.id,stable);unique(vertices,stable,vertex);
  }
  for(VariableHeightOrthogonalBRep.Edge edge:shell.edges()){
   String stable=name("edge",Arrays.asList(vertexId(edge.firstVertexId),vertexId(edge.secondVertexId)));
   edgeNames.put(edge.id,stable);unique(edges,stable,edge);
  }
  for(VariableHeightOrthogonalBRep.Face face:shell.faces()){
   List<String> keys=new ArrayList<>();
   for(String vertex:face.vertexIds())keys.add(vertexId(vertex));
   double[] normal=face.outwardNormal();
   for(int k=0;k<3;k++)keys.add("normal/"+k+"/"+(normal[k]==0?0:normal[k]));
   String stable=name("face",keys);faceNames.put(face.id,stable);unique(faces,stable,face);
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
  return name("patch",keys);
 }
}
