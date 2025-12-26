#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import subprocess
from pathlib import Path


def _fc_match(font_pattern: str) -> Path | None:
    """
    Find a font file path via fontconfig (fc-match).
    Returns None if fontconfig can't find it.
    """
    try:
        proc = subprocess.run(
            ["fc-match", "-f", "%{file}\n", font_pattern],
            check=True,
            capture_output=True,
            text=True,
        )
    except Exception:
        return None

    p = proc.stdout.strip()
    if not p:
        return None
    path = Path(p)
    return path if path.exists() else None


def _draw_text(draw, xy, text, font, fill, bold_px: int = 0):
    # Cheap bold: draw multiple times with small offsets.
    if bold_px <= 0:
        draw.text(xy, text, font=font, fill=fill)
        return
    x, y = xy
    for dx in range(0, bold_px + 1):
        for dy in range(0, bold_px + 1):
            draw.text((x + dx, y + dy), text, font=font, fill=fill)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Render tech poster to PNG. For guaranteed Chinese + FangZheng YaHei, place font at poster/fonts/FZYaHei.ttf"
    )
    parser.add_argument("--input", default=str(Path(__file__).with_name("background.svg")), help="Input background SVG path")
    parser.add_argument("--output", default=str(Path(__file__).with_name("out") / "poster.png"), help="Output PNG path")
    parser.add_argument(
        "--font",
        default=str(Path(__file__).with_name("fonts") / "FZYaHei.ttf"),
        help="FangZheng YaHei font file path (ttf/otf). If exists, it will be used for all Chinese text.",
    )
    parser.add_argument(
        "--fallback-font",
        default="Noto Sans CJK SC",
        help="Fontconfig pattern for fallback (used when --font does not exist)",
    )
    parser.add_argument("--width", type=int, default=1080, help="Output width in pixels")
    parser.add_argument("--dpi", type=float, default=96.0, help="Output DPI (affects text metrics in some renderers)")
    args = parser.parse_args()

    in_path = Path(args.input)
    out_path = Path(args.output)
    font_path = Path(args.font)

    if not in_path.exists():
        raise SystemExit(f"Input SVG not found: {in_path}")

    # 1) Render background SVG (no text) to PNG.
    import cairosvg  # type: ignore

    svg_bytes = in_path.read_bytes()
    bg_png = cairosvg.svg2png(bytestring=svg_bytes, output_width=args.width, dpi=args.dpi)

    # 2) Draw all text using PIL with a real font file (so Chinese will not become tofu).
    from PIL import Image, ImageDraw, ImageFont  # type: ignore

    img = Image.open(io.BytesIO(bg_png)).convert("RGBA")
    draw = ImageDraw.Draw(img)

    if font_path.exists():
        chosen_font = font_path
        print(f"[render] Using FangZheng YaHei font: {chosen_font}")
    else:
        matched = _fc_match(args.fallback_font)
        if not matched:
            raise SystemExit(
                f"Font not found: {font_path}. Also failed to find fallback via fc-match: {args.fallback_font}"
            )
        chosen_font = matched
        print(f"[render] FangZheng font missing; using fallback: {chosen_font}")

    # Fonts
    title_font = ImageFont.truetype(str(chosen_font), 92)
    subtitle_font = ImageFont.truetype(str(chosen_font), 28)
    body_font = ImageFont.truetype(str(chosen_font), 40)
    chip_font = ImageFont.truetype(str(chosen_font), 22)

    # Colors
    white = (234, 244, 255, 255)
    white_92 = (234, 244, 255, int(255 * 0.92))
    white_78 = (234, 244, 255, int(255 * 0.78))
    white_62 = (234, 244, 255, int(255 * 0.62))

    # Chips
    _draw_text(draw, (110, 150), "TECH POSTER", chip_font, (234, 244, 255, int(255 * 0.88)), bold_px=1)
    _draw_text(draw, (390, 150), "MODEL EVOLUTION", chip_font, (234, 244, 255, int(255 * 0.88)), bold_px=1)

    # Title block
    _draw_text(draw, (90, 235), "AI编程大模型", title_font, white, bold_px=1)
    _draw_text(draw, (90, 355), "从 3.5 到 4.5，编程能力跃迁", subtitle_font, white_78, bold_px=0)

    # Body (keep the exact sentence, line-broken for layout)
    _draw_text(draw, (90, 565), "AI编程大模型在今年一路从Claude 3.5", body_font, white_92, bold_px=0)
    _draw_text(draw, (90, 625), "升级到3.7、4.0、4.5，", body_font, white_92, bold_px=0)
    _draw_text(draw, (90, 685), "编程能力得到巨大的提升。", body_font, white_92, bold_px=0)

    # Timeline labels
    _draw_text(draw, (180, 970), "升级路径", subtitle_font, white_62, bold_px=0)
    _draw_text(draw, (170, 1105), "Claude 3.5", subtitle_font, white_78, bold_px=0)
    _draw_text(draw, (400, 1105), "3.7", subtitle_font, white_78, bold_px=0)
    _draw_text(draw, (620, 1105), "4.0", subtitle_font, white_78, bold_px=0)
    _draw_text(draw, (860, 1105), "4.5", subtitle_font, white_78, bold_px=0)

    # Footer
    _draw_text(draw, (90, 1805), "2025 - AI Software Engineering", subtitle_font, white_62, bold_px=0)
    # Right aligned "vNEXT"
    footer_right = "vNEXT"
    bbox = draw.textbbox((0, 0), footer_right, font=subtitle_font)
    _draw_text(draw, (990 - (bbox[2] - bbox[0]), 1805), footer_right, subtitle_font, white_62, bold_px=0)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)
    print(f"[render] Wrote: {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

