package es.josxtm.mycad.cad;

import java.util.*;

/**
 * Non-destructive coplanar face grouping for stepped orthogonal B-Rep shells.
 * Groups only edge-connected faces on the same oriented plane. This does not
 * replace half-edges or claim that a group is a single simple polygon.
 */
public final class SteppedCoplanarFaceGroups {
 private SteppedCoplanarFaceGroups(){}

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
