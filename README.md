# MyCAD — CAD 2D/3D para Android

## Descargar MyCAD 0.5.0 (APK de desarrollo)

**Estado:** GitHub Actions compiló correctamente MyCAD 0.5.0. La APK todavía necesita pruebas funcionales en un teléfono Android.

**[⬇ Descargar el artefacto myCad0.5.0 — compilación correcta](https://github.com/josxtm/mycad/actions/runs/40140494636)**

### Instalación en Android

1. Entra en el enlace de la compilación e inicia sesión en GitHub si te lo solicita.
2. Desplázate hasta **Artifacts**, al final de la página.
3. Pulsa **myCad0.5.0** para descargar el archivo ZIP.
4. Abre el ZIP desde **Archivos** en Android y extrae la APK que contiene.
5. Abre la APK extraída y confirma la instalación. Si Android lo solicita, autoriza temporalmente la instalación desde la aplicación con la que has abierto el archivo.

> **Importante:** no pulses **Source code (zip)** en la página del repositorio: contiene el código fuente, no la APK. Los artefactos de Actions pueden caducar; si el enlace deja de funcionar, ejecuta de nuevo el workflow desde la pestaña **Actions**.

**[Ver todas las compilaciones](https://github.com/josxtm/mycad/actions)** · **[Documentación de desarrollo 0.5.0](docs/README.md)**


Aplicación experimental de diseño asistido por ordenador para Android, inspirada en el flujo de trabajo táctil de programas como Shapr3D. Desarrollada en Java y compilada en la nube con **GitHub Actions**. No requiere un ordenador local para descargar e instalar el APK.

**Versión actual del código: 0.5.0 (desarrollo)** · **Compilación 0.5.0: correcta (GitHub Actions)** · **Pruebas en móvil de 0.5.0: pendientes**. La última versión validada en móvil es 0.4.8.

> **Importante:** compilar correctamente no equivale a comprobar todas las funciones en un teléfono. El usuario confirmó la creación y movimiento XYZ del prisma en 0.4.6 y la rotación Z en 0.4.7. El usuario confirmó el funcionamiento de los aros táctiles XYZ y la rotación numérica en 0.4.8.

## Documentación y siguiente etapa

La planificación de **MyCAD 0.5.0** y todas las fases posteriores se documentan en [docs/README.md](docs/README.md). Incluye diseño funcional, roadmap, referencias de vídeo, arquitectura, pruebas y registro de decisiones. El código 0.5.0 todavía no está implementado.

## Versión 0.4.8 — rotación XYZ táctil

- Herramienta **GIRAR 3D**: ángulos numéricos independientes X, Y y Z en grados.
- Manipulador de rotación con tres aros: rojo X, verde Y y azul Z, arrastrables con el dedo.
- Representación 3D del prisma con rotación XYZ y almacenamiento de los ángulos en JSON.
- Corrección de compilación: variable `rotationMode` compartida correctamente con `MainActivity`.
- **Compilación Android confirmada**. El usuario confirmó el funcionamiento de los gestos en el móvil.
- Limitaciones: el visor GPU, las operaciones con mallas importadas y las transformaciones avanzadas aún no ofrecen paridad con Shapr3D.

## Versión 0.4.7 — primera rotación Z

- Botón **GIRAR Z** para introducir ángulo en grados.
- Rotación Z visible y persistida en el archivo de proyecto.
- Rotación Z confirmada funcional en el móvil.

## Versión 0.4.6 — interfaz móvil y manipulador XYZ

- Barra inferior permanente para NAVEGAR, MOVER, EDITAR y posición XYZ numérica.
- Manipulador táctil de tres ejes sobre la pieza seleccionada (X rojo, Y verde, Z azul); arrastre restringido al eje elegido.
- Árbol de cuerpos accesible desde el lateral derecho, con selección directa y dimensiones.
- Lectura de coordenadas XYZ en milímetros durante el movimiento.
- Esta versión implementa una primera aproximación de interfaz inspirada en CAD táctil; **no** incluye aún operaciones avanzadas de Shapr3D como booleanas, extrusión de perfiles, edición por caras o historial paramétrico.
- Pendiente: comprobar compilación GitHub Actions y probar gestos en Android real.

## Instalación y compilación

1. Entra en [GitHub Actions — APK Android](https://github.com/josxtm/mycad/actions).
2. Abre una ejecución con resultado **Success** y desplázate hasta **Artifacts**.
3. Descarga el artefacto `myCad<VERSIÓN>` (por ejemplo, `myCad0.4.5`).
4. Descomprime el ZIP e instala `myCad<VERSIÓN>.apk` en Android, autorizando la instalación de APK si es necesario.

El proyecto usa **Android SDK 35**, **minSdk 26**, **Java 17** y **Gradle 8.9** en el flujo CI. La tarea ejecuta `gradle :app:assembleDebug :app:lintDebug --no-daemon`. La salida es una **APK debug**, no una publicación firmada para tienda. El nombre del APK y del artefacto se calcula a partir de `versionName` en `app/build.gradle`.

**Última ejecución v0.4.5 verificada:** [GitHub Actions #40132020733](https://github.com/josxtm/mycad/actions/runs/40132020733) — compilación, Lint y subida de artefacto correctos.

## Funcionalidades

- Crear y editar piezas rectangulares y cilíndricas con medidas en milímetros.
- Bocetos 2D táctiles de líneas, rectángulos y círculos.
- Selección de piezas, controles geométricos, desplazamiento y restricciones de movimiento.
- Vistas 2D y 3D, cuadrícula configurable, ejes y ajuste a cuadrícula.
- Deshacer/rehacer, tema claro/oscuro, ajustes de sensibilidad y modo película de cámara.
- Guardar y abrir diseños `.mycad.json` con el selector de documentos de Android; borrador interno automático.
- **Desde v0.4.4:** módulo `MeshEngine.java` de visualización de mallas con OpenGL ES 3.0; importación STL binario/ASCII y OBJ; exportación STL binario y OBJ; triangulación de piezas básicas para exportación. El visor GPU se abre desde la interfaz como visor adicional: **no sustituye aún toda la escena CAD anterior**.
- **Desde v0.4.5:** selector de modos **Navegar / Mover / Editar** para separar el giro de cámara, el movimiento de piezas y el arrastre de controladores.

## Historial de versiones

Todas las versiones descritas corresponden a las iteraciones de este repositorio. Se distinguen los cambios de código de su verificación real en Android.

| Versión | Cambios realizados | Estado conocido |
| --- | --- | --- |
| **0.1** | Primera prueba de concepto de MyCAD para Android, anterior a las mejoras de archivos y herramientas. | Prototipo histórico; alcance detallado no documentado. |
| **0.2** | Piezas básicas con dimensiones y espesor; vista 3D; guardar y abrir `.mycad.json` mediante Android; borrador, guía y plan de desarrollo. | Documentada en el README original; pruebas históricas no verificadas aquí. |
| **0.3** | Creación, selección y movimiento básico de piezas; coordenadas XYZ; deshacer/rehacer; archivos JSON. | Compilada y confirmada funcional por el usuario, según historial del proyecto. |
| **0.4.1** | Barras laterales compactas, menú de proyecto e iconos provisionales. | Compilada y confirmada funcional por el usuario, según historial. |
| **0.4.2** | Bocetos táctiles de línea, rectángulo y círculo; selección y controladores; edición por arrastre; ajustes de tema y sensibilidad; modo película 3D; revisión de guardado/recuperación de bocetos, historial y cámara. | Compilaciones correctas; se detectaron limitaciones de selección y gestos que motivaron versiones posteriores. |
| **0.4.3** | Controladores de dimensiones de piezas 3D (W/D/H/R); bloqueo de cámara durante edición; movimientos libre, ortogonal, por ejes, radial y exacto; cuadrícula con planos XY/XZ/YZ, pasos y Snap; persistencia de ajustes; corrección de Lint y nombre automático del APK. | **Compilación correcta y funciones confirmadas por el usuario en móvil**. |
| **0.4.4** | Nuevo motor gráfico GPU basado en OpenGL ES 3.0; visor de mallas triangulares; lectores STL/OBJ; exportadores STL/OBJ; triangulación de prismas y cilindros; menú de importación/exportación. | **APK compilado**; no consta validación completa de importación/exportación ni rendimiento en móvil. |
| **0.4.5** | Separación de interacción en modos **Navegar, Mover y Editar**; giro con un dedo sobre la escena aunque haya selección; gesto de dos dedos para desplazamiento de vista; zoom con pellizco; eliminación de la dependencia del doble/triple toque para mover/editar; desplazamiento aplicado a la proyección 2D/3D. | **APK compilado y artefacto publicado**; ergonomía y gestos pendientes de confirmación en móvil. |

### Compilaciones de referencia

| Versión | Ejecución GitHub Actions | Resultado |
| --- | --- | --- |
| 0.4.2 (revisión) | [40128738815](https://github.com/josxtm/mycad/actions/runs/40128738815) | Success |
| 0.4.3 | [40130175267](https://github.com/josxtm/mycad/actions/runs/40130175267) | Success |
| 0.4.4 | [40131327164](https://github.com/josxtm/mycad/actions/runs/40131327164) | Success |
| 0.4.5 | [40132020733](https://github.com/josxtm/mycad/actions/runs/40132020733) | Success |

## Formatos de archivo

| Formato | Abrir/importar | Guardar/exportar | Observaciones |
| --- | --- | --- | --- |
| `.mycad.json` | Sí | Sí | Proyecto nativo; no equivale a un sólido CAD industrial. |
| `.stl` | Sí, binario/ASCII | Sí, binario | Malla triangular; se asumen milímetros. |
| `.obj` | Sí, geometría de caras | Sí | Malla triangular; el lector triangula polígonos. No hay soporte completo de materiales/texturas MTL. |
| `.dxf` | Pendiente | Pendiente | Importación CAD 2D/3D aún no implementada. |
| `.step` / `.stp` | Pendiente | Pendiente | Requiere un núcleo geométrico CAD adicional. |
| `.iges` / `.igs` | Pendiente | Pendiente | No implementado. |
| `.dwg`, `.3mf`, `.cad` | Pendiente | Pendiente | No implementado; `.cad` no es un formato universal. |

**Limitaciones actuales:** STL/OBJ importados son mallas y no conservan operaciones paramétricas originales. El visor GPU es un componente adicional, no el motor de edición de toda la interfaz; no hay todavía operaciones booleanas, sólidos B-Rep, extrusión paramétrica de perfiles ni exportación STEP. El rendimiento con modelos complejos depende del dispositivo y del tamaño de la malla; no existe un límite de triángulos validado en pruebas.

## Organización del proyecto

- `app/src/main/java/es/josxtm/mycad/MainActivity.java`: interfaz, herramientas, escena CAD clásica y flujo de documentos.
- `app/src/main/java/es/josxtm/mycad/MeshEngine.java`: visor de mallas acelerado por OpenGL ES 3.0.
- `app/src/main/java/es/josxtm/mycad/MeshIO.java`: lectura y escritura de STL/OBJ.
- `app/build.gradle`: configuración Android, número de versión y nombre del APK.
- `.github/workflows/android.yml`: compilación y publicación del artefacto APK.
- `HISTORIAL_CAMBIOS.md`: registro técnico histórico.
- `DIRECTRIZ_GESTOS.md`: especificación original de interacción; **las reglas de doble/triple toque de ese documento fueron sustituidas en la v0.4.5 por modos explícitos**.
- `GUIA.md`: documentación de uso y pruebas, que puede contener referencias a versiones anteriores.

## Próximos pasos

1. Validar en el teléfono los modos Navegar/Mover/Editar de v0.4.5 y el comportamiento de selección.
2. Comprobar importación y exportación STL/OBJ con modelos reales, orientación, dimensiones y archivos grandes.
3. Integrar el visor GPU en la experiencia CAD principal y mejorar iluminación, rendimiento, selección y manipulación de mallas.
4. Añadir compatibilidad DXF y estudiar motores CAD para STEP/IGES y operaciones de sólidos.
5. Actualizar guías y documentación de gestos tras la validación en móvil.

---

**Fecha de actualización:** 24 de octubre de 2026. Este README documenta los cambios comprobados en el código, las compilaciones conocidas y las validaciones comunicadas; no presenta como probadas funciones que aún requieren ensayos en Android.
