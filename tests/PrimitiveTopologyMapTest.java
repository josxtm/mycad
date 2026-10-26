import es.josxtm.mycad.cad.*;
public final class PrimitiveTopologyMapTest {
 static void yes(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  String bodyA="55b7c2f1-1fb3-4a45-8c06-ce709dd38d0d";
  String bodyB="6f2cd127-2c19-438e-bf68-ea0e5188b4ad";
  PrimitiveTopologyMap m=new PrimitiveTopologyMap();
  m.addBox(0,bodyA,17); // Imported geometry and boolean meshes precede bodies.
  m.addCylinder(1,bodyB,29,48);
  PrimitiveTopologyMap.Hit face=m.resolve(17);
  yes(face!=null&&face.partIndex==0&&face.kind==PrimitiveTopologyMap.Kind.BOX,"Box mapping");
  yes(face.boxFace==BoxFaceSelection.Face.Z_MIN,"Box bottom");
  yes(face.highlightTriangle()==17,"Prism pair highlight");
  yes((bodyA+"/face/Z_MIN").equals(face.faceId),"Stable key");
  yes(m.resolve(28).boxFace==BoxFaceSelection.Face.X_MAX,"Six face tessellation");
  yes(m.resolve(16)==null,"Offset before geometry");
  PrimitiveTopologyMap.Hit side=m.resolve(29);
  yes(side.kind==PrimitiveTopologyMap.Kind.CYLINDER_SIDE&&side.highlightTriangle()==29,"First cylinder segment");
  yes(m.resolve(30).faceId.equals(side.faceId),"Shared cylinder side key");
  yes(m.resolve(31).kind==PrimitiveTopologyMap.Kind.CYLINDER_BOTTOM,"Cylinder base");
  yes(m.resolve(32).kind==PrimitiveTopologyMap.Kind.CYLINDER_TOP,"Cylinder top");
  yes(m.resolve(29+47*4).faceId.equals(side.faceId),"Side same key across segments");
  yes(m.resolve(29+192)==null,"Outside cylinder");
  PrimitiveTopologyMap changed=new PrimitiveTopologyMap();
  changed.addCylinder(0,bodyB,400,64);changed.addBox(1,bodyA,656);
  yes(changed.resolve(400).faceId.equals(side.faceId),"Key independent from reorder/resolution");
  yes(changed.resolve(656).faceId.equals(face.faceId),"Key independent from model offsets");
  boolean invalid=false;
  try{m.addBox(2,bodyA,221);}catch(IllegalArgumentException e){invalid=true;}
  yes(invalid,"Reject duplicate body ID");
  invalid=false;try{m.addBox(2,"new-body",20);}catch(IllegalArgumentException e){invalid=true;}
  yes(invalid,"Reject overlap");
  // Overlap must not poison the body ID registry: retry at the first free triangle.
  m.addBox(2,"new-body",221);
  yes(m.resolve(221)!=null&&"new-body".equals(m.resolve(221).bodyId),
      "Retry same body ID after rejected overlap");
  // Boundary arithmetic must remain valid for the largest representable triangle.
  PrimitiveTopologyMap limit=new PrimitiveTopologyMap();
  int lastStart=Integer.MAX_VALUE-11;
  limit.addBox(0,"last-valid",lastStart);
  yes(limit.resolve(Integer.MAX_VALUE)!=null,"Last addressable triangle resolves");
  yes(limit.resolve(lastStart-1)==null,"Triangle before final range stays unmapped");
  PrimitiveTopologyMap overflow=new PrimitiveTopologyMap();
  invalid=false;
  try{overflow.addBox(0,"too-large",Integer.MAX_VALUE);}catch(IllegalArgumentException e){invalid=true;}
  yes(invalid,"Reject ranges whose end exceeds addressable triangle indices");
  overflow.addBox(0,"too-large",Integer.MAX_VALUE-11);
  yes(overflow.resolve(Integer.MAX_VALUE)!=null,"Retry rejected overflow ID on valid range");
  CadFaceReference ref=new CadFaceReference(0,bodyA,BoxFaceSelection.Face.X_MIN);
  yes(ref.isValidFor(2,bodyA)&&!ref.isValidFor(2,bodyB),"Stale index protection");
  yes(ref.key().equals(bodyA+"/face/X_MIN"),"Stable reference key");
  yes(ref.equals(new CadFaceReference(1,bodyA,BoxFaceSelection.Face.X_MIN)),"Stable ID if reordered");
  yes(!ref.equals(new CadFaceReference(0,bodyB,BoxFaceSelection.Face.X_MIN)),"Different body");
  System.out.println("PrimitiveTopologyMapTest: OK");
 }
}
