# MyCAD 0.6.0 — ViewCube y cámara GPU en ambos sentidos

En la versión anterior, tocar o arrastrar el ViewCube modificaba correctamente el visor OpenGL, pero girar la cámara con un dedo **en el visor GPU** no actualizaba el cubo dibujado en el lienzo principal. El gesto de tres dedos para volver a la vista inicial tampoco actualizaba la orientación visible del cubo.

`MeshEngine.CameraOrientationListener` publica ahora las orientaciones del visor tras una órbita táctil o el reinicio 3D. Usa el hilo de UI para actualizar el lienzo; no modifica el modelo CAD ni la geometría. El controlador `MainActivity` convierte ángulos de grados (OpenGL) a radianes (lienzo) por `CameraOrientationSync.sceneRadians` y actualiza `scene.yaw`, `scene.pitch` e `invalidate()`. **No se llama de nuevo a `setViewOrientation` desde el callback**, para evitar un bucle de eventos.

La conversión también normaliza el heading a ±180° y limita pitch a ±89°; se reduce la pérdida de precisión cuando se gira muchas vueltas. El cubo sigue iniciando cambios desde la interfaz como antes.

CI: `CameraOrientationSyncTest` cubre conversiones, vueltas completas, polos y valores no finitos. El script de integridad verifica que las tres rutas de creación del visor registran el callback y que órbita/reinicio notifican al lienzo. Gradle/Lint Android, sin APK.

Pendiente verificación del comportamiento visual de ambos paneles y la fluidez del callback en Nothing Phone 2a. No añade un cubo superpuesto dentro del panel OpenGL.
