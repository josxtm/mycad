# MyCAD 0.6.0 — Motor B-Rep unificado de primitivas

Este incremento introduce `PrimitiveBRepBody` como **fachada geométrica Java pura** compartida por prismas y cilindros. Construye sólidos con sus UUID persistentes; describe las caras analíticas, aristas, áreas, volumen, normales y adyacencias; y resuelve operaciones de desplazamiento de caras por su ID estable. Las mallas son productos secundarios del renderizado, no la fuente de geometría.

## Cambios de Android

- `experimentalCadMesh()` utiliza la misma fachada para los dos tipos de sólidos. En prismas ya no se generan UUID aleatorios en cada repintado.
- La edición no vinculada de caras de prisma y cilindro pasa por `offsetFace(faceId, mm)`, que construye un **nuevo estado propuesto e inmutable**. El editor aplica sus valores solo tras validar el resultado y registrar el punto de deshacer.
- Nuevo elemento en **Proyecto → Inspeccionar sólido B-Rep (caras y aristas)**. Presenta volumen, superficie y lista de entidades. Tocando una cara muestra su área, normal y límites; tocando una arista muestra longitud, tipo y caras vecinas.
- Se conservan el vínculo de bocetos y las restricciones existentes; las operaciones no alteran directamente el grafo ni convierten mallas booleanas en sólidos editables.

## Pruebas automáticas

`PrimitiveBRepBodyTest` verifica topología y adyacencia para ambos sólidos, conservación de UUID durante edición, offset de caras en prismas/cilindros rotados, conservación de la cara opuesta, cotas, volumen, área, mallas independientes, y rechazo de IDs obsoletos y geometría degenerada. La verificación corre con Java puro en `scripts/verify-project.sh` y con la CI Android habitual, sin generar APK.

## Limitación fundamental

No es B-Rep general: no resuelve aristas compartidas después de booleanas, trimado de superficies, filetes, chaflanes, vaciado, ni importación STEP. Faltan raycasting preciso de aristas y controladores táctiles completos. La prueba funcional en teléfono todavía está pendiente.
