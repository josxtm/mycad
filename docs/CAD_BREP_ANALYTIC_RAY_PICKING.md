# MyCAD 0.6.0 — Selección exacta de superficies B-Rep

`AnalyticPrimitiveRayPicker` calcula intersecciones rayo-superficie geométrica **sin depender de la triangulación** del visor: seis planos delimitados para prismas y una superficie lateral de cilindro más sus dos tapas circulares. Las coordenadas mundiales se transforman al marco local XYZ del cuerpo con `RigidTransform.inversePoint`. Devuelve el ID estable de cara, punto mundial y distancia real en milímetros; admite rayos originados dentro de sólidos y cuerpos rotados.

La integración Android mantiene primero `CadRayPicker` para identificar el **cuerpo visible** al tocar un triángulo (las mallas importadas y booleanas siguen siendo triangulares). `MeshEngine` envía además el origen y dirección del rayo. Si el impacto analítico con ese mismo cuerpo está suficientemente cerca del punto de la malla, la selección se actualiza con el punto y la cara B-Rep exactos. Si la geometría no coincide, se conserva la selección de malla anterior para evitar saltos inesperados.

La diferencia es especialmente útil en cilindros: el cálculo geométrico de sus tapas y superficie lateral ya no usa únicamente triángulos facetados. La edición posterior conserva IDs persistentes de primitivas.

Pruebas: `AnalyticPrimitiveRayPickerTest` cubre caja y cilindro, caras y tapas, distancias normalizadas, rayos desde dentro, fallos por límites, tangententes, rotaciones XYZ y modificaciones de diámetro. CI Android valida la integración del callback y compilación/Lint. Sin APK.

**Límite:** no se implementa selección analítica de filetes, chaflanes, intersecciones booleanas o mallas importadas. La tolerancia al refinar prioriza la consistencia con el cuerpo ya visible en el visor. Falta probar visualmente en Nothing Phone 2a.

## Incremento: coherencia entre rayo exacto y arista (26/10/2026)

La selección de aristas ahora recibe el impacto `AnalyticPrimitiveRayPicker.Hit`
completo cuando el visor acepta el refinamiento analítico. El selector
`AnalyticEdgePicker.nearestEdgeOnSurfaceHit` verifica identidad de pieza,
tipo, dimensiones y transformación XYZ antes de clasificar únicamente las
aristas de la cara exacta. Esto impide reutilizar puntos de un rayo antiguo
tras mover, girar o redimensionar un cuerpo. Las mallas que no se refinan
conservan la selección por cara de triángulo como alternativa explícita.

Las pruebas cubren prismas y cilindros girados, reconstrucción idéntica,
selección de tapa, rechazo de geometría obsoleta y tolerancias inválidas.
Verificación Java y Android mediante `verify`; no se genera APK ni se
presume validación táctil en Nothing Phone 2a.

## Corrección: límites precisos para primitivas micrométricas (0.6.0)

La clasificación de una cara analítica usa ahora una tolerancia en mm
**proporcional a las dimensiones reales** del cuerpo, ampliada únicamente
por el error de redondeo de su traslación mundial. El mínimo fijo de 1 mm
empleado al estimar tolerancias podía aceptar impactos externos en cajas
y cilindros de tamaño micrométrico. La tolerancia del discriminante
cuadrático del cilindro usa igualmente la escala de sus términos en
lugar de elevarla artificialmente a 1.

Regresiones: impacto interior y fallo exterior para caja de 1 µm,
lateral y tapa de cilindro de 1 µm, además de los casos previos con
cuerpos rotados y desplazados. Es una corrección del selector analítico
de **primitivas**, no un cambio en STL/OBJ, B-Rep general o gestos.
No se presupone verificación física; este cambio solo requiere CI Java
y Android, **sin generar APK**.
