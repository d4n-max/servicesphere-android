from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "play-store-assets" / "servicesphere-feature-graphic.png"

W, H = 1024, 500
PURPLE = (124, 58, 237)
INDIGO = (67, 56, 202)
INK = (15, 23, 42)
MUTED = (100, 116, 139)
CARD = (255, 255, 255)


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(f"C:/Windows/Fonts/{name}", size)


FONT_BLACK = font("arialbd.ttf", 56)
FONT_HEAVY = font("arialbd.ttf", 42)
FONT_HEAD = font("arialbd.ttf", 50)
FONT_BOLD = font("arialbd.ttf", 24)
FONT_MED = font("arial.ttf", 24)
FONT_SMALL_BOLD = font("arialbd.ttf", 19)
FONT_SMALL = font("arial.ttf", 18)


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius, fill=255)
    return mask


def shadow(canvas: Image.Image, box: tuple[int, int, int, int], radius: int, alpha: int = 70, blur: int = 20) -> None:
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.rounded_rectangle(box, radius, fill=(15, 23, 42, alpha))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def gradient_background() -> Image.Image:
    img = Image.new("RGBA", (W, H))
    px = img.load()
    for y in range(H):
        for x in range(W):
            t = x / (W - 1)
            v = y / (H - 1)
            r = int(PURPLE[0] * (1 - t) + INDIGO[0] * t)
            g = int(PURPLE[1] * (1 - t) + INDIGO[1] * t)
            b = int(PURPLE[2] * (1 - t) + INDIGO[2] * t)
            lift = int(28 * (1 - v))
            px[x, y] = (min(r + lift, 255), min(g + lift, 255), min(b + lift, 255), 255)
    return img


def draw_icon_badge(canvas: Image.Image) -> None:
    icon = Image.open(ROOT / "play-store-assets" / "servicesphere-play-icon-512.png").convert("RGBA")
    icon = icon.resize((70, 70), Image.Resampling.LANCZOS)
    icon.putalpha(rounded_mask((70, 70), 18))
    shadow(canvas, (58, 38, 128, 108), 18, alpha=45, blur=14)
    canvas.alpha_composite(icon, (58, 38))
    d = ImageDraw.Draw(canvas)
    d.text((144, 48), "ServiceSphere", font=FONT_BOLD, fill=(255, 255, 255))
    d.text((144, 77), "for field service teams", font=FONT_SMALL, fill=(224, 231, 255))


def draw_text(canvas: Image.Image) -> None:
    d = ImageDraw.Draw(canvas)
    d.text((58, 146), "Jobs, Quotes &", font=FONT_HEAD, fill=(255, 255, 255))
    d.text((58, 203), "Invoices", font=FONT_HEAD, fill=(255, 255, 255))
    d.text((58, 260), "from your phone", font=FONT_HEAVY, fill=(237, 233, 254))
    d.text((60, 324), "Built for solo trades and small service crews", font=FONT_MED, fill=(224, 231, 255))


def draw_support_card(canvas: Image.Image, x: int, y: int, label: str, mark: str, accent: tuple[int, int, int]) -> None:
    card_w = 186
    card_h = 58
    icon_size = 32
    gap = 12
    shadow(canvas, (x, y, x + card_w, y + card_h), 16, alpha=35, blur=12)
    d = ImageDraw.Draw(canvas)
    d.rounded_rectangle((x, y, x + card_w, y + card_h), 16, fill=(255, 255, 255, 238))

    text_box = d.textbbox((0, 0), label, font=FONT_SMALL_BOLD)
    mark_box = d.textbbox((0, 0), mark, font=FONT_SMALL_BOLD)
    text_w = text_box[2] - text_box[0]
    text_h = text_box[3] - text_box[1]
    mark_w = mark_box[2] - mark_box[0]
    mark_h = mark_box[3] - mark_box[1]
    group_w = icon_size + gap + text_w
    group_x = x + (card_w - group_w) // 2
    icon_x = group_x
    icon_y = y + (card_h - icon_size) // 2
    text_x = icon_x + icon_size + gap
    text_y = y + (card_h - text_h) // 2 - 2

    d.ellipse((icon_x, icon_y, icon_x + icon_size, icon_y + icon_size), fill=accent)
    d.text((icon_x + (icon_size - mark_w) / 2, icon_y + (icon_size - mark_h) / 2 - 2), mark, font=FONT_SMALL_BOLD, fill=(255, 255, 255))
    d.text((text_x, text_y), label, font=FONT_SMALL_BOLD, fill=INK)


def draw_phone(canvas: Image.Image) -> None:
    outer = (710, 18, 972, 488)
    inner_x, inner_y = 728, 37
    inner_w, inner_h = 226, 432
    shadow(canvas, outer, 44, alpha=95, blur=24)
    d = ImageDraw.Draw(canvas)
    d.rounded_rectangle(outer, 44, fill=(14, 18, 36))
    d.rounded_rectangle((697, 30, 935, 476), 36, fill=(245, 247, 251))

    shot = Image.open(ROOT / "task18-dashboard4.png").convert("RGBA")
    ratio = inner_w / shot.width
    scaled_h = int(shot.height * ratio)
    shot = shot.resize((inner_w, scaled_h), Image.Resampling.LANCZOS)
    crop_top = 0
    screen = shot.crop((0, crop_top, inner_w, crop_top + inner_h))
    screen.putalpha(rounded_mask((inner_w, inner_h), 28))
    canvas.alpha_composite(screen, (inner_x, inner_y))

    d.rounded_rectangle((779, 30, 853, 38), 4, fill=(14, 18, 36))
    d.rounded_rectangle((697, 30, 935, 476), 36, outline=(255, 255, 255, 80), width=2)


def main() -> None:
    canvas = gradient_background()
    d = ImageDraw.Draw(canvas)

    draw_icon_badge(canvas)
    draw_text(canvas)
    draw_support_card(canvas, 58, 399, "Photo proof", "P", (34, 197, 94))
    draw_support_card(canvas, 250, 399, "Signature", "S", PURPLE)
    draw_support_card(canvas, 442, 399, "PDF invoices", "D", (14, 165, 233))
    draw_phone(canvas)

    d.rounded_rectangle((656, 118, 812, 176), 18, fill=(255, 255, 255, 244))
    d.text((676, 133), "3 jobs today", font=FONT_SMALL_BOLD, fill=INK)
    d.rounded_rectangle((620, 248, 796, 306), 18, fill=(255, 255, 255, 244))
    d.text((639, 263), "$1,240 unpaid", font=FONT_SMALL_BOLD, fill=INK)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(OUT, quality=95, optimize=True)
    print(OUT)


if __name__ == "__main__":
    main()
