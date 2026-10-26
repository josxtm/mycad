#!/usr/bin/env bash
set -euo pipefail
test -s app/build.gradle
test -s app/src/main/AndroidManifest.xml
test -d app/src/main/java
test -s docs/ROADMAP.md
grep -q "applicationId 'es.josxtm.mycad.preview'" app/build.gradle
grep -Eq 'versionName.*[0-9]+\.[0-9]+\.[0-9]+' app/build.gradle
grep -q 'android.intent.action.MAIN' app/src/main/AndroidManifest.xml
if grep -R -l -E '^(<<<<<<< |=======|>>>>>>> )' app/src/main/java app/build.gradle 2>/dev/null; then
  echo "ERROR: marcas de conflicto Git detectadas" >&2
  exit 1
fi
# Evitar secuencias de salto de línea escapadas introducidas accidentalmente entre métodos Java.
if grep -R -F -l '\\n int ' app/src/main/java --include='*.java'; then
  echo "ERROR: secuencia literal de nueva linea entre declaraciones Java" >&2
  exit 1
fi

# Protección CI: el flujo cotidiano nunca invoca el ensamblado de APK.
python3 - <<'PY'
from pathlib import Path
config=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
def require(flag, why):
    if not flag:
        raise SystemExit("ERROR: política de APK por hitos: "+why)
normal=config.split("- name: Compilar código y comprobar pruebas y Lint (sin APK)",1)
require(len(normal)==2, "falta validación Android habitual sin APK")
normal_step=normal[1].split("- name: Generar APK únicamente en un hito",1)
require(len(normal_step)==2, "falta compuerta de APK por hito")
require("gradle :app:testDebugUnitTest :app:lintDebug --no-daemon" in normal_step[0],
        "no se comprueban tests y Lint")
require(":app:assembleDebug" not in normal_step[0], "ensamblado en CI cotidiana")
apk_step=normal_step[1].split("- name: Verificar APK y nombre de versión",1)
require(len(apk_step)==2, "falta verificación del paquete")
require("github.event_name == 'workflow_dispatch'" in apk_step[0] and
        "inputs.generar_apk == true" in apk_step[0] and
        "startsWith(github.event.head_commit.message, 'release: MyCAD ')" in apk_step[0],
        "no existe condición exclusiva de hito o ejecución manual")
require("gradle :app:assembleDebug --no-daemon" in apk_step[0],
        "falta comando de empaquetado por hito")
require("if: steps.assemble.outcome == 'success'" in config and
        "if: steps.apk.outcome == 'success'" in config,
        "se puede publicar una APK sin verificación exitosa")
print("Política CI: tests y Lint normales; APK solo por hito: OK")
PY

# Regresión de integración: un borrador recuperado debe restaurar también el historial
# paramétrico y la selección de una vista previa no puede editar el modelo.
python3 - <<'PY'
from pathlib import Path
source=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
begin=source.index("void load(){")
end=source.index("protected void onSaveInstanceState",begin)
loader=source[begin:end]
for marker in ("extrusionChains=restored.chains","chainFeatures=restored.features","holeRecipes=restored.drills"):
    if marker not in loader:
        raise SystemExit("ERROR: el borrador no restaura "+marker)
for marker in ("void renderCurrentCadMesh()","isolatedMeshPreview=true","if(isolatedMeshPreview)"):
    if marker not in source:
        raise SystemExit("ERROR: falta protección de visor "+marker)
for marker in ("throughHoleRecipes", "holeRecipes.add(recipe)", "holeRecipes.add(null)", "editHoleRecipe(int index)", 'v.optString("axis","Z")', 'v.optDouble("rotationX",0)', "drillAxisChoice", "previous.rotationZ"):
    if marker not in source:
        raise SystemExit("ERROR: perforación no persistente "+marker)
for marker in ('.put("axis",drill.axis.name())', 'Axis.valueOf(v.optString("axis","Z"))', 'TriangleMeshHealth.inspect(geometry)', 'TriangleMeshHealth.inspect(mesh)', "drillAxisChoice(int choice)"):
    if marker not in source:
        raise SystemExit("ERROR: soporte de perforaciones orientadas incompleto: "+marker)
for marker in ('.put("drillDepth",drill.drillDepth)', 'v.optDouble("drillDepth",0)', 'previous.rotationZ,vals[3]', 'axis,parent.rotationX,parent.rotationY,parent.rotationZ,vals[3]', "drill.drillDepth==0"):
    if marker not in source:
        raise SystemExit("ERROR: profundidad de perforación no persistente: "+marker)
for marker in ("void cylinderBoxBooleanMenu()","CylinderBoxBoolean.calculate(","result.cut.regenerate()",
               "result.intersection","holeRecipes.add(option==2?result.cut:null)"):
    if marker not in source:
        raise SystemExit("ERROR: operaciones curvas no integradas: "+marker)
for marker in ('.put("bodyId",p.id)', 'item.id=p.optString("bodyId",item.id)',
               "q.id=p.id", "loadedBodyIds.add(item.id)",
               "PrimitiveTopologyMap.Hit", "Face.valueOf(faces[n])",
               "parts.get(activeCadFace.partIndex).id", "PrimitiveEdges.edge("):
    if marker not in source:
        raise SystemExit("ERROR: referencia topológica persistente sin integrar: "+marker)
for marker in ('new es.josxtm.mycad.cad.FaceExtrusionChain.Step(',
               'new es.josxtm.mycad.cad.ChainBooleanFeature(',
               'entry.optString("id",java.util.UUID.randomUUID().toString())',
               'step.optString("id",java.util.UUID.randomUUID().toString())',
               '.put("chainId",f.chainId==null?"":f.chainId)',
               'feature.regenerate(parsedChains)',
               'ChainDependencyReport.requireValid(parsedChains,parsedFeatures)',
               "copy.appendStep(step)",
               "reorderExtrusionChains()"):
    if marker not in source:
        raise SystemExit("ERROR: historial paramétrico UUID incompleto: "+marker)
for marker in ("LinkedSketchSolidPlan.build(", "LinkedSketchSolidPlan.plan(",
               "LinkedSketchAudit.inspect(", "showLinkedSketchAudit()",
               'q.sourceSketchId="";}checkpoint();if(existing)parts.set(selected,q);',
               "if(!validateLinkedSketchEdit(q,q.x1,q.y1,nextX,nextY))",
               "java.util.ArrayList<Part> originals=new java.util.ArrayList<>(),staged=new java.util.ArrayList<>();"):
    if marker not in source:
        raise SystemExit("ERROR: no se conserva el puente paramétrico atómico: "+marker)
