# MyCAD 0.6.0 — Selección exacta de superficies B-Rep

`AnalyticPrimitiveRayPicker` calcula intersecciones rayo-superficie geométrica **sin depender de la triangulación** del visor: seis planos delimitados para prismas y una superficie lateral de cilindro más sus dos tapas circulares. Las coordenadas mundiales se transforman al marco local XYZ del cuerpo con `RigidTransform.inversePoint`. Devuelve el ID estable de cara, punto mundial y distancia real en milímetros; admite rayos originados dentro de sólidos y cuerpos rotados.

La integración Android mantiene primero `CadRayPicker` para identificar el **cuerpo visible** al tocar un triángulo (las mallas importadas y booleanas siguen siendo triangulares). `MeshEngine` envía además el origen y dirección del rayo. Si el impacto analítico con ese mismo cuerpo está suficientemente cerca del punto de la malla, la selección se actualiza con el punto y la cara B-Rep exactos. Si la geometría no coincide, se conserva la selección de malla anterior para evitar saltos inesperados.

La diferencia es especialmente útil en cilindros: el cálculo geométrico de sus tapas y superficie lateral ya no usa únicamente triángulos facetados. La edición posterior conserva IDs persistentes de primitivas.

Pruebas: `AnalyticPrimitiveRayPickerTest` cubre caja y cilindro, caras y tapas, distancias normalizadas, rayos desde dentro, fallos por límites, tangententes, rotaciones XYZ y modificaciones de diámetro. CI Android valida la integración del callback y compilación/Lint. Sin APK.

**Límite:** no se implementa selección analítica de filetes, chaflanes, intersecciones booleanas o mallas importadas. La tolerancia al refinar prioriza la consistencia con el cuerpo ya visible en el visor. Falta probar visualmente en Nothing Phone 2a.
