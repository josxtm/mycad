# Selección de triángulos pequeños: tolerancia relativa (MyCAD 0.6.0)

## Problema

`CadRayPicker` descartaba intersecciones si el determinante de
Möller–Trumbore era menor que `1e-12` en valor absoluto. Como el
determinante escala con el **área** del triángulo, una cara de
aproximadamente 1 µm × 1 µm (en unidades internas mm) podía resultar
inseleccionable aun siendo una geometría válida.

## Cambio

El rechazo de rayos paralelos usa ahora una tolerancia relativa:
`abs(det) <= 1e-14 * ||edge1|| * ||edge2||`.
Se rechazan aristas de longitud nula y valores no finitos. Esta
comparación no introduce un área mínima absoluta y mantiene el filtro
de direcciones casi paralelas.

No se cambian los IDs de triángulo, el desempate por distancia,
la transformación de cámara ni los datos de la malla.

## Regresiones

- Impacto y fallo de impacto en triángulo de 1 µm.
- Distancia física del impacto y dirección del rayo escalada.
- Concordancia entre raycast lineal y árbol BVH de 65 triángulos,
  con la cara micrométrica en el último bloque.

La comprobación automática ejecuta las pruebas Java del motor y
la compilación/Lint Android. **No se genera APK** en este incremento.

## Límites

El motor sigue usando coordenadas `float` para las mallas, por lo que
los detalles muy pequeños lejos del origen pueden perderse antes del
raycast. No se ha validado la precisión táctil real ni la representación
visual en Nothing Phone 2a. Este ajuste no constituye un kernel B-Rep
de precisión arbitraria.
