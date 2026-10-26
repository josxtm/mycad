# MyCAD 0.6.0 — Vínculo paramétrico de contornos cerrados

## Lo que funciona en esta fase

Cada extrusión nueva de un circuito de líneas XY/XZ/YZ conserva un
identificador único de operación, el UUID de cada segmento realmente
usado, el UUID de la línea que inició la extrusión y su distancia
con signo. El resultado sigue siendo una malla triangular cerrada.

- **Resultados booleanos → Editar distancia de contorno:** regenera la
  misma operación con una nueva altura positiva o negativa.
- **Editar boceto → cambiar coordenadas o acotar longitud:** reconstruye
  primero en memoria *todos* los resultados de contornos que dependen
  de la línea. Si un circuito deja de ser válido, el cambio no se guarda;
  ni las líneas, ni las mallas ni el historial se modifican.
- **Eliminar boceto:** opción de mantener su malla como copia
  independiente o eliminar las mallas que dependen de la línea.
- **Guardar, abrir y deshacer/rehacer:** las recetas viajan con el JSON
  del proyecto sin alterar la versión general del formato; documentos
  antiguos sin recetas se importan como mallas independientes.
- La carga rechaza UUID duplicados, referencias huérfanas o una malla
  que no coincida con su receta reconstruida.

## Limitaciones importantes

Las operaciones vinculadas son **solo para extrusiones generadas a
partir de líneas del boceto** después de este cambio. No se recuperan
vínculos retroactivamente desde mallas antiguas. No hay todavía
topología B-Rep general, edición de caras/aristas de estos resultados,
agujeros, cotas restringidas, arcos ni regeneración de booleanas
arbitrarias sobre estas mallas.

El test Java LoopExtrusionFeatureTest comprueba identidad y vínculo
por UUID, alturas firmadas, regeneración tras modificar un boceto y
rechazo de origen roto. GitHub Actions valida también compilación
Android, tests unitarios y Lint, **sin ensamblar APK por cada commit**.
El funcionamiento táctil debe probarse en Nothing Phone 2a.
