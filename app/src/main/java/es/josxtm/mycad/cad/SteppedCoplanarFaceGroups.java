package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Non-destructive coplanar face grouping for stepped orthogonal B-Rep shells.
 * Groups only edge-connected faces on the same oriented plane. This does not
 * replace half-edges or claim that a group is a single simple polygon.
 */
public final class SteppedCoplanarFaceGroups {
 private SteppedCoplanarFaceGroups(){}

 /** Aggregate the analytic surface area of each edge-connected coplanar patch.
  * Face and half-edge IDs remain unchanged; these are read-only patch metrics.
  */
 /** Exposed boundary edges per coplanar patch, excluding internal shared edges.
  * Each edge ID appears at most once per patch; original half-edges are retained.
  */
 /**
  * Ordered oriented boundary half-edge loops for each coplanar patch.
  * Each loop is closed implicitly (last half-edge ends at the first start).
  * Multiple loops are retained separately, including potential holes.
  */
 /**
  * Signed projected area of each oriented patch loop, measured along its
  * outward face normal. Positive loops are exterior; negative loops are holes.
  * The signed sum must reproduce the corresponding analytic patch area.
  */
 /** Immutable counts of exterior contours and holes for every coplanar patch.
  * This is a diagnostic summary, not a topological face replacement.
  */
 public static List<List<Integer>> loopCounts(VariableHeightOrthogonalBRep shell){
  List<List<Double>> signedAreas=boundaryLoopAreas(shell);
  List<List<Integer>> result=new ArrayList<>();
  for(List<Double> patch:signedAreas){
   int exterior=0,holes=0;
   for(double area:patch){
    if(area>0)exterior++;
    else holes++;
   }
   if(exterior==0)throw new IllegalArgumentException("Patch without exterior boundary");
   result.add(Collections.unmodifiableList(Arrays.asList(exterior,holes)));
  }
  return Collections.unmodifiableList(result);
 }

 public static List<List<Double>> boundaryLoopAreas(VariableHeightOrthogonalBRep shell){
  List<List<String>> groups=find(shell);
  List<List<List<String>>> loops=boundaryLoops(shell);
  List<Double> expected=areas(shell);
  Map<String,VariableHeightOrthogonalBRep.Face> faces=new HashMap<>();
  for(VariableHeightOrthogonalBRep.Face face:shell.faces())faces.put(face.id,face);
  Map<String,VariableHeightOrthogonalBRep.Vertex> vertices=new HashMap<>();
  for(VariableHeightOrthogonalBRep.Vertex vertex:shell.vertices())vertices.put(vertex.id,vertex);
  Map<String,VariableHeightOrthogonalBRep.HalfEdge> halfEdges=new HashMap<>();
  for(VariableHeightOrthogonalBRep.HalfEdge edge:shell.halfEdges())halfEdges.put(edge.id,edge);
  List<List<Double>> result=new ArrayList<>();
  for(int i=0;i<groups.size();i++){
   double[] normal=faces.get(groups.get(i).get(0)).outwardNormal();
   int axis=0;
   for(int a=1;a<3;a++)if(Math.abs(normal[a])>Math.abs(normal[axis]))axis=a;
   if(Math.abs(normal[axis])<1-1e-8)
    throw new IllegalArgumentException("Non-axis-aligned patch");
   List<Double> patch=new ArrayList<>();
   double total=0;
   for(List<String> loop:loops.get(i)){
    double signed=0;
    for(String id:loop){
     VariableHeightOrthogonalBRep.HalfEdge edge=halfEdges.get(id);
     VariableHeightOrthogonalBRep.Vertex a=vertices.get(edge.fromId);
     VariableHeightOrthogonalBRep.Vertex b=vertices.get(edge.toId);
     if(axis==0)signed+=a.y*b.z-b.y*a.z;
     else if(axis==1)signed+=a.z*b.x-b.z*a.x;
     else signed+=a.x*b.y-b.x*a.y;
    }
    signed*=0.5*normal[axis];
    if(!Double.isFinite(signed)||Math.abs(signed)<1e-9)
     throw new IllegalArgumentException("Degenerate patch loop area");
    patch.add(signed);total+=signed;
   }
   if(Math.abs(total-expected.get(i))>1e-5*Math.max(1,expected.get(i)))
    throw new IllegalArgumentException("Patch loop area conservation failed");
   result.add(Collections.unmodifiableList(patch));
  }
  return Collections.unmodifiableList(result);
 }

