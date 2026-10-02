"""Extrae sin redibujar los pictogramas entregados por el usuario.

Las láminas originales quedan en design/iconos_riesgo. Los recortes son
recursos Android de 256 px, transparentes y centrados, para conservar el estilo
exacto en la interfaz y en el marcador del mapa aun sin conexión.
"""

from pathlib import Path
from PIL import Image, ImageDraw


RAIZ = Path(__file__).resolve().parents[1]
ORIGEN = RAIZ / "design" / "iconos_riesgo"
DESTINO = RAIZ / "app" / "src" / "main" / "res" / "drawable-nodpi"

# (lámina, caja de contenido). Las cajas se midieron sobre los píxeles opacos
# de cada figura; no abarcan íconos vecinos.
ICONOS = {
    "riesgo_i_0_23": ("grupo_i.png", (44, 162, 606, 642)),
    "riesgo_i_2_9": ("grupo_i.png", (626, 162, 1238, 632)),
    "riesgo_i_embarazo": ("grupo_i.png", (476, 682, 802, 1198)),
    "riesgo_ii_0_23": ("grupo_ii.png", (124, 60, 590, 430)),
    "riesgo_ii_2_9": ("grupo_ii.png", (712, 90, 1222, 426)),
    "riesgo_ii_10_19": ("grupo_ii.png", (122, 482, 602, 820)),
    "riesgo_ii_20_64": ("grupo_ii.png", (688, 476, 1172, 824)),
    "riesgo_ii_65_mas": ("grupo_ii.png", (118, 856, 608, 1200)),
    "riesgo_ii_embarazo": ("grupo_ii.png", (804, 838, 1070, 1218)),
    "riesgo_iii_0_23": ("grupo_iii.png", (40, 164, 392, 432)),
    "riesgo_iii_2_9": ("grupo_iii.png", (420, 174, 820, 434)),
    "riesgo_iii_10_19": ("grupo_iii.png", (838, 108, 1220, 434)),
    "riesgo_iii_20_64": ("grupo_iii.png", (180, 488, 610, 810)),
    "riesgo_iii_65_mas": ("grupo_iii.png", (686, 508, 1088, 810)),
    "riesgo_iii_embarazo": ("grupo_iii.png", (302, 824, 552, 1208)),
    "riesgo_iii_embarazo_adolescente": ("grupo_iii.png", (742, 830, 986, 1210)),
    "riesgo_iv_fisica": ("grupo_iv.png", (92, 66, 640, 668)),
    "riesgo_iv_apoyo": ("grupo_iv.png", (734, 84, 1176, 658)),
    "riesgo_iv_auditiva": ("grupo_iv.png", (120, 706, 640, 1178)),
    "riesgo_iv_intelectual": ("grupo_iv.png", (722, 692, 1174, 1186)),
    "riesgo_desnutricion_cronica": ("estrategias.png", (106, 68, 362, 432)),
    "riesgo_tuberculosis": ("estrategias.png", (446, 94, 814, 424)),
    "riesgo_salud_mental": ("estrategias.png", (882, 82, 1188, 436)),
    "riesgo_diabetes": ("estrategias.png", (62, 516, 394, 868)),
    "riesgo_paliativos": ("estrategias.png", (470, 508, 838, 844)),
    "riesgo_vih": ("estrategias.png", (916, 496, 1154, 844)),
    "riesgo_hipertension": ("estrategias.png", (442, 870, 854, 1210)),
}


def extraer() -> None:
    DESTINO.mkdir(parents=True, exist_ok=True)
    laminas = {nombre: Image.open(ORIGEN / nombre).convert("RGBA") for nombre, _ in ICONOS.values()}
    for nombre, (lamina, caja) in ICONOS.items():
        original = laminas[lamina]
        assert original.size == (1254, 1254), lamina
        recorte = original.crop(caja)
        if nombre == "riesgo_paliativos":
            # La lámina tiene tres puntos sueltos del pictograma vecino.
            ImageDraw.Draw(recorte).rectangle((0, 0, 74, 65), fill=(0, 0, 0, 0))
        recorte.thumbnail((232, 232), Image.Resampling.LANCZOS)
        icono = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        icono.alpha_composite(recorte, ((256 - recorte.width) // 2, (256 - recorte.height) // 2))
        icono.save(DESTINO / f"{nombre}.png", optimize=True)
    for lamina in laminas.values():
        lamina.close()
    print(f"{len(ICONOS)} íconos extraídos en {DESTINO}")


if __name__ == "__main__":
    extraer()
