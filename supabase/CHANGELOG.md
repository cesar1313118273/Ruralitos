# Changelog de Supabase

## 2026-07-18 — Salas y acceso territorial

- Estado: aplicado y verificado en el proyecto remoto.
- Tipo: cambio compatible y no destructivo.
- Añade Salas por centro de salud, EAIS, barrios/comunidades y acceso compartido con alcance.
- Reemplaza el acceso general a fichas por políticas RLS de Sala, EAIS o territorio.
- Conserva y migra los miembros y fichas existentes con acceso completo equivalente.
- Restringe los RPC de Salas al rol autenticado e indexa sus claves foráneas.
- Vincula de forma segura la Sala existente con el centro identificado por su ficha activa.
