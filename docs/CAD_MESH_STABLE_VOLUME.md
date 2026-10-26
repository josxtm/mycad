# MyCAD 0.6.0 — volumen de mallas estable con coordenadas lejanas

## Problema
`TriangleMeshHealth.inspect` calcula el volumen orientado de la sopa de triángulos para diagnosticar sólidos cerrados. El determinante de vértices expresados en coordenadas absolutas produce cancelación numérica cuando una pieza pequeña está situada a millones de milímetros del origen.

## Cambio
- Se traslada **solo el origen del cálculo de volumen** al primer vértice de la malla. No se modifican posiciones, triángulos, orientación ni IDs.
- Se suman los volúmenes tetraédricos con compensación de Neumaier para preservar contribuciones pequeñas.
- `Report.signedVolume` mantiene su contrato: positivo o negativo según orientación, en mm³. `isWatertight()` conserva sus comprobaciones topológicas existentes.
- El caso de malla vacía continúa informando volumen cero.

## Regresión
`TriangleMeshHealthTest` valida el volumen 1/6 mm³ de un tetraedro en el origen y trasladado a (+10 000 000,+10 000 000,+10 000 000) mm y a la posición opuesta, con tolerancia absoluta 1e-12. Comprueba además que invertir todas las caras invierte el signo del volumen sin alterar la condición de malla cerrada. Se ejecuta en el job `verify` junto con los tests de geometría y Android/Lint, sin generar APK.

## Límites
La traslación usa un único origen para toda la malla; componentes desconectados separados por distancias extremas aún pueden perder precisión. El volumen orientado no demuestra por sí solo que no haya autointersecciones, ni reemplaza un núcleo B-Rep o un validador de sólidos arbitrarios. Los vértices son `float`, por lo que detalles menores que su resolución ya se pierden antes del cálculo. No se ha validado este cambio en Nothing Phone 2a.
