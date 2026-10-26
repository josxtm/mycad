# MyCAD 0.6.0 — Resaltado GPU de aristas analíticas

Al tocar cerca de una arista en el visor CAD experimental, `AnalyticEdgePicker` identifica su ID lógico. A partir de ese ID, `PrimitiveEdgeCurve` construye una **línea en coordenadas mundiales** para pintar el contorno completo:

- Prismas: dos vértices del segmento B-Rep transformado con XYZ.
- Cilindros: una circunferencia evaluada matemáticamente y muestreada en 64 segmentos para el **dibujo**. Su topología sigue siendo un único borde circular, independiente de la tessellación.
- `MeshEngine.highlightEdge` dibuja la curva con `GL_LINE_STRIP` sobre la escena. La GPU conserva el modelo real: no se incorporan esos segmentos al sólido ni a las exportaciones STL/OBJ.
- Tocar fuera, cambiar a vista aislada, editar y repintar o cancelar una previsualización elimina el resaltado anterior.

Las pruebas `PrimitiveEdgeCurveTest` cubren líneas y circunferencias bajo rotación XYZ, cierre periódico exacto, actualización tras editar cotas, UUIDs y rechazo de IDs y calidades inválidos. CI sin APK.

**Limitaciones:** líneas anchas pueden depender del controlador OpenGL; algunos dispositivos renderizan anchura de 1 px. La curva se dibuja por encima de la profundidad de la malla para destacar la selección, no como geometría adicional. No hay todavía controladores 3D en los vértices para arrastrar el borde; sigue pendiente la validación visual en Nothing Phone 2a.
