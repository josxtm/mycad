package es.josxtm.mycad.cad;

import java.util.*;

/** Exact identity-set reconciliation. Missing entities remain unresolved;
 * no geometric-proximity fallback transfers a reference to a different edge. */
public final class TopologyReconciliation {
 public static final class Change {
  public final Set<String> retained,created,removed;
  private Change(Set<String> oldIds,Set<String> newIds){
   Set<String> keep=new TreeSet<>(oldIds);keep.retainAll(newIds);
   Set<String> add=new TreeSet<>(newIds);add.removeAll(oldIds);
   Set<String> drop=new TreeSet<>(oldIds);drop.removeAll(newIds);
   retained=Collections.unmodifiableSet(keep);created=Collections.unmodifiableSet(add);removed=Collections.unmodifiableSet(drop);
  }
 }
 public final TopologySnapshot before,after;
 private final Map<TopologySnapshot.Kind,Change> changes;
 private TopologyReconciliation(TopologySnapshot before,TopologySnapshot after){
  if(before==null||after==null||!before.bodyId.equals(after.bodyId))
   throw new IllegalArgumentException("Cannot reconcile different topology owners");
  this.before=before;this.after=after;
  Map<TopologySnapshot.Kind,Change> built=new EnumMap<>(TopologySnapshot.Kind.class);
  for(TopologySnapshot.Kind kind:TopologySnapshot.Kind.values())built.put(kind,new Change(before.ids(kind),after.ids(kind)));
  changes=Collections.unmodifiableMap(built);
 }
 public static TopologyReconciliation between(TopologySnapshot before,TopologySnapshot after){
  return new TopologyReconciliation(before,after);
 }
 public Change change(TopologySnapshot.Kind kind){
  if(kind==null)throw new IllegalArgumentException("Missing entity kind");return changes.get(kind);
 }
 /** Explicitly rebind only descriptors from the precise before snapshot. */
 public TopologySnapshot.Reference rebind(TopologySnapshot.Reference old){
  return before.resolveExact(old)==null?null:after.resolvePersistent(old);
 }
}
