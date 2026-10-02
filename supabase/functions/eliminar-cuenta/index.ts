import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.110.7";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function respuesta(codigo: number, cuerpo: Record<string, unknown>) {
  return new Response(JSON.stringify(cuerpo), { status: codigo, headers: cors });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return respuesta(405, { message: "Método no permitido." });

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const clavePublica =
    Deno.env.get("SUPABASE_ANON_KEY") ?? Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
  const claveServicio = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const authorization = req.headers.get("Authorization") ?? "";
  const token = authorization.replace(/^Bearer\s+/i, "").trim();
  if (!supabaseUrl || !clavePublica || !claveServicio || !token) {
    return respuesta(401, { message: "No existe una sesión autenticada." });
  }

  try {
    const cuerpo = await req.json();
    const codigo = String(cuerpo?.codigo ?? "").trim();
    if (!/^[0-9]{6,8}$/.test(codigo)) {
      return respuesta(400, { message: "El código de verificación no es válido." });
    }

    const usuarioCliente = createClient(supabaseUrl, clavePublica, {
      global: { headers: { Authorization: authorization } },
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: usuarioData, error: usuarioError } =
      await usuarioCliente.auth.getUser(token);
    if (usuarioError || !usuarioData.user) {
      return respuesta(401, { message: "La sesión venció. Inicia sesión nuevamente." });
    }

    const administrador = createClient(supabaseUrl, claveServicio, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: validacion, error: validacionError } = await administrador.rpc(
      "validar_eliminacion_cuenta",
      { p_usuario_id: usuarioData.user.id, p_codigo: codigo },
    );
    if (validacionError) {
      return respuesta(400, {
        message: validacionError.message || "El código es incorrecto o expiró.",
      });
    }

    const archivos = Array.isArray(validacion?.archivos) ? validacion.archivos : [];
    const porBucket = new Map<string, string[]>();
    for (const item of archivos) {
      const bucket = String(item?.bucket ?? "");
      const nombre = String(item?.name ?? "");
      if (!bucket || !nombre) continue;
      porBucket.set(bucket, [...(porBucket.get(bucket) ?? []), nombre]);
    }
    for (const [bucket, nombres] of porBucket) {
      for (let inicio = 0; inicio < nombres.length; inicio += 1000) {
        const { error } = await administrador.storage
          .from(bucket)
          .remove(nombres.slice(inicio, inicio + 1000));
        if (error) throw new Error("No se pudieron eliminar los archivos de la cuenta.");
      }
    }

    const { data: preparacion, error: preparacionError } = await administrador.rpc(
      "preparar_eliminacion_cuenta",
      { p_usuario_id: usuarioData.user.id },
    );
    if (preparacionError) throw preparacionError;

    const { error: eliminarError } =
      await administrador.auth.admin.deleteUser(usuarioData.user.id, false);
    if (eliminarError) throw eliminarError;

    return respuesta(200, {
      eliminado: true,
      salas_eliminadas: preparacion?.salas_eliminadas ?? 0,
      salas_transferidas: preparacion?.salas_transferidas ?? 0,
    });
  } catch (error) {
    const mensaje = error instanceof Error
      ? error.message
      : "No se pudo eliminar la cuenta.";
    return respuesta(500, { message: mensaje });
  }
});