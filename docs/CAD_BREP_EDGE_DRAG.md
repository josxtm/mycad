# MyCAD 0.6.0 — Manipulación táctil sobre aristas

Con el visor CAD experimental activado, toca un borde de prisma/cilindro, o selecciónalo desde el inspector B-Rep. Abre **Proyecto → Arrastrar arista directamente en 3D** y elige una de las **dos caras incidentes**. MyCAD sitúa un asa amarilla sobre el borde y la orienta según la normal de la cara elegida; al arrastrarla muestra la nueva geometría provisional.

`PrimitiveEdgeHandle` calcula la posición nominal del control (punto medio del segmento lineal o punto de la circunferencia) y el vector normal mundial de la cara adyacente. Las transformaciones XYZ se respetan.

Solo se desplaza **una dirección por gesto**, no ambas a la vez. Para la otra dirección se repite la operación. Durante el movimiento no se tocan los datos de proyecto; al levantar se presenta **Aplicar / Cancelar**. La confirmación revalida la selección y geometría, registra un único `checkpoint` y guarda. Los cuerpos vinculados a bocetos no admiten la operación.

CI sin APK: `PrimitiveEdgeHandleTest` valida el anclaje y normales en prismas y cilindros rotados, ID persistente, geometría propuesta, copias defensivas y errores de selección. Comprobación adicional del enlace entre editor, preview y confirmación.

**Limitaciones:** este arrastre se limita a desplazar las superficies del sólido primitivo a ambos lados del borde, sin chaflán, filete ni edición arbitraria de la topología. La experiencia multitáctil y sensibilidad requieren prueba real en Nothing Phone 2a.

## Asa situada en el punto real de la arista (26/10/2026)

La selección táctil de un borde ahora conserva su punto analítico más
cercano y una firma de geometría (tipo, dimensiones y pose XYZ). El
manipulador se ancla a **ese punto**, no al centro fijo del segmento ni
al ángulo cero de la circunferencia. Para la cara lateral de cilindros
girados, la normal del asa sigue el ángulo radial tocado.

`PrimitiveEdgeHandle.fromHit` rechaza puntos de otra pieza o calculados
antes de modificar cualquier dimensión, posición o rotación. Una pieza
reconstruida de forma idéntica sí puede reutilizar el impacto. El
inspector por lista mantiene el anclaje nominal porque no hay punto
táctil. Al deshacer, abrir otro proyecto, cambiar de visor o aceptar
una edición se descarta el impacto anterior.

Las regresiones Java prueban anclajes descentrados, normales XYZ en
cilindros, rechazo de impactos obsoletos y copia defensiva; el
verificador CI comprueba la conexión Android. **No hay APK nueva ni
validación táctil en Nothing Phone 2a.**