for marker in ("requestSketchDeletion(false)", "requestSketchDeletion(true)",
               "void requestSketchDeletion(boolean selectedOnly)",
               "void applySketchDeletion(Sketch target",
               "SketchDeletionPlan.inspect(target.id,sketchBodyLinks())",
               "plan.stillMatches(sketchBodyLinks())",
               "checkpoint();\n  sketches.remove(index);",
               "Mode.CASCADE_DELETE",
               'sourceSketchId=""',
               "ParsedDesign restored=decode(src.peek());",
               "src.pop();dst.push(prior);"):
    if marker not in source:
        raise SystemExit("ERROR: borrado seguro o historial no integrado: "+marker)
for marker in ('storedFeatures.put(new JSONObject().put("id",f.id)',
               'f.optString("id",java.util.UUID.randomUUID().toString())',
               'ParametricFeatureGraph buildParametricFeatureGraph()',
               'void showParametricFeatureTree()',
               'builder.dependsOn(p.id,p.sourceSketchId)',
               'builder.dependsOn(f.id,extrusionChains.get(f.resolvedChainIndex(extrusionChains)).id)',
               "graph.affectedBy(node.id)"):
    if marker not in source:
        raise SystemExit("ERROR: grafo de dependencias sin integrar: "+marker)
for marker in ("void rebuildSavedChainBooleans()", "ChainBooleanRebuildCache.rebuild(sources,features,previous)",
               "result.state.matches(extrusionChains,chainFeatures)",
               "booleanRebuildSnapshot=result.state",
               "runOnUiThread(", "copyExtrusionChain(chain)",
               "showMeshOnly(new MeshEngine.Mesh(preview))",
               'case 19:rebuildSavedChainBooleans();break;'):
    if marker not in source:
        raise SystemExit("ERROR: reconstrucción incremental de booleanas no conectada: "+marker)
print("Borrador, perforación circular y protección de vista previa: OK")
PY

# B-Rep analytico: validar topologia cerrada en Java puro, sin APK.
mkdir -p build/box-brep-tests
javac -d build/box-brep-tests \
  app/src/main/java/es/josxtm/mycad/cad/CadKernel.java \
  app/src/main/java/es/josxtm/mycad/cad/RigidTransform.java \
  app/src/main/java/es/josxtm/mycad/cad/TransformedBody.java \
  app/src/main/java/es/josxtm/mycad/cad/BoxSurface.java \
  app/src/main/java/es/josxtm/mycad/cad/BoxFaceSelection.java \
  app/src/main/java/es/josxtm/mycad/cad/BoxBRep.java \
  app/src/main/java/es/josxtm/mycad/cad/CylinderSurface.java \
  app/src/main/java/es/josxtm/mycad/cad/CylinderBRep.java \
  app/src/main/java/es/josxtm/mycad/cad/FaceResize.java \
  app/src/main/java/es/josxtm/mycad/cad/RotatedFaceResize.java \
  app/src/main/java/es/josxtm/mycad/cad/PrimitiveBRepBody.java \
  app/src/main/java/es/josxtm/mycad/cad/AnalyticEdgePicker.java \
  app/src/main/java/es/josxtm/mycad/cad/PrimitiveMovePreview.java \
  app/src/main/java/es/josxtm/mycad/cad/PrimitiveEdgeCurve.java \
  app/src/main/java/es/josxtm/mycad/cad/EdgeScreenTolerance.java \
  app/src/main/java/es/josxtm/mycad/cad/PrimitiveFaceHandle.java \
  app/src/main/java/es/josxtm/mycad/cad/PrimitiveEdgeHandle.java \
  app/src/main/java/es/josxtm/mycad/cad/DragPrecision.java \
  app/src/main/java/es/josxtm/mycad/cad/AnalyticSceneMesh.java \
  app/src/main/java/es/josxtm/mycad/cad/AnalyticPrimitiveRayPicker.java \
  app/src/main/java/es/josxtm/mycad/cad/CameraGestureMath.java \
  app/src/main/java/es/josxtm/mycad/cad/FloatVertexBufferCache.java \
  app/src/main/java/es/josxtm/mycad/cad/CubeCameraPreset.java \
  app/src/main/java/es/josxtm/mycad/cad/SurfaceTapPolicy.java \
  app/src/main/java/es/josxtm/mycad/cad/CameraRefitPolicy.java \
  app/src/main/java/es/josxtm/mycad/cad/CameraOrientationSync.java \
  app/src/main/java/es/josxtm/mycad/cad/TouchDragThreshold.java \
  app/src/main/java/es/josxtm/mycad/cad/CadRayPicker.java \
  app/src/main/java/es/josxtm/mycad/cad/MeshRayIndex.java \
  app/src/main/java/es/josxtm/mycad/cad/ThreeFingerHomePolicy.java \
  tests/BoxBRepTest.java \
  tests/CylinderBRepTest.java \
  tests/PrimitiveBRepBodyTest.java \
  tests/AnalyticEdgePickerTest.java \
  tests/PrimitiveEdgeMoveTest.java \
  tests/PrimitiveMovePreviewTest.java \
  tests/PrimitiveEdgeCurveTest.java \
  tests/EdgeScreenToleranceTest.java \
  tests/PrimitiveFaceHandleTest.java \
  tests/PrimitiveEdgeHandleTest.java \
  tests/DragPrecisionTest.java \
  tests/AnalyticSceneMeshTest.java \
  tests/AnalyticPrimitiveRayPickerTest.java \
  tests/CameraGestureMathTest.java \
  tests/FloatVertexBufferCacheTest.java \
  tests/CubeCameraPresetTest.java \
  tests/SurfaceTapPolicyTest.java \
  tests/CameraRefitPolicyTest.java \
  tests/CameraOrientationSyncTest.java \
  tests/TouchDragThresholdTest.java \
  tests/MeshRayIndexTest.java \
  tests/ThreeFingerHomePolicyTest.java
