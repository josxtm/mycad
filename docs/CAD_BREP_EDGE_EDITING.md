# MyCAD 0.6.0 — Edición restringida de aristas por caras adyacentes

Este bloque permite modificar una arista primitiva seleccionada mediante **dos distancias numéricas independientes**: una para cada cara que comparte esa arista. La operación analítica `PrimitiveBRepBody.offsetEdge(id, firstMm, secondMm)` compone dos desplazamientos de cara sobre un **cuerpo propuesto inmutable**, valida ambas operaciones y entrega el sólido final solo si todo es geométricamente válido.

- Prisma rectangular: modifica las dos direcciones normales a la arista, preservando en cada dirección la cara opuesta y la orientación rígida XYZ.
- Cilindro: en cualquiera de sus dos bordes circulares, modifica el radio a través de la lateral y/o la altura desde la tapa correspondiente.
- Si ambos valores son cero, los valores no son finitos o las nuevas dimensiones colapsan, se rechaza el cambio sin mutar el cuerpo original.

## Uso en la interfaz Android (cuando publiquemos un APK)

1. Selecciona un prisma o cilindro independiente.
2. Abre **Proyecto → Inspeccionar sólido B-Rep (caras y aristas)** y toca la arista deseada; pulsa **Editar desplazamiento**. También puedes tocar cerca de la arista desde el visor CAD experimental y entrar en **Proyecto → Editar arista B-Rep seleccionada**.
3. Introduce los desplazamientos de sus dos caras. `0` conserva esa dirección. Valores positivos mueven la cara hacia fuera; negativos hacia dentro.
4. Confirma con Aplicar, comprueba volumen/dimensiones y usa Deshacer/Rehacer. Cancelar no registra cambios.
5. La edición de un cuerpo vinculado a un boceto se bloquea para evitar romper la relación paramétrica.

La pantalla comprueba la identidad de cuerpo y arista, el estado original de posición/cotas/rotaciones y las nuevas cotas `float` antes de registrar el cambio. Conserva el UUID y los campos de proyecto `.mycad.json`.

**Limitaciones:** esta operación no redondea ni bisela la arista y tampoco ofrece un controlador de arrastre táctil. Es un primer manipulador numérico restringido. No se edita B-Rep de mallas importadas o booleanas. Falta comprobar usabilidad en Nothing Phone 2a.

Pruebas: `tests/PrimitiveEdgeMoveTest.java` + comprobaciones de integración en `scripts/verify-project.sh`; sin APK habitual.
