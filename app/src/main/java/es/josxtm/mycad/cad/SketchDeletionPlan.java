package es.josxtm.mycad.cad;
import java.util.*;

/**
 * Read-only dependency plan for deleting a sketch.
 * The UI must confirm KEEP (detach) or CASCADE (delete dependent solids).
 * No live model is modified until the complete plan is accepted.
 */
public final class SketchDeletionPlan {
 private SketchDeletionPlan(){}
 public enum Mode { KEEP_SOLIDS, CASCADE_DELETE }
 public static final class BodyLink {
  public final String bodyId,sourceSketchId;
  public BodyLink(String bodyId,String sourceSketchId){
   if(bodyId==null||bodyId.isEmpty()||sourceSketchId==null)
    throw new IllegalArgumentException("Invalid body dependency");
   this.bodyId=bodyId;this.sourceSketchId=sourceSketchId;
  }
  BodyLink detached(){return new BodyLink(bodyId,"");}
  @Override public boolean equals(Object obj){
   if(!(obj instanceof BodyLink))return false;
   BodyLink other=(BodyLink)obj;
   return bodyId.equals(other.bodyId)&&sourceSketchId.equals(other.sourceSketchId);
  }
  @Override public int hashCode(){return Objects.hash(bodyId,sourceSketchId);}
 }
 public static final class Plan {
  public final String targetSketchId;
  public final List<String> dependentBodyIds;
  private final List<BodyLink> original;
  private Plan(String target,List<BodyLink> original,List<String> affected){
   targetSketchId=target;
   this.original=Collections.unmodifiableList(new ArrayList<>(original));
   dependentBodyIds=Collections.unmodifiableList(new ArrayList<>(affected));
  }
  public boolean stillMatches(List<BodyLink> current){
   return original.equals(current);
  }
  /** Pure immutable result, without modifying the input or original bodies. */
  public List<BodyLink> after(Mode mode){
   if(mode==null)throw new IllegalArgumentException("Deletion mode required");
   ArrayList<BodyLink> result=new ArrayList<>();
   for(BodyLink link:original){
    if(!targetSketchId.equals(link.sourceSketchId)){result.add(link);continue;}
    if(mode==Mode.KEEP_SOLIDS)result.add(link.detached());
   }
   return Collections.unmodifiableList(result);
  }
 }
 public static Plan inspect(String targetSketchId,List<BodyLink> existing){
  if(targetSketchId==null||targetSketchId.isEmpty()||existing==null)
   throw new IllegalArgumentException("Missing sketch or bodies");
  ArrayList<String> affected=new ArrayList<>();
  HashSet<String> seen=new HashSet<>();
  for(BodyLink item:existing){
   if(item==null||!seen.add(item.bodyId))
    throw new IllegalArgumentException("Null or duplicate body");
   if(targetSketchId.equals(item.sourceSketchId))affected.add(item.bodyId);
  }
  return new Plan(targetSketchId,existing,affected);
 }
}