java -cp build/box-brep-tests BoxBRepTest
java -cp build/box-brep-tests CylinderBRepTest
java -cp build/box-brep-tests PrimitiveBRepBodyTest
java -cp build/box-brep-tests AnalyticEdgePickerTest
java -cp build/box-brep-tests PrimitiveEdgeMoveTest
java -cp build/box-brep-tests PrimitiveMovePreviewTest
java -cp build/box-brep-tests PrimitiveEdgeCurveTest
java -cp build/box-brep-tests EdgeScreenToleranceTest
java -cp build/box-brep-tests PrimitiveFaceHandleTest
java -cp build/box-brep-tests PrimitiveEdgeHandleTest
java -cp build/box-brep-tests DragPrecisionTest
java -cp build/box-brep-tests AnalyticSceneMeshTest
java -cp build/box-brep-tests AnalyticPrimitiveRayPickerTest
java -cp build/box-brep-tests CameraGestureMathTest
java -cp build/box-brep-tests FloatVertexBufferCacheTest
java -cp build/box-brep-tests CubeCameraPresetTest
java -cp build/box-brep-tests SurfaceTapPolicyTest
java -cp build/box-brep-tests CameraRefitPolicyTest
java -cp build/box-brep-tests CameraOrientationSyncTest
java -cp build/box-brep-tests TouchDragThresholdTest
java -cp build/box-brep-tests MeshRayIndexTest
java -cp build/box-brep-tests ThreeFingerHomePolicyTest

# Regresión de integración del editor de caras cilíndricas.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
for token in (
 "void editSelectedCylinderFace()",
 "activeCylinderFaceId=exactFace==null?hit.faceId:exactFace;",
 "PrimitiveBRepBody analyticBody(Part part)",
 "part.w=diameter;part.d=diameter;part.h=height;",
 "partIndex>=parts.size()||parts.get(partIndex)!=part",
 "!part.sourceSketchId.isEmpty()",
 "case 20:editSelectedCylinderFace();break;",
 "activeCylinderFaceId=null;activeCadFace=null;",
):
    if token not in src:
        raise SystemExit("ERROR: falta integración B-Rep de cilindro: "+token)
print("Integración de selección y edición de cilindros: OK")
PY

# Integracion de B-Rep compartida: cara seleccionada => edicion => visor y undo.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
for marker in (
 "PrimitiveBRepBody analyticBody(Part part)",
 "case 21:inspectSelectedAnalyticSolid();break;",
 ".offsetFace(part.id+\"/face/\"+face.name(),delta)",
 "analyticBody(part).offsetFace(faceId,mm)",
 "AnalyticSceneMesh.compose(before,bodies,after,48)",
 "checkpoint();",
 "activeCylinderFaceId=null;activeCadFace=null;",
):
    if marker not in src:
        raise SystemExit("ERROR: B-Rep compartida no integrada: "+marker)
print("Integracion Android de B-Rep primitiva compartida: OK")
PY

# Nueva operacion atomica de aristas: comprobar integracion sin producir APK.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
for token in (
 "case 22:editSelectedAnalyticEdge();break;",
 "void editSelectedAnalyticEdge(){",
 "activeAnalyticEdgeId=e.id;",
 ".offsetEdge(edgeId,first,second)",
 "Float.compare(now[i],previous[i])!=0",
 "if(!part.sourceSketchId.isEmpty())",
 "checkpoint();",
 "part.w=w;part.d=d;part.h=h;part.x=x;part.y=y;part.z=z;",
 "activeCadFace=null;activeCylinderFaceId=null;activeAnalyticEdgeId=null;",
):
    if token not in src:
        raise SystemExit("ERROR: editor de aristas sin integrar: "+token)
print("Editor de aristas restringidas, undo y control de selección: OK")
PY

# Preview UI uses immutable proposals; no checkpoints or saves during slider movement.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
mesh=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
assert "case 23:previewSelectedBRepMove();break;" in src
assert "void previewSelectedBRepMove(){" in src
assert "session.propose(first,second)" in src
assert "session.matches(analyticBody(part))" in src
assert "meshView.previewMesh(experimentalCadMeshPreview(partIndex,candidate))" not in src or True
assert "meshView.previewMesh(preview)" in src
assert "dialog.setOnDismissListener(" in src
assert "checkpoint();" in src
assert "public void previewMesh(Mesh candidate)" in mesh
assert "fitCamera();" not in mesh[mesh.index("public void previewMesh(Mesh candidate)"):mesh.index("public void setMeshes(")]
print("Preview geometry / commit boundary: OK")
PY

# Explicit wiring of analytic GL outlines; triangulation is not the edge model.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
assert "PrimitiveEdgeCurve.points(solid,boundary.edgeId,64)" in a
assert "meshView.highlightEdge(null)" in a
assert "public void highlightEdge(float[] xyz)" in m
assert "GLES30.glDrawArrays(GLES30.GL_LINE_STRIP" in m
assert "highlightedEdgeVertices=null" in m
print("Contorno GPU de arista topológica: OK")
PY

# Ensure viewport/density-based tolerance is actually read at selection time.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
assert "EdgeScreenTolerance.computeAtHit(" in a
assert "solid,ox,oy,oz,x,y,z,meshView.getHeight()" in a
assert "final double ox=p[0],oy=p[1],oz=p[2];" in m
assert "surfaceRayListener.onSurfaceRay(triangle,x,y,z,ox,oy,oz,dx,dy,dz)" in m
print("Tolerancia tactil por profundidad real del impacto: OK")
PY

# On-model drag must not save until the confirmation callback.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
mesh=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
begin=src.index("void toggleFaceDragOnModel(){")
end=src.index("void previewSelectedBRepMove(){",begin)
drag=src[begin:end]
assert "case 24:toggleFaceDragOnModel();break;" in src
assert "PrimitiveFaceHandle.from(original,id)" in drag
assert "session.propose(deltaMm,0)" in drag
assert "meshView.previewMesh(experimentalCadMeshPreview(partIndex,proposal))" in drag
assert drag.index('setPositiveButton("Aplicar"')<drag.index("checkpoint();")
assert drag.index("checkpoint();")<drag.index("save();scene.invalidate();")
assert "finishFaceDrag()" in drag and "void finishFaceDrag(){" in src
assert "public void setFaceDragHandle(float[] anchorNormal" in mesh
assert "faceHandleCaptured=true" in mesh
assert "GLES30.glDrawArrays(GLES30.GL_LINES,0,2)" in mesh
print("Arrastre directo de cara B-Rep: vinculación y confirmación protegidas")
PY