 public static List<List<List<String>>> boundaryLoops(VariableHeightOrthogonalBRep shell){
  List<List<String>> groups=find(shell);
  Map<String,Integer> patchByFace=new HashMap<>();
  for(int i=0;i<groups.size();i++)
   for(String face:groups.get(i))patchByFace.put(face,i);
  Map<String,VariableHeightOrthogonalBRep.HalfEdge> byId=new HashMap<>();
  for(VariableHeightOrthogonalBRep.HalfEdge half:shell.halfEdges())byId.put(half.id,half);
  List<Map<String,VariableHeightOrthogonalBRep.HalfEdge>> outgoing=new ArrayList<>();
  for(int i=0;i<groups.size();i++)outgoing.add(new TreeMap<>());
  for(VariableHeightOrthogonalBRep.HalfEdge half:shell.halfEdges()){
   VariableHeightOrthogonalBRep.HalfEdge twin=byId.get(half.twinId);
   if(twin==null)throw new IllegalArgumentException("Missing patch twin");
   Integer a=patchByFace.get(half.faceId),b=patchByFace.get(twin.faceId);
   if(a==null||b==null)throw new IllegalArgumentException("Missing patch face");
   if(!a.equals(b)){
    VariableHeightOrthogonalBRep.HalfEdge previous=outgoing.get(a).put(half.fromId,half);
    if(previous!=null)throw new IllegalArgumentException("Ambiguous patch boundary vertex");
   }
  }
  List<List<List<String>>> patches=new ArrayList<>();
  for(Map<String,VariableHeightOrthogonalBRep.HalfEdge> byStart:outgoing){
   if(byStart.isEmpty())throw new IllegalArgumentException("Empty patch boundary");
   Set<String> visited=new HashSet<>();
   List<List<String>> loops=new ArrayList<>();
   for(VariableHeightOrthogonalBRep.HalfEdge start:byStart.values()){
    if(visited.contains(start.id))continue;
    List<String> loop=new ArrayList<>();
    VariableHeightOrthogonalBRep.HalfEdge current=start;
    do{
     if(!visited.add(current.id))
      throw new IllegalArgumentException("Repeated boundary half-edge");
     loop.add(current.id);
     current=byStart.get(current.toId);
     if(current==null)throw new IllegalArgumentException("Open patch boundary");
     if(loop.size()>byStart.size())throw new IllegalArgumentException("Unclosed patch boundary");
    }while(!current.id.equals(start.id));
    if(loop.size()<4)throw new IllegalArgumentException("Degenerate patch boundary loop");
    loops.add(Collections.unmodifiableList(loop));
   }
   if(visited.size()!=byStart.size())throw new IllegalArgumentException("Unvisited patch boundary");
   patches.add(Collections.unmodifiableList(loops));
  }
  return Collections.unmodifiableList(patches);
 }

 public static List<List<String>> boundaryEdgeIds(VariableHeightOrthogonalBRep shell){
  List<List<String>> groups=find(shell);
  Map<String,Integer> groupByFace=new HashMap<>();
  for(int i=0;i<groups.size();i++)
   for(String faceId:groups.get(i))groupByFace.put(faceId,i);
  List<List<String>> boundaries=new ArrayList<>();
  for(int i=0;i<groups.size();i++)boundaries.add(new ArrayList<>());
  for(VariableHeightOrthogonalBRep.Edge edge:shell.edges()){
   List<String> owners=edge.adjacentFaceIds();
   if(owners.size()!=2)throw new IllegalArgumentException("Nonmanifold patch edge");
   Integer a=groupByFace.get(owners.get(0)),b=groupByFace.get(owners.get(1));
   if(a==null||b==null)throw new IllegalArgumentException("Unknown patch face");
   if(!a.equals(b)){
    boundaries.get(a).add(edge.id);
    boundaries.get(b).add(edge.id);
   }
  }
  List<List<String>> result=new ArrayList<>();
  for(List<String> boundary:boundaries){
   Collections.sort(boundary);
   if(boundary.isEmpty())throw new IllegalArgumentException("Patch has no boundary");
   result.add(Collections.unmodifiableList(boundary));
  }
  return Collections.unmodifiableList(result);
 }

