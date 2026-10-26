# MyCAD 0.6.0 — Vínculo paramétrico de contornos cerrados

## Lo que funciona en esta fase

Cada extrusión nueva de un circuito de líneas XY/XZ/YZ conserva un
identificador único de operación, el UUID de cada segmento realmente
usado, el UUID de la línea que inició la extrusión y su distancia
con signo. El resultado sigue siendo una malla triangular cerrada.

- **Resultados booleanos → Editar distancia de contorno:** regenera la
  misma operación con una nueva altura positiva o negativa.
- **Visor GPU 3D → tocar dos veces una malla de contorno:** el primer
  toque selecciona la malla y marca el triángulo; el segundo abre las
  acciones de editar distancia, **editar perfil original**, aislar la
  malla, exportarla a STL/OBJ o consultar el gestor.
  Las mallas importadas y otras mallas guardadas se identifican sin
  confundirlas con prismas y cilindros.
- **Editar perfil original:** comprueba primero que todas las líneas
  origen sigan formando una extrusión válida, localiza su UUID estable,
  vuelve al lienzo 2D del plano correspondiente y abre la edición de
  coordenadas. Al aplicar, se validan y regeneran los cuerpos dependientes
  de manera atómica; cancelar no cambia el diseño.
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

## Primera etapa del núcleo B-Rep poligonal

- Cada contorno cerrado de líneas, incluidos perfiles cóncavos, puede
  reconstruirse como `PolygonPrismBRep` con **2N vértices, 3N aristas
  compartidas, N+2 caras planas y 6N semiaristas dirigidas**, para N
  segmentos de origen. La malla STL sigue siendo únicamente la vista.
- Las semiaristas enlazan cara, arista, vértice de origen y destino,
  gemela, anterior y siguiente. Se validan las parejas de orientación
  opuesta y la característica de Euler V−E+F=2.
- Los IDs de las caras laterales derivan del UUID del segmento fuente,
  y las aristas y vértices de sus vínculos, de modo que conservar el
  contorno conserva identidades aun tras cambiar coordenadas/altura.
- Se admite extrusión hacia ambos lados en XY/XZ/YZ. La pantalla de
  resultados y la selección GPU permiten **Inspeccionar topología B-Rep**.
- **Elegir cara B-Rep desde el visor 3D:** cada triángulo tocado se
  relaciona con su cara analítica (identidad persistente de cara superior,
  inferior o lateral vinculada a la línea de origen). El segundo toque
  permite editar esa cara: en una tapa se modifica la distancia de
  extrusión firmada; en una lateral se abre **la línea original concreta**
  del boceto 2D para cambiar sus coordenadas. La validación de la
  extrusión y sus dependencias permanece atómica.
- La edición de cara lateral todavía no desplaza automáticamente la
  superficie entera manteniendo las restricciones del perfil; la
  interfaz abre el segmento fuente. No se anuncia fillet/booleanas
  generales o manipulación B-Rep arbitraria.
- Crear un contorno ahora verifica su nuevo sólido B-Rep en memoria
  antes de guardar la operación. El proyecto mantiene el formato
  JSON v1 y las mallas compatibles con versiones anteriores.

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