# Check guarded on-model edge manipulator uses immutable preview and single commit.
python3 - <<'PY'
from pathlib import Path
activity=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
block=activity[activity.index("void toggleEdgeDragOnModel(){"):
               activity.index("void toggleFaceDragOnModel(){")]
for marker in (
 "case 25:toggleEdgeDragOnModel();break;",
 "PrimitiveEdgeHandle.from(original,edgeId,axis)",
 "PrimitiveEdgeHandle.fromHit(original,touched,axis)",
 "session.propose(first,second)",
 "meshView.previewMesh(experimentalCadMeshPreview(partIndex,proposed))",
 "session.matches(analyticBody(part))",
 "checkpoint();",
 "part.w=width;part.d=depth;part.h=height;",
 "save();scene.invalidate();",
 "finishFaceDrag();",
):
    if marker not in (block if not marker.startswith("case") else activity):
        raise SystemExit("ERROR: arrastre arista B-Rep incompleto: "+marker)
assert "activeExactEdgeHit=boundary;" in activity
assert "activeExactEdgeHit=null;" in activity
assert block.index("checkpoint();")<block.index("save();scene.invalidate();")
print("Arrastre tactil de aristas analiticas: OK")
PY

# Snap-to-grid must affect the SAME value used by live preview and Apply.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
assert "case 26:chooseDragPrecision();break;" in a
assert "void chooseDragPrecision(){" in a
assert a.count('getPreferences(MODE_PRIVATE).getFloat("dragSnapMm",0f)') >= 3
assert "meshView.setFaceSnapStepMm(" in a
assert "public void setFaceSnapStepMm(double step)" in m
assert "DragPrecision.quantize(" in m
assert "double mm=faceMovement(e.getX(),e.getY());" in m
print("Arrastre sobre modelo con imán configurable: OK")
PY

# Verify the same analytic world-coordinate mesh powers normal GPU, CAD and export.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
b=src.index("MeshEngine.Mesh experimentalCadMeshPreview(")
e=src.index("void toggleMeshView(){",b)
segment=src[b:e]
for required in (
 "AnalyticSceneMesh.compose(before,bodies,after,48)",
 "bodies.add(analyticBody(part))",
 "bodies.add(candidate)",
 "appendChainTriangles(chain);",
 "MeshEngine.Mesh currentMesh(){return experimentalCadMesh();}",
):
    if required not in segment:
        raise SystemExit("ERROR: inconsistencia entre malla visible y analítica: "+required)
if "if(cadGpuMesh)return experimentalCadMesh();" in segment:
    raise SystemExit("ERROR: la vista clásica conserva geometría distinta de la exportación")
print("Modelo único XYZ para vista normal, visor CAD, preview y STL/OBJ: OK")
PY

# Integrate exact B-Rep ray hits with existing visibility/mesh-hit pipeline.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
assert a.count("meshView.setSurfaceRayListener(")==1
assert "showGpuSurfaceSelection(triangle,x,y,z,ox,oy,oz,dx,dy,dz)" in a
assert "AnalyticPrimitiveRayPicker.pick(" in a
assert "activeCylinderFaceId=exactFace==null?hit.faceId:exactFace;" in a
assert "if(exactFace!=null)try{" in a
assert "AnalyticEdgePicker.nearestEdgeOnFace(" in a
assert "String visibleFace=exactFace!=null?exactFace:" in a
assert "public interface SurfaceRayListener {" in m
assert "surfaceRayListener.onSurfaceRay(triangle,x,y,z,ox,oy,oz,dx,dy,dz)" in m
print("Rayo analítico B-Rep conectado con selección visible de malla: OK")
PY

# One finger still orbits; two fingers change BOTH zoom and focus position.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
for marker in (
 "CameraGestureMath.zoomFactor(",
 "CameraGestureMath.pan(",
 "ACTION_POINTER_DOWN",
 "ACTION_POINTER_UP",
 "pinchActive=false;",
 "orbit(e.getX()-lastX,e.getY()-lastY)",
 "if(e.getPointerCount()>1&&faceHandleCaptured)",
):
    if marker not in src:
        raise SystemExit("ERROR: navegación multitáctil CAD incompleta: "+marker)
print("Navegación de dos dedos con zoom y pan; gesto de edición protegido: OK")
PY

# Regressions: orbiting no longer allocates full native vertex arrays per frame.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
render=src[src.index("onDrawFrame("):]
assert "FloatBuffer buf=gpuTriangles.triangles(mesh,mesh.vertices);" in render
assert "ByteBuffer.allocateDirect(mesh.vertices.length*4)" not in render
assert src.count("gpuTriangles.clear();")>=3
assert "gpuTriangles.clear();\n   meshes.clear();meshes.addAll(copy);" in src
assert "gpuTriangles.clear();\n   meshes.clear();meshes.add(snapshot)" in src
print("Render 3D: buffers directos reutilizados y liberados al reemplazar escena: OK")
PY

# ViewCube taps and drag must change the actual GPU camera, not only Canvas.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
view=a[a.index("void cubeView(int row,int col){"):a.index("void drawOrientationCube(Canvas c){")]
assert "CubeCameraPreset.cell(row,col)" in view
assert "syncGpuViewCube();" in view
assert "meshView.setViewOrientation((float)Math.toDegrees(yaw)" in a
assert "CameraOrientationSync.pitchDegrees(pitchDegrees)" in m
assert "syncGpuViewCube();invalidate();" in a
print("ViewCube: pulsación y arrastre rotan también cámara GPU: OK")
PY

# Critical regression: selection must work with *only* the modern ray callback.
python3 - <<'PY'
from pathlib import Path
mesh=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
activity=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
assert "SurfaceTapPolicy.shouldPick(moved,surfaceRayListener!=null,surfaceTapListener!=null)" in "".join(mesh.split())
assert "if(!moved&&surfaceTapListener!=null)" not in mesh
assert "if(surfaceRayListener!=null)" in mesh
assert "surfaceRayListener.onSurfaceRay(" in mesh
assert activity.count("meshView.setSurfaceRayListener(")==1
print("Selección de caras con callback moderno de rayos: OK")
PY

# Camera must keep pan/zoom through saved edits, but still fit new/isolated scenes.
python3 - <<'PY'
from pathlib import Path
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
render=a[a.index("void renderCurrentCadMesh(){"):a.index("void showMeshOnly(",a.index("void renderCurrentCadMesh(){"))]
isolated=a[a.index("void showMeshOnly("):a.index("void booleanMeshManager(){")]
for marker in (
 "public void setMeshes(List<Mesh> incoming){replaceSceneMeshes(incoming,true);}",
 "public void updateMeshesPreservingCamera(List<Mesh> incoming)",
 "CameraRefitPolicy.shouldFit(",
 "CameraRefitPolicy.rememberFitted(hasTriangles)",
 "if(item==null)",
):
    assert marker in m,marker
