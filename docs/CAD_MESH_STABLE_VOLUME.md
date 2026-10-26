# MyCAD 0.6.0 — volumen de mallas estable con coordenadas lejanas

## Problema
`TriangleMeshHealth.inspect` calcula el volumen orientado de la sopa de triángulos para diagnosticar sólidos cerrados. El determinante de vértices expresados en coordenadas absolutas produce cancelación numérica cuando una pieza pequeña está situada a millones de milímetros del origen.

## Cambio
- Se traslada **solo el origen del cálculo de volumen** al primer vértice de **cada componente de caras conectadas por aristas**. No se modifican posiciones, triángulos, orientación ni IDs.
- Se suman los volúmenes tetraédricos con compensación de Neumaier dentro de cada componente y se combinan los totales con otra suma compensada. Dos sólidos separados, aunque estén a millones de milímetros, conservan su volumen independiente.
- `Report.signedVolume` mantiene su contrato: positivo o negativo según orientación, en mm³. `isWatertight()` conserva sus comprobaciones topológicas existentes.
- El caso de malla vacía continúa informando volumen cero.

## Regresión
`TriangleMeshHealthTest` valida el volumen 1/6 mm³ de un tetraedro en el origen y trasladado a (+10 000 000,+10 000 000,+10 000 000) mm y a la posición opuesta, con tolerancia absoluta 1e-12. Comprueba además tres tetraedros cerrados separados entre sí por hasta 20 millones de mm (volumen total 1/2 mm³), uno de ellos invertido (volumen total 1/6 mm³), y que invertir todas las caras invierte el signo del volumen sin alterar la condición de malla cerrada. Se ejecuta en el job `verify` junto con los tests de geometría y Android/Lint, sin generar APK.

## Límites
El origen se calcula por componente conexa de caras; no se garantiza precisión absoluta si una misma componente se extiende a distancias extremas. El volumen orientado no demuestra por sí solo que no haya autointersecciones, ni reemplaza un núcleo B-Rep o un validador de sólidos arbitrarios. Los vértices son `float`, por lo que detalles menores que su resolución ya se pierden antes del cálculo. No se ha validado este cambio en Nothing Phone 2a.
