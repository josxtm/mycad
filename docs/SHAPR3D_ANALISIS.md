# Estudio funcional de Shapr3D para MyCAD 0.6.x

Actualización: 2026-10-25.

## Alcance, procedencia y límites
Se estudia la **conducta pública documentada** de Shapr3D, no se copia su código, recursos ni geometría propietaria. REA es una herramienta opcional para inspección autorizada de software disponible legítimamente; **no se ha ejecutado REA contra Shapr3D**. No asumir que el resultado de ingeniería inversa proporciona código fuente portable ni un motor CAD.

Fuentes:
- https://support.shapr3d.com/hc/en-us/articles/7874453786908-Extrude
- https://support.shapr3d.com/hc/en-us/articles/13778812916636-Using-History-and-Extrude
- https://support.shapr3d.com/hc/en-us/articles/16031803954972-5-720-Save-orientation-views

## Equivalencias funcionales y pruebas de aceptación

| Función | Entrada | Resultado exigido | Prueba mínima |
|---|---|---|---|
| ViewCube | Toque en cara, arista o esquina | Cámara orientada a vista ortográfica/isométrica | XY/XZ/YZ; rotación por arrastre; el cuerpo no se mueve |
| Sketch | Plano XY, XZ o YZ y gestos | Entidades 2D persistidas en coordenadas del plano | Guardar/reabrir cada plano y comparar puntos |
| Perfil cerrado | Rectángulo, círculo o polilínea cerrada | Región seleccionable con interior y contorno | Rechazar línea abierta y autointersección |
| Extrusión | Perfil, altura firmada en mm y modo | Sólido con volumen, tapas y caras coherentes | 20×10×5 mm = 1000 mm³; altura negativa invierte dirección |
| Edición extrusión | Cambio de altura | Reconstrucción válida de la operación | 5→8 mm: volumen 1600 mm³, sin duplicados |
| Booleanas | Dos sólidos y modo union/cut/intersect | Resultado sólido sin caras internas | Dos cubos solapados; validar volúmenes |
| Historial | Cambiar parámetro anterior | Recalcular dependencias o error explícito | Cambiar sketch y reabrir proyecto |

## Fases de implementación (sin romper 0.5.x)
1. **Auditoría de código 0.6.0**: localizar MainActivity, perfilado, cámara, MeshEngine, MeshIO y formatos JSON; fijar pruebas de regresión.
2. **Control cámara**: ViewCube realmente interactivo con conversión de coordenadas, animación opcional y separación completa cámara/objeto.
3. **Perfil → sólido**: pasar de triángulos sueltos a perfiles válidos, topología consistente y extrusión con tapas; empezar con rectángulos y círculos.
4. **API geométrica independiente**: definir interfaces de sólido/face/edge/feature, probar un kernel B-Rep apto para Android y licencias (Open CASCADE es candidato, no integración confirmada).
5. **Operaciones**: booleanas, redondeos, chaflanes, vaciados y edición de caras, sujetas a kernel y tests.
6. **Persistencia/historial**: IDs estables, versión de documento, migración desde JSON existente y prueba abrir/guardar.
7. **CI y móvil**: compilación, testDebugUnitTest, lintDebug y artefacto APK solamente si pasan; después validación táctil en Nothing Phone 2a.

## Política de entrega
Distinguir **diseñado**, **implementado**, **compilado** y **validado en móvil**. Un artefacto compilado no prueba que extrusión ni ViewCube funcionen. El flujo CI falla ante errores de tests/lint/compilación y evita publicar un APK fallido.


## Auditoría recuperada y contrastada — 2026-10-25

**Referencia REA:** https://github.com/morluto/rea (Reverse Engineer Anything).
REA es una herramienta de análisis mediante agentes/CLI/MCP, no una biblioteca CAD ni un motor geométrico intercambiable. Se puede emplear con software y artefactos a los que se tenga acceso autorizado y cumpliendo sus licencias. Para ingeniería inversa nativa requiere herramientas adicionales y los resultados deben verificarse; no consta ninguna ejecución de REA contra Shapr3D.

**Código real de MyCAD revisado:**

| Módulo | Implementación actual | Limitación relevante |
|---|---|---|
| `MainActivity.java` | Editor táctil con primitivas, bocetos, extrusión, cámara, selección proyectada, almacenamiento JSON | Lógica y renderizado mayoritariamente concentrados en una Activity |
| `MeshEngine.java` | Renderización GPU OpenGL ES 3 de mallas triangulares XYZ | No tiene entidades topológicas paramétricas |
| `BoxBooleanMesh.java` | Genera superficies triangulares de unión/resta/intersección de dos cajas alineadas a ejes | No generaliza a caras inclinadas, cilindros ni geometría arbitraria |
| `MeshIO.java` | Lectura/escritura STL y OBJ | Los formatos triangulares no preservan features o bocetos CAD |
| Documento JSON v1 | Guarda piezas, bocetos enlazados y mallas booleanas | No incluye un árbol general de operaciones CAD reconstruibles |

**Resultado:** seguir añadiendo controles visuales de edición facial antes de introducir una representación sólida/topológica extensible sería arriesgado. La selección actual de caras se basa en polígonos proyectados y profundidad media: no equivale a ray casting de precisión ni a caras B-Rep.

### Próximo bloque recomendado

1. Crear pruebas de aceptación geométrica sobre sólidos (volumen, orientación, tolerancias, manifold, guardado y reapertura).
2. Extraer de `MainActivity` una capa independiente `CadDocument` / `CadBody` / `CadFace` / `CadEdge` / `CadFeature` que mantenga las operaciones y sus relaciones.
3. Evaluar un kernel B-Rep existente compatible con Android (por ejemplo, Open CASCADE), incluyendo compilación NDK, huella de APK, rendimiento y condiciones de licencia. **Evaluación no equivale a integración**.
4. Mantener el actual flujo de mallas para visualización y STL/OBJ mientras el nuevo kernel se prueba en paralelo.
5. Usar REA como investigación secundaria para interacciones de Shapr3D y sus evidencias observables; no copiar código propietario, formatos privados ni recursos.

**Criterio de salida:** editar una cara, regenerar el sólido, deshacer y guardar/reabrir sin pérdida topológica. Hasta entonces, MyCAD 0.6.0 continúa en fase experimental.
