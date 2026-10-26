# MyCAD 0.6.0 — Manipulador 3D anclado al impacto analítico

## Problema resuelto

Antes, el arrastre de una cara cilíndrica lateral situaba el punto amarillo
siempre en el eje local **+X**, aunque se hubiese tocado otra zona de la
superficie. En piezas rotadas, el control podía quedar detrás del sólido.

Ahora `AnalyticPrimitiveRayPicker.Hit` devuelve, además del ID estable de
la cara, distancia y posición XYZ mundial, la **normal exterior unitaria
en el punto de impacto**. En la cara lateral del cilindro la normal se
calcula a partir del ángulo radial real, no de la malla triangulada.

`PrimitiveFaceHandle.fromHit(body, hit)` comprueba que el impacto pertenece
al mismo cuerpo y a una cara aún existente, y que su posición sigue sobre
la superficie geométrica actual. Para prismas valida el plano y los límites
de la cara; para cilindros, radio y altura de la lateral o disco de la tapa.
Los controles de cara se colocan en el punto realmente tocado.

## Integración Android

La selección en el visor GPU continúa respetando la visibilidad: primero
`CadRayPicker` identifica el triángulo/cuerpo, luego se acepta el impacto
analítico solo si concuerda con el hit de la malla. El editor conserva el
último impacto **analítico aceptado** para anclar el control de arrastre
al activar «Arrastrar cara directamente en 3D».

Si el punto ya no pertenece a la geometría actual o no coincide con la
cara seleccionada, se utiliza el centro de cara anterior como alternativa
segura. Las propuestas de arrastre siguen siendo inmutables; **Cancelar**
no altera el sólido y **Aplicar** registra un único checkpoint de deshacer.

## Pruebas y límites

`AnalyticPrimitiveRayPickerTest` verifica normales de prismas y cilindros
girados, copias defensivas y rechazo de rayos casi tangentes exteriores.
`PrimitiveFaceHandleTest` verifica anclajes descentrados, normal radial
XYZ, tapas y rechazo de selecciones obsoletas después de cambiar medidas.
La CI ejecuta estas pruebas Java, integridad, tests Android y Lint sin APK.

Este cambio no crea un kernel B-Rep general, no mejora la edición de
mallas STL/OBJ ni garantiza todavía la ergonomía táctil. Debe validarse
visualmente en el Nothing Phone 2a en un futuro hito 0.6.0.
