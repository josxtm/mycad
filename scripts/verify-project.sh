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
        "contains(github.event.head_commit.message, '[apk]')" in apk_step[0],
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
print("Borrador, perforación circular y protección de vista previa: OK")
PY

echo "Comprobaciones básicas del proyecto: OK"
