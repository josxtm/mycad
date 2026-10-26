# MyCAD 0.6.0 — Renderizado sin reservas nativas por fotograma

Antes, `MeshEngine.onDrawFrame` reservaba con `ByteBuffer.allocateDirect` todo el contenido de cada malla **en cada fotograma**, incluso si la pieza no había cambiado. En modelos grandes y durante giros o pellizcos, eso genera presión sobre la memoria nativa y pausas perceptibles.

Este bloque introduce `FloatVertexBufferCache`: prepara una única vez por objeto `Mesh` el buffer de triángulos XYZ y devuelve vistas `FloatBuffer.duplicate()` con cursores independientes para dibujar. El renderizado no necesita copiar de nuevo todos los datos cuando únicamente varían los parámetros de cámara. Al sustituir una escena o una previsualización, se vacía la caché para liberar referencias a la memoria directa; al recrear el contexto EGL se descarta su contenido.

La caché trata el array de una malla como inmutable mientras esté en pantalla; la aplicación ya sustituye objetos `Mesh` completos al recalcular geometría. Si se cambia el array in situ, es obligatorio invalidar la caché. No altera el formato de proyecto, la selección, la geometría, ni la calidad de triangulación. Mantiene un límite de 36 MB de datos triangulares en memoria caché, igual al compositor de escena.

CI con `FloatVertexBufferCacheTest`: buffers directos, cursor independiente por fotograma, conservación de contenido, renovación por identidad, limpieza, aislamiento del origen y rechazo de datos inválidos. Verificación de integración y Gradle/Lint Android. **Sin APK** hasta hito de pruebas en teléfono.

**Pendiente:** medir fotogramas y consumo de memoria en Nothing Phone 2a; el resaltado de aristas y los manejadores de control, mucho más pequeños, aún crean sus propios buffers temporales.
