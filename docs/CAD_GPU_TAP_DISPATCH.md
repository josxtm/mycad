# MyCAD 0.6.0 — Corrección crítica: toque sobre geometría GPU

## Incidencia

El visor `MeshEngine` evolucionó desde `SurfaceTapListener` hacia `SurfaceRayListener` para pasar el rayo mundial necesario para seleccionar caras y aristas B-Rep exactas. Sin embargo, la condición de `ACTION_UP` seguía comprobando exclusivamente `surfaceTapListener != null` antes de ejecutar `pickSurfaceAt`. La actividad principal registra **solo** `setSurfaceRayListener`, de modo que un toque breve sobre el sólido podía no iniciar el picking, aunque los cálculos matemáticos fueran correctos.

## Cambio

`SurfaceTapPolicy.shouldPick(moved, hasRay, hasLegacy)` permite el raycast cuando no ha habido un gesto de movimiento y existe **cualquiera** de los dos callbacks. Se conserva la compatibilidad con el listener antiguo y se evita seleccionar una cara tras órbita/desplazamiento de la cámara.

La ruta de hit-testing sigue siendo la ya existente: raycast de malla visible → refinamiento analítico exacto en la cara → selección topológica estable → resaltado y manipuladores.

## Pruebas

`SurfaceTapPolicyTest` verifica todas las combinaciones de listeners y movimiento; `scripts/verify-project.sh` asegura que Android registra el callback moderno y que la puerta `ACTION_UP` no vuelve a exigir el legado. CI Gradle/Lint normal, sin APK.

**Pendiente:** confirmar físicamente en Nothing Phone 2a la sensibilidad del toque y los distintos modos de edición.
