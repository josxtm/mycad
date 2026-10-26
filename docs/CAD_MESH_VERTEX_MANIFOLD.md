# MyCAD 0.6.0 — Validación de vértices no manifold en mallas 3D

## Problema corregido

Una malla formada por **dos sólidos cerrados que comparten únicamente un vértice**
tiene todas sus aristas incidentes a dos triángulos con orientación opuesta.
El validador anterior comprobaba aristas, pero podía clasificar erróneamente
ese contacto puntual como una superficie 2-manifold cerrada. Este caso es
peligroso al usar una malla como resultado de cortes o extrusiones.

## Algoritmo

`TriangleMeshHealth.inspect` mantiene la comprobación de aristas y añade
una comprobación de **abanicos de caras conectados alrededor de cada vértice**:

1. Cada esquina de triángulo recibe un identificador de conjunto disjunto.
2. Dos esquinas del mismo vértice se conectan solo cuando sus triángulos
   comparten una arista completa (no por tocarse en un punto).
3. Para cada vértice geométrico se comparan las raíces de los conjuntos
   de sus aristas incidentes. Si hay más de una, se registra un vértice
   no manifold.
4. `Report.nonManifoldVertices` expone el recuento y `isWatertight()`
   lo exige igual a cero.

Las coordenadas se comparan **exactamente como float**, con normalización
de `-0.0`, siguiendo la política previa de coincidencia de aristas.
No se realiza soldadura por tolerancia; mallas con pequeñas grietas siguen
siendo rechazadas. El algoritmo no convierte mallas trianguladas en B-Rep.

## Regresión automática

`TriangleMeshHealthTest` añade:
- Dos tetraedros cerrados, cada uno válido.
- Los mismos tetraedros unidos por un único vértice: cero defectos de
  aristas pero **un vértice no manifold**, por lo que se rechazan.
- Dos tetraedros cerrados y espacialmente separados: siguen siendo
  válidos (varios componentes cerrados son permitidos).

El test se ejecuta en `verify` mediante el paso de geometría Java puro,
seguido de Gradle/Lint Android. **No se genera APK** por este cambio.

## Límites

El criterio detecta conectividad local de caras; no comprueba
autointersecciones de triángulos no adyacentes, volumen positivo,
orientación exterior absoluta ni tolerancias de reparación de STL.
La validación en Nothing Phone 2a queda pendiente de un hito de APK.
