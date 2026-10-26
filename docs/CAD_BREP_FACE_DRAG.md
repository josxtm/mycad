# MyCAD 0.6.0 — Primer manipulador sobre la pieza 3D

En el visor CAD experimental, tocar una cara y abrir **Proyecto → Arrastrar cara directamente en 3D** activa un **punto de control amarillo en la propia cara**. El usuario puede tocar ese punto y deslizarlo para desplazar la cara a lo largo de su normal, mientras MyCAD reconstruye temporalmente la pieza.

La entidad geométrica `PrimitiveFaceHandle` proporciona el centro nominal de una cara y su normal mundial bajo rotaciones XYZ, para prismas y cilindros. La interacción de `MeshEngine` proyecta el extremo del asa con la misma cámara en perspectiva que el visor y convierte píxeles de desplazamiento a distancia de modelo. La cámara queda bloqueada solo mientras se arrastra ese asa, sin impedir navegar fuera de ella.

Al levantar el dedo aparece **Aplicar / Cancelar**. Cancelar, un error geométrico o un cambio de selección restaura la malla real sin mutar medidas ni guardar; aplicar vuelve a validar identidad, cotas y rotaciones, realiza `checkpoint`, modifica la pieza y la persiste. Cuerpos vinculados a bocetos se bloquean para no romper sus dependencias. Volver a elegir la acción del menú cancela el modo de arrastre.

CI sin APK: `PrimitiveFaceHandleTest` comprueba seis caras planas y tres cilíndricas, orientación XYZ, referencias persistentes y buffers defensivos. El guion verifica la integración de vista previa, confirmación y señal de toque.

**Límites conocidos:** el gesto es un primer prototipo de desplazamiento **de cara**, no arrastre libre de aristas ni edición de geometría B-Rep general. Para normales proyectadas casi paralelas a la dirección de mirada se utiliza movimiento vertical alternativo. El punto GPU puede variar de tamaño según controlador OpenGL. Se necesita validación real en Nothing Phone 2a.
