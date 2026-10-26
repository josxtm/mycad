# MyCAD 0.6.0 — Selección de aristas sensible al zoom

El primer selector de aristas empleaba una tolerancia absoluta de milímetros. Ahora `EdgeScreenTolerance` calcula un radio de selección aproximado a partir de la distancia de cámara, la altura del visor, una perspectiva de 45°, y la densidad de pantalla Android. El objetivo es mantener el tacto útil al hacer zoom sin confundir aristas lejanas.

`MeshEngine.cameraDistance()` publica únicamente la distancia de vista (lectura atómica de un `float volatile`). En cada toque sobre una superficie se calcula:

`mm/px = 2 * distanciaCamara * tan(22,5°) / altoPixel`

Se usan 12 dp como objetivo aproximado, con mínimo y máximo proporcionados a la menor dimensión del sólido y un tope máximo de 8 mm. La geometría de `AnalyticEdgePicker` no cambia, solo la aceptación del impacto en torno a una arista.

CI: `EdgeScreenToleranceTest` cubre cambios de zoom, densidad, resolución, límites de piezas pequeñas y entradas inválidas. Sin APK. La sensación real del toque debe validarse y ajustarse en Nothing Phone 2a.

**Limitaciones:** tolerancia geométrica aproximada al plano de interés, no una proyección exacta por píxel de cada curva. No sustituye el picking por rayo, ni corrige el ocultamiento de geometría compleja.

## Incremento: distancia real al punto tocado (26/10/2026)

La distancia al **centro de la cámara** no es la distancia a todos los
sólidos de una escena: una pieza situada delante del foco proyecta una
arista más grande que otra detrás. En el visor GPU, el cálculo de la
tolerancia de 12 dp usa ahora la distancia desde el origen del rayo en el
plano cercano de OpenGL hasta el **impacto 3D concreto**. El cálculo
`EdgeScreenTolerance.computeAtHit` reutiliza los mismos límites de
seguridad geométrica que `compute`, sin alterar la B-Rep ni la selección
de caras.

Regresiones: piezas a diferentes profundidades, desplazamiento global,
rayos oblicuos, entradas no finitas y puntos coincidentes. `verify`
compila las pruebas Java y la integración Android; **sin APK**.

**Límite:** se usa longitud del rayo hasta el impacto como aproximación
al factor de perspectiva. En los extremos de la pantalla puede diferir
de la profundidad proyectada exacta; calibración táctil pendiente.
