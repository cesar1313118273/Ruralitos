# Supabase de Ruralitos

Proyecto remoto: `Ruralitos App`

- Referencia: `gqpspzirkuasmiehdsjg`
- Región: São Paulo (`sa-east-1`)
- PostgreSQL: 17
- Storage privado: `fichas-adjuntos`

## Migraciones aplicadas

1. `202607170001_esquema_inicial_ruralitos.sql`
   - Perfiles vinculados a Supabase Auth.
   - Organizaciones, roles e invitaciones.
   - Tablas completas de la ficha familiar.
   - UUID, borrado lógico, auditoría y control de versiones para sincronización.
   - RLS por organización y almacenamiento privado.
2. `202607170002_endurecer_seguridad_indices.sql`
   - Funciones internas fuera del Data API público.
   - RPC de incorporación limitadas a usuarios autenticados.
   - Índices para todas las claves foráneas.

3. `202607170003_preparar_suscripciones.sql`
   - Plan `PRUEBA` y estado de suscripción preparados por organización.
   - Fechas de período y gracia disponibles para una futura pasarela de pagos.
   - `pagos_habilitados = false`: durante la prueba no se cobra ni se bloquea el acceso.

4. `202607170004_credencial_profesional.sql`
   - Código SENESCYT único por profesional.
   - El código se registra con la cuenta y se reutiliza automáticamente en las fichas.

5. `202607170005_salas_eais_territorios_y_acceso.sql`
   - Sala vinculada a un centro de salud, con múltiples Salas por cuenta.
   - Jerarquía EAIS y barrio/comunidad.
   - Códigos de acceso para Sala completa, EAIS o territorio.
   - Permisos LECTOR y EDITOR con RLS por ficha, historial, firma y adjuntos.
   - Cambio no destructivo: conserva las organizaciones, usuarios y fichas existentes.
6. `202607180001_restringir_rpc_salas.sql`
   - Impide que el rol anónimo ejecute las funciones de creación y acceso a Salas.
7. `202607180002_indexar_claves_foraneas_salas.sql`
   - Añade los índices de claves foráneas requeridos para escalar las consultas.
8. `202607180003_vincular_salas_existentes.sql`
   - Vincula una Sala antigua a su centro cuando existe un único código UO válido.

## Acceso y recuperación

- Inicio de sesión y registro: correo + contraseña de Supabase Auth.
- Confirmación de correo: activada.
- Recuperación de contraseña: activada.
- URL de retorno permitida: `ruralitos://auth-callback`.
- Acceso sin internet: PIN local de 6 dígitos, protegido por Android Keystore y SQLite cifrado.

## Sincronización

La app guarda primero en Room/SQLite y WorkManager sincroniza automáticamente en
ambos sentidos las fichas, secciones, historial, firmas, adjuntos y eliminaciones
cuando existe una red validada. Los adjuntos descargados del Storage privado se
conservan dentro del almacenamiento interno de la aplicación.
Las sesiones de Supabase se almacenan cifradas y se renuevan con el refresh token.

Cómo se evitan pérdidas de datos al compartir fichas entre usuarios:

- **Descarga atómica por ficha.** Primero se trae todo de la red a memoria y luego cada ficha (con sus datos hijos
  y su estado `SINCRONIZADO`) se aplica en una sola transacción de la base local. Una ficha con cambios sin subir
  (`PENDIENTE`, `ERROR` o `CONFLICTO`) nunca se pisa al descargar.
- **Descarga incremental.** Después de una primera descarga completa solo se piden las filas con `updated_at` posterior
  a la última marca (con 2 minutos de margen), por cursor `(updated_at, id)` en vez de `limit/offset`. Una descarga
  completa cada 24 horas recoge lo que el modo incremental no ve (borrados reales, permisos retirados).
- **Conflictos en fichas.** La cabecera se sube con `PATCH ... version=eq.N`: si otra persona la cambió antes, la
  ficha pasa a `CONFLICTO` y la persona elige «conservar la mía» o «usar la del equipo». Nada se sobrescribe en silencio.
- **Adjuntos.** El servidor admite un adjunto por ficha y tipo (`unique (ficha_id, tipo)`, aunque esté borrado): al
  reemplazar o volver a crear uno se reutiliza el identificador existente y se reactiva (`deleted_at = null`).
  Al eliminar un adjunto o una ficha también se borran sus archivos del Storage.
- **Accesos.** Un administrador ve quién tiene acceso a la Sala y puede quitarlo (`miembros_organizacion.activo` y
  `accesos_sala.activo` en `false`). En el teléfono de esa persona, la próxima sincronización retira las fichas ya
  sincronizadas de la Sala (y las que ya no son visibles para ella). Las que tengan cambios sin subir se conservan.
- **Un teléfono, una cuenta.** Al iniciar sesión con otra cuenta se borran los datos de pacientes de la anterior; si
  la anterior dejó cambios sin sincronizar, el cambio de cuenta se bloquea hasta que se sincronicen.

## Datos de referencia

La tabla `establecimientos_salud` se cargó desde
`app/src/main/assets/establecimientos_salud.csv`.

Validación de la carga:

- 1.934 establecimientos activos.
- 1.934 códigos UO únicos.
- 24 provincias.
- 221 cantones.
- 1.111 parroquias.

Las contraseñas de usuarios no se almacenan en las tablas públicas. Supabase Auth
administra la contraseña de la cuenta; el PIN y la biometría sin conexión se
protegerán localmente con Android Keystore y la base SQLite cifrada.

## Migración pendiente de aplicar

- `202610020001_perfil_sexo_apellidos.sql`
  - Columnas `sexo` (`H`/`M`) y `apellidos` en `perfiles`, y el trigger que las toma del registro.
  - La app funciona aunque aún no se aplique: guarda el sexo y los apellidos en el teléfono y en los datos
    de la cuenta (`user_metadata`). Al aplicarla, también quedan en la tabla `perfiles`.
