package es.josxtm.mycad;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.content.Context;
import java.nio.*;
import java.util.*;
import java.io.*;

/** GPU mesh viewer foundation for MyCAD 0.4.4. Coordinates in millimetres. */
public final class MeshEngine extends GLSurfaceView implements GLSurfaceView.Renderer {
 public static final class Mesh {
  public final float[] vertices; // xyz triangles
  private es.josxtm.mycad.cad.MeshRayIndex pickIndex;
  private double[] preciseWorld;
  /** Optional analytic shell, present only for exact regenerated preview meshes. */
  public final es.josxtm.mycad.cad.VariableHeightOrthogonalBRep steppedShell;
  public Mesh(float[] v){this(v,null);}
  public Mesh(es.josxtm.mycad.cad.VariableHeightOrthogonalBRep shell){
   this(shell==null?null:shell.triangles(),shell);
  }
  private Mesh(float[] v,es.josxtm.mycad.cad.VariableHeightOrthogonalBRep shell){
   if(v==null||v.length%9!=0)throw new IllegalArgumentException("Triangle data required");
   if(shell!=null&&!Arrays.equals(v,shell.triangles()))
    throw new IllegalArgumentException("Stepped shell does not match mesh");
   vertices=v;steppedShell=shell;
  }
  /** The existing world-float buffer remains the export/legacy contract.
   * Accurate double geometry is retained separately for the GPU conversion. */
  public static Mesh analyticBox(es.josxtm.mycad.cad.BoxBRep box){
   if(box==null)throw new IllegalArgumentException("Missing analytic box");
   return withWorldCoordinates(box.triangles(),box.triangleCoordinates());
  }
  static Mesh withWorldCoordinates(float[] legacy,double[] world){
   if(world==null||legacy==null||world.length!=legacy.length)throw new IllegalArgumentException("Invalid precise preview");
   for(int i=0;i<world.length;i++)if(!Double.isFinite(world[i])||(float)world[i]!=legacy[i])
    throw new IllegalArgumentException("Precise preview disagrees with legacy mesh");
   Mesh mesh=new Mesh(legacy);mesh.preciseWorld=world.clone();return mesh;
  }
  double[] worldCoordinates(){
   if(preciseWorld!=null)return preciseWorld.clone();
   double[] world=new double[vertices.length];for(int i=0;i<world.length;i++)world[i]=vertices[i];return world;
  }
  private Mesh previewCopy(){
   Mesh copy=new Mesh(vertices.clone());copy.preciseWorld=preciseWorld==null?null:preciseWorld.clone();return copy;
  }
  public int triangles(){return vertices.length/9;}
  /** Lazy GL-thread ray broad phase: built once per immutable mesh instance. */
  public es.josxtm.mycad.cad.MeshRayIndex rayIndex(){
   if(pickIndex==null)pickIndex=new es.josxtm.mycad.cad.MeshRayIndex(vertices);
   return pickIndex;
  }
 }
 private final List<Mesh> meshes=new ArrayList<>();
 /** GL-thread ownership map; invalidated on every scene/preview replacement. */
 private es.josxtm.mycad.cad.SteppedScenePatchRegistry steppedPatches=
  es.josxtm.mycad.cad.SteppedScenePatchRegistry.register(
   Collections.<float[]>emptyList(),
   Collections.<es.josxtm.mycad.cad.SteppedScenePatchRegistry.Binding>emptyList());
 private void rebuildSteppedPatches(){
  ArrayList<float[]> rendered=new ArrayList<>();
  ArrayList<es.josxtm.mycad.cad.SteppedScenePatchRegistry.Binding> bindings=
   new ArrayList<>();
  for(int i=0;i<meshes.size();i++){
   Mesh mesh=meshes.get(i);
   rendered.add(mesh.vertices);
   if(mesh.steppedShell!=null)bindings.add(
    new es.josxtm.mycad.cad.SteppedScenePatchRegistry.Binding(i,mesh.steppedShell));
  }
  steppedPatches=es.josxtm.mycad.cad.SteppedScenePatchRegistry.register(rendered,bindings);
 }
 /** Renderer-thread cache: reused across orbit/zoom frames; discarded on scene change. */
 private final es.josxtm.mycad.cad.FloatVertexBufferCache gpuTriangles=
  new es.josxtm.mycad.cad.FloatVertexBufferCache();
 private int program,position,modelViewProjection,colorUniform;
 private final float[] projection=new float[16],view=new float[16],mvp=new float[16];
 private float yaw=35,pitch=25;
 private volatile double centerX,centerY,centerZ;
 private volatile es.josxtm.mycad.cad.RelativeSceneCoordinates coordinates=
  es.josxtm.mycad.cad.RelativeSceneCoordinates.from(Collections.<double[]>emptyList());
 private es.josxtm.mycad.cad.RelativeSceneCoordinates prepareCoordinates(List<Mesh> incoming){
  List<double[]> world=new ArrayList<>();for(Mesh mesh:incoming)world.add(mesh.worldCoordinates());
  return es.josxtm.mycad.cad.RelativeSceneCoordinates.from(world);
 }
 /** Shared by GL drawing, ray construction and UI handle projection. */
 private void cameraView(float[] target,es.josxtm.mycad.cad.RelativeSceneCoordinates frame){
  float[] focus=frame.localPoint(centerX,centerY,centerZ);
  double a=Math.toRadians(yaw),b=Math.toRadians(pitch);
  Matrix.setLookAtM(target,0,focus[0]+(float)(distance*Math.cos(b)*Math.cos(a)),
   focus[1]+(float)(distance*Math.cos(b)*Math.sin(a)),focus[2]+(float)(distance*Math.sin(b)),
   focus[0],focus[1],focus[2],0,0,1);
 }
 private boolean cameraHasFit=false; // GL-thread only; rearm if scene becomes empty
 private volatile float distance=300;
 /** Camera distance used for zoom-aware CAD hit tolerance in the UI thread. */
 public float cameraDistance(){return distance;}
 private float lastX,lastY,downX,downY; private boolean dragging,moved;
 private boolean pinchActive=false;
 private float lastPinchSpan,lastPinchMidX,lastPinchMidY;
 private boolean twoFingerMoved=false,threeFingerTracking=false;
 private boolean threeFingerEligible=false,suppressUntilLastUp=false;
 private long firstTouchTimeMs=0;
 private final int[] homePointerIds=new int[3];
 private final float[] homeStartX=new float[3],homeStartY=new float[3];
 private double homeMaxTravel=0;

