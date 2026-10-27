# MyCAD 0.6.0 — Validación de interfaz en tablet Android de 12 pulgadas

Estado: plan de desarrollo y aceptación; **no afirma implementación ni pruebas físicas**.

## Objetivo
Mantener un único APK adaptable a teléfono y tablet, sin bifurcar el motor CAD.

## Criterios de aceptación
1. En orientación horizontal y vertical, lienzo CAD visible sin que barras, diálogos o teclado oculten el sólido seleccionado.
2. ViewCube y controles de órbita/zoom accesibles con dedo; comprobar que no interceptan el trazado 2D.
3. La selección de cara/arista mantiene una zona táctil expresada en dp y no cambia por la densidad de píxeles.
4. En modo Editar, el arrastre de una cara no rota accidentalmente la cámara.
5. El panel de herramientas y el árbol de cuerpos se adaptan al ancho disponible; no se superponen a Guardar/Abrir.
6. Con teclado visible, cotas y controles numéricos permanecen accesibles.
7. Guardar, cerrar y reabrir conserva bocetos, extrusiones, booleanas y encuadre según la política existente.
8. Repetir las pruebas en Nothing Phone 2a para detectar regresiones.

## Pruebas geométricas de referencia
- Extruir un rectángulo de 60 × 60 mm a 30 mm.
- Restar un prisma interior de 20 × 20 mm y comprobar agujero pasante.
- Orbitar, guardar/abrir, deshacer/rehacer y exportar STL/OBJ.
- Repetir con planos XY, XZ e YZ y extrusión negativa.

## Secuencia técnica
1. Auditar medidas fijas en píxeles, tamaños de controles y manejo de insets/teclado.
2. Introducir dimensiones responsivas sin alterar cálculos de geometría.
3. Añadir pruebas automatizadas para layout cuando sea posible.
4. Abrir PR, exigir verificación verde y solo entonces integrar.
5. Generar APK de hito y validar en tablet física.

No se conoce todavía la resolución, densidad ni versión de Android de la tablet; no presuponerlas.
