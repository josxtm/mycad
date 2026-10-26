# MyCAD 0.6.0 — Cámara estable durante regeneración CAD

Antes, cada llamada a `MainActivity.renderCurrentCadMesh()` reconstruía la malla mediante `MeshEngine.setMeshes()` y ejecutaba `fitCamera()`. Como resultado, tras confirmar una operación geométrica, borrar o regenerar una operación, el visor volvía a centrar la escena y sustituía el nivel de zoom elegido por el usuario.

Ahora existe una separación explícita:

- `MeshEngine.setMeshes(...)`: se utiliza para abrir/encuadrar una escena nueva o una malla aislada.
- `MeshEngine.updateMeshesPreservingCamera(...)`: actualiza geometría renderizada y limpia el resaltado, conservando punto de enfoque, distancia y ángulos de cámara.
- En la primera escena no vacía, o después de vaciar completamente el proyecto, una actualización puede solicitar `fitCamera` automáticamente mediante `CameraRefitPolicy`. Un proyecto vacío no se intenta encuadrar.

`renderCurrentCadMesh()` utiliza la segunda vía; `showMeshOnly()` continúa usando encuadre explícito. No se alteran objetos, proyecto JSON, coordenadas ni historial. Las reglas CI comprueban la conexión de ambos flujos y `CameraRefitPolicyTest` verifica las transiciones de primera vista, ediciones, malla aislada y escena vacía.

**Pendiente:** validación de experiencia de cámara y controles táctiles en Nothing Phone 2a. No se genera APK para este incremento.