 private volatile int highlightedFirstTriangle=-1;
 private volatile int[] highlightedPatchTriangles=new int[0];
 /** UI changes invalidate callbacks already queued by the renderer. */
 private volatile long sceneRevision;
 private long renderedSceneRevision; // GL-thread snapshot, distinct from queued UI replacements
 long sceneRevision(){return sceneRevision;}
 private float[] highlightedEdgeVertices=null;
 private float[] faceHandleGpu=null;
 private volatile float[] faceHandleUi=null;
 private FaceDragListener faceDragListener=null;
 private boolean faceHandleCaptured=false;
 private float faceDragStartX,faceDragStartY,faceDragAxisX,faceDragAxisY;
 private double faceDragLimit=0;
 private volatile double faceSnapStepMm=0;
 /** Zero for free movement; optional snap grid in physical mm. */
 public void setFaceSnapStepMm(double step){
  if(!Double.isFinite(step)||step<0||step>10)
   throw new IllegalArgumentException("Invalid snapping grid");
  faceSnapStepMm=step;
 }

 public interface FaceDragListener {
  void onDrag(double deltaMm,boolean finished,boolean cancelled);
 }
 /**
  * Install a face handle that starts ON the selected 3D face and extends along
  * its world-space normal. Hit it with one finger to edit without orbiting.
  * Passing null clears the handle and restores ordinary camera interactions.
  */
 public void setFaceDragHandle(float[] anchorNormal,double maxMm,FaceDragListener listener){
  if(anchorNormal!=null&&(anchorNormal.length!=6||listener==null||
       !Double.isFinite(maxMm)||maxMm<=0))
   throw new IllegalArgumentException("Invalid face drag handle");
  float[] copy=anchorNormal==null?null:anchorNormal.clone();
  if(copy!=null)for(float f:copy)if(!Float.isFinite(f))
   throw new IllegalArgumentException("Nonfinite face handle");
  faceHandleUi=copy;
  faceDragListener=copy==null?null:listener;
  faceDragLimit=copy==null?0:maxMm;
  faceHandleCaptured=false;
  queueEvent(()->{faceHandleGpu=copy;requestRender();});
 }
 private float handleLength(){
  return Math.max(2f,Math.min(25f,distance*.08f));
 }
 /** UI-thread projection, using the same perspective/camera as GL renderer. */
 float[] projectPoint(double x,double y,double z){
  if(getWidth()<=0||getHeight()<=0)return null;
  es.josxtm.mycad.cad.RelativeSceneCoordinates frame=coordinates;
  float[] camera=new float[16],proj=new float[16],mvpLocal=new float[16];
  cameraView(camera,frame);
  Matrix.perspectiveM(proj,0,45f,(float)getWidth()/getHeight(),.1f,1000000f);
  Matrix.multiplyMM(mvpLocal,0,proj,0,camera,0);
  float[] out=new float[4];
  float[] local=frame.localPoint(x,y,z);
  Matrix.multiplyMV(out,0,mvpLocal,0,new float[]{local[0],local[1],local[2],1f},0);
  if(out[3]<=1e-6f||!Float.isFinite(out[3]))return null;
  return new float[]{getWidth()*(out[0]/out[3]+1)/2f,
                     getHeight()*(1-out[1]/out[3])/2f};
 }
 private float[] projectedHandle(){
  float[] f=faceHandleUi;if(f==null)return null;
  float arm=handleLength();
  double px=(double)f[0]+f[3]*arm,py=(double)f[1]+f[4]*arm,pz=(double)f[2]+f[5]*arm;
  float[] tip=projectPoint(px,py,pz);
  float[] next=projectPoint(px+f[3],py+f[4],pz+f[5]);
  if(tip==null||next==null)return null;
  return new float[]{tip[0],tip[1],next[0]-tip[0],next[1]-tip[1]};
 }
 private double faceMovement(float sx,float sy){
  double denom=faceDragAxisX*faceDragAxisX+faceDragAxisY*faceDragAxisY;
  if(denom<1e-10)return 0;
  double mm=((sx-faceDragStartX)*faceDragAxisX+
             (sy-faceDragStartY)*faceDragAxisY)/denom;
  return es.josxtm.mycad.cad.DragPrecision.quantize(
   mm,faceDragLimit,faceSnapStepMm);
 }

