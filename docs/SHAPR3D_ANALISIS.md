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
