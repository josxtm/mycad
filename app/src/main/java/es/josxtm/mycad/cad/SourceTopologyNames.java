package es.josxtm.mycad.cad;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Shared source-coordinate naming; the stepped v1 byte format stays unchanged. */
final class SourceTopologyNames {
 private final String owner,namespace;
 private final List<Map<Integer,String>> axes=new ArrayList<>();
 SourceTopologyNames(String owner,PolygonPrismBRep first,PolygonPrismBRep second,
                     int plane,String operation){
  if(owner==null||!UUID.fromString(owner).toString().equals(owner)||first==null||second==null)
   throw new IllegalArgumentException("Invalid topology naming sources");
  this.owner=owner;namespace=first.id()+"|"+second.id()+"/"+plane+"/"+operation+"/";
  List<Map<Integer,SortedSet<String>>> origins=new ArrayList<>();
  for(int k=0;k<3;k++){origins.add(new HashMap<>());axes.add(new HashMap<>());}
  for(PolygonPrismBRep source:Arrays.asList(first,second))
   for(PolygonPrismBRep.Vertex vertex:source.vertices()){
    double[] xyz={vertex.x,vertex.y,vertex.z};
    for(int k=0;k<3;k++)origins.get(k).computeIfAbsent(coordinate(xyz[k]),v->new TreeSet<>()).add(vertex.id);
   }
  for(int k=0;k<3;k++)for(Map.Entry<Integer,SortedSet<String>> entry:origins.get(k).entrySet())
   axes.get(k).put(entry.getKey(),k+":"+String.join(",",entry.getValue()));
 }
 private static int coordinate(double value){
  float f=(float)value;
  if(!Float.isFinite(f))throw new IllegalArgumentException("Invalid naming coordinate");
  return Float.floatToIntBits(f==0?0:f);
 }
 String name(String kind,Collection<String> sourceKeys){
  ArrayList<String> keys=new ArrayList<>(sourceKeys);Collections.sort(keys);
  String seed=namespace+String.join("|",keys);
  return owner+"/"+kind+"/"+UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
 }
 String vertexName(double x,double y,double z){
  double[] xyz={x,y,z};List<String> keys=new ArrayList<>();
  for(int k=0;k<3;k++){
   String origin=axes.get(k).get(coordinate(xyz[k]));
   if(origin==null)throw new IllegalArgumentException("Shell coordinate has no source identity");
   keys.add(origin);
  }
  return name("vertex",keys);
 }
 String edgeName(String first,String second){return name("edge",Arrays.asList(first,second));}
 String faceName(Collection<String> corners,double[] normal){
  List<String> keys=new ArrayList<>(corners);
  for(int k=0;k<3;k++)keys.add("normal/"+k+"/"+(normal[k]==0?0:normal[k]));
  return name("face",keys);
 }
}
