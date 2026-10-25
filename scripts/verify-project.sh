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
for marker in ("throughHoleRecipes", "holeRecipes.add(recipe)", "holeRecipes.add(null)", "editHoleRecipe(int index)"):
    if marker not in source:
        raise SystemExit("ERROR: perforación no persistente "+marker)
print("Borrador, perforación circular y protección de vista previa: OK")
PY

echo "Comprobaciones básicas del proyecto: OK"
