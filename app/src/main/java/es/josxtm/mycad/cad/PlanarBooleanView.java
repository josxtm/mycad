package es.josxtm.mycad.cad;

import java.util.*;

/** Normalized read-only face view for the existing grid and convex Boolean families. */
public final class PlanarBooleanView {
 public static final class Face {
  public final String id,snapshotId;
  public final double area;
  private Face(String id,String snapshot,double area){this.id=id;snapshotId=snapshot;this.area=area;}
 }
 public final String bodyId,geometryId;
 public final int solidCount,holeCount,vertexCount,edgeCount,faceCount;
 public final TopologySnapshot topology;
 private final float[] triangles;
 private final Face[] triangleFaces;
 private final Map<String,Face> faces;
 private PlanarBooleanView(String body,String geometry,int solids,int holes,int vertices,int edges,
      float[] triangles,List<Face> named,List<String> triangleIds,TopologySnapshot topology){
  bodyId=body;geometryId=geometry;solidCount=solids;holeCount=holes;vertexCount=vertices;edgeCount=edges;
  this.triangles=triangles.clone();this.topology=topology;faceCount=named.size();
  Map<String,Face> bySnapshot=new HashMap<>(),byId=new LinkedHashMap<>();
  for(Face face:named){bySnapshot.put(face.snapshotId,face);byId.put(face.id,face);}
  if(byId.size()!=named.size()||triangles.length/9!=triangleIds.size())throw new IllegalArgumentException("Invalid planar face map");
  triangleFaces=new Face[triangleIds.size()];
  for(int i=0;i<triangleFaces.length;i++){
   triangleFaces[i]=bySnapshot.get(triangleIds.get(i));
   if(triangleFaces[i]==null)throw new IllegalArgumentException("Unknown rendered Boolean face");
  }
  faces=Collections.unmodifiableMap(byId);
 }
 public float[] triangles(){return triangles.clone();}
 public Face findFace(String id){return faces.get(id);}
 public Face faceAtTriangle(int triangle){return triangle<0||triangle>=triangleFaces.length?null:triangleFaces[triangle];}
 public int[] trianglesForFace(String id,int first){
  if(first<0||first>Integer.MAX_VALUE-triangleFaces.length)throw new IllegalArgumentException("Invalid global triangle offset");
  int count=0;for(Face face:triangleFaces)if(face.id.equals(id))count++;
  int[] result=new int[count];int at=0;
  for(int i=0;i<triangleFaces.length;i++)if(triangleFaces[i].id.equals(id))result[at++]=first+i;
  return result;
 }
 public static PlanarBooleanView from(NamedGridHoleBody body){
  if(body==null)throw new IllegalArgumentException("Missing named grid");
  List<Face> faces=new ArrayList<>();List<String> triangles=new ArrayList<>();
  for(OrthogonalGridHoleBRep.Face face:body.shell.faces()){
   faces.add(new Face(body.faceId(face.id),face.id,face.area));triangles.add(face.id);triangles.add(face.id);
  }
  return new PlanarBooleanView(body.bodyId,body.shell.id,body.shell.solidCount,body.shell.holeCount,
   body.shell.vertices().size(),body.shell.edges().size(),body.shell.triangles(),faces,triangles,TopologySnapshot.from(body));
 }
 public static PlanarBooleanView from(NamedConvexIntersection body){
  if(body==null)throw new IllegalArgumentException("Missing named intersection");
  List<Face> faces=new ArrayList<>();List<String> triangles=new ArrayList<>();
  for(PolygonPrismBRep.Face face:body.shell.faces())faces.add(new Face(body.faceId(face.id),face.id,face.area));
  float[] mesh=body.shell.triangles();
  for(int i=0;i<mesh.length/9;i++){
   PolygonPrismBRep.Face face=PolygonPrismFacePicker.resolve(body.shell,i);
   if(face==null)throw new IllegalArgumentException("Missing intersection face");triangles.add(face.id);
  }
  return new PlanarBooleanView(body.bodyId,body.shell.id(),1,0,body.shell.vertices().size(),body.shell.edges().size(),
   mesh,faces,triangles,TopologySnapshot.from(body));
 }
 public static PlanarBooleanView from(NamedPlanarBoolean body){
  if(body==null)throw new IllegalArgumentException("Missing named planar Boolean");
  List<Face> faces=new ArrayList<>();List<String> triangles=new ArrayList<>();
  int vertices=0,edges=0;StringBuilder geometry=new StringBuilder();
  for(PolygonPrismBRep shell:body.solids()){
   geometry.append(shell.id()).append('/');vertices+=shell.vertices().size();edges+=shell.edges().size();
   for(PolygonPrismBRep.Face face:shell.faces())faces.add(new Face(body.faceId(face.id),face.id,face.area));
   float[] mesh=shell.triangles();
   for(int i=0;i<mesh.length/9;i++){
    PolygonPrismBRep.Face face=PolygonPrismFacePicker.resolve(shell,i);
    if(face==null)throw new IllegalArgumentException("Missing planar Boolean face");triangles.add(face.id);
   }
  }
  return new PlanarBooleanView(body.bodyId,geometry.toString(),body.solids().size(),0,vertices,edges,
   body.triangles(),faces,triangles,TopologySnapshot.from(body));
 }
}