 public static List<Double> areas(VariableHeightOrthogonalBRep shell){
  List<List<String>> groups=find(shell);
  Map<String,Double> byId=new HashMap<>();
  for(VariableHeightOrthogonalBRep.Face face:shell.faces())
   byId.put(face.id,face.area);
  List<Double> result=new ArrayList<>();
  for(List<String> group:groups){
   double area=0;
   for(String id:group){
    Double value=byId.get(id);
    if(value==null||!Double.isFinite(value)||value<=0)
     throw new IllegalArgumentException("Invalid coplanar patch face area");
    area+=value;
   }
   if(!Double.isFinite(area)||area<=0)
    throw new IllegalArgumentException("Invalid coplanar patch area");
   result.add(area);
  }
  return Collections.unmodifiableList(result);
 }

 public static List<List<String>> find(VariableHeightOrthogonalBRep shell){
  if(shell==null)throw new IllegalArgumentException("Missing stepped B-Rep");
  Map<String,VariableHeightOrthogonalBRep.Face> faces=new LinkedHashMap<>();
  Map<String,Set<String>> adjacency=new LinkedHashMap<>();
  for(VariableHeightOrthogonalBRep.Face f:shell.faces()){
   faces.put(f.id,f);adjacency.put(f.id,new TreeSet<>());
  }
  Map<String,VariableHeightOrthogonalBRep.Vertex> vertices=new HashMap<>();
  for(VariableHeightOrthogonalBRep.Vertex v:shell.vertices())vertices.put(v.id,v);
  for(VariableHeightOrthogonalBRep.Edge edge:shell.edges()){
   List<String> owners=edge.adjacentFaceIds();
   if(owners.size()!=2)throw new IllegalArgumentException("Nonmanifold edge");
   VariableHeightOrthogonalBRep.Face a=faces.get(owners.get(0)),b=faces.get(owners.get(1));
   if(a==null||b==null)throw new IllegalArgumentException("Missing adjacent face");
   double[] an=a.outwardNormal(),bn=b.outwardNormal();
   double dot=an[0]*bn[0]+an[1]*bn[1]+an[2]*bn[2];
   VariableHeightOrthogonalBRep.Vertex av=vertices.get(a.vertexIds().get(0));
   VariableHeightOrthogonalBRep.Vertex bv=vertices.get(b.vertexIds().get(0));
   if(av==null||bv==null)throw new IllegalArgumentException("Missing face vertex");
   double offset=an[0]*(av.x-bv.x)+an[1]*(av.y-bv.y)+an[2]*(av.z-bv.z);
   if(dot>1-1e-8&&Math.abs(offset)<1e-5){
    adjacency.get(a.id).add(b.id);
    adjacency.get(b.id).add(a.id);
   }
  }
  List<List<String>> groups=new ArrayList<>();
  Set<String> seen=new HashSet<>();
  for(String id:adjacency.keySet()){
   if(!seen.add(id))continue;
   ArrayDeque<String> queue=new ArrayDeque<>();queue.add(id);
   List<String> group=new ArrayList<>();
   while(!queue.isEmpty()){
    String current=queue.removeFirst();group.add(current);
    for(String neighbor:adjacency.get(current))
     if(seen.add(neighbor))queue.addLast(neighbor);
   }
   Collections.sort(group);
   groups.add(Collections.unmodifiableList(group));
  }
  return Collections.unmodifiableList(groups);
 }
}
