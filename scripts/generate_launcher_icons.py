#!/usr/bin/env python3
"""Generate ScriptHost launcher icons (legacy PNGs + adaptive-icon assets).

Pure-Python PNG writer (no Pillow required). Draws the brand-blue rounded
square with a white "run" triangle. Run from the repository root:

    python3 scripts/generate_launcher_icons.py
"""

import struct
import zlib
from pathlib import Path

BRAND_BLUE = (0, 122, 255)
WHITE = (255, 255, 255)

# Density bucket -> icon size in pixels
LEGACY_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


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


def in_triangle(x: float, y: float, a, b, c) -> bool:
    def sign(p1, p2, p3):
        return (p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1])

    d1 = sign((x, y), a, b)
    d2 = sign((x, y), b, c)
    d3 = sign((x, y), c, a)
    has_neg = d1 < 0 or d2 < 0 or d3 < 0
    has_pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (has_neg and has_pos)


def legacy_icon(size: int, round_: bool) -> bytes:
    """Rounded-square (or circle) blue icon with a white play triangle."""
    radius = size * 0.20
    center = size / 2.0
    tri = (
        (size * 0.42, size * 0.32),
        (size * 0.42, size * 0.68),
        (size * 0.72, size * 0.50),
    )

    def pixel(x: int, y: int):
        if round_:
            inside = (x - center) ** 2 + (y - center) ** 2 <= (size / 2.0) ** 2
        else:
            inside = in_rounded_rect(x, y, size, size, radius)
        if not inside:
            return (0, 0, 0, 0)
        color = WHITE if in_triangle(x + 0.5, y + 0.5, *tri) else BRAND_BLUE
        return (*color, 255)

    return png_bytes(size, size, pixel)


def write_assets() -> None:
    root = Path(__file__).resolve().parent.parent
    res = root / "android" / "app" / "src" / "main" / "res"

    for bucket, size in LEGACY_SIZES.items():
        (res / bucket).mkdir(parents=True, exist_ok=True)
        (res / bucket / "ic_launcher.png").write_bytes(legacy_icon(size, round_=False))
        (res / bucket / "ic_launcher_round.png").write_bytes(legacy_icon(size, round_=True))
        print(f"  generated {bucket}/ic_launcher*.png ({size}px)")

    print("Launcher icons written.")


if __name__ == "__main__":
    write_assets()
