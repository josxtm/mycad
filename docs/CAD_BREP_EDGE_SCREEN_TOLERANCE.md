# MyCAD 0.6.0 — Selección de aristas sensible al zoom

El primer selector de aristas empleaba una tolerancia absoluta de milímetros. Ahora `EdgeScreenTolerance` calcula un radio de selección aproximado a partir de la distancia de cámara, la altura del visor, una perspectiva de 45°, y la densidad de pantalla Android. El objetivo es mantener el tacto útil al hacer zoom sin confundir aristas lejanas.

`MeshEngine.cameraDistance()` publica únicamente la distancia de vista (lectura atómica de un `float volatile`). En cada toque sobre una superficie se calcula:

`mm/px = 2 * distanciaCamara * tan(22,5°) / altoPixel`

Se usan 12 dp como objetivo aproximado, con mínimo y máximo proporcionados a la menor dimensión del sólido y un tope máximo de 8 mm. La geometría de `AnalyticEdgePicker` no cambia, solo la aceptación del impacto en torno a una arista.

CI: `EdgeScreenToleranceTest` cubre cambios de zoom, densidad, resolución, límites de piezas pequeñas y entradas inválidas. Sin APK. La sensación real del toque debe validarse y ajustarse en Nothing Phone 2a.

**Limitaciones:** tolerancia geométrica aproximada al plano de interés, no una proyección exacta por píxel de cada curva. No sustituye el picking por rayo, ni corrige el ocultamiento de geometría compleja.
