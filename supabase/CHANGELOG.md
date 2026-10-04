# Changelog de Supabase

## 2026-10-08 — Huella de cambios para sincronizar rápido

- Estado: aplicado en el proyecto remoto (`migrations/20261008100000_huella_de_cambios.sql`).
- Tipo: cambio compatible; solo agrega la función `huella_de_cambios(organizacion)`.
- Devuelve la fecha del último cambio visible en la Sala (fichas, sus datos y los accesos de quien pregunta). Usa los índices de sincronización que ya existían. La app la consulta cada pocos segundos con la aplicación abierta y descarga solo si cambió.


## 2026-10-07 — Se elimina asignar visitas

- Estado: aplicado en el proyecto remoto (`migrations/20261007400000_quitar_asignar_visita.sql`).
- Tipo: elimina la función `asignar_visita` (creada en 20261006200000); ninguna tabla ni dato cambia. Las visitas ya asignadas, si las hubo, siguen en la agenda de cada persona.


## 2026-10-07 — info_fichas_compartidas por Sala y más liviana

- Estado: aplicado en el proyecto remoto (`migrations/20261007300000_info_compartidas_por_sala.sql`).
- Tipo: cambio compatible (agrega la columna `organizacion_id`; la app anterior la ignora).
- Ya no devuelve una fila por cada ficha propia, solo las compartidas o editadas por otra persona (PostgREST corta en 1000 filas).
- La app la usa en cada sincronización para bajar las fichas recién compartidas y retirar las que perdió.


## 2026-10-07 — Solo el autor elimina su ficha

- Estado: aplicado en el proyecto remoto (`migrations/20261007200000_solo_el_autor_elimina_su_ficha.sql`).
- Tipo: cambio compatible. Trigger `fichas_proteger_baja`: si alguien que no es el autor marca una ficha como eliminada, el cambio se ignora.
- Motivo: una cuenta que recibió una ficha con permiso de edición subió una baja pendiente y eliminó la ficha original de la nube. La ficha y sus datos se recuperaron.


## 2026-10-07 — Avisos de ediciones ajenas y traspaso de fichas

- Estado: aplicado en el proyecto remoto (`migrations/20261007100000_avisos_y_traspaso_de_fichas.sql`).
- Tipo: cambio compatible. Reemplaza `info_fichas_compartidas()` (ahora devuelve también `editor_nombre` y `editada_en`; la app antigua sigue funcionando porque ignora columnas extra).
- Trigger `fichas_marcar_actualizado_por`: `actualizado_por` ahora se llena en cada edición (antes solo al crear).
- `traspasar_ficha(ficha, usuario)`: el autor pasa la ficha a alguien con acceso; el autor anterior conserva acceso de edición y los accesos dados ficha por ficha se anulan.


## 2026-10-06 — Detalle por ficha, quitar ficha por ficha y asignar visitas

- Estado: aplicado en el proyecto remoto (`migrations/20261006200000_visitas_y_acceso_por_ficha.sql`).
- Tipo: cambio compatible; solo agrega funciones, no toca tablas ni datos.
- `personas_con_acceso_ficha`, `fichas_compartidas_con` y `quitar_acceso_ficha`: ver quién ve cada ficha mía y quitar el acceso dado ficha por ficha (solo su autor).
- `asignar_visita`: el autor de una ficha agenda una visita a alguien con quien ya la compartió; se escribe en `agenda_privada` del compañero y le llega al sincronizar.


## 2026-10-06 — Etiquetas de fichas compartidas y «quitarme el acceso»

- Estado: aplicado en el proyecto remoto (`migrations/20261006100000_fichas_compartidas_etiquetas.sql`).
- Tipo: cambio compatible; solo agrega funciones, no toca tablas ni datos.
- `info_fichas_compartidas()`: para cada ficha visible, si me la compartieron (autor y permiso) o a cuántas personas se la compartí.
- `listar_accesos_recibidos()` y `quitar_mi_acceso(organizacion, autor)`: quién me compartió fichas y cómo quitarme yo mismo ese acceso.
- Antes también se aplicaron `20261005200000_compartir_lo_propio.sql` (cada quien comparte lo propio) y `20261005220000_quitar_acceso_heredado.sql`.


## 2026-10-05 — Compartir acceso con varios centros, EAIS, barrios o fichas

- Estado: pendiente de aplicar (`migrations/20261005120000_acceso_por_alcances.sql`).
- Tipo: cambio compatible; los códigos de Sala, EAIS o barrio de siempre siguen funcionando igual.
- Añade `accesos_ficha` (fichas sueltas compartidas) e `invitaciones_alcances` (lo que lleva cada código).
- Añade la función `crear_codigo_acceso_varios` y extiende `aceptar_invitacion` para entregar cada parte del código.
- Las políticas de lectura/edición de fichas y de sus tablas hijas ahora también aceptan el acceso por ficha suelta.
- La app funciona sin esta migración: solo «compartir un solo centro, EAIS o barrio» usa la función antigua; lo demás avisa que falta aplicarla.


## 2026-07-18 — Salas y acceso territorial

- Estado: aplicado y verificado en el proyecto remoto.
- Tipo: cambio compatible y no destructivo.
- Añade Salas por centro de salud, EAIS, barrios/comunidades y acceso compartido con alcance.
- Reemplaza el acceso general a fichas por políticas RLS de Sala, EAIS o territorio.
- Conserva y migra los miembros y fichas existentes con acceso completo equivalente.
- Restringe los RPC de Salas al rol autenticado e indexa sus claves foráneas.
- Vincula de forma segura la Sala existente con el centro identificado por su ficha activa.
