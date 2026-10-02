-- Una Sala sin otro propietario se elimina junto con sus fichas. Las tablas
-- dependientes de cada ficha ya tienen ON DELETE CASCADE.

create or replace function public.preparar_eliminacion_cuenta(p_usuario_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_organizacion record;
  v_relevo uuid;
  v_salas_eliminadas integer := 0;
  v_salas_transferidas integer := 0;
begin
  if p_usuario_id is null
     or not exists (select 1 from auth.users where id = p_usuario_id) then
    raise exception 'La cuenta ya no existe.' using errcode = 'P0002';
  end if;

  for v_organizacion in
    select o.id
    from public.organizaciones o
    where o.creado_por = p_usuario_id
    for update
  loop
    v_relevo := null;
    select m.usuario_id
    into v_relevo
    from public.miembros_organizacion m
    where m.organizacion_id = v_organizacion.id
      and m.usuario_id <> p_usuario_id
      and m.activo
    order by (m.rol = 'ADMIN') desc, m.creado_en
    limit 1;

    if v_relevo is null then
      delete from public.fichas_familiares
      where organizacion_id = v_organizacion.id;
      delete from public.organizaciones
      where id = v_organizacion.id;
      v_salas_eliminadas := v_salas_eliminadas + 1;
    else
      update public.organizaciones
      set creado_por = v_relevo, updated_at = now()
      where id = v_organizacion.id;

      update public.eais
      set creado_por = v_relevo, updated_at = now()
      where organizacion_id = v_organizacion.id
        and creado_por = p_usuario_id;
      update public.territorios
      set creado_por = v_relevo, updated_at = now()
      where organizacion_id = v_organizacion.id
        and creado_por = p_usuario_id;
      update public.accesos_sala
      set creado_por = v_relevo, updated_at = now()
      where organizacion_id = v_organizacion.id
        and creado_por = p_usuario_id;
      update public.invitaciones_organizacion
      set creado_por = v_relevo
      where organizacion_id = v_organizacion.id
        and creado_por = p_usuario_id;
      update public.miembros_organizacion
      set rol = 'ADMIN', updated_at = now()
      where organizacion_id = v_organizacion.id
        and usuario_id = v_relevo;

      insert into public.accesos_sala (
        organizacion_id, usuario_id, alcance, permiso, activo, creado_por
      ) values (
        v_organizacion.id, v_relevo, 'SALA', 'ADMINISTRADOR', true, v_relevo
      )
      on conflict (organizacion_id, usuario_id, alcance, eais_id, territorio_id)
      do update set
        permiso = 'ADMINISTRADOR',
        activo = true,
        creado_por = excluded.creado_por,
        updated_at = now();

      v_salas_transferidas := v_salas_transferidas + 1;
    end if;
  end loop;

  update public.eais e
  set creado_por = o.creado_por, updated_at = now()
  from public.organizaciones o
  where e.organizacion_id = o.id and e.creado_por = p_usuario_id;

  update public.territorios t
  set creado_por = o.creado_por, updated_at = now()
  from public.organizaciones o
  where t.organizacion_id = o.id and t.creado_por = p_usuario_id;

  update public.invitaciones_organizacion i
  set creado_por = o.creado_por
  from public.organizaciones o
  where i.organizacion_id = o.id and i.creado_por = p_usuario_id;

  update public.accesos_sala
  set creado_por = null, updated_at = now()
  where creado_por = p_usuario_id;

  delete from public.accesos_sala where usuario_id = p_usuario_id;
  delete from public.miembros_organizacion where usuario_id = p_usuario_id;

  return jsonb_build_object(
    'salas_eliminadas', v_salas_eliminadas,
    'salas_transferidas', v_salas_transferidas
  );
end;
$function$;