 public void highlightFace(int firstTriangle){
  highlightedPatchTriangles=new int[0];highlightedFirstTriangle=firstTriangle;requestRender();
 }
 public void highlightPatch(int[] triangles){
  if(triangles==null)throw new IllegalArgumentException("Missing patch triangles");
  int[] copy=triangles.clone();
  for(int triangle:copy)if(triangle<0)throw new IllegalArgumentException("Invalid patch triangle");
  highlightedFirstTriangle=-1;highlightedPatchTriangles=copy;requestRender();
 }
 /** A genuine analytic edge outline, not arbitrary tessellation triangle edges. */
 public void highlightEdge(float[] xyz){
  if(xyz!=null&&(xyz.length<6||xyz.length%3!=0))
   throw new IllegalArgumentException("Invalid selected edge outline");
  final float[] copy=xyz==null?null:xyz.clone();
  queueEvent(()->{highlightedEdgeVertices=copy;requestRender();});
 }

 public interface SurfaceTapListener {void onSurfaceTap(int triangle,double x,double y,double z);}
 /** World-ray metadata allows analytic B-Rep picking after GPU mesh visibility test. */
 public interface SurfaceRayListener {
  void onSurfaceRay(int triangle,double x,double y,double z,
                    double ox,double oy,double oz,double dx,double dy,double dz);
 }
 private SurfaceTapListener surfaceTapListener;
 private SurfaceRayListener surfaceRayListener;
 /** Additive opt-in callback: existing surface ray/tap tools still receive hits. */
 public interface SteppedPatchTapListener {
  void onSteppedPatchTap(es.josxtm.mycad.cad.SteppedVisiblePatchBridge.Hit hit);
 }
 private volatile SteppedPatchTapListener steppedPatchTapListener;
 /** Notify the Canvas ViewCube after a gesture changes GPU camera heading. */
 public interface CameraOrientationListener {
  void onCameraOrientation(float yawDegrees,float pitchDegrees);
 }
 private CameraOrientationListener cameraOrientationListener;
 public void setCameraOrientationListener(CameraOrientationListener listener){
  cameraOrientationListener=listener;
 }
 private void publishCameraOrientation(){
  final CameraOrientationListener receiver=cameraOrientationListener;
  if(receiver==null)return;
  final float currentYaw=yaw,currentPitch=pitch;
  // Canvas is a UI View; notifications from the EGL thread must use UI.
  post(()->{
   if(cameraOrientationListener==receiver)
    receiver.onCameraOrientation(currentYaw,currentPitch);
  });
 }
 public void setSurfaceTapListener(SurfaceTapListener listener){surfaceTapListener=listener;}
 public void setSurfaceRayListener(SurfaceRayListener listener){surfaceRayListener=listener;}
 public void setSteppedPatchTapListener(SteppedPatchTapListener listener){
  steppedPatchTapListener=listener;
 }
 public MeshEngine(Context context){super(context);setEGLContextClientVersion(3);setRenderer(this);setRenderMode(RENDERMODE_WHEN_DIRTY);}
 /** Transient preview: swap geometry on the GL thread without refitting camera. */
 public void previewMesh(Mesh candidate){
  if(candidate==null)throw new IllegalArgumentException("Missing preview mesh");
  replaceSceneMeshes(Collections.singletonList(candidate.previewCopy()),false);

 }
 /** New/open/isolated model: explicitly frame the whole scene. */
 public void setMeshes(List<Mesh> incoming){replaceSceneMeshes(incoming,true);}
 /** Saved editing: update triangles without resetting the user's pan or zoom. */
 public void updateMeshesPreservingCamera(List<Mesh> incoming){
  replaceSceneMeshes(incoming,false);
 }
 private void replaceSceneMeshes(List<Mesh> incoming,boolean forceFit){
  if(incoming==null)throw new IllegalArgumentException("Missing scene meshes");
  final ArrayList<Mesh> copy=new ArrayList<>(incoming);
  for(Mesh item:copy)if(item==null)
   throw new IllegalArgumentException("Null scene mesh");
  final es.josxtm.mycad.cad.RelativeSceneCoordinates frame=prepareCoordinates(copy);
  final long nextRevision=++sceneRevision;
  highlightedPatchTriangles=new int[0];highlightedFirstTriangle=-1;
  queueEvent(()->installSceneMeshes(copy,frame,forceFit,nextRevision));
 }
 /** GL-thread installation. The same entry point is exercised without EGL in Android tests. */
 void installSceneMeshes(List<Mesh> incoming,es.josxtm.mycad.cad.RelativeSceneCoordinates frame,boolean forceFit,long revision){
  renderedSceneRevision=revision;gpuTriangles.clear();
  meshes.clear();meshes.addAll(incoming);coordinates=frame;
  rebuildSteppedPatches();highlightedEdgeVertices=null;
  boolean hasTriangles=false;for(Mesh item:meshes)if(item.vertices.length>0){hasTriangles=true;break;}
  if(es.josxtm.mycad.cad.CameraRefitPolicy.shouldFit(cameraHasFit,forceFit,hasTriangles))fitCamera();
  cameraHasFit=es.josxtm.mycad.cad.CameraRefitPolicy.rememberFitted(hasTriangles);
  requestRender();
 }
 /**
  * ViewCube orientation command; preserve model focus and camera distance.
  * The canvas exposes radians and the GPU camera expects degrees.
  */
 public void setViewOrientation(float yawDegrees,float pitchDegrees){
  if(!Float.isFinite(yawDegrees)||!Float.isFinite(pitchDegrees))
   throw new IllegalArgumentException("Invalid ViewCube orientation");
  yaw=es.josxtm.mycad.cad.CameraOrientationSync.yawDegrees(yawDegrees);
  pitch=es.josxtm.mycad.cad.CameraOrientationSync.pitchDegrees(pitchDegrees);
  requestRender();
 }
 public void orbit(float dx,float dy){
  yaw=es.josxtm.mycad.cad.CameraOrientationSync.yawDegrees(yaw+dx*.35f);
  pitch=es.josxtm.mycad.cad.CameraOrientationSync.pitchDegrees(pitch+dy*.35f);
  publishCameraOrientation();
  requestRender();
 }
 public void zoom(float factor){distance=Math.max(.1f,Math.min(100000f,distance/factor));requestRender();}
 /** Three quick fingers reset camera to the initial 3D isometric view. */
 private void homeCamera(){
  queueEvent(()->{
   yaw=35;pitch=25;
   fitCamera();
   publishCameraOrientation();
   boolean hasGeometry=false;
   for(Mesh mesh:meshes)if(mesh.vertices.length>0){hasGeometry=true;break;}
   cameraHasFit=hasGeometry;
   requestRender();
  });
 }
 private void collectHomeMovement(android.view.MotionEvent e){
  if(!threeFingerTracking)return;
  for(int k=0;k<3;k++){
   int idx=e.findPointerIndex(homePointerIds[k]);
   if(idx<0){threeFingerEligible=false;continue;}
   homeMaxTravel=Math.max(homeMaxTravel,Math.hypot(
    e.getX(idx)-homeStartX[k],e.getY(idx)-homeStartY[k]));
  }
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent e){
  int action=e.getActionMasked();
  if(action==android.view.MotionEvent.ACTION_DOWN){
   firstTouchTimeMs=e.getEventTime();
   twoFingerMoved=false;
   threeFingerTracking=false;
   threeFingerEligible=false;
   suppressUntilLastUp=false;
  }
  // Third pointer means neither pinch nor orbit nor CAD selection.
  if(action==android.view.MotionEvent.ACTION_POINTER_DOWN&&
      e.getPointerCount()>=3){
   if(!threeFingerTracking&&e.getPointerCount()==3){
    threeFingerTracking=true;
    threeFingerEligible=!twoFingerMoved&&
     es.josxtm.mycad.cad.ThreeFingerHomePolicy.canStart(
      firstTouchTimeMs,e.getEventTime());
    homeMaxTravel=0;
    for(int k=0;k<3;k++){
     homePointerIds[k]=e.getPointerId(k);
     homeStartX[k]=e.getX(k);homeStartY[k]=e.getY(k);
    }
   }else threeFingerEligible=false;
   if(faceHandleCaptured){
    faceHandleCaptured=false;
    if(faceDragListener!=null)faceDragListener.onDrag(0,false,true);
   }
   pinchActive=false;dragging=false;moved=true;
   return true;
  }
  if(threeFingerTracking){
   collectHomeMovement(e);
   if(action==android.view.MotionEvent.ACTION_POINTER_UP){
    if(e.getPointerCount()==3&&
       es.josxtm.mycad.cad.ThreeFingerHomePolicy.shouldReset(
        threeFingerEligible,firstTouchTimeMs,e.getEventTime(),
        homeMaxTravel,12*getResources().getDisplayMetrics().density))
     homeCamera();
    threeFingerTracking=false;
    suppressUntilLastUp=true;
   }else if(action==android.view.MotionEvent.ACTION_CANCEL||
            action==android.view.MotionEvent.ACTION_UP){
    threeFingerTracking=false;suppressUntilLastUp=false;
   }
   return true;
  }
  if(suppressUntilLastUp){
   if(action==android.view.MotionEvent.ACTION_UP||
      action==android.view.MotionEvent.ACTION_CANCEL)
    suppressUntilLastUp=false;
   return true;
  }
  if(e.getPointerCount()>1&&faceHandleCaptured){
   faceHandleCaptured=false;
   if(faceDragListener!=null)faceDragListener.onDrag(0,false,true);
   return true;
  }
  if(action==android.view.MotionEvent.ACTION_DOWN&&faceHandleUi!=null){
   float[] p=projectedHandle();
   float threshold=26*getResources().getDisplayMetrics().density;
   if(p!=null&&Math.hypot(e.getX()-p[0],e.getY()-p[1])<=threshold){
    faceHandleCaptured=true;
    faceDragStartX=e.getX();faceDragStartY=e.getY();
    faceDragAxisX=p[2];faceDragAxisY=p[3];
    // When the face normal points into the camera, fall back to vertical
    // sliding with a model-space scale instead of an undefined projected axis.
    if(Math.hypot(faceDragAxisX,faceDragAxisY)<.25f){
     faceDragAxisX=0;
     faceDragAxisY=(float)(-getHeight()/
       (2*Math.max(.01,distance)*Math.tan(Math.toRadians(22.5))));
    }
    return true;
   }
  }
  if(faceHandleCaptured){
   if(action==android.view.MotionEvent.ACTION_MOVE||
      action==android.view.MotionEvent.ACTION_UP){
    double mm=faceMovement(e.getX(),e.getY());
    boolean done=action==android.view.MotionEvent.ACTION_UP;
    if(done)faceHandleCaptured=false;
    if(faceDragListener!=null)faceDragListener.onDrag(mm,done,false);
    return true;
   }
   if(action==android.view.MotionEvent.ACTION_CANCEL){
    faceHandleCaptured=false;
    if(faceDragListener!=null)faceDragListener.onDrag(0,false,true);
    return true;
   }
  }

  // True two-finger CAD navigation: relative pinch changes zoom, centroid
  // movement pans the world XYZ camera target without rotating the solid.
  if(action==android.view.MotionEvent.ACTION_POINTER_DOWN||
     (e.getPointerCount()>=2&&!pinchActive)){
   if(e.getPointerCount()>=2){
    lastPinchSpan=(float)Math.hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0));
    lastPinchMidX=(e.getX(0)+e.getX(1))*.5f;
    lastPinchMidY=(e.getY(0)+e.getY(1))*.5f;
    pinchActive=lastPinchSpan>1;
    dragging=false;moved=true;
   }
   return true;
  }
  if(e.getPointerCount()>=2&&pinchActive){
   if(action==android.view.MotionEvent.ACTION_MOVE){
    twoFingerMoved=true;
    float span=(float)Math.hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0));
    float mx=(e.getX(0)+e.getX(1))*.5f,my=(e.getY(0)+e.getY(1))*.5f;
    if(span>1&&lastPinchSpan>1){
     double factor=es.josxtm.mycad.cad.CameraGestureMath.zoomFactor(
       lastPinchSpan,span);
     zoom((float)factor);
     double[] focus=es.josxtm.mycad.cad.CameraGestureMath.pan(
       centerX,centerY,centerZ,yaw,pitch,distance,getHeight(),
       mx-lastPinchMidX,my-lastPinchMidY);
     centerX=focus[0];centerY=focus[1];centerZ=focus[2];
     requestRender();
     lastPinchSpan=span;
    }
    lastPinchMidX=mx;lastPinchMidY=my;
   }else if(action==android.view.MotionEvent.ACTION_POINTER_UP){
    pinchActive=false;dragging=false;moved=true;
   }
   return true;
  }
  if(action==android.view.MotionEvent.ACTION_POINTER_UP){
   pinchActive=false;dragging=false;moved=true;return true;
  }
  if(action==android.view.MotionEvent.ACTION_DOWN){
   pinchActive=false;
   lastX=e.getX();lastY=e.getY();downX=lastX;downY=lastY;
   moved=false;dragging=true;return true;
  }
  if(e.getActionMasked()==android.view.MotionEvent.ACTION_MOVE&&dragging){
   moved=es.josxtm.mycad.cad.TouchDragThreshold.moved(
    moved,downX,downY,e.getX(),e.getY(),
    12*getResources().getDisplayMetrics().density);
   // Do not orbit on tiny tap jitter; the ray still selects the same face.
   if(moved)orbit(e.getX()-lastX,e.getY()-lastY);
   lastX=e.getX();lastY=e.getY();return true;
  }
  if(e.getActionMasked()==android.view.MotionEvent.ACTION_UP){
   // Some drivers deliver a final position only with UP; reject such drags.
   moved=es.josxtm.mycad.cad.TouchDragThreshold.moved(
    moved,downX,downY,e.getX(),e.getY(),
    12*getResources().getDisplayMetrics().density);
   dragging=false;
   if(es.josxtm.mycad.cad.SurfaceTapPolicy.shouldPick(
      moved,surfaceRayListener!=null,surfaceTapListener!=null,
      steppedPatchTapListener!=null)){
    final float x=e.getX(),y=e.getY();
    queueEvent(()->pickSurfaceAt(x,y));
   }
  }
  return true;
 }
 void pickSurfaceAt(float sx,float sy){
  int width=getWidth(),height=getHeight();
  if(width<=0||height<=0||meshes.isEmpty())return;
  // Use the same near/far-clipped world ray as the analytic stepped B-Rep
  // picker. Keep triangle indices and callbacks unchanged for older tools.
  es.josxtm.mycad.cad.RelativeSceneCoordinates frame=coordinates;
  float[] camera=new float[16],combined=new float[16];cameraView(camera,frame);
  Matrix.multiplyMM(combined,0,projection,0,camera,0);
  double[] viewProjection=new double[16];
  for(int i=0;i<16;i++)viewProjection[i]=combined[i];
  ArrayList<es.josxtm.mycad.cad.ViewportMeshRayPicker.Candidate> candidates=
   new ArrayList<>();
  for(int i=0;i<meshes.size();i++)candidates.add(
   new es.josxtm.mycad.cad.ViewportMeshRayPicker.Candidate(frame.rayIndex(i),meshes.get(i).triangles()));
  es.josxtm.mycad.cad.ViewportMeshRayPicker.Hit chosen;
  try{
   chosen=es.josxtm.mycad.cad.ViewportMeshRayPicker.pick(
    candidates,viewProjection,width,height,sx,sy);
   chosen=frame.worldHit(chosen);
  }catch(IllegalArgumentException invalidViewport){return;}
  if(chosen==null)return;
  final es.josxtm.mycad.cad.SteppedVisiblePatchBridge.Hit patch=
   steppedPatchTapListener==null?null:steppedPatches.resolve(chosen);
  final long revision=renderedSceneRevision;
  final es.josxtm.mycad.cad.ViewportMeshRayPicker.Hit worldHit=chosen;
  post(()->dispatchSurfaceHit(revision,worldHit,patch));
 }
 /** UI-thread delivery; a replaced/previewed scene cannot select an old face. */
 void dispatchSurfaceHit(long revision,
     es.josxtm.mycad.cad.ViewportMeshRayPicker.Hit visible,
     es.josxtm.mycad.cad.SteppedVisiblePatchBridge.Hit patch){
   if(revision!=sceneRevision||visible==null)return;
   final int triangle=visible.triangle;
   final double x=visible.x,y=visible.y,z=visible.z;
   final double ox=visible.ox,oy=visible.oy,oz=visible.oz;
   final double dx=visible.dx,dy=visible.dy,dz=visible.dz;
   if(patch!=null&&steppedPatchTapListener!=null)
    steppedPatchTapListener.onSteppedPatchTap(patch);
   if(revision!=sceneRevision)return;
   if(surfaceRayListener!=null)
    surfaceRayListener.onSurfaceRay(triangle,x,y,z,ox,oy,oz,dx,dy,dz);
   else if(surfaceTapListener!=null)
    surfaceTapListener.onSurfaceTap(triangle,x,y,z);
 }
 private void fitCamera(){
  double[] bounds=coordinates.worldBounds();
  centerX=coordinates.originX;centerY=coordinates.originY;centerZ=coordinates.originZ;
  double span=Math.max(bounds[3]-bounds[0],Math.max(bounds[4]-bounds[1],bounds[5]-bounds[2]));
  distance=(float)Math.max(10,Math.min(Float.MAX_VALUE,span*2.5));
 }
 @Override public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl,javax.microedition.khronos.egl.EGLConfig cfg){
  gpuTriangles.clear(); // EGL context recreation invalidates renderer-owned resources.
  GLES30.glEnable(GLES30.GL_DEPTH_TEST);GLES30.glEnable(GLES30.GL_CULL_FACE);
  int v=shader(GLES30.GL_VERTEX_SHADER,"#version 300 es\nlayout(location=0) in vec3 aPosition;uniform mat4 uMVP;void main(){gl_Position=uMVP*vec4(aPosition,1.0);gl_PointSize=15.0;}");
  int f=shader(GLES30.GL_FRAGMENT_SHADER,"#version 300 es\nprecision mediump float;uniform vec4 uColor;out vec4 fragColor;void main(){fragColor=uColor;}");
  program=GLES30.glCreateProgram();GLES30.glAttachShader(program,v);GLES30.glAttachShader(program,f);GLES30.glLinkProgram(program);
  int[] status=new int[1];GLES30.glGetProgramiv(program,GLES30.GL_LINK_STATUS,status,0);if(status[0]==0)throw new IllegalStateException(GLES30.glGetProgramInfoLog(program));
  GLES30.glDeleteShader(v);GLES30.glDeleteShader(f);modelViewProjection=GLES30.glGetUniformLocation(program,"uMVP");colorUniform=GLES30.glGetUniformLocation(program,"uColor");
 }
 private int shader(int type,String source){int id=GLES30.glCreateShader(type);GLES30.glShaderSource(id,source);GLES30.glCompileShader(id);int[] ok=new int[1];GLES30.glGetShaderiv(id,GLES30.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES30.glGetShaderInfoLog(id));return id;}
 @Override public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl,int w,int h){GLES30.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,45f,(float)w/Math.max(1,h),.1f,1000000f);}
 @Override public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl){
  GLES30.glClearColor(.07f,.1f,.15f,1);GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT|GLES30.GL_DEPTH_BUFFER_BIT);
  es.josxtm.mycad.cad.RelativeSceneCoordinates frame=coordinates;
  cameraView(view,frame);
  Matrix.multiplyMM(mvp,0,projection,0,view,0);GLES30.glUseProgram(program);GLES30.glUniformMatrix4fv(modelViewProjection,1,false,mvp,0);GLES30.glUniform4f(colorUniform,.3f,.78f,.84f,1);
  GLES30.glEnableVertexAttribArray(0);
  int triangleOffset=0;
  for(int meshIndex=0;meshIndex<meshes.size();meshIndex++){
   Mesh mesh=meshes.get(meshIndex);
   // No native allocations per frame: keep a direct buffer per immutable mesh.
   FloatBuffer buf=frame.buffer(meshIndex,gpuTriangles,mesh);
   GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,12,buf);
   GLES30.glUniform4f(colorUniform,.3f,.78f,.84f,1);
   GLES30.glDrawArrays(GLES30.GL_TRIANGLES,0,mesh.vertices.length/3);
   int local=highlightedFirstTriangle-triangleOffset;
   if(local>=0&&local+1<mesh.triangles()){
    GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL);
    GLES30.glPolygonOffset(-2f,-2f);
    GLES30.glUniform4f(colorUniform,1f,.65f,.16f,1);
    GLES30.glDrawArrays(GLES30.GL_TRIANGLES,local*3,6);
    GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL);
   }
   int[] selectedPatch=highlightedPatchTriangles;
   if(selectedPatch.length>0){
    GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL);
    GLES30.glPolygonOffset(-2f,-2f);
    GLES30.glUniform4f(colorUniform,1f,.65f,.16f,1);
    for(int global:selectedPatch){
     int patchLocal=global-triangleOffset;
     if(patchLocal>=0&&patchLocal<mesh.triangles())
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES,patchLocal*3,3);
    }
    GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL);
   }
   triangleOffset+=mesh.triangles();
  }
  // Draw the selected topological line/rim as an overlay in world XYZ.
  // Disable depth temporarily so its complete path is visible, then restore it.
  if(highlightedEdgeVertices!=null){
   float[] outline=frame.localPoints(highlightedEdgeVertices);
   FloatBuffer edgeBuf=ByteBuffer.allocateDirect(outline.length*4)
     .order(ByteOrder.nativeOrder()).asFloatBuffer();
   edgeBuf.put(outline).position(0);
   GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,12,edgeBuf);
   GLES30.glDisable(GLES30.GL_DEPTH_TEST);
   GLES30.glUniform4f(colorUniform,1f,.65f,.16f,1);
   GLES30.glLineWidth(3f);
   GLES30.glDrawArrays(GLES30.GL_LINE_STRIP,0,outline.length/3);
   GLES30.glEnable(GLES30.GL_DEPTH_TEST);
  }
  if(faceHandleGpu!=null){
   float[] handle=faceHandleGpu, anchor=frame.localPoint(handle[0],handle[1],handle[2]), h={
    anchor[0],anchor[1],anchor[2],
    anchor[0]+handle[3]*handleLength(),
    anchor[1]+handle[4]*handleLength(),
    anchor[2]+handle[5]*handleLength()};
   FloatBuffer buffer=ByteBuffer.allocateDirect(h.length*4)
     .order(ByteOrder.nativeOrder()).asFloatBuffer();
   buffer.put(h).position(0);
   GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,12,buffer);
   GLES30.glDisable(GLES30.GL_DEPTH_TEST);
   GLES30.glUniform4f(colorUniform,1f,.45f,.08f,1);
   GLES30.glLineWidth(3f);
   GLES30.glDrawArrays(GLES30.GL_LINES,0,2);
   buffer.position(3);
   GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,12,buffer);
   GLES30.glUniform4f(colorUniform,1f,.96f,.35f,1);
   GLES30.glDrawArrays(GLES30.GL_POINTS,0,1);
   GLES30.glEnable(GLES30.GL_DEPTH_TEST);
  }
  GLES30.glDisableVertexAttribArray(0);
 }
}
