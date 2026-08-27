#!/usr/bin/env python3
"""Generate SAM launcher icons (legacy PNGs + adaptive-icon foreground PNGs).

Pure-Python PNG writer (no Pillow required). White background with a compact
bold black "SAM" wordmark drawn from 5x7 bitmap glyphs. Run from the
repository root:

    python3 scripts/generate_launcher_icons.py
"""

import struct
import zlib
from pathlib import Path

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)

# Density bucket -> legacy icon size in pixels
LEGACY_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Density bucket -> adaptive-icon foreground size in pixels (108dp canvas)
FOREGROUND_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

# 5x7 bitmap glyphs (rows top-to-bottom, '#' = ink)
GLYPHS = {
    "S": [
        ".####",
        "#....",
        "#....",
        ".###.",
        "....#",
        "....#",
        "####.",
    ],
    "A": [
        ".###.",
        "#...#",
        "#...#",
        "#####",
        "#...#",
        "#...#",
        "#...#",
    ],
    "M": [
        "#...#",
        "##.##",
        "#.#.#",
        "#.#.#",
        "#...#",
        "#...#",
        "#...#",
    ],
}

WORDMARK = "SAM"
GLYPH_W = 5
GLYPH_H = 7
LETTER_SPACING = 2  # in glyph units


def png_bytes(width: int, height: int, pixel_fn) -> bytes:
    """Encode an RGBA image. pixel_fn(x, y) -> (r, g, b, a)."""
    def chunk(kind: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    rows = []
    for y in range(height):
        row = bytearray(b"\x00")
        for x in range(width):
            row.extend(pixel_fn(x, y))
        rows.append(bytes(row))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
        + chunk(b"IEND", b"")
    )


def in_rounded_rect(x: int, y: int, w: int, h: int, r: float) -> bool:
    if x < 0 or y < 0 or x >= w or y >= h:
        return False

    def in_corner(cx: float, cy: float) -> bool:
        return (x - cx) ** 2 + (y - cy) ** 2 <= r * r

    if x < r and y < r:
        return in_corner(r, r)
    if x >= w - r and y < r:
        return in_corner(w - r, r)
    if x < r and y >= h - r:
        return in_corner(r, h - r)
    if x >= w - r and y >= h - r:
        return in_corner(w - r, h - r)
    return True


def wordmark_fn(size: int, width_fraction: float):
    """Return a predicate (x, y) -> True when the pixel is wordmark ink.

    The "SAM" glyphs are scaled so the whole wordmark spans
    ``width_fraction`` of the canvas, centered both ways.
    """
    total_units = len(WORDMARK) * GLYPH_W + (len(WORDMARK) - 1) * LETTER_SPACING
    unit = size * width_fraction / total_units
    text_w = total_units * unit
    text_h = GLYPH_H * unit
    origin_x = (size - text_w) / 2.0
    origin_y = (size - text_h) / 2.0

    def is_ink(x: float, y: float) -> bool:
        lx = (x + 0.5 - origin_x) / unit
        ly = (y + 0.5 - origin_y) / unit
        if ly < 0 or ly >= GLYPH_H or lx < 0 or lx >= total_units:
            return False
        row = GLYPHS and int(ly)
        col = int(lx)
        letter_index = col // (GLYPH_W + LETTER_SPACING)
        within = col % (GLYPH_W + LETTER_SPACING)
        if within >= GLYPH_W:
            return False  # letter spacing gap
        glyph = GLYPHS[WORDMARK[letter_index]]
        return glyph[row][within] == "#"

    return is_ink


def legacy_icon(size: int, round_: bool) -> bytes:
    """White rounded-square (or circle) icon with the black SAM wordmark."""
    radius = size * 0.20
    center = size / 2.0
    is_ink = wordmark_fn(size, width_fraction=0.72)

    def pixel(x: int, y: int):
        if round_:
            inside = (x - center) ** 2 + (y - center) ** 2 <= (size / 2.0) ** 2
        else:
            inside = in_rounded_rect(x, y, size, size, radius)
        if not inside:
            return (0, 0, 0, 0)
        color = BLACK if is_ink(x, y) else WHITE
        return (*color, 255)

    return png_bytes(size, size, pixel)


def foreground_icon(size: int) -> bytes:
    """Transparent adaptive-icon foreground with the black SAM wordmark.

    The wordmark is kept inside the central safe zone (~66% of the 108dp
    canvas) so launcher masks never clip it.
    """
    is_ink = wordmark_fn(size, width_fraction=0.58)

    def pixel(x: int, y: int):
        if is_ink(x, y):
            return (*BLACK, 255)
        return (0, 0, 0, 0)

    return png_bytes(size, size, pixel)


def write_assets() -> None:
    root = Path(__file__).resolve().parent.parent
    res = root / "android" / "app" / "src" / "main" / "res"

    for bucket, size in LEGACY_SIZES.items():
        (res / bucket).mkdir(parents=True, exist_ok=True)
        (res / bucket / "ic_launcher.png").write_bytes(legacy_icon(size, round_=False))
        (res / bucket / "ic_launcher_round.png").write_bytes(legacy_icon(size, round_=True))
        print(f"  generated {bucket}/ic_launcher*.png ({size}px)")

    for bucket, size in FOREGROUND_SIZES.items():
        (res / bucket).mkdir(parents=True, exist_ok=True)
        (res / bucket / "ic_launcher_foreground.png").write_bytes(foreground_icon(size))
        print(f"  generated {bucket}/ic_launcher_foreground.png ({size}px)")

    print("Launcher icons written.")


if __name__ == "__main__":
    write_assets()
