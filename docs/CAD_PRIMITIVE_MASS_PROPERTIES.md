# MyCAD 0.6.0 — Propiedades físicas de sólidos B-Rep

## Nueva función

En **Proyecto → Propiedades físicas de la pieza**, el usuario selecciona un prisma o cilindro B-Rep y proporciona una densidad en **gramos por centímetro cúbico (g/cm³)**. La app presenta:

- Volumen en mm³, superficie en mm² y masa estimada en gramos.
- Centro de masa tridimensional en coordenadas mundiales XYZ (mm).
- Las seis componentes independientes del tensor de inercia respecto al centro de masa, en **g·mm²**.

La densidad se conserva como preferencia local para la siguiente consulta, no se inserta en el archivo del proyecto y la operación nunca modifica piezas, bocetos ni historial.

## Motor matemático

`PrimitiveMassProperties` usa las dimensiones y el pose del sólido analítico. Para prismas macizos, calcula el tensor diagonal local mediante `Ixx=m(d²+h²)/12`, `Iyy=m(w²+h²)/12`, `Izz=m(w²+d²)/12`. Para cilindros de radio `r`, `Ixx=Iyy=m(3r²+h²)/12` e `Izz=mr²/2`. La masa se obtiene de `V(mm³)·densidad(g/cm³)/1000`. Los tres ejes locales del sólido se rotan al sistema mundial según el mismo orden XYZ del visor y el tensor final se calcula `R I Rᵀ`. El centro de masa se sitúa a mitad de la altura del cuerpo y se transforma con `RigidTransform`.

El cálculo es exacto para las dos primitivas matemáticas, sin depender de su triangulación. Las coordenadas y los resultados se entregan mediante copias defensivas. Se rechazan densidades nulas, negativas, no finitas o fuera del rango admitido.

## Limitaciones

Se supone material **macizo y homogéneo**. Esta primera fase no incorpora operaciones de taladrado/booleanas, huecos, resultados de malla, piezas compuestas ni propiedades relativas a un eje externo. Si el sólido incluye operaciones posteriores que cambian su forma, los valores representan únicamente su primitiva de origen y **no son válidos para fabricar o certificar masa/inercia final**.

## Pruebas

`PrimitiveMassPropertiesTest` cubre prisma y cilindro, masa/densidad, centroides XYZ, rotaciones de 45°/90° y combinaciones XYZ, simetría y traza invariante del tensor, modificación de altura, copias defensivas y valores inválidos. El script de CI verifica la integración en Android y que el menú es de solo lectura. No se genera APK en esta fase.
