# MyCAD 0.6.0 — Aceleración de picking para mallas grandes

**Problema:** el visor GPU recorría todos los triángulos de cada pieza cuando se tocaba la pantalla, incluso los pertenecientes a regiones muy alejadas del rayo. El giro de cámara ya tenía buffers optimizados, pero la selección podía provocar pausas en proyectos con triangulación elevada.

**Solución:** `MeshRayIndex` es un índice espacial ligero que divide los triángulos de una malla en grupos de 64, calculando la caja AABB de cada grupo una sola vez. En el toque:
1. El rayo mundial se normaliza y se compara con cada caja mediante slabs conservadores.
2. Si no atraviesa una caja, se omiten sus triángulos.
3. Si la atraviesa, `CadRayPicker.pickRange()` hace el mismo cálculo geométrico Möller-Trumbore del visor original, con el índice **global** del triángulo en el array.
4. Se elige el primer impacto visible más cercano, conservando el desempate original. La identidad de partes B-Rep no cambia.

El índice se crea de forma perezosa en el hilo del visor y se asocia a la malla completa. Al recalcular o previsualizar, MyCAD sustituye el objeto `Mesh`, de modo que los índices temporales se descartan junto a la malla vieja. Los archivos JSON y STL/OBJ no se alteran. Los triángulos no finitos se ignoran igual que en el selector anterior.

Pruebas `MeshRayIndexTest`: comparación entre selector antiguo y acelerado, muchos bloques, distintas direcciones y origen, bordes y vértices, mallas vacías y dañadas, prisma XYZ rotado y cilindro rotado, IDs de triángulos, rayos inválidos. El script CI verifica la conexión real de Android. Sin APK.

**Limitaciones:** no es un BVH recursivo; si las cajas de los bloques se solapan mucho, seguirá revisando muchos triángulos. Se mantiene el comportamiento geométrico ya existente. Se recomienda medir latencia de toque en Nothing Phone 2a durante el próximo hito de pruebas.

## Incremento: orden de proximidad de los bloques (0.6.0)

El raycast ordena las cajas candidatas por distancia de entrada conservadora al rayo y visita primero las cercanas. Cuando ya existe un impacto y la siguiente caja comienza más lejos que ese impacto, detiene el recorrido exacto. Se conserva el desempate por índice global original cuando dos triángulos están a la misma profundidad. Esto evita recorrer caras lejanas en escenas donde los bloques están almacenados en orden inverso a la vista, sin alterar el resultado de selección.

Se añaden regresiones de empate exacto entre bloques y 300 rayos pseudoaleatorios deterministas comparados con el selector exhaustivo. La ordenación ocurre solo al tocar, no en cada fotograma GPU. Sin APK: el cambio requiere primero `verify` verde en PR y main.