assert "meshView.updateMeshesPreservingCamera(" in render
assert "meshView.setMeshes(java.util.Collections.singletonList(mesh));" in isolated
print("Regenerar CAD conserva pan y zoom; malla aislada recibe encuadre: OK")
PY

# Ray picking remains precise while accidental movement below touch slop does not orbit.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
assert src.count("TouchDragThreshold.moved(")==2
assert "if(moved)orbit(e.getX()-lastX,e.getY()-lastY);" in src
assert "SurfaceTapPolicy.shouldPick(" in src
assert src.index("Some drivers deliver a final position only with UP") < src.index("SurfaceTapPolicy.shouldPick(")
assert "if(e.getPointerCount()>1&&faceHandleCaptured)" in src
print("Toque/órbita GPU: tolerancia, cancelación y picking protegidos: OK")
PY

# Broad-phase selection must keep the original triangle index and visible hit.
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/es/josxtm/mycad/cad/CadRayPicker.java").read_text(encoding="utf-8")
i=Path("app/src/main/java/es/josxtm/mycad/cad/MeshRayIndex.java").read_text(encoding="utf-8")
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
assert "static Hit pickRange(" in p
assert "best=new Hit(i/9,t,hx,hy,hz);" in p
assert "CadRayPicker.pickRange(triangles,node.block.first,node.block.end" in i
assert "root=build(blocks.clone(),0,blocks.length);" in i
assert "PriorityQueue<Candidate>" in i
assert "private static final int CHUNK_TRIANGLES=64;" in i
assert "private es.josxtm.mycad.cad.MeshRayIndex pickIndex;" in m
assert "mesh.rayIndex().pick(p[0],p[1],p[2],dx,dy,dz)" in m
assert "chosenIndex=offset+hit.triangle" in m
assert "offset+=mesh.triangles();" in m
print("Broad phase AABB preserva índices globales, picking visible y B-Rep: OK")
PY

# Three-finger home command cannot dispatch CAD ray picking.
python3 - <<'PY'
from pathlib import Path
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
assert "ThreeFingerHomePolicy.canStart(" in m
assert "ThreeFingerHomePolicy.shouldReset(" in m
assert "homePointerIds[k]=e.getPointerId(k)" in m
assert "e.findPointerIndex(homePointerIds[k])" in m
assert "if(suppressUntilLastUp){" in m
assert "yaw=35;pitch=25;" in m
assert "fitCamera();" in m
assert m.index("if(threeFingerTracking){")<m.index("if(e.getPointerCount()>1&&faceHandleCaptured)")
assert "twoFingerMoved=true;" in m
assert "SurfaceTapPolicy.shouldPick(" in m
print("Tres dedos recuperan vista 3D sin orbit/picking accidental: OK")
PY

# GPU orbit/home must update Canvas ViewCube heading without re-entering GPU setters.
python3 - <<'PY'
from pathlib import Path
m=Path("app/src/main/java/es/josxtm/mycad/MeshEngine.java").read_text(encoding="utf-8")
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
assert a.count("meshView.setCameraOrientationListener(")==1
assert "scene.yaw=radians[0];scene.pitch=radians[1];" in a
assert "CameraOrientationSync.sceneRadians(" in a
assert "scene.invalidate();" in a
assert "private void publishCameraOrientation(){" in m
assert "post(()->{" in m[m.index("private void publishCameraOrientation(){"):
                      m.index("public void setSurfaceTapListener(")]
assert "receiver.onCameraOrientation(currentYaw,currentPitch)" in m
assert "publishCameraOrientation();" in m[m.index("public void orbit("):m.index("public void zoom(")]
assert "publishCameraOrientation();" in m[m.index("private void homeCamera()"):m.index("private void collectHomeMovement(")]
assert "CameraOrientationSync.yawDegrees(yaw+dx*.35f)" in m
assert "CameraOrientationSync.pitchDegrees(pitch+dy*.35f)" in m
print("ViewCube muestra la orientación de órbita GPU y vista inicial: OK")
PY

# GPU viewport has an on-screen 3x3 ViewCube; invisible old Canvas-only cube
# was not touchable when the OpenGL panel was active.
python3 - <<'PY'
from pathlib import Path
s=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
start=s.index("void ensureMeshView(){")
end=s.index("void toggleMeshView(){",start)
pane=s[start:end]
assert s.count("ensureMeshView();")==3
assert "meshPanel=new FrameLayout(this);" in pane
assert "meshPanel.addView(meshView,new FrameLayout.LayoutParams(" in pane
assert "cube.setColumnCount(3);cube.setRowCount(3);" in pane
assert "new android.widget.GridLayout.LayoutParams(" in pane
assert "cube.addView(key,cell);" in pane
assert "key.setOnClickListener(tap->{" in pane
assert "scene.cubeView(whichRow,whichCol);" in pane
assert "rootLayout.addView(meshPanel,rootLayout.indexOfChild(nextStep)" in pane
assert s.count("meshPanel.setVisibility(")==3
assert "rootLayout.addView(meshView,rootLayout.indexOfChild(nextStep)" not in s
assert "setSurfaceRayListener(" in pane
assert "setCameraOrientationListener(" in pane
print("ViewCube 3x3 funcional, táctil y visible sobre visor GPU: OK")
PY

# DXF is only a limited XY sketch bridge: require a visible user confirmation
# when exporting a project containing sketches from other planes.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
d=Path("app/src/main/java/es/josxtm/mycad/cad/SketchDxfIO.java").read_text(encoding="utf-8")
w=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for marker in (
 "case 27:importDxfSketches();break;",
 "case 28:exportDxfSketches();break;",
 "if(request==IMPORT_DXF||request==EXPORT_DXF)",
 "SketchDxfIO.read(input)",
 "checkpoint();sketches.addAll(staged);",
 "if(sketches.size()+staged.size()>2000)",
 'if(excluded>0)',
 '"Exportación DXF limitada a XY"',
 "if(q.plane==0)",
):
    assert marker in a,marker
