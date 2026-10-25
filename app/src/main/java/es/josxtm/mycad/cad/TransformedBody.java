package es.josxtm.mycad.cad;

/** Immutable box geometry plus its rigid world-space transform. */
public final class TransformedBody {
 public final CadKernel.Box local;
 public final RigidTransform transform;
 public TransformedBody(CadKernel.Box local,RigidTransform transform){
  if(local==null||transform==null)throw new IllegalArgumentException("Box and transform required");
  this.local=local;this.transform=transform;
 }
 public double[][] worldVertices(){return transform.vertices(local);}
 public double[] worldBounds(){return transform.bounds(local);}
 public double volume(){return local.volume();}
 public double[] center(){
  return transform.point(local.x+local.width/2,local.y+local.depth/2,local.z+local.height/2);
 }
}
