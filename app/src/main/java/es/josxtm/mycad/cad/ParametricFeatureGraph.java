package es.josxtm.mycad.cad;
import java.util.*;

/**
 * Stable-ID dependency graph across sketches, linked primitive bodies,
 * extrusion chains and chain Boolean features.
 *
 * Read-only topology and impact planning, not a B-Rep kernel. Ordering is
 * deterministic (insertion order, parents before descendants).
 */
public final class ParametricFeatureGraph {
 public enum Kind { SKETCH, BODY, CHAIN, BOOLEAN }
 public static final class Node {
  public final String id,label;
  public final Kind kind;
  private final List<String> parents;
  private Node(String id,Kind kind,String label,List<String> parents){
   this.id=id;this.kind=kind;this.label=label;
   this.parents=Collections.unmodifiableList(new ArrayList<>(parents));
  }
  public List<String> parentIds(){return parents;}
 }
 public static final class Builder {
  private final LinkedHashMap<String,Node> nodes=new LinkedHashMap<>();
  private final LinkedHashMap<String,LinkedHashSet<String>> edges=new LinkedHashMap<>();
  public Builder add(String id,Kind kind,String label){
   if(id==null||id.isEmpty()||kind==null||label==null||nodes.containsKey(id))
    throw new IllegalArgumentException("Invalid/duplicate feature ID");
   nodes.put(id,new Node(id,kind,label,Collections.emptyList()));
   edges.put(id,new LinkedHashSet<>());
   return this;
  }
  /** Child depends on parent; parent changes invalidate child. */
  public Builder dependsOn(String childId,String parentId){
   if(childId==null||parentId==null||childId.equals(parentId))
    throw new IllegalArgumentException("Invalid feature dependency");
   if(!edges.containsKey(childId)||!edges.containsKey(parentId))
    throw new IllegalArgumentException("Missing feature in dependency edge");
   edges.get(childId).add(parentId);
   return this;
  }
  public ParametricFeatureGraph build(){
   LinkedHashMap<String,Node> copy=new LinkedHashMap<>();
   for(Node source:nodes.values())
    copy.put(source.id,new Node(source.id,source.kind,source.label,
     new ArrayList<>(edges.get(source.id))));
   return new ParametricFeatureGraph(copy);
  }
 }
 private final Map<String,Node> nodes;
 private final List<Node> sorted;
 private final Map<String,List<String>> children;
 private ParametricFeatureGraph(LinkedHashMap<String,Node> source){
  nodes=Collections.unmodifiableMap(new LinkedHashMap<>(source));
  LinkedHashMap<String,List<String>> downstream=new LinkedHashMap<>();
  LinkedHashMap<String,Integer> indegree=new LinkedHashMap<>();
  for(Node node:source.values()){
   downstream.put(node.id,new ArrayList<>());
   indegree.put(node.id,node.parentIds().size());
  }
  for(Node child:source.values())for(String parent:child.parentIds())
   downstream.get(parent).add(child.id);
  ArrayDeque<String> pending=new ArrayDeque<>();
  for(String key:source.keySet())if(indegree.get(key)==0)pending.add(key);
  ArrayList<Node> order=new ArrayList<>();
  while(!pending.isEmpty()){
   String id=pending.removeFirst();
   order.add(source.get(id));
   for(String child:downstream.get(id)){
    int remaining=indegree.get(child)-1;
    indegree.put(child,remaining);
    if(remaining==0)pending.addLast(child);
   }
  }
  if(order.size()!=source.size())throw new IllegalArgumentException("Cycle in feature dependencies");
  sorted=Collections.unmodifiableList(order);
  LinkedHashMap<String,List<String>> immutable=new LinkedHashMap<>();
  for(Map.Entry<String,List<String>> entry:downstream.entrySet())
   immutable.put(entry.getKey(),Collections.unmodifiableList(entry.getValue()));
  children=Collections.unmodifiableMap(immutable);
 }
 public List<Node> executionOrder(){return sorted;}
 public Node find(String id){return nodes.get(id);}
 public int size(){return nodes.size();}
 /** Transitive invalidations, excluding source. Kept in dependency-safe order. */
 public List<Node> affectedBy(String sourceId){
  if(!nodes.containsKey(sourceId))throw new IllegalArgumentException("Unknown source");
  HashSet<String> affected=new HashSet<>();
  ArrayDeque<String> pending=new ArrayDeque<>();pending.add(sourceId);
  while(!pending.isEmpty()){
   for(String id:children.get(pending.removeFirst()))
    if(affected.add(id))pending.addLast(id);
  }
  ArrayList<Node> ordered=new ArrayList<>();
  for(Node node:sorted)if(affected.contains(node.id))ordered.add(node);
  return Collections.unmodifiableList(ordered);
 }
}