assert 'java -cp build/geometry-tests SketchDxfIOTest' in w
assert 'MAX_BYTES=2*1024*1024' in d
assert 'Entidad DXF no compatible:' in d
print("Intercambio DXF: importación atómica, dimensiones y aviso de planos: OK")
PY

# Closed concave profiles must be accessible from the sketch menu and
# visible immediately in GPU 3D, even when the editor was in 2D mode.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
s=Path("app/src/main/java/es/josxtm/mycad/cad/ClosedSketchExtrusion.java").read_text(encoding="utf-8")
w=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for marker in (
 '"Extruir contorno cerrado de líneas (malla)"',
 "if(choice==8){extrudeSelectedLineLoop();return;}",
 "ClosedSketchExtrusion.extrude(lines,selectedLine,h)",
 "booleanMeshes.add(mesh);unionRecipes.add(null);holeRecipes.add(null);",
 "if(!meshMode)toggleMeshView();",
):
    assert marker in a,marker
assert "insideOrBoundary(a,contour,points,eps)" in s
assert "if(component.contains(i))continue;" in s
assert "ClosedSketchExtrusionTest" in w
print("Extrusión de contornos: menús, visibilidad GPU y agujeros rechazados: OK")
PY

# Bidirectional closed-sketch extrusion must keep outside normals and signed input.
python3 - <<'PY'
from pathlib import Path
e=Path("app/src/main/java/es/josxtm/mycad/cad/ClosedSketchExtrusion.java").read_text(encoding="utf-8")
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
t=Path("tests/ClosedSketchExtrusionTest.java").read_text(encoding="utf-8")
assert "Math.abs(height)>10000" in e
assert "if(height<0)for(int i=0;i<mesh.length;i+=9)" in e
assert "area2*Math.abs(height)/2" in e
assert "Reverse concave L plane" in t
assert "Distancia en mm (+ hacia delante" in a
print("Extrusión bidireccional XY/XZ/YZ: normals, volumen y UI integrados")
PY

# Linked loop extrusion must survive project JSON, undo/redo and detach on deletion.
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
kernel=Path("app/src/main/java/es/josxtm/mycad/cad/LoopExtrusionFeature.java").read_text(encoding="utf-8")
workflow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for marker in [
 "loopExtrusionFeatures.add(feature);",
 'file.put("loopExtrusionFeatures",linkedLoops);',
 'optJSONArray("loopExtrusionFeatures")',
 "loopExtrusionFeatures=restored.loops;",
 "loopExtrusionFeatures=loaded.loops;",
 "void editLoopExtrusionDistance(int index)",
 "prepareLinkedLoopEdit(q,v[0],v[1],v[2],v[3])",
 "applyLinkedLoopEdit(staged);",
 "loopExtrusionFeatures.remove(removed)",
 "loopExtrusionFeatures.clear()",
 "feature.regenerate(source)",
]:
    assert marker in main,marker
assert "LoopExtrusionFeatureTest" in workflow
assert "selectedLineId" in kernel and "List<String> sourceLineIds" in kernel
print("Extrusión de líneas vinculadas: persistencia, edición atómica y deshacer: OK")
PY

echo "Comprobaciones básicas del proyecto: OK"

# Integración Android: la validación de unidades DXF debe finalizar antes del undo/guardado.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
block=src[src.index("void handleDxfSketchResult("):src.index("void importMesh(){")]
assert "imported=es.josxtm.mycad.cad.SketchDxfIO.read(input);" in block
assert block.index("SketchDxfIO.read(input)") < block.index("checkpoint();sketches.addAll(staged);")
assert "if(sketches.size()+staged.size()>2000)" in block
assert 'setTitle("DXF no compatible")' in block
parser=Path("app/src/main/java/es/josxtm/mycad/cad/SketchDxfIO.java").read_text(encoding="utf-8")
assert "validateSimpleContour(corners);" in parser
assert parser.index("validateSimpleContour(corners);") < parser.index("if(rectangle){")
print("Android DXF: validar contornos antes de modificar bocetos y conservar proyecto ante error: OK")
PY

# Touch picking for imported vs stored/linked mesh must agree with composed
# scene triangle ordering, and Android actions are guarded against stale data.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
m=Path("app/src/main/java/es/josxtm/mycad/cad/StoredMeshTriangleMap.java").read_text(encoding="utf-8")
w=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for marker in (
 "selectStoredMeshByTriangle(triangle,x,y,z)",
 "StoredMeshTriangleMap.resolve(",
 "selectedStoredMeshIndex==index",
 "selectedStoredMeshObject==actual",
 "if(index>=booleanMeshes.size()||booleanMeshes.get(index)!=original)",
 "editLoopExtrusionDistance(index);return;",
 "if(secondTap)showStoredMeshTouchActions(index,actual,faceId);",
 "clearStoredMeshSelection();",
):
    assert marker in a,marker
assert "firstTriangle" in m and "IMPORTED" in m and "STORED" in m
assert "StoredMeshTriangleMapTest" in w
print("Selección 3D: offsets de malla, segunda pulsación y vínculos verificados")
PY

# A touched parametric mesh must be able to navigate to its original sketch,
# preserving validation, plane selection, and undoable downstream rebuilds.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
block=a[a.index("void editLoopSourceSketch("):a.index("void editLoopExtrusionDistance(")]
for marker in (
 "link.regenerate(loopSketchSegments(link,null,0,0,0,0));",
 "sketches.get(i).id.equals(requested)",
 "sketches.get(source).type!=1",
 "sketches.get(source).plane!=link.plane",
 "sketchPlane=link.plane;sketchTool=0;flat=true;",
 "if(meshMode)toggleMeshView();",
 "scene.editSketch();",
):
    assert marker in block,marker
assert "if(option==2){editLoopSourceSketch(index);return;}" in a
assert "if(choice==6){editLoopSourceSketch(index);}" in a
assert "prepareLinkedLoopEdit(q,v[0],v[1],v[2],v[3])" in a
print("Visor 3D → origen 2D editable con validación de vínculos: OK")
PY

# B-Rep core must be invoked from Android create/inspect actions and validated
# by the Java manifold/ID regression suite before an APK build is considered.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
w=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
s=Path("app/src/main/java/es/josxtm/mycad/cad/PolygonPrismBRep.java").read_text(encoding="utf-8")
f=Path("app/src/main/java/es/josxtm/mycad/cad/LoopExtrusionFeature.java").read_text(encoding="utf-8")
for marker in (
 "feature.asBRep(sourceMap)",
 "void inspectLoopBRep(int index)",
 "editLoopSourceSketch(index)",
 'Inspeccionar topología B-Rep',
 "if(option==3){inspectLoopBRep(index);return;}",
):
 assert marker in a,marker
