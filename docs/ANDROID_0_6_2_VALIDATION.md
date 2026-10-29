# Validación funcional Android — MyCAD 0.6.2

Estado: **preparada; ninguna prueba física registrada**. Ejecutar en Nothing
Phone 2a y en la tablet Android de 12 pulgadas. No comenzar otra fase de desarrollo
durante esta validación. Esta preparación solo añade documentación y datos de ensayo.

## APK y código de referencia

Comprobado el 29 de octubre de 2026 mediante GitHub:

| Evidencia | Valor |
| --- | --- |
| Repositorio | `josxtm/mycad` |
| Último commit integrado en main | `1e2cafa5036e4cdc6a14b333324568f189310b27` |
| APK de esa integración | [myCad0.6.2, artefacto 12908082325](https://github.com/josxtm/mycad/actions/runs/40898190332/artifacts/12908082325) |
| Flujo de la APK | [android.yml #660 — success](https://github.com/josxtm/mycad/actions/runs/40898190332) |
| Identidad del artefacto | `workflow_run.head_sha` igual al commit de main anterior; no caducado al comprobarlo |
| Versión esperada | `0.6.2`, código `62`, paquete `es.josxtm.mycad.preview` |
| Compatibilidad declarada | Android 8/API 26 o posterior; target/compile SDK 35 |

SHA-256 **del ZIP descargable**, no de la APK extraída:

```text
d28fb7d3cd674044de61ade24cd1065dba4da0961e7cf7c97a0fb1b059454da8
```

CI #660 superó integridad, geometría, Gradle/tests/Lint, generación, comprobación
y publicación de la APK. Eso no certifica instalación, renderizado EGL ni gestos
en un dispositivo real. La documentación de esta validación se mantiene en una
rama independiente, sin fusionar, para conservar main igual a la APK de referencia.

## Preparación y registro

1. Guardar una copia externa de cualquier diseño propio antes de usar «Nuevo diseño».
   Trabajar con archivos de prueba y no sobrescribir los originales.
2. Descargar el artefacto anterior, extraer `myCad0.6.2.apk` e instalarlo mediante
   el procedimiento habitual de Android. Si falla la instalación/actualización,
   registrar el mensaje; no desinstalar ni borrar datos para ocultar el fallo.
3. Anotar modelo exacto, Android/API, orientación, versión instalada y tipo de
   instalación en [ANDROID_0_6_2_RESULTS.csv](ANDROID_0_6_2_RESULTS.csv).
   La versión Android y el modelo de tablet están pendientes de registrar.
4. Usar el dedo en ambos dispositivos. Registrar si se emplea otro dispositivo
   de entrada. Hacer capturas antes/después y un vídeo corto si falla un gesto.
5. Los iconos tienen descripciones como «Boceto 2D», «Extruir boceto»,
   «Resultados booleanos» y «Visor 3D GPU»; pueden consultarse con accesibilidad.

Estados: `PENDIENTE`, `APROBADO`, `FALLO`, `BLOQUEADO`, `NO_APLICA`.
No registrar APROBADO sin ejecutar el caso y contrastar su resultado. Indicar
motivo si está bloqueado o no aplica. La hoja empieza con **36 filas PENDIENTE**,
18 casos por dispositivo; no representa 36 pruebas ya ejecutadas.

Las filas P0 son obligatorias; P1 amplían la cobertura geométrica. Un fallo P0
o pérdida de geometría impide aceptar el hito. Si una dependencia falla, marcar
los casos afectados BLOQUEADO y ejecutar los casos independientes con los datos
preparados, sin convertir por ello el caso original en APROBADO.

## Proyectos de ensayo

Descargar cada JSON desde su enlace con «Download raw file» en GitHub y copiarlo
a Descargas del dispositivo. Abrirlo desde Proyecto → Abrir archivo → Elegir archivo.
Para preparar contornos sin sólido, volver primero al lienzo mediante Visor 3D GPU
si el visor está visible; elegir Boceto 2D → plano del archivo → Salir de boceto
y tocar una línea antes de pulsar Extruir. Esta preparación de vista no sustituye
la comprobación de refresco al abrir archivos con GPU activo en PER-01.

| Archivo | Contenido y valor de referencia |
| --- | --- |
| [01-contorno-xy-40x30](fixtures/android-0.6.2/01-contorno-xy-40x30.mycad.json) | Cuatro líneas XY, 40 × 30 mm; sin sólido. Extrusión de 10 mm: 12000 mm³. |
| [02-contorno-xz-40x30](fixtures/android-0.6.2/02-contorno-xz-40x30.mycad.json) | El mismo contorno en XZ; sin sólido. Extrusión ±10 mm: 12000 mm³. |
| [03-contorno-yz-40x30](fixtures/android-0.6.2/03-contorno-yz-40x30.mycad.json) | El mismo contorno en YZ; sin sólido. Extrusión ±10 mm: 12000 mm³. |
| [04-contorno-concavo-xy](fixtures/android-0.6.2/04-contorno-concavo-xy.mycad.json) | L: (0,0), (40,0), (40,10), (20,10), (20,30), (0,30). Área 800 mm²; altura 10: 8000 mm³. |
| [05-resta-xy-40x30x10](fixtures/android-0.6.2/05-resta-xy-40x30x10.mycad.json) | Dos extrusiones y una resta vinculada: 40×30×10 menos 10×10×10. Un agujero; resultado 11000 mm³. Conserva ambos operandos. |
| [06-version-invalida](fixtures/android-0.6.2/06-version-invalida-rechazo-esperado.mycad.json) | Versión 999, intencionadamente incompatible. Debe rechazarse conservando el diseño abierto. |

Los cinco proyectos válidos se generaron con `MainActivity.encode()` de 0.6.2 y
se reabrieron con su codec real en Robolectric/SDK35. Se comprobaron fuentes,
volúmenes, mallas cerradas y, en la resta, identidades topológicas. El sexto se
comprobó como rechazo esperado. Los hashes figuran en el
[manifiesto de preparación](fixtures/android-0.6.2/MANIFEST.json).
**Estos datos no sustituyen la prueba de dibujo con el dedo.**

## Casos de prueba

### INST-01 — Instalación e identificación (P0)

Instalar la APK, abrirla y confirmar MyCAD 0.6.2. Registrar versión Android,
modelo y cualquier error. Esperado: inicio sin cierre, controles utilizables y
versión correcta. No inferir que está instalada únicamente porque se descargó.

### DIB-01 — Contorno cerrado dibujado con el dedo (P0)

En un diseño de prueba nuevo: Boceto 2D → XY → Línea. Dibujar cuatro segmentos
formando un rectángulo y hacer coincidir sus extremos. Ampliar para cerrar el
último extremo si hace falta. Salir de la herramienta mediante Boceto 2D → XY →
Salir de boceto; tocar un borde y pulsar Extruir boceto → 10 → Extruir sólido.
Esperado: cuatro líneas utilizables y un sólido cerrado, sin añadir segmentos
accidentales al navegar o seleccionar. No se exige 40×30 exactos a mano alzada.

Registrar especialmente la dificultad de cierre: la tolerancia actual es 0,15 mm;
esta guía no presupone snapping automático. Si no puede cerrarse razonablemente,
registrar el fallo/usabilidad con vídeo; abrir el archivo 01 para probar el motor
por separado **no aprueba DIB-01**.

### DIB-02 — Perfil abierto rechazado (P0)

En otro diseño de prueba, dibujar tres lados de una U, salir de Línea, seleccionar
un segmento e intentar extruir 10 mm. Esperado: mensaje de rechazo, sin sólido
residual ni pérdida del boceto. Cancelar un nuevo intento también debe conservarlo.

### EXT-01 — Perfil XY exacto → sólido (P0)

Abrir el archivo 01, seleccionar una línea y usar Extruir boceto con 10 mm.
Abrir Resultados booleanos → resultado → Inspeccionar topología B-Rep.
Esperado: un prisma 40×30×10, volumen 12000 mm³ y altura editable.
Comprobar visualmente que tapas y caras laterales están cerradas. Guardar como
`QA062_<dispositivo>_XY10.mycad.json` para los siguientes casos.

### EXT-02 y EXT-03 — XZ e YZ, ambos signos (P0)

Repetir EXT-01 con los archivos 02 y 03. Probar 10 mm y luego editar distancia
a −10 mm. Esperado: 12000 mm³ en ambos signos y cambio de sentido, sin invertir
indebidamente las caras ni crear otro cuerpo. Registrar ambos signos en la fila.

| Plano | Coordenadas del contorno | Eje de altura |
| --- | --- | --- |
| XY | X, Y | Z |
| XZ | X, Z | Y |
| YZ | Y, Z | X |

### EXT-04 — Perfil cóncavo (P1)

Abrir 04 y extruir 10 mm. Esperado: sólido en L de 8000 mm³, sin rellenar el
hueco exterior de la L, sin caras ausentes ni autointersección visible.

### EXT-05 — Cancelación y alturas inválidas (P0)

Reabrir 01 sin sólido. Probar Cancelar; después intentar 0 y 10001 mm en intentos
separados. Esperado: ningún sólido nuevo, boceto intacto y mensaje para los valores
inválidos. No considerar que el diálogo cerrado significa operación aceptada.

### NAV-01 — Órbita, zoom, desplazamiento y selección (P0)

Con XY10 visible en GPU, arrastrar un dedo sobre el fondo, pellizcar con dos dedos
y desplazar ambos dedos juntos. Después tocar una cara sin arrastrar. Esperado:
la cámara responde, el toque selecciona sin órbita accidental y los gestos de
navegación no editan dimensiones, duplican cuerpos ni crean bocetos.
Volver a inspeccionar el sólido: volumen 12000 mm³ y altura 10.

### NAV-02 — Controles actuales de orientación (P0)

Probar SUP, FREN, DER y una celda ISO del control superpuesto al visor GPU;
alternar Canvas/GPU con Visor 3D GPU. Esperado: cambios coherentes de orientación,
sin mover el sólido, alterar medidas ni dejar controles que intercepten gestos
fuera de su zona. Registrar cualquier desajuste Canvas/GPU. Este caso comprueba
el control 3×3 existente; no certifica un ViewCube 3D completo con aristas/esquinas.

### EDI-01 — Altura, Deshacer y Rehacer (P0)

Con el prisma XY10 sin resta: Resultados booleanos → resultado → Editar distancia
de contorno → 12. Esperado: mismo cuerpo, altura 12 y volumen 14400 mm³.
Deshacer debe recuperar 10/12000; Rehacer, 12/14400. Volver a 10 antes de PERS.
No se exige que las pilas de Deshacer/Rehacer sobrevivan a un cierre de proceso.

### PER-01 — Archivo externo, nuevo diseño y reapertura (P0)

Guardar XY10 mediante Guardar archivo, elegir una ubicación real y esperar
«Archivo guardado · Copia externa creada». Anotar nombre/ruta. Crear un diseño
nuevo de prueba: el lienzo/visor debe quedar vacío. Abrir el archivo guardado
mediante Proyecto → Abrir archivo, incluyendo un intento con GPU visible.
Esperado: cuatro líneas, un resultado, altura 10, volumen 12000 y edición disponible.
Si el visor conserva un modelo anterior o no se vacía al crear el diseño nuevo,
registrarlo como fallo de sincronización; no ocultarlo alternando vistas para aprobar.
No basta cerrar/reabrir la actividad: eso solo comprobaría el borrador interno.

### PER-02 — Cierre real y borrador interno (P0)

Después de guardar la copia externa, forzar la detención desde Ajustes de Android
y volver a iniciar MyCAD, sin borrar datos. Esperado: recuperación del último
modelo válido con su altura y geometría. Si el visor se inicia oculto, abrir
Resultados booleanos → resultado → Ver solo este resultado antes de juzgar pérdida.

### PER-03 — Intercambio entre teléfono y tablet (P0)

Copiar el archivo externo generado en un dispositivo al otro y abrirlo.
Esperado: mismas líneas, sólido, altura y volumen; editar una copia y guardarla
sin sobrescribir el original. Registrar origen/destino y archivo usado.

### PER-04 — Apertura inválida y selectores cancelados (P0)

Con XY10 abierto, cancelar los selectores de Guardar y Abrir. Intentar abrir 06.
Esperado: cancelación/rechazo comprensibles y XY10 conservado. Volver a inspeccionar
altura/volumen y guardar una copia; no debe aparecer el proyecto vacío del archivo
inválido ni quedar bloqueada la aplicación.

### DIS-01 — Teléfono/tablet y cambios de orientación (P0)

Con el archivo externo ya guardado, pasar entre vertical y horizontal y repetir
un toque de selección, un diálogo de altura y navegación. Esperado: sin pérdida
de modelo, valores editables y botones accesibles con teclado abierto. Registrar
si el sistema tiene bloqueo de rotación; no atribuirlo a la aplicación sin comprobarlo.

### BREP-01 — Resultado con agujero visible (P1)

Abrir 05: Resultados booleanos → tercera malla → Ver solo este resultado.
Esperado: agujero rectangular pasante, un sólido, volumen 11000 mm³.
Orbitar y comprobar que se ve a través del agujero. Se conservan los dos operandos;
sin aislar la resta pueden tapar visualmente el agujero. Esto no elimina sus fuentes.

### BREP-02 — Edición de cara y dependencia (P1)

En 05, aislar el primer prisma. Seleccionar su cara lateral exterior X=40 y tocarla
otra vez → Editar cara B-Rep seleccionada → Desplazar cara (mm) → 5. Aislar otra
vez la tercera malla. Esperado: resta regenerada a 12500 mm³ con un agujero;
Deshacer recupera 11000 y Rehacer 12500. Guardar/abrir y volver a inspeccionarla.
Si no puede seleccionarse la cara, registrar el bloqueo y vídeo, sin cambiar otras caras.

## Limitaciones que deben quedar visibles en el informe

- Los contornos B-Rep se gestionan en Resultados booleanos. El Árbol de cuerpos
  actual enumera las primitivas `Part`, no todos estos resultados; anotar esta
  limitación de usabilidad, sin confundirla automáticamente con pérdida de datos.
- Los datos antiguos Sketch/Part aún usan float y el cierre admite una tolerancia
  de 0,15 mm. No se certifica precisión arbitraria ni geometría curva general.
- Las restas de esta familia de agujeros exigen alturas firmadas iguales; no
  usar un cambio unilateral incompatible como prueba de que deba aceptarse.
- Sin dispositivo real no se validan EGL, rendimiento, ergonomía ni instalación.
  Los resultados automatizados previos y la preparación de fixtures son evidencia separada.

## Cierre y siguientes fases

Adjuntar las dos hojas de resultados, versiones Android, archivos guardados y
evidencia de los fallos. Indicar caso, pasos, esperado, observado y frecuencia
de reproducción. No modificar la aplicación para convertir una fila en aprobada.
No fusionar la PR documental como si certificara pruebas físicas ya hechas.

Tras revisar las pruebas físicas y sus bloqueos, priorizar **ViewCube 3D → ampliar
operaciones B-Rep → migración compatible de Sketch/Part a double**. Cualquier PR
futura requiere CI verde, fusionabilidad y ausencia de bloqueos.
