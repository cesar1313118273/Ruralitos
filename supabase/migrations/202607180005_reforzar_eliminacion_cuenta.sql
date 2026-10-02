-- La validacion de reautenticacion solo se invoca desde la Edge Function
-- autenticada. Ningun cliente puede ejecutar este SECURITY DEFINER.

drop function if exists public.validar_eliminacion_cuenta(text);

create or replace function public.validar_eliminacion_cuenta(
  p_usuario_id uuid,
  p_codigo text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_usuario auth.users%rowtype;
  v_identificador text;
  v_hash text;
  v_archivos jsonb := '[]'::jsonb;
begin
  if p_usuario_id is null then
    raise exception 'La cuenta no es valida.' using errcode = '22023';
  end if;
  if coalesce(btrim(p_codigo), '') !~ '^[0-9]{6,8}$' then
    raise exception 'El codigo de verificacion no es valido.' using errcode = '22023';
  end if;

  select * into v_usuario
  from auth.users
  where id = p_usuario_id
  for update;

  if not found then
    raise exception 'La cuenta ya no existe.' using errcode = 'P0002';
  end if;

  v_identificador := coalesce(nullif(v_usuario.email, ''), nullif(v_usuario.phone, ''));
  if v_identificador is null
     or v_usuario.reauthentication_sent_at is null
     or v_usuario.reauthentication_sent_at < now() - interval '10 minutes'
     or coalesce(v_usuario.reauthentication_token, '') = '' then
    raise exception 'El codigo expiro. Solicita uno nuevo.' using errcode = '22023';
  end if;

  v_hash := encode(
    extensions.digest(v_identificador || btrim(p_codigo), 'sha224'),
    'hex'
  );
  if v_hash <> v_usuario.reauthentication_token then
    raise exception 'El codigo es incorrecto.' using errcode = '22023';
  end if;

  select coalesce(
    jsonb_agg(
      jsonb_build_object('bucket', o.bucket_id, 'name', o.name)
      order by o.bucket_id, o.name
    ),
    '[]'::jsonb
  )
  into v_archivos
  from storage.objects o
  where o.owner = p_usuario_id or o.owner_id = p_usuario_id::text;

  return jsonb_build_object('archivos', v_archivos);
end;
$function$;

revoke all on function public.validar_eliminacion_cuenta(uuid, text) from public;
revoke all on function public.validar_eliminacion_cuenta(uuid, text) from anon;
revoke all on function public.validar_eliminacion_cuenta(uuid, text) from authenticated;
grant execute on function public.validar_eliminacion_cuenta(uuid, text) to service_role;
