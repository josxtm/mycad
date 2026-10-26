# MyCAD 0.6.0 — Malla única de escena para visor y exportación

**Problema corregido:** la función `currentMesh()` antigua utilizaba coordenadas locales simplificadas para la vista normal: el prisma podía ignorar parte de las rotaciones X/Y y el cilindro se dibujaba solo vertical en algunos flujos. El visor CAD experimental ya disponía de posiciones XYZ transformadas mediante `PrimitiveBRepBody`.

**Solución:** `currentMesh()` y la vista CAD experimental utilizan ahora **la misma fuente analítica de geometría**. El compositor `AnalyticSceneMesh` concatena, siempre en el mismo orden:
1. Mallas importadas y resultados booleanos existentes, sin cambiar su triangulación.
2. Cada prisma o cilindro, transformado mediante su B-Rep analítica y su ID persistente.
3. Mallas de features y cadenas de extrusión existentes.

La representación temporal de edición sustituye solo el sólido propuesto dentro de esta composición, sin alterar otros cuerpos ni el JSON. El nuevo ensamblador evita usar una lista de objetos `Float` para toda la malla visible, compone un único buffer de `float`, valida longitud múltiplo de triángulos (XYZ), coordenadas finitas e IDs únicos, y limita la memoria del buffer a 36 MB.

**Pruebas:** `AnalyticSceneMeshTest` cubre prismas/cilindros con rotaciones XYZ, orden estable de geometría importada y features, corrección de malla provisional, determinismo, copias defensivas y rechazo de entradas dañadas. La CI comprueba además que no se usa la implementación clásica simplificada.

**Alcance:** no convierte booleanas triangulares en B-Rep editables ni resuelve todavía filetes, chaflanes o superficies generales. No se genera APK hasta un hito funcional.
