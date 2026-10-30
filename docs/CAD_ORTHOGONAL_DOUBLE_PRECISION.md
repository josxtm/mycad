# Precisión de booleanas rectangulares

La ruta `OrthogonalPrismBRepUnion` reconstruía la unión y la diferencia
convirtiendo a `float` tanto los segmentos como la semilla de UUID. Una edición
de `1e-7 mm` podía perderse y reutilizar la identidad del resultado anterior.
En perfiles de 0,8 mm situados en ±9000 mm, el redondeo también alteraba el área
y podía hacer que una operación válida fallase la comprobación de volumen.

La reconstrucción y la semilla conservan ahora las coordenadas `double`.
Las alturas aceptadas dentro de la tolerancia se comparan simétricamente y
usan la altura común de menor magnitud, como la unión convexa. Seis regresiones
en los tres planos y ambos signos comprueban identidad, coordenadas y volumen
exactamente iguales al invertir los operandos con alturas casi iguales.
No cambian las restricciones de las operaciones: rectángulos del mismo plano,
alturas firmadas compatibles y un único contorno simple; una resta que produce
agujeros, varios cuerpos o volumen nulo sigue siendo rechazada por esta ruta.

Al conservar esas coordenadas apareció otro caso: la triangulación de una
extrusión cóncava válida puede contener un triángulo que se aplana al convertir
sus coordenadas globales a `float`. La comprobación de estanqueidad usa ahora
el centro de los límites del sólido y las coordenadas relativas, como el visor.
No se cambia el orden de los triángulos ni se modifica la geometría para hacer
pasar la comprobación. El búfer histórico de coordenadas globales `float` sigue
siendo una aproximación; no es una fuente de precisión analítica.

## Regresiones

`GridDoubleCoordinatesTest` conserva su batería y su hash histórico, y añade:

- Unión, intersección y diferencia en origen y ±9000 mm, los tres planos y
  ambos signos: 18 escenarios, con conservación de área y volumen a `1e-12`.
- Ediciones de `1e-7 mm` con los mismos UUID de origen: seis configuraciones
  de plano/signo para cada una de las dos operaciones corregidas.
- Vértices y aristas únicos, Euler, semiaristas opuestas y recíprocas,
  ciclos de caras completos, incidencias y longitudes de aristas.
- Orientación de triángulos respecto a la cara analítica, una envolvente
  estanca en coordenadas relativas y volumen positivo de la vista previa.
- Regeneración determinista, conmutatividad de unión/intersección, operandos
  intactos y referencias topológicas exactas rechazadas tras una edición.

Las regresiones de área y UUID fallan antes de corregir la ruta ortogonal.
Con esa ruta corregida, la edición cóncava reproduce además el rechazo erróneo
de la extrusión cuando se valida el búfer global redondeado.

`RelativeGpuCoordinatesAndroidTest` añade 18 escenarios para las tres
operaciones en los tres planos y ambos signos. Usa los métodos públicos de
instalación y actualización del visor: comprueba las coordenadas relativas,
la sustitución de la geometría, la retirada de la selección de cara y arista,
la conservación de la cámara y la correspondencia de triángulos con caras.
La cola GL se ejecuta mediante un shadow de Robolectric, sin un controlador EGL.

## Proyectos guardados e integración

No hay migración ni cambio de versión JSON. Se conservan los UUID de recetas
y bocetos guardados. Un resultado regenerado puede adquirir un UUID analítico
distinto cuando antes su geometría se cuantizaba a `float`; las referencias de
una versión geométrica anterior deben resolverse explícitamente de nuevo.

Esta corrección parte de `main ef856c7` y es independiente de #163, #164 y #165:
actúa sobre la ruta rectangular, no sobre la envolvente convexa de #165.
Las correcciones GPU de #162 y los fixtures de MyCAD 0.6.2 se conservan.
Los 36 casos físicos siguen pendientes; las pruebas JVM no acreditan EGL,
gestos en dispositivos ni la aceptación de la APK publicada.
