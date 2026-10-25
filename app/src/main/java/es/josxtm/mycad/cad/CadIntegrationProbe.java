package es.josxtm.mycad.cad;

import java.util.*;

/** Read-only compatibility probe between Android's legacy sketch data and the new CAD core. */
public final class CadIntegrationProbe {
 private CadIntegrationProbe(){}
 public static final class SketchData {
  public final String id; public final int type,plane;public final double x1,y1,x2,y2;
  public SketchData(String id,int type,int plane,double x1,double y1,double x2,double y2){
   this.id=id;this.type=type;this.plane=plane;this.x1=x1;this.y1=y1;this.x2=x2;this.y2=y2;
  }
 }
 public static final class PartData {
  public final String sourceSketchId;public final double height;
  public PartData(String sourceSketchId,double height){this.sourceSketchId=sourceSketchId;this.height=height;}
 }
 public static final class Report {
  public final int compatibleSketches,compatibleExtrusions,unsupportedSketches,unlinkedParts,invalidExtrusions;
  public final String parametricSnapshot;
  Report(int s,int e,int u,int p,int i,String snapshot){compatibleSketches=s;compatibleExtrusions=e;unsupportedSketches=u;unlinkedParts=p;invalidExtrusions=i;parametricSnapshot=snapshot;}
  public boolean readyForMigration(){return unsupportedSketches==0&&unlinkedParts==0&&invalidExtrusions==0;}
 }
 public static Report inspect(List<SketchData> sketches,List<PartData> parts){
  if(sketches==null||parts==null)throw new IllegalArgumentException("Data required");
  ParametricSketchDocument model=new ParametricSketchDocument(new PrimitiveKernel());
  Map<String,String> linked=new HashMap<>();
  int unsupported=0,invalid=0,unlinked=0,extrusions=0;
  for(SketchData sketch:sketches){
   if(sketch==null||sketch.id==null||sketch.type!=2||sketch.plane<0||sketch.plane>2){unsupported++;continue;}
   PlaneExtrusion.Plane plane=PlaneExtrusion.Plane.values()[sketch.plane];
   String id=model.addRectangleOnPlane(plane,Math.min(sketch.x1,sketch.x2),Math.min(sketch.y1,sketch.y2),
    Math.abs(sketch.x2-sketch.x1),Math.abs(sketch.y2-sketch.y1));
   if(id==null||linked.putIfAbsent(sketch.id,id)!=null)unsupported++;
  }
  for(PartData part:parts){
   if(part==null||part.sourceSketchId==null||part.sourceSketchId.isEmpty()){unlinked++;continue;}
   String source=linked.get(part.sourceSketchId);
   if(source==null){invalid++;continue;}
   if(!model.extrude(source,0,part.height).isSuccess())invalid++;
   else extrusions++;
  }
  if(!model.validateIntegrity().isEmpty())throw new IllegalStateException("Probe generated invalid geometry");
  String snapshot=model.exportSnapshot();
  ParametricSketchDocument restored=ParametricSketchDocument.importSnapshot(new PrimitiveKernel(),snapshot);
  if(!restored.validateIntegrity().isEmpty()||restored.bodies().size()!=extrusions)
   throw new IllegalStateException("Failed roundtrip of parametric preview");
  return new Report(linked.size(),extrusions,unsupported,unlinked,invalid,snapshot);
 }
}
