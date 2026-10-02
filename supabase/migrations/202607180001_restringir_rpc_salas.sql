-- Las funciones nuevas fueron creadas en un proyecto con grants automáticos.
-- Retira explícitamente el acceso directo de anon y conserva solo usuarios autenticados.

revoke execute on function public.crear_sala(text) from anon;
revoke execute on function public.crear_sala(text) from public;

revoke execute on function public.crear_codigo_acceso(
  uuid, text, uuid, uuid, text, text, integer
) from anon;
revoke execute on function public.crear_codigo_acceso(
  uuid, text, uuid, uuid, text, text, integer
) from public;

grant execute on function public.crear_sala(text) to authenticated;
grant execute on function public.crear_codigo_acceso(
  uuid, text, uuid, uuid, text, text, integer
) to authenticated;