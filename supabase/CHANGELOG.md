# Changelog de Supabase

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
