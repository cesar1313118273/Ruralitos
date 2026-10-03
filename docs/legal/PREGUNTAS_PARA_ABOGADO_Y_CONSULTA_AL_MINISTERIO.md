# Preguntas para el abogado y consulta a las instituciones

Documento de trabajo, no es asesoría legal. Sirve para llevar a la reunión con un abogado de protección de datos y salud en Ecuador. Marcar cada punto cuando esté resuelto.

## 1. Relación con el Ministerio de Salud Pública

Estado actual de la app (ya aplicado): sin logo, nombre ni textos institucionales del Ministerio en las plantillas de Excel/PDF ni en la interfaz; la institución ya no se escribe por defecto; aviso de independencia en «Acerca de» y en los términos.

- [ ] Los formatos de la ficha familiar y del registro general: ¿son de uso libre, necesitan licencia o autorización para reproducirlos o adaptarlos? La plantilla actual es una versión modificada en estructura que conserva los colores.
- [ ] ¿Reproducir los **campos y la numeración** de la ficha (aunque el diseño sea propio) requiere autorización?
- [ ] El listado de establecimientos de salud (códigos UO, nombres, ubicación): ¿bajo qué licencia o política de datos abiertos se publicó? ¿Se puede redistribuir dentro de una app comercial?
- [ ] La clasificación CIE-10 (`cie10.json`): licencia de uso de la OMS/OPS y de la versión en español.
- [ ] Si los clientes son unidades del sistema público: ¿existe una norma que limite usar sistemas privados o servidores fuera de la red del Ministerio con datos de pacientes?

## 2. Protección de datos personales (datos de salud)

- [ ] Roles: ¿Ruralitos es responsable o encargado del tratamiento? ¿Cambia según el cliente (centro privado, municipio, profesional independiente)?
- [ ] Base de legitimación para datos de salud y para los de personas que no son usuarias de la app (los pacientes). ¿Quién informa a los pacientes y cómo?
- [ ] Delegado de protección de datos: ¿es obligatorio? ¿Registro ante la autoridad?
- [ ] Alojamiento en Supabase: ¿se puede guardar en servidores fuera de Ecuador? ¿Qué cláusulas hacen falta (transferencia internacional)?
- [ ] Plazos de conservación de fichas e historiales clínicos; eliminación al terminar el contrato o si una persona retira su acceso.
- [ ] Procedimiento y plazos para ejercer derechos (acceso, rectificación, eliminación) y para avisar de una filtración de datos.
- [ ] Menores de edad y personas con discapacidad registrados en las fichas: ¿requisitos adicionales?

## 3. Contrato y comercialización

- [ ] Modelo de contrato con el cliente: autorización institucional, roles, responsabilidad, soporte, disponibilidad, terminación y devolución/borrado de datos.
- [ ] Propiedad intelectual de la app, de las plantillas propias y de los datos de cada cliente.
- [ ] Licencias de código abierto incluidas (OpenStreetMap/ODbL, Protomaps, MapLibre, Valhalla, Apache POI, SQLCipher, fuentes Liberation y Noto): ¿alcanza la pantalla «Acerca de» como atribución?
- [ ] Forma societaria, RUC y facturación electrónica para vender el servicio.
- [ ] ¿La app es un «dispositivo médico/software sanitario» ante la agencia reguladora? (Hoy no hace diagnóstico ni recomendaciones clínicas automáticas más allá de agrupar y calificar según la normativa.)

## 4. Qué conviene preguntar directamente a la institución

- [ ] ¿Existe un procedimiento de aval o registro para herramientas de terceros que registren fichas familiares?
- [ ] ¿Pueden los equipos de atención primaria usar una herramienta externa para la ficha familiar si los datos están cifrados y la institución es la titular de la información?
