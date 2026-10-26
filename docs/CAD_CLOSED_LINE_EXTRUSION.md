# MyCAD 0.6.0 — Extrusión de contornos cerrados de líneas

## Flujo de uso

1. Crear un conjunto de líneas rectas que forme un contorno cerrado
   simple en el mismo plano XY, XZ o YZ.
2. Seleccionar una de las líneas y abrir **Boceto táctil →
   Extruir contorno cerrado de líneas (malla)**.
3. Introducir una distancia de extrusión **positiva o negativa** en milímetros. La positiva sale por la normal del plano y la negativa en el sentido opuesto. El resultado se
   añade como malla 3D a la escena y al guardado nativo; se puede
   exportar a STL/OBJ y deshacer con el historial habitual.

El motor busca solamente las líneas de la **componente conectada**
a la línea seleccionada; otros contornos desconectados no se modifican.

## Geometría y comprobaciones

- Topología esperada: un único ciclo, exactamente dos segmentos por
  vértice y al menos tres aristas.
- Coincidencia de extremos a distancia máxima 0,15 mm. Si varios
  extremos presentan un emparejamiento ambiguo se rechaza.
- Rechazo de perfiles abiertos, bifurcaciones, cruces, contacto consigo
  mismo o área nula.
- Si hay líneas ajenas dentro del contorno o atravesando su perímetro,
  se rechaza la operación para evitar llenar silenciosamente agujeros.
- Tras aceptar se abre el visor GPU 3D y se muestra el resultado.
- Triangulación de polígonos convexos y cóncavos sin agujeros.
- Caras laterales, superior e inferior con normales orientadas; la
  transformación del plano XZ corrige el cambio de orientación.
- La geometría resultante pasa por el comprobador de mallas cerradas
  orientadas. Para distancias negativas se invierte la orientación de los
  triángulos, manteniendo volumen positivo y normales exteriores.
  Máximo 200 segmentos y **distancia absoluta** de 10000 mm.

## Limitaciones

**No es todavía una extrusión paramétrica tipo Shapr3D.** El resultado
es una malla triangulada conservada en el JSON del proyecto; no se
regenera automáticamente cuando cambian las líneas y no ofrece caras
y aristas B-Rep editables. Tampoco incluye agujeros internos, arcos,
splines, redondeos, perfiles con ramas ni operaciones booleanas
generales sobre ese cuerpo.

Las pruebas CI verifican la geometría Java, compilación Android y
Lint. La ergonomía táctil, precisión de extremos y exportación real
a STL/OBJ deben comprobarse en Nothing Phone 2a antes de considerar
estable este bloque.
