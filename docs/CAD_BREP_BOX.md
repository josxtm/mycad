# MyCAD 0.6.0 · Primera capa B-Rep para prismas

Se añade `BoxBRep.java` para describir un sólido rectangular mediante 8 vértices, 12 aristas compartidas y 6 caras planas orientadas. Cada entidad mantiene nombres lógicos estables basados en el UUID del cuerpo, compatibles con `PrimitiveEdges` y `BoxFaceSelection`.

## Calidad y pruebas

La construcción verifica dimensiones y coordenadas finitas, caras no degeneradas, orientación exterior, aristas compartidas por exactamente dos caras con orientaciones opuestas, característica de Euler V-E+F=2 y volumen firmado igual al volumen geométrico. El cálculo de la malla de visualización queda separado de la representación analítica.

`BoxBRepTest` cubre orientación XYZ, conectividad, áreas, volumen, cambio dimensional sin pérdida de identificadores, rechazo de geometría degenerada e inmutabilidad de la interfaz. La CI lo ejecuta en Java puro, sin generar APK.

## Límites

Es una **base B-Rep real pero exclusivamente de prismas**. No hay kernel B-Rep general, caras curvas, operaciones booleanas B-Rep, fillet, chamfer, shell, ni historial estable cuando una operación divide o fusiona caras. Falta conectar esta estructura analítica al editor de Android y comprobarla en el Nothing Phone 2a.

Siguiente bloque: superficies analíticas cilíndricas y primer comando de edición que use la conectividad de caras/aristas.
