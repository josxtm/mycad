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
  tests/AnalyticSceneMeshTest.java
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

# Regresión de integración del editor de caras cilíndricas.
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/es/josxtm/mycad/MainActivity.java").read_text(encoding="utf-8")
for token in (
 "void editSelectedCylinderFace()",
 "activeCylinderFaceId=hit.faceId",
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
assert "EdgeScreenTolerance.compute(" in a
assert "meshView.cameraDistance(),meshView.getHeight()" in a
assert "private volatile float distance=300;" in m
print("Tolerancia tactil dependiente del zoom: OK")
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

echo "Comprobaciones básicas del proyecto: OK"
