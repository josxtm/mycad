# MyCAD 0.5.0 — Diseño funcional inicial

**Fecha:** 2026-10-25  
**Estado:** diseñado inicialmente; implementación pendiente.  
**Referencia:** vídeos 4 y 10 suministrados por el usuario; muestreo de fotogramas, no auditoría cuadro a cuadro.

## Objetivo principal

Completar en un teléfono Android el flujo:

**Nuevo documento → elegir plano XY → dibujar rectángulo → acotar 60 × 30 mm → seleccionar perfil → extruir +20 mm → sólido → guardar → reabrir.**

## Estados de la interfaz

1. **Navegar:** orbitar la cámara con un dedo sobre fondo, pellizcar para zoom, desplazar con dos dedos, tocar para seleccionar.
2. **Elegir plano:** XY/XZ/YZ, cámara normal al plano, cuadrícula y orientación visibles.
3. **Boceto:** línea, rectángulo y círculo, entidades editables, ajuste opcional y cotas.
4. **Perfil seleccionado:** resaltar un contorno cerrado; mostrar **Extruir** en menú contextual.
5. **Extrusión:** controlador perpendicular al plano, cota flotante, previsualización, confirmar o cancelar.
6. **Sólido seleccionado:** mover, girar, editar, ocultar y gestionar desde el árbol.

## Interfaz para móvil

- Mantener el lienzo 3D como superficie principal.
- Barra superior compacta: documento, deshacer/rehacer, guardar y opciones.
- Herramientas contextuales según modo y selección; evitar barras permanentes saturadas.
- Panel de árbol plegable con bocetos, perfiles y cuerpos.
- Indicador de plano y orientación; controles numéricos cerca de la geometría.
- Áreas táctiles suficientemente grandes para utilizar con los dedos.
- Priorizar modo oscuro y legibilidad de cotas.

## Modelo de datos previsto

- **Document:** unidades, planos, sketches, features, bodies y versión de esquema.
- **Sketch:** plano, entidades 2D, restricciones y cotas.
- **Profile:** bucles cerrados y posibles agujeros.
- **ExtrudeFeature:** perfil de origen, distancia firmada y tipo de operación.
- **Body:** resultado geométrico, transformación y visibilidad.
- **History:** acciones reversibles y referencias estables.

## Compatibilidad de archivos

El documento nativo será JSON versionado; se debe migrar el esquema v1 existente. STL y OBJ representan mallas, no sustituyen a los bocetos y features. DXF queda como fase posterior y requiere definir qué entidades 2D soportaremos.

## Criterios de aceptación

1. Crear un rectángulo de 60 × 30 mm en XY.
2. Seleccionar el área cerrada sin seleccionar accidentalmente la cámara.
3. Extruir +20 mm con previsualización continua.
4. Cancelar sin añadir un cuerpo.
5. Guardar y reabrir conservando cotas, boceto y extrusión.
6. Rechazar extrusión de contornos abiertos, con explicación clara.
7. Abrir diseños JSON v1 sin perder piezas.
8. Exportar STL/OBJ y verificar dimensiones.
9. Usar órbita, zoom y pan sin modificar la geometría.
10. Mostrar herramientas contextuales correctas para cada selección.

## Fuera de alcance inicial

Booleanas generales, fillets, chamfers, shell, patrones complejos, ensamblajes y B-Rep completo. No anunciar estas funciones como disponibles hasta que estén implementadas y probadas.