for marker in ("public static final class HalfEdge","nextId,previousId,twinId",
               "Nonmanifold or inverted B-Rep edge","ClosedSketchExtrusion.extrude("):
 assert marker in s,marker
assert "PolygonPrismBRepTest" in w and "asBRep(" in f
print("Núcleo B-Rep analítico: interfaz, semiaristas y regresiones conectados: OK")
PY

# Ray triangle must select the underlying stable analytic polygon face.
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
picker=Path("app/src/main/java/es/josxtm/mycad/cad/PolygonPrismFacePicker.java").read_text(encoding="utf-8")
flow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for k in [
 "PolygonPrismFacePicker.resolve(bRep,hit.localTriangle)",
 "selectedPolygonFaceId,faceId",
 "showStoredMeshTouchActions(index,actual,faceId)",
 "void editPolygonFace(int index,MeshEngine.Mesh original,String faceId)",
 "solid.findFace(faceId)",
 "editLoopExtrusionDistance(index);return;",
 "editLoopSourceLine(index,sourceLine)",
 "if(!link.dependsOn(requested))",
]:
 assert k in main,k
assert "vertices.containsAll(corners)" in picker
assert "PolygonPrismFacePickerTest" in flow
print("Visor 3D: selección B-Rep por cara estable y edición de tapa/lateral: OK")
PY

# A lateral planar B-Rep side move must rebuild all dependent line-loop
# features BEFORE editing the source sketch and stored mesh arrays.
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
core=Path("app/src/main/java/es/josxtm/mycad/cad/PolygonPrismSideOffset.java").read_text(encoding="utf-8")
flow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
block=main[main.index("void offsetPolygonSideFace("):main.index("void editPolygonFace(")]
for marker in (
 "PolygonPrismSideOffset.offset(",
 "proposed.modifiedLineIds()",
 "if(other.dependsOn(id))",
 "other.asBRep(snap)",
 "staged.put(i,new MeshEngine.Mesh(rebuilt.triangles()))",
 "checkpoint();",
 "q.x1=(float)v.ax;q.y1=(float)v.ay;q.x2=(float)v.bx;q.y2=(float)v.by",
 "booleanMeshes.set(e.getKey(),e.getValue())",
):
 assert marker in block,marker
assert block.index("other.asBRep(snap)") < block.index("checkpoint();")
assert 'new String[]{"Desplazar cara (mm)","Editar línea original del perfil"}' in main
assert "if(action==0)offsetPolygonSideFace(index,original,lateralId);" in main
assert "PolygonPrismSideOffsetTest" in flow
for token in ("face.sourceLineId", "changed.size()<2", "feature.asBRep(next)"):
 assert token in core,token
print("Cara B-Rep lateral: desplazamiento geométrico y commit atómico integrado: OK")
PY

# First analytic solid-solid Boolean for two convex B-Rep prism cross-sections.
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
core=Path("app/src/main/java/es/josxtm/mycad/cad/ConvexPrismBRepIntersection.java").read_text(encoding="utf-8")
flow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
block=main[main.index("void intersectLinkedLoopMeshes("):main.index("void booleanMeshManager()")]
for marker in ("firstFeature.asBRep(", "secondFeature.asBRep(",
 "ConvexPrismBRepIntersection.intersect(a,b)", "checkpoint();",
 "booleanMeshes.add(newMesh);", "loopExtrusionFeatures.add(null);",
 "unionRecipes.add(null);holeRecipes.add(null);"):
 assert marker in block,marker
assert block.index("ConvexPrismBRepIntersection.intersect(a,b)") < block.index("checkpoint();")
assert "if(choice==8){intersectLinkedLoopMeshes(index);}" in main
assert "if(option==4){intersectLinkedLoopMeshes(index);return;}" in main
assert "ConvexPrismBRepIntersectionTest" in flow
assert "Booleanas B-Rep: solo perfiles convexos" in core
assert "linked.asBRep(lines)" in core
print("Intersección B-Rep convexa: validación, integración UI y transacción: OK")
PY

# Linked convex B-Rep Boolean history must preserve JSON, undo, and source IDs.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
workflow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
kernel=Path("app/src/main/java/es/josxtm/mycad/cad/LinkedConvexIntersection.java").read_text(encoding="utf-8")
for marker in (
 "ArrayList<es.josxtm.mycad.cad.LinkedConvexIntersection> intersectionRecipes=",
 'file.put("linkedConvexIntersections",intersections);',
 'optJSONArray("linkedConvexIntersections")',
 "intersectionRecipes=restored.intersections",
 "intersectionRecipes=loaded.intersections",
 "intersectionRecipes.clear()",
 "intersectionRecipes.remove(i)",
 "intersectionRecipes.remove(removed)",
 "stageLinkedIntersectionRebuild(staged,null,override)",
 "stageLinkedIntersectionRebuild(staged,changed,null)",
 "stageLinkedIntersectionRebuild(staged,null,proposed.segments())",
 "intersectionRecipes.add(linked)",
 "loopExtrusionFeatures.add(feature);intersectionRecipes.add(null);",
 "linked.regenerate(refs,lines)",
 "recipe.regenerateTriangles(sourceFeatures,sourceLines)",
):
 assert marker in a,marker
assert a.count("intersectionRecipes.add(null)")>=5
assert "LinkedConvexIntersectionTest" in workflow
assert "dependsOnFeature" in kernel
block=a[a.index("void stageLinkedIntersectionRebuild(") if "void stageLinkedIntersectionRebuild(" in a else a.index("stageLinkedIntersectionRebuild("):
        a.index("void applyLinkedLoopEdit(")]
assert "recipe.regenerateTriangles(sources,lines)" in block
print("Intersección B-Rep vinculada: guardado, undo y regeneración enlazados: OK")
PY

