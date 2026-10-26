# Índice jerárquico de rayos sobre mallas (0.6.0)

## Alcance
La selección de triángulos de mallas grandes (STL/OBJ y vistas CAD trianguladas) utiliza un árbol binario de cotas AABB construido una vez por malla inmutable. Las hojas conservan los bloques de 64 triángulos y sus índices globales originales.

El árbol se divide por la mediana espacial del eje con mayor dispersión de centros de bloque. En cada toque se recorren primero los nodos con menor distancia de entrada al rayo; tras el primer impacto, los nodos cuyo AABB comienza más lejos que el impacto se descartan completos. Se conserva el margen numérico conservador en las cotas y el desempate por índice global de triángulo.

## Garantías y límites
- La geometría y los identificadores de triángulo no se reordenan; solo se ordenan referencias de bloques al construir el árbol.
- Los triángulos dañados con valores no finitos se excluyen de las cotas, como antes.
- El índice se reconstruye cuando cambia la malla; no es válido modificar su array de coordenadas después de construirlo.
- No sustituye el selector analítico B-Rep ni ofrece selección de aristas o booleanas exactas.
- La mejora estructural reduce el número de bloques inspeccionados en escenas dispersas; no se promete una aceleración concreta sin medición en dispositivo.

## Regresiones
`tests/MeshRayIndexTest.java` compara con `CadRayPicker.pick` para rayos de acierto/fallo, direcciones invertidas, planos paralelos, empates exactos y 250 rayos pseudoaleatorios sobre 96 bloques espacialmente invertidos. Se ejecuta mediante `scripts/verify-project.sh` y el job `verify` de GitHub Actions. No se genera APK en este incremento.
