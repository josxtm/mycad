# MyCAD 0.6.0 · Edición táctil de cilindros sobre B-Rep analítica

El visor CAD experimental triangula cada cilindro usando `CylinderBRep`, que mantiene una topología con tres caras y dos circunferencias independientes de la malla. En la selección GPU, cada triángulo de la lateral, tapa superior o inferior obtiene un ID persistente (`PrimitiveTopologyMap`).

## Cómo probar en Android cuando publiquemos la próxima APK

1. Activar el visor 3D GPU y el modo CAD experimental desde **Proyecto**.
2. Crear un cilindro independiente, tocar su cara lateral o una tapa.
3. Abrir **Proyecto → Editar cara cilíndrica B-Rep**. Introducir desplazamiento positivo o negativo en milímetros.
4. En el lateral, `+2` desplaza el radio 2 mm (el diámetro crece 4 mm). En tapa superior, `+2` aumenta altura 2 mm con base fija. En tapa inferior, `+2` extiende hacia abajo 2 mm, conservando fija la tapa superior incluso con rotación XYZ.
5. Comprobar que el número de piezas no cambia, la selección mantiene UUID y funcionan Deshacer, Rehacer, Guardar y Abrir.
6. Confirmar que **Cancelar** no modifica el sólido y que cotas degeneradas son rechazadas.

## Protecciones

- Se compara la identidad del cuerpo y la cara tocada al confirmar, evitando actuar sobre una selección obsoleta.
- No se permiten desplazamientos de caras de cilindros dependientes de un boceto: se exige editar el perfil/vínculo por la vía existente.
- El plano analítico se valida primero; solo entonces se registra el punto de deshacer y se actualizan diámetro, fondo, altura y posición XYZ.
- El identificador de cara temporal se limpia al cambiar de vista, abrir/crear proyecto o ejecutar Deshacer/Rehacer.
- La CI verifica presencia de la integración y ejecuta las regresiones geométricas sin ensamblar APK.

**Limitaciones:** la operación solo afecta primitivas cilíndricas. No modifica mallas booleanas ni implementa redondeos, STEP o B-Rep general. La prueba táctil en Nothing Phone 2a queda pendiente del siguiente hito APK.