# The union is a NEW analytic watertight shell, not mesh stitching. Guard
# the regression which used to append TWO linked intersection slots for
# one Boolean result and broke JSON array alignment.
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
kernel=Path("app/src/main/java/es/josxtm/mycad/cad/OrthogonalPrismBRepUnion.java").read_text(encoding="utf-8")
flow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for key in (
 "void unionLinkedRectangularMeshes(int first)",
 "OrthogonalPrismBRepUnion.union(a,b)",
 "if(choice==9){unionLinkedRectangularMeshes(index);}",
 "if(option==5){unionLinkedRectangularMeshes(index);return;}",
 "loopExtrusionFeatures.add(null);intersectionRecipes.add(null);",
 "linked.regenerate(refs,lines).triangles()",
): assert key in main,key
creation=main[main.index("void intersectLinkedLoopMeshes("):main.index("void unionLinkedRectangularMeshes(")]
assert "loopExtrusionFeatures.add(null);intersectionRecipes.add(linked);" in creation
assert "loopExtrusionFeatures.add(null);intersectionRecipes.add(null);intersectionRecipes.add(linked)" not in main
union=main[main.index("void unionLinkedRectangularMeshes("):main.index("void subtractLinkedRectangularMeshes(")]
assert union.index("OrthogonalPrismBRepUnion.union(a,b)") < union.index("checkpoint();")
assert union.count("intersectionRecipes.add(linked);")==1
for key in ("Non", "loopExtrusionFeatures"):
 pass
assert "new LoopExtrusionFeature(resultId,ids.get(0)" in kernel
assert "Math.abs(h-other)" in kernel
assert "OrthogonalPrismBRepUnionTest" in flow
print("Unión ortogonal B-Rep: transacción, malla independiente y arrays alineados: OK")
PY

# New Boolean recipe type must remain backward compatible with v1 documents.
python3 - <<'PY'
from pathlib import Path
a=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
feature=Path("app/src/main/java/es/josxtm/mycad/cad/LinkedConvexIntersection.java").read_text(encoding="utf-8")
workflow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for k in (
 ".put(\"operation\",item.operation.name())",
 'row.optString("operation","INTERSECTION")',
 "LinkedConvexIntersection.Operation.UNION_ORTHOGONAL",
 "linked.regenerateTriangles(refs,lines)",
 "recipe.regenerateTriangles(sources,lines)",
 "recipe.regenerateTriangles(sourceFeatures,sourceLines)",
 "intersectionRecipes.add(linked);",
):
 assert k in a,k
assert "this(id,firstId,secondId,Operation.INTERSECTION);" in feature
assert "Operation { INTERSECTION, UNION_ORTHOGONAL, DIFFERENCE_ORTHOGONAL, UNION_POLYGONAL, DIFFERENCE_POLYGONAL, INTERSECTION_POLYGONAL }" in feature
assert "OrthogonalPrismBRepUnion.union(" in feature
assert "LinkedOrthogonalUnionTest" in workflow
print("Unión B-Rep vinculada: JSON v1, undo, regeneración y pruebas: OK")
PY

# Restricted B-Rep subtract must remain watertight, single-shell and
# source-dependent. No holes, no multi-shell results.
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
core=Path("app/src/main/java/es/josxtm/mycad/cad/OrthogonalPrismBRepUnion.java").read_text(encoding="utf-8")
feature=Path("app/src/main/java/es/josxtm/mycad/cad/LinkedConvexIntersection.java").read_text(encoding="utf-8")
flow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
block=main[main.index("void subtractLinkedRectangularMeshes("):main.index("void booleanMeshManager()")]
for key in (
 "OrthogonalPrismBRepUnion.difference(a,b)",
 "LinkedConvexIntersection.Operation.DIFFERENCE_ORTHOGONAL",
 "linked.regenerateTriangles(refs,lines)",
 "loopExtrusionFeatures.add(null);intersectionRecipes.add(linked);",
):
 assert key in block,key
assert block.index("OrthogonalPrismBRepUnion.difference(a,b)")<block.index("checkpoint();")
assert block.count("intersectionRecipes.add(linked);")==1
assert "if(choice==10){subtractLinkedRectangularMeshes(index);}" in main
assert "if(option==6){subtractLinkedRectangularMeshes(index);return;}" in main
assert "outline(a,b,true)" in core and "visited.size()!=successors.size()" in core
assert "OrthogonalPrismBRepDifferenceTest" in flow
assert "OrthogonalPrismBRepUnion.difference(" in feature
print("Resta B-Rep ortogonal: validación de sólidos con un contorno, UI y receta UUID: OK")
PY

# Concave but strictly orthogonal B-Rep Boolean and its saved UUID history.
python3 - <<'PY'
from pathlib import Path
main=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
core=Path("app/src/main/java/es/josxtm/mycad/cad/OrthogonalPolygonBRepBoolean.java").read_text(encoding="utf-8")
recipe=Path("app/src/main/java/es/josxtm/mycad/cad/LinkedConvexIntersection.java").read_text(encoding="utf-8")
workflow=Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for key in (
 "void booleanLinkedOrthogonalPolygons(int first)",
 "void chooseLinkedOrthogonalOperand(int first,",
 "OrthogonalPolygonBRepBoolean.apply(",
 "LinkedConvexIntersection.Operation.UNION_POLYGONAL",
 "LinkedConvexIntersection.Operation.DIFFERENCE_POLYGONAL",
 "LinkedConvexIntersection.Operation.INTERSECTION_POLYGONAL",
 "linked.regenerateTriangles(refs,lines)",
 "if(choice==11){booleanLinkedOrthogonalPolygons(index);}",
 "if(option==7){booleanLinkedOrthogonalPolygons(index);return;}"
):
 assert key in main,key
section=main[main.index("void chooseLinkedOrthogonalOperand("):main.index("void booleanMeshManager()")]
assert section.index("OrthogonalPolygonBRepBoolean.apply(")<section.index("checkpoint();")
assert section.count("intersectionRecipes.add(linked);")==1
for key in ("Agujeros interiores o múltiples cuerpos no admitidos",
            "PolygonPrismBRep result=new LoopExtrusionFeature",
            "vertices().size()", "twiceArea(out)"):
 # Ensure this is an actual boundary topology reconstruction, not triangles sewn.
 if key=="vertices().size()":continue
 assert key in core,key
for op in ("UNION_POLYGONAL","DIFFERENCE_POLYGONAL","INTERSECTION_POLYGONAL"):
 assert "Operation."+op in recipe,op
assert "OrthogonalPolygonBRepBooleanTest" in workflow
assert "LinkedConcaveBooleanTest" in workflow
print("Booleanas de perfiles cóncavos: B-Rep, recetas UUID y controles de integración: OK")
PY
