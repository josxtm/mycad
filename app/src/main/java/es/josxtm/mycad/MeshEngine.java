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
  public Mesh(float[] v){if(v.length%9!=0)throw new IllegalArgumentException("Triangle data required");vertices=v;}
  public int triangles(){return vertices.length/9;}
 }
 private final List<Mesh> meshes=new ArrayList<>();
 private int program,position,modelViewProjection,colorUniform;
 private final float[] projection=new float[16],view=new float[16],mvp=new float[16];
 private float yaw=35,pitch=25,centerX,centerY,centerZ;
 private volatile float distance=300;
 /** Camera distance used for zoom-aware CAD hit tolerance in the UI thread. */
 public float cameraDistance(){return distance;}
 private float lastX,lastY,downX,downY; private boolean dragging,moved;
 private volatile int highlightedFirstTriangle=-1;
 private float[] highlightedEdgeVertices=null;
 private float[] faceHandleGpu=null;
 private volatile float[] faceHandleUi=null;
 private FaceDragListener faceDragListener=null;
 private boolean faceHandleCaptured=false;
 private float faceDragStartX,faceDragStartY,faceDragAxisX,faceDragAxisY;
 private double faceDragLimit=0;
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
 private float[] projectPoint(float x,float y,float z){
  if(getWidth()<=0||getHeight()<=0)return null;
  double a=Math.toRadians(yaw),b=Math.toRadians(pitch);
  float[] camera=new float[16],proj=new float[16],mvpLocal=new float[16];
  Matrix.setLookAtM(camera,0,centerX+(float)(distance*Math.cos(b)*Math.cos(a)),
    centerY+(float)(distance*Math.cos(b)*Math.sin(a)),
    centerZ+(float)(distance*Math.sin(b)),centerX,centerY,centerZ,0,0,1);
  Matrix.perspectiveM(proj,0,45f,(float)getWidth()/getHeight(),.1f,1000000f);
  Matrix.multiplyMM(mvpLocal,0,proj,0,camera,0);
  float[] out=new float[4];
  Matrix.multiplyMV(out,0,mvpLocal,0,new float[]{x,y,z,1f},0);
  if(out[3]<=1e-6f||!Float.isFinite(out[3]))return null;
  return new float[]{getWidth()*(out[0]/out[3]+1)/2f,
                     getHeight()*(1-out[1]/out[3])/2f};
 }
 private float[] projectedHandle(){
  float[] f=faceHandleUi;if(f==null)return null;
  float arm=handleLength();
  float px=f[0]+f[3]*arm,py=f[1]+f[4]*arm,pz=f[2]+f[5]*arm;
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
  return Math.max(-faceDragLimit,Math.min(faceDragLimit,mm));
 }

 public void highlightFace(int firstTriangle){highlightedFirstTriangle=firstTriangle;requestRender();}
 /** A genuine analytic edge outline, not arbitrary tessellation triangle edges. */
 public void highlightEdge(float[] xyz){
  if(xyz!=null&&(xyz.length<6||xyz.length%3!=0))
   throw new IllegalArgumentException("Invalid selected edge outline");
  final float[] copy=xyz==null?null:xyz.clone();
  queueEvent(()->{highlightedEdgeVertices=copy;requestRender();});
 }

 public interface SurfaceTapListener {void onSurfaceTap(int triangle,double x,double y,double z);}
 private SurfaceTapListener surfaceTapListener;
 public void setSurfaceTapListener(SurfaceTapListener listener){surfaceTapListener=listener;}
 public MeshEngine(Context context){super(context);setEGLContextClientVersion(3);setRenderer(this);setRenderMode(RENDERMODE_WHEN_DIRTY);}
 /** Transient preview: swap geometry on the GL thread without refitting camera. */
 public void previewMesh(Mesh candidate){
  if(candidate==null)throw new IllegalArgumentException("Missing preview mesh");
  final Mesh snapshot=new Mesh(candidate.vertices.clone());
  queueEvent(()->{
   meshes.clear();meshes.add(snapshot);
   highlightedEdgeVertices=null;
   highlightedFirstTriangle=-1;
   requestRender();
  });
 }
 public void setMeshes(List<Mesh> incoming){final ArrayList<Mesh> copy=new ArrayList<>(incoming);highlightedFirstTriangle=-1;queueEvent(()->{meshes.clear();meshes.addAll(copy);highlightedEdgeVertices=null;fitCamera();requestRender();});}
 public void orbit(float dx,float dy){yaw+=dx*.35f;pitch=Math.max(-89,Math.min(89,pitch+dy*.35f));requestRender();}
 public void zoom(float factor){distance=Math.max(.1f,Math.min(100000f,distance/factor));requestRender();}
 @Override public boolean onTouchEvent(android.view.MotionEvent e){
  int action=e.getActionMasked();
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

  if(e.getPointerCount()>1){if(e.getActionMasked()==android.view.MotionEvent.ACTION_MOVE){float dy=e.getY()-lastY;zoom((float)Math.exp(-dy*.008));}lastY=e.getY();return true;}
  if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();downX=lastX;downY=lastY;moved=false;dragging=true;return true;}
  if(e.getActionMasked()==android.view.MotionEvent.ACTION_MOVE&&dragging){if(Math.hypot(e.getX()-downX,e.getY()-downY)>12*getResources().getDisplayMetrics().density)moved=true;orbit(e.getX()-lastX,e.getY()-lastY);lastX=e.getX();lastY=e.getY();return true;}
  if(e.getActionMasked()==android.view.MotionEvent.ACTION_UP){dragging=false;if(!moved&&surfaceTapListener!=null){final float x=e.getX(),y=e.getY();queueEvent(()->pickSurfaceAt(x,y));}}
  return true;
 }
 private void pickSurfaceAt(float sx,float sy){
  if(getWidth()<=0||getHeight()<=0||meshes.isEmpty())return;
  double a=Math.toRadians(yaw),b=Math.toRadians(pitch);
  float[] camera=new float[16],combined=new float[16],inverse=new float[16];
  Matrix.setLookAtM(camera,0,centerX+(float)(distance*Math.cos(b)*Math.cos(a)),
   centerY+(float)(distance*Math.cos(b)*Math.sin(a)),centerZ+(float)(distance*Math.sin(b)),
   centerX,centerY,centerZ,0,0,1);
  Matrix.multiplyMM(combined,0,projection,0,camera,0);
  if(!Matrix.invertM(inverse,0,combined,0))return;
  float nx=2*sx/getWidth()-1,ny=1-2*sy/getHeight();
  float[] near={nx,ny,-1,1},far={nx,ny,1,1},p=new float[4],q=new float[4];
  Matrix.multiplyMV(p,0,inverse,0,near,0);Matrix.multiplyMV(q,0,inverse,0,far,0);
  if(Math.abs(p[3])<1e-8||Math.abs(q[3])<1e-8)return;
  for(int j=0;j<3;j++){p[j]/=p[3];q[j]/=q[3];}
  double dx=q[0]-p[0],dy=q[1]-p[1],dz=q[2]-p[2];
  es.josxtm.mycad.cad.CadRayPicker.Hit chosen=null;int offset=0,chosenIndex=-1;
  for(Mesh mesh:meshes){
   es.josxtm.mycad.cad.CadRayPicker.Hit hit=
    es.josxtm.mycad.cad.CadRayPicker.pick(mesh.vertices,p[0],p[1],p[2],dx,dy,dz);
   if(hit!=null&&(chosen==null||hit.distance<chosen.distance)){chosen=hit;chosenIndex=offset+hit.triangle;}
   offset+=mesh.triangles();
  }
  if(chosen!=null){
   final int triangle=chosenIndex;final double x=chosen.x,y=chosen.y,z=chosen.z;
   post(()->{if(surfaceTapListener!=null)surfaceTapListener.onSurfaceTap(triangle,x,y,z);});
  }
 }
 private void fitCamera(){float[] bounds={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
  for(Mesh m:meshes)for(int i=0;i<m.vertices.length;i+=3)for(int k=0;k<3;k++){float v=m.vertices[i+k];bounds[k]=Math.min(bounds[k],v);bounds[k+3]=Math.max(bounds[k+3],v);}
  if(bounds[0]==Float.MAX_VALUE)return;
  centerX=(bounds[0]+bounds[3])/2;centerY=(bounds[1]+bounds[4])/2;centerZ=(bounds[2]+bounds[5])/2;
  float span=Math.max(bounds[3]-bounds[0],Math.max(bounds[4]-bounds[1],bounds[5]-bounds[2]));distance=Math.max(10,span*2.5f);
 }
 @Override public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl,javax.microedition.khronos.egl.EGLConfig cfg){
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
  double a=Math.toRadians(yaw),b=Math.toRadians(pitch);
  Matrix.setLookAtM(view,0,centerX+(float)(distance*Math.cos(b)*Math.cos(a)),centerY+(float)(distance*Math.cos(b)*Math.sin(a)),centerZ+(float)(distance*Math.sin(b)),centerX,centerY,centerZ,0,0,1);
  Matrix.multiplyMM(mvp,0,projection,0,view,0);GLES30.glUseProgram(program);GLES30.glUniformMatrix4fv(modelViewProjection,1,false,mvp,0);GLES30.glUniform4f(colorUniform,.3f,.78f,.84f,1);
  GLES30.glEnableVertexAttribArray(0);
  int triangleOffset=0;
  for(Mesh mesh:meshes){
   FloatBuffer buf=ByteBuffer.allocateDirect(mesh.vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
   buf.put(mesh.vertices).position(0);
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
   triangleOffset+=mesh.triangles();
  }
  // Draw the selected topological line/rim as an overlay in world XYZ.
  // Disable depth temporarily so its complete path is visible, then restore it.
  if(highlightedEdgeVertices!=null){
   float[] outline=highlightedEdgeVertices;
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
   float[] handle=faceHandleGpu, h={
    handle[0],handle[1],handle[2],
    handle[0]+handle[3]*handleLength(),
    handle[1]+handle[4]*handleLength(),
    handle[2]+handle[5]*handleLength()};
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
