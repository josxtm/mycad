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
  extrusión firmada; en una lateral puede **desplazarse la cara completa
  en la normal de su perfil** o abrir la línea fuente en 2D.
- **Desplazamiento de cara lateral B-Rep:** se trasladan los dos vértices
  de la línea fuente y se conectan con los extremos de sus dos líneas
  vecinas. El motor conserva los UUIDs y comprueba antes de aplicar
  que el sólido siga cerrado y orientado. Si otras extrusiones utilizan
  alguna línea movida, todas ellas se regeneran antes del commit;
  el cambio completo es deshacible en una sola operación.
- Limitación: solo caras laterales rectas de prismas poligonales simples;
  no realiza operaciones de desplazamiento sobre superficies curvas,
  restricciones generales, agujeros, fillets ni booleanas B-Rep.
- Crear un contorno ahora verifica su nuevo sólido B-Rep en memoria
  antes de guardar la operación. El proyecto mantiene el formato
  JSON v1 y las mallas compatibles con versiones anteriores.

## Primera operación booleana entre sólidos B-Rep

- En **Resultados booleanos** o al pulsar dos veces una malla vinculada:
  `Intersectar con otro contorno B-Rep`. Permite seleccionar un segundo
  perfil cerrado extruido y calcula **la intersección volumétrica real** de
  dos prismas con contornos convexos, incluidos triángulos y polígonos
  oblicuos. Ambos deben estar en el mismo plano XY, XZ o YZ, extruidos
  desde el origen hacia el mismo lado. La altura final es la menor de
  ambas alturas, conservando el signo.
- El núcleo ejecuta recorte de polígonos mediante semiplanos convexos,
  simplifica vértices collineales, verifica el volumen y genera un
  **nuevo sólido B-Rep analítico** con caras, aristas y semiaristas.
- **Intersección paramétrica vinculada:** la app guarda la malla resultado
  y una receta UUID que referencia sus dos extrusiones originales. Si se
  modifican sus alturas, las coordenadas de líneas fuente o la posición
  de una cara lateral, primero se regeneran **todas** las intersecciones
  afectadas en memoria y solo después se guarda la transacción completa.
  Un resultado no válido cancela la edición sin cambiar ninguna malla.
- **Guardar, abrir, deshacer/rehacer** incluyen esas recetas dentro del JSON
  existente v1, con validación de todos los UUID y comparación exacta de la
  malla reconstruida frente a la persistida antes de cargar. Documentos
  antiguos siguen funcionando como mallas independientes.
- Al eliminar una fuente, se permite conservar la malla derivada como
  instantánea desligada o eliminarla en cascada. Eliminar manualmente una
  fuente desliga automáticamente las intersecciones que dependían de ella.
- No es todavía un cuerpo Boolean B-Rep con selección de sus caras de
  resultado: se conserva una malla triangulada enlazada a sus dos fuentes.
- Se rechazan explícitamente perfiles cóncavos, sólidos de distintos
  planos, extrusiones opuestas, volúmenes sin solape y polígonos
  degenerados. Todavía no hay unión/diferencia B-Rep generales, huecos
  interiores ni varias regiones desconectadas.

## Unión B-Rep de dos prismas rectangulares

- **Unir dos prismas B-Rep rectangulares**, disponible desde el visor 3D
  o Resultados. Requiere dos perfiles rectangulares cerrados con
  **mismo plano y la misma altura firmada**, solapados con volumen.
- La unión se calcula mediante descomposición de una cuadrícula
  ortogonal, reconocimiento de la frontera exterior, eliminación de
  vértices collineales y reconstrucción de **una topología B-Rep nueva**.
  Los resultados pueden ser cóncavos, en forma de **L** o cruz, y se
  comprueban sus normales, volumen y malla cerrada.
- La unión conserva **una receta paramétrica** con UUID de sus dos
  extrusiones originales. Al editar un perfil, desplazar una cara
  lateral o cambiar la altura de una de ellas, el motor intenta
  regenerar todas las uniones afectadas **antes de confirmar** el cambio.
  Si la modificación convierte el perfil en oblicuo, pierde el solape
  o genera distintas alturas, se rechaza sin alterar el proyecto.
- Su malla y la receta se conservan en el JSON v1, junto al historial
  de deshacer y rehacer. Los proyectos antiguos sin campo de operación
  mantienen la semántica de **INTERSECTION** por defecto; las nuevas
  recetas utilizan **UNION_ORTHOGONAL**.
- Igual que en la intersección vinculada, la topología B-Rep resultante
  se regenera en memoria, pero lo que se persiste y se muestra sigue
  siendo una malla asociada a una operación, no un cuerpo B-Rep libre
  editable directamente por caras.
- Se rechazan contornos oblicuos o con múltiples regiones, ausencia de
  solape volumétrico y alturas distintas, incluso si los dos sólidos
  tienen la misma dirección de extrusión.
- Corrección de integridad: crear una intersección paramétrica existente
  agrega ahora exactamente **una** receta al listado de booleanas,
  evitando duplicar su entrada y desalinear el proyecto JSON.

## Resta B-Rep ortogonal con historial paramétrico

- Desde un contorno vinculado en el visor o en Resultados, seleccionar
  **Restar otro prisma B-Rep rectangular**. El primer sólido es la pieza;
  el segundo, el cortador. Ambos deben estar extruidos en el mismo plano
  y con igual altura firmada.
- Se calcula una verdadera operación de diferencia en la sección
  ortogonal, reconstruyendo un sólido B-Rep con caras, aristas y semiaristas.
  Admite cortes de esquina **en L**, muescas laterales y cortes de banda
  que dejan un único sólido conexo.
