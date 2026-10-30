# Intersecciones convexas: concavidad e identidad reproducible

`ConvexPrismBRepIntersection` valida la convexidad con una tolerancia local de
giro, proporcional a las dos aristas adyacentes. La superficie total de un perfil
grande no debe ocultar un vértice cóncavo y permitir rellenar su muesca.

El recorte de dos perfiles oblicuos también necesita un orden de cálculo estable.
Al invertir los operandos, el redondeo de las intersecciones en `double` podía
cambiar las últimas cifras de los vértices y producir otro UUID para el mismo
resultado. Normalizar el primer vértice del contorno final no resolvía esa diferencia.

Antes del recorte se ordenan las secciones por el UUID de origen. Si coinciden
los UUID, se comparan el número de vértices y sus coordenadas `double`; esto cubre
dos snapshots distintos de una misma feature sin cuantizar ni perder precisión.
El resto de las validaciones, el recorte de alturas y el formato de proyecto se conservan.
Los operandos y los UUID persistidos no se modifican. Un resultado regenerado que
antes dependía del orden puede adquirir la identidad canónica corregida.

`ConvexPrismBRepIntersectionTest` reproduce el fallo anterior y comprueba:

- perfiles oblicuos en el origen y a ±9000 mm, XY/XZ/YZ y ambos signos;
- fuentes con UUID diferentes y coincidentes;
- igualdad exacta de coordenadas analíticas, malla e IDs topológicos al invertir operandos;
- área, volumen de altura común, característica de Euler y malla cerrada;
- operandos intactos y rechazo de concavidades leves;
- conservación de los cambios inferiores a la resolución de `float` cubiertos por #166.

Alcance: intersección de prismas convexos sobre el mismo plano. No amplía el motor
a perfiles cóncavos arbitrarios, curvas ni planos distintos; la validación física
Android sigue siendo independiente de estas pruebas automatizadas.
