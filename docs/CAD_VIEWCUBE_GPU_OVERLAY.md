# MyCAD 0.6.0 — ViewCube táctil dentro del visor OpenGL

## Problema

La cámara de `MeshEngine` ya podía recibir orientaciones del ViewCube del lienzo principal, pero ese control estaba dibujado sobre `Scene`, **fuera del visor OpenGL**. Al abrir el panel GPU y tocarlo, el usuario podía no disponer de un control de orientación visible y operativo dentro del propio panel de geometría.

## Solución

`MainActivity.ensureMeshView()` crea un contenedor `FrameLayout` de 240 dp que incluye el `MeshEngine` y un `GridLayout` de 3×3 superpuesto en su esquina superior derecha. Las nueve celdas son botones táctiles visibles con rótulos ISO, SUP, INF, IZQ, FREN y DER; al pulsarlas llaman a `Scene.cubeView(row,col)` y reutilizan `CubeCameraPreset`, que envía yaw/pitch correctos a la cámara GPU. Si excepcionalmente no existe `Scene`, el preset se envía directamente a `MeshEngine`.

El control ocupa 123×123 dp y únicamente intercepta toques dentro de esa esquina. El resto del visor conserva órbita, desplazamiento de dos dedos, picking geométrico y manipuladores B-Rep.

La creación del visor, su callback de rayos y su sincronización de orientación se centralizan en `ensureMeshView()`, sustituyendo tres rutas duplicadas. Al ocultar el visor GPU se oculta también el `FrameLayout`, sin dejar una zona vacía de 240 dp; al abrir una previsualización aislada se muestra.

## Pruebas y límites

El script de integridad comprueba que las tres rutas usan el mismo visor, las nueve celdas invocan la orientación compartida, el contenedor y el visor muestran/ocultan conjuntamente y los listeners de rayos/ángulos están registrados. El flujo normal ejecuta Gradle, Lint y tests de geometría, sin APK.

**Pendiente:** comprobar en Nothing Phone 2a las dimensiones reales, la legibilidad, la respuesta de los toques y el comportamiento al tocar muy cerca de una arista. No sustituye todavía por un ViewCube 3D con caras inclinadas o un aro de rotación libre.