- Si la resta generaría un **agujero interno**, dos piezas desconectadas
  o cero volumen, se rechaza sin modificar el documento; estas
  topologías exigirán soporte de varios contornos/cascarones.
- Se guarda una receta `DIFFERENCE_ORTHOGONAL` con referencias a las dos
  extrusiones originales, siguiendo el historial de undo y guardado JSON v1.
  Los cambios de boceto, altura o cara lateral se comprueban y propagan
  antes del commit. Si invalidan la resta, se cancela toda la edición.
- Se mantienen los originales; la vista del resultado es una malla
  triangulada vinculada, no todavía una B-Rep general editable por caras.

## Booleanas con perfiles ortogonales cóncavos

- La nueva opción **Booleanas de perfiles ortogonales (cóncavos)** del
  visor 3D y Resultados permite elegir unión, resta o intersección y un
  segundo sólido. Admite perfiles cerrados de segmentos horizontales y
  verticales, aunque sean **L, U, cruces u otras formas cóncavas**.
- Ambos sólidos deben compartir plano de boceto XY/XZ/YZ y tener la misma
  altura firmada. La operación genera un nuevo **sólido B-Rep analítico**
  con caras y semiaristas, verifica la conservación de área y volumen, y
  expone una malla triangulada cerrada para el visor.
- Los resultados **no** pueden contener agujeros interiores ni múltiples
  componentes desconectados: si existen, se rechaza la operación con un
  error explícito antes del guardado. También rechaza perfiles oblicuos.
- Cada unión, resta o intersección mantiene una receta por UUID del
  origen (operaciones `UNION_POLYGONAL`, `DIFFERENCE_POLYGONAL` e
  `INTERSECTION_POLYGONAL`) que se regenera al modificar un boceto,
  altura o cara lateral. Deshacer, borrado y archivo JSON v1 preservan los
  vínculos. Las operaciones ortogonales rectangulares anteriores siguen
  cargando y regenerándose con su algoritmo previo.
- No se anuncia soporte de agujeros, operación Booleana general de 3D
  rotado/curvo ni selección de caras del resultado derivado.

## Primer soporte B-Rep para varias piezas separadas

- Nuevo modo **Booleanas B-Rep de varios sólidos** al seleccionar un
  contorno vinculado desde el visor o el gestor de resultados. Ejecuta
  unión, resta o intersección de perfiles ortogonales del mismo plano
  XY/XZ/YZ y misma altura firmada.
- La topología se representa con **uno o varios sólidos B-Rep analíticos**:
  cada componente tiene sus propias caras, aristas y semiaristas cerradas,
  sin recurrir a triángulos cosidos artificialmente. Se verifican
  independencia de componentes, Euler por cada sólido y volumen total.
- Una sola malla de resultado **agrupa las varias piezas** para verlas,
  exportarlas y conservarlas en JSON con deshacer. La receta UUID
  `UNION_MULTISHELL`, `DIFFERENCE_MULTISHELL` o
  `INTERSECTION_MULTISHELL` regenera todos los componentes al cambiar
  los bocetos y alturas compatibles de sus dos extrusiones fuente.
- Esta modalidad acepta una unión de perfiles disjuntos, y una resta
  que divide un sólido en dos cuerpos. Los formatos antiguos de recetas
  mantienen su comportamiento y sus mallas exactas.
- **Agujeros interiores no admitidos:** los ciclos de perímetro internos
  se detectan por orientación y se rechazan, al igual que puntos de
  contacto no-manifold y más de 16 componentes.
- La UI expone el conjunto como un resultado con varias carcasas; aún
  no permite editar individualmente las caras de las piezas derivadas.

## Primer B-Rep analítico con agujeros pasantes

- Nuevo modo **Booleanas B-Rep con agujeros pasantes** del visor y el
  gestor de resultados. Permite unión, resta e intersección de dos
  secciones poligonales ortogonales cerradas del mismo plano XY/XZ/YZ
  y de igual altura firmada. Admite vaciados interiores que atraviesan
  toda la extrusión, además de varios cuerpos separados.
- La nueva representación `OrthogonalGridHoleBRep` mantiene **vértices
  3D, caras planas (quads), aristas compartidas, dos semiaristas por
  arista, direcciones y relaciones de gemelas**, incluyendo las paredes
  interiores de los huecos. Descompone las tapas en celdas ortogonales
  que coinciden exactamente con las caras vecinas (sin T-junctions).
- Cada operación comprueba topología manifold mediante gemelas opuestas,
  característica de Euler `V-E+F=2·sólidos-2·agujeros`, orientación,
  volumen y cierre de malla. El resultado sigue siendo una malla
  triangulada exportable en el visor, derivada del B-Rep analítico.
- El nuevo historial `UNION_WITH_HOLES`, `DIFFERENCE_WITH_HOLES` e
  `INTERSECTION_WITH_HOLES` conserva los UUID de los dos bocetos
  extruidos: regeneración antes de confirmar cambios, deshacer/rehacer
  y carga JSON v1 con comparación exacta de la malla reconstruida.
- **Límites**: máximo 16 cuerpos y 16 agujeros pasantes, perfiles
  ortogonales y misma altura desde el origen. No se admiten superficies
  curvas, taladros ciegos de profundidad parcial, desplazamientos
  de planos base ni selección/editado de caras del resultado compuesto.
  Las operaciones anteriores siguen usando su topología y formatos sin
  cambios.

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
