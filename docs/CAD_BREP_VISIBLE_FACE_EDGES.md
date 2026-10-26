# MyCAD 0.6.0 — aristas restringidas a la cara visible

## Motivación

La selección de aristas del visor OpenGL utilizaba el punto de impacto
tridimensional de una superficie, pero comparaba ese punto contra **todas**
las aristas de la pieza. Con tolerancias en milímetros dependientes del zoom,
un borde próximo perteneciente a otra cara podía competir con el contorno
de la superficie seleccionada, especialmente en piezas delgadas.

## Implementación

`AnalyticEdgePicker.nearestEdgeOnFace(body, faceId, x, y, z, tolerance)`
limita los candidatos a las aristas que `PrimitiveBRepBody` declara
adyacentes a la cara identificada. El ID debe pertenecer al mismo cuerpo
y existir; de lo contrario la operación rechaza el dato. No hay fallback
silencioso a búsqueda global.

- Prisma: se usan las cuatro aristas del contorno de la cara plana,
  conservando los IDs estables y la distancia exacta a cada segmento.
- Cilindro: la cara lateral comparte ambas circunferencias; una tapa
  solo comparte su propia circunferencia, calculada analíticamente.
- La selección Android usa la cara del impacto analítico aceptado;
  cuando no se acepta ese refinamiento, usa la cara del triángulo visible.
  Se mantiene la tolerancia actual de pantalla y el resaltado OpenGL.
- `nearestEdge` continúa disponible para los consumidores que
  explícitamente necesiten buscar entre todas las aristas.

## Calidad y límites

`AnalyticEdgePickerTest` cubre prismas/cilindros con rotaciones XYZ,
aristas de la cara correcta, exclusión de bordes de la cara opuesta,
IDs ajenos e inexistentes, y compatibilidad de la búsqueda anterior.
La CI verifica Java puro, tests Android y Lint; no genera APK.

Esto **no** realiza raycasting directo de aristas ni prueba por sí solo
la visibilidad de cada punto de borde. La malla visible sigue identificando
el cuerpo y la cara antes de clasificar aristas. La experiencia táctil
real deberá probarse en Nothing Phone 2a cuando se publique un hito 0.6.0.
