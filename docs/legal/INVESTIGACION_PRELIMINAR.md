# Investigación preliminar (fuentes públicas) — para llevar al abogado

**No es asesoría legal.** Es un resumen de lo que dicen fuentes públicas, con enlaces, para que el abogado confirme qué aplica a Ruralitos. Algunas fuentes son resúmenes de estudios jurídicos y no el texto oficial; el abogado debe verificar contra el Registro Oficial. Investigado el 3 de octubre de 2026.

## 1. Datos de salud en la LOPDP (Ley Orgánica de Protección de Datos Personales)

- Los datos de salud son **datos sensibles / de categoría especial** (art. 30): exigen protección reforzada y una base de legitimación más estricta. Fuente: [LegalTech Ecuador](https://legaltech.ec/lopdp-ecuador).
- Las instituciones del Sistema Nacional de Salud y los profesionales de la salud pueden tratar los datos de salud de sus pacientes, cumpliendo los parámetros mínimos que fije la autoridad. Fuente: texto de la ley, [CPCCS](https://www.cpccs.gob.ec/wp-content/uploads/2025/07/LEY-ORGANICA-DE-PROTECCION-DE-DATOS.pdf) (leído por medio de un buscador; confirmar el artículo).
- Un resumen indica que las historias clínicas se conservan **15 años** desde la última atención; eso viene de normas sanitarias y conviene confirmarlo.
- La ley aplica a toda persona natural o jurídica que trate datos personales en Ecuador, sin excepción por tamaño. Los derechos de acceso, rectificación, eliminación y oposición se responden en **15 días**. Multas a entidades privadas: 0,1 %–0,7 % del volumen de negocio (leves) y 0,7 %–1 % (graves). Fuente: [LegalTech Ecuador](https://legaltech.ec/lopdp-ecuador).

**Qué implica para Ruralitos (a confirmar):** la app trata datos de salud, que es una actividad de riesgo alto. Hay que definir si Ruralitos es responsable o encargado y dejar por contrato quién atiende los derechos de los pacientes.

## 2. Delegado de protección de datos y registro

- La designación del delegado es obligatoria para el sector público, para determinados sectores privados (**salud**, finanzas, educación, telecomunicaciones, tecnología…) y para tratamientos a gran escala de datos sensibles. Fuentes: [LegalTech](https://legaltech.ec/lopdp-ecuador), [PBP Law](https://www.pbplaw.com/publicaciones/reglamento-del-delegado-de-proteccion-de-datos-personales-en-ecuador/).
- Reglamento del Delegado: Resolución SPDP-SPD-2025-0028-R (30 de julio de 2025). El delegado debe tener certificación oficial de la SPDP y actuar con independencia. El registro del sector privado tenía como fecha del **1 de noviembre al 31 de diciembre de 2025** (ya pasó). Fuente: [Lexis](https://www.lexis.com.ec/noticias/reglamento-del-delegado-de-proteccion-de-datos-personales-entra-en-vigencia-en-ecuador).
- Los responsables deben inscribir sus bases de datos en el Registro de Protección de Datos Personales (un resumen habla de 10 días desde el inicio del tratamiento). Fuente: [NMS](https://nmslaw.com.ec/blog/2023/11/08/ecuador-reglamento-lopdp-2023/).

**Pregunta clave:** ¿una app de fichas de salud usada por equipos de salud está obligada a tener delegado y registro, y desde cuándo?

## 3. Avisar de una vulneración de seguridad

- El responsable debe notificar a la Superintendencia (y a ARCOTEL cuando corresponda) **lo antes posible y a más tardar en 5 días** desde que la conoce. El encargado debe avisar al responsable en **2 días**. Fuente: [PwC Ecuador](https://www.pwc.ec/es/publicaciones/LOPDP_PwC_Ecuador.pdf) y resúmenes del reglamento.

**Qué implica:** hay que tener un procedimiento escrito de incidentes (quién avisa a quién y cuándo) antes de vender.

## 4. Datos en el extranjero (Supabase)

- La Resolución SPDP-SPD-2026-0004-R (expedida el 28 de enero de 2026, publicada el 13 de febrero de 2026) regula las transferencias nacionales e internacionales. Tres vías para enviar datos al exterior: (1) un país con **nivel adecuado** reconocido por la SPDP; (2) **garantías adecuadas**, como cláusulas contractuales tipo, normas corporativas, códigos de conducta o certificaciones; y (3) autorización excepcional de la SPDP con análisis de riesgos. Hay que conservar la documentación por 3 años. Fuentes: [NMS](https://nmslaw.com.ec/blog/2026/02/02/spdp-transferencias-comunicaciones-datos-personales-ecuador-internacionales/), [Lexis](https://www.lexis.com.ec/noticias/se-expide-norma-general-para-la-transferencia-nacional-e-internacional-de-datos-personales), [texto de la resolución](https://spdp.gob.ec/wp-content/uploads/2026/01/04.01.01-SPSP-SPD-2026-0004-R-Norma-general-de-transferencias-signed.pdf).
- Supabase tiene una región en Sudamérica (**São Paulo, `sa-east-1`**), ofrece un acuerdo de procesamiento de datos (DPA) y cumple SOC 2 Tipo 2. Supabase advierte que elegir región «es un control de ubicación, no una prueba de cumplimiento normativo». Fuentes: [regiones de Supabase](https://supabase.com/docs/guides/platform/regions), [seguridad de Supabase](https://supabase.com/security).

**Qué hacer:** comprobar en qué región está tu proyecto de Supabase (no pude verlo desde aquí) y preguntarle al abogado si basta el DPA de Supabase como «garantía adecuada» para sacar datos a ese país.

## 5. Formatos oficiales del Ministerio

- La ficha familiar y los formularios de la historia clínica única son formularios estandarizados del MSP (SNS-MSP/HCU), «acordados y difundidos por el Consejo Nacional de Salud» en instituciones públicas, semipúblicas y privadas. Fuente: [manual de uso de formularios](https://smart-medic.com/wp-content/uploads/2021/07/MANUAL-HISTORIA-CLINICA-MSP.pdf).
- **No encontré** una norma que diga si pueden reproducirse libremente en software de terceros. Queda como pregunta para el abogado y para el Ministerio.

## 6. Listado de establecimientos de salud

- El conjunto público más parecido (**Licenciamientos a Establecimientos de Salud**, de la ACESS) está en el portal de datos abiertos con licencia **Creative Commons No Comercial**. Fuente: [datos abiertos](https://www.datosabiertos.gob.ec/dataset/licenciamientos-a-establecimientos-de-salud).
- **Riesgo:** si el catálogo de centros que lleva la app (1.934 establecimientos con código UO) se obtuvo de una fuente con esa licencia, venderlo dentro de una app comercial podría no estar permitido. No sé de dónde salió ese archivo; hay que confirmarlo. Alternativas: que cada cliente escriba su centro, o pedir autorización a la entidad que lo publica.

## 7. CIE-10

- La OMS es propietaria de los derechos de la CIE-10; las adaptaciones requieren su autorización y las solicitudes de reproducción se dirigen a la OPS ([paho.org/permissions](https://www.paho.org/permissions)). Para la CIE-11, la OMS aclara que los usos comerciales están sujetos a sus derechos de autor. Fuentes: [OPS, volumen 2 de la CIE-10](https://ais.paho.org/classifications/chapters/pdf/volume2.pdf), [OMS, derechos de autor y licencias](https://www.who.int/es/about/policies/publishing/copyright).

**Riesgo:** la app incluye un catálogo de diagnósticos CIE-10 (`cie10.json`). Hay que confirmar con la OPS/OMS si basta su política de reproducción o hace falta una licencia para uso comercial.

## Resumen de riesgos, de mayor a menor

1. Delegado de protección de datos y registro de la base (posible obligación para el sector salud).
2. Datos fuera de Ecuador: qué región usa Supabase y qué garantía se necesita.
3. Licencia del catálogo de establecimientos (CC No Comercial si viene de ACESS).
4. Licencia de uso comercial de la CIE-10.
5. Reproducción de los formatos oficiales (sin norma clara encontrada).
6. Procedimiento de incidentes (5 días) y de atención a derechos (15 días).
