-- Al traspasar una ficha, el «responsable del llenado» (nombre y codigo profesional que salen en el PDF y el Excel) pasa
-- a ser el de la nueva duena, no se queda con el de quien la creo.
-- Aplicar DESPUES de 20261009200000_roles_iguales.sql.

create or replace function public.aceptar_traspaso(p_codigo text)
returns integer
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_traspaso public.traspasos%rowtype;
  v_ficha record;
  v_total integer := 0;
  v_nombre text;
  v_codigo text;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;

  select * into v_traspaso
  from public.traspasos
  where codigo_hash = extensions.digest(upper(btrim(p_codigo)), 'sha256')
    and usado_en is null
    and expira_en > now()
  for update;
  if not found then
    raise exception 'El codigo es invalido, ya fue usado o expiro.' using errcode = '22023';
  end if;
  if v_traspaso.de_usuario = v_yo then
    raise exception 'Este codigo lo creaste tu: entregaselo a otra persona.' using errcode = '22023';
  end if;

  select nullif(btrim(coalesce(nombres, '')), ''), nullif(btrim(coalesce(codigo_senescyt, '')), '')
  into v_nombre, v_codigo
  from public.perfiles where id = v_yo;

  for v_ficha in
    select f.id, f.organizacion_id
    from public.traspasos_fichas tf
    join public.fichas_familiares f on f.id = tf.ficha_id
    where tf.traspaso_id = v_traspaso.id
      and f.creado_por = v_traspaso.de_usuario
      and f.deleted_at is null
  loop
    insert into public.miembros_organizacion (organizacion_id, usuario_id, establecimiento_id, rol, activo, creado_por)
    values (
      v_ficha.organizacion_id, v_yo,
      (select establecimiento_id from public.organizaciones where id = v_ficha.organizacion_id),
      'MEDICO', true, v_traspaso.de_usuario
    )
    on conflict (organizacion_id, usuario_id) do update
    set rol = case when public.miembros_organizacion.rol = 'ADMIN' then 'ADMIN' else 'MEDICO' end,
        activo = true,
        updated_at = now();

    update public.accesos_ficha
    set activo = false, updated_at = now()
    where ficha_id = v_ficha.id and activo and creado_por = v_traspaso.de_usuario;

    update public.fichas_familiares
    set creado_por = v_yo,
        responsable_nombre = coalesce(v_nombre, responsable_nombre),
        responsable_codigo = coalesce(v_codigo, responsable_codigo)
    where id = v_ficha.id;

    insert into public.accesos_ficha (organizacion_id, usuario_id, ficha_id, permiso, activo, creado_por)
    values (v_ficha.organizacion_id, v_yo, v_ficha.id, 'EDITOR', true, v_yo)
    on conflict (usuario_id, ficha_id) do update
    set permiso = 'EDITOR', activo = true, creado_por = v_yo, updated_at = now();

    insert into public.fichas_traspasadas (ficha_id, de_usuario, a_usuario)
    values (v_ficha.id, v_traspaso.de_usuario, v_yo);

    v_total := v_total + 1;
  end loop;

  update public.traspasos set usado_en = now(), a_usuario = v_yo where id = v_traspaso.id;
  return v_total;
end;
$function$;

create or replace function public.traspasar_ficha(p_ficha_id uuid, p_usuario_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_org uuid;
  v_nombre text;
  v_codigo text;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;
  if p_usuario_id = v_yo then
    raise exception 'La ficha ya es tuya.' using errcode = '22023';
  end if;
  select organizacion_id into v_org
  from public.fichas_familiares
  where id = p_ficha_id and creado_por = v_yo and deleted_at is null;
  if v_org is null then
    raise exception 'Solo quien creo la ficha puede traspasarla.' using errcode = '42501';
  end if;
  if not exists (select 1 from private.accesos_de_ficha(p_ficha_id, v_yo) x where x.usuario_id = p_usuario_id) then
    raise exception 'Primero comparte la ficha con esa persona.' using errcode = '42501';
  end if;

  select nullif(btrim(coalesce(nombres, '')), ''), nullif(btrim(coalesce(codigo_senescyt, '')), '')
  into v_nombre, v_codigo
  from public.perfiles where id = p_usuario_id;

  update public.accesos_ficha
  set activo = false, updated_at = now()
  where ficha_id = p_ficha_id and activo and creado_por = v_yo;

  update public.fichas_familiares
  set creado_por = p_usuario_id,
      responsable_nombre = coalesce(v_nombre, responsable_nombre),
      responsable_codigo = coalesce(v_codigo, responsable_codigo)
  where id = p_ficha_id;

  insert into public.accesos_ficha (organizacion_id, usuario_id, ficha_id, permiso, activo, creado_por)
  values (v_org, p_usuario_id, p_ficha_id, 'EDITOR', true, p_usuario_id)
  on conflict (usuario_id, ficha_id) do update
  set permiso = 'EDITOR', activo = true, creado_por = p_usuario_id, updated_at = now();

  insert into public.fichas_traspasadas (ficha_id, de_usuario, a_usuario) values (p_ficha_id, v_yo, p_usuario_id);
end;
$function$;

-- Fichas ya traspasadas antes de este cambio: su responsable pasa al de quien las recibio.
update public.fichas_familiares f
set responsable_nombre = coalesce(nullif(btrim(coalesce(p.nombres, '')), ''), f.responsable_nombre),
    responsable_codigo = coalesce(nullif(btrim(coalesce(p.codigo_senescyt, '')), ''), f.responsable_codigo)
from public.perfiles p
where p.id = f.creado_por
  and f.deleted_at is null
  and exists (select 1 from public.fichas_traspasadas t where t.ficha_id = f.id and t.a_usuario = f.creado_por);
