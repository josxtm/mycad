# MyCAD 0.6.0 — Triángulos degenerados sin umbral de tamaño arbitrario

## Problema

El validador `TriangleMeshHealth.inspect` consideraba degenerado cualquier triángulo cuyo vector normal cumpliera `nx² + ny² + nz² < 10⁻¹⁶`. Como ese valor tiene unidades físicas de longitud a la cuarta potencia, rechazaba triángulos geométricamente válidos cuando el sólido era muy pequeño o delgado. Un tetraedro de 0,00001 mm de arista puede tener un volumen real no nulo y, aun así, sufrir esta clasificación.

## Corrección

Se comprueban vértices coincidentes y que **los tres componentes del producto vectorial sean exactamente cero** después de convertir las coordenadas `float` a `double` para calcular las diferencias. Se evitan umbrales globales que dependen de la escala del diseño.

Se mantienen independientes las comprobaciones de topología, orientación de aristas, abanicos de vértices, volumen por componente y rechazo de una componente cerrada de volumen exactamente cero. No se modifican las coordenadas ni la triangulación, ni se usa una tolerancia capaz de borrar detalles pequeños legítimos.

## Verificación

`TriangleMeshHealthTest`, ejecutado por el job geométrico de GitHub Actions, incluye tetraedros cerrados de arista 0,001 mm, 0,00001 mm y 0,000001 mm (1 nanómetro), un tetraedro con elevada relación de aspecto y un triángulo verdaderamente colineal a escala de 0,0000001 mm. La compilación Java, pruebas Android y Lint quedan sujetas al flujo habitual verde. Sin generar APK.

**Límites:** esta mejora no recupera precisión perdida al almacenar coordenadas como `float` ni demuestra ausencia de autointersecciones; tampoco implica precisión mecánica fabricable a escala nanométrica.
