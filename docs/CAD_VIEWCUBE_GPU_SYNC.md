# MyCAD 0.6.0 — ViewCube vinculado al visor GPU

El cubo de orientación de la interfaz ya controlaba la cámara del lienzo 3D propio, pero **no necesariamente la cámara independiente del visor OpenGL**. Desde este bloque ambos comparten el comando de orientación.

`CubeCameraPreset.cell(row,col)` define las nueve posiciones del cubo (superior, inferior, frente, izquierda, derecha y diagonales), devolviendo ángulos yaw/pitch en radianes, compatibles con el lienzo existente. Al pulsar una casilla del ViewCube, `Scene.cubeView` adopta el preset y lo envía con conversión de radianes a grados a `MeshEngine.setViewOrientation`. Al arrastrar el cubo con el dedo también se actualiza el visor GPU. No se reemplazan piezas ni se modifica la selección, la historia o el archivo.

La cámara GPU conserva el centro y la distancia actuales, cambiando solo su orientación. Los ángulos verticales se limitan a ±89° para evitar una dirección de mirada degenerada. Este comportamiento es independiente de la vista previa de piezas B-Rep.

`CubeCameraPresetTest` verifica las nueve direcciones, correspondencia radianes-grados, limites e índices no válidos. Las pruebas de integración verifican la conexión del cubo y el visor Android; Gradle/Lint sigue en CI sin APK.

**Limitaciones:** todavía no hay un cubo superpuesto dentro de la superficie OpenGL, que es un panel independiente; se utiliza el ViewCube visible del lienzo principal. La actualización inversa (girar con el dedo en GPU y actualizar una figura animada del cubo) queda pendiente. Prueba visual en Nothing Phone 2a pendiente.
