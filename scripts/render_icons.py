"""Offline block icon renderer: reproduces StarMade's IconTextureBakery look without running the game.

    python scripts/render_icons.py            # render LuaMade block icons -> src/main/resources/icons/blocks_0.png
    python scripts/render_icons.py --check    # compare against vanilla's baked sheets (calibration test)
    python scripts/render_icons.py --starmade-master PATH   # default: ../StarMade-Master

Cube blocks are calibrated against the vanilla sheets re-baked with the engine's baker (build-icons-00/05/06):
orthographic 3/4 view, face texture mapping, the engine's texture inset, per-face light and glow from the _NRM
alpha. Model (LOD) blocks reuse the same camera and lighting, blended by surface normal, with the LOD shader's
max(glow, lit) glow; they're turned front-left and framed to fill the cell so small face panels stay readable.
"""
import argparse
import math
import os
import re
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
SS = 4      # supersampling per output pixel
CELL = 64   # StarLoader icon size

# ---- calibration (fitted with --check's data against the re-baked vanilla sheets) ----
# IconTextureBakery's cube chain is rotY(180) * rotX(-20) * rotY(135) at 32px/unit; the shipped sheets fit
# slightly off that, so these are the measured values.
CAM = dict(yaw=137.25, pitch=-21.125, scale=32.25, dx=0.031, dy=0.062)
INSET = 0.072   # the engine samples each 256px tile inset by this fraction per side
# visible face -> (block side index FRONT,BACK,TOP,BOTTOM,RIGHT,LEFT ; texture orientation, see DIHEDRAL)
FACE_MAP = {'+x': (4, 2), '+y': (2, 0), '+z': (0, 6)}
LIGHT = {'+x': (0.030, 1.347), '+y': (-0.002, 1.668), '+z': (0.064, 1.160)}  # out = a + k * tex
GLOW = 1.029    # cube shader: lit * (1 - e) + GLOW * tex * e, e = (nrm.a - 0.5) * 2


def rx(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


VIEW = ry(180) @ rx(CAM['pitch']) @ ry(CAM['yaw'])
FACES = {  # normal, u axis, v axis
    '+x': (np.array([1., 0, 0]), np.array([0., 0, 1]), np.array([0., 1, 0])),
    '+y': (np.array([0., 1, 0]), np.array([1., 0, 0]), np.array([0., 0, 1])),
    '+z': (np.array([0., 0, 1]), np.array([1., 0, 0]), np.array([0., 1, 0])),
}
DIHEDRAL = [lambda a, b: (a, b), lambda a, b: (1 - b, a), lambda a, b: (1 - a, 1 - b), lambda a, b: (b, 1 - a),
            lambda a, b: (1 - a, b), lambda a, b: (b, a), lambda a, b: (a, 1 - b), lambda a, b: (1 - b, 1 - a)]


def sample(tex, s, t):
    """Bilinear sample of tex (H,W,C) at normalized s (x) and t (y, 0 = top row)."""
    h, w = tex.shape[:2]
    x = np.clip(s * w - 0.5, 0, w - 1); y = np.clip(t * h - 0.5, 0, h - 1)
    x0 = np.floor(x).astype(int); y0 = np.floor(y).astype(int)
    x1 = np.minimum(x0 + 1, w - 1); y1 = np.minimum(y0 + 1, h - 1)
    fx = (x - x0)[..., None]; fy = (y - y0)[..., None]
    return (tex[y0, x0] * (1 - fx) + tex[y0, x1] * fx) * (1 - fy) + (tex[y1, x0] * (1 - fx) + tex[y1, x1] * fx) * fy


def to_screen(p, scale=None, center=(0, 0)):
    sc = CAM['scale'] if scale is None else scale
    return np.stack([(32 + CAM['dx'] + sc * (p[..., 0] - center[0])) * SS,
                     (32 + CAM['dy'] - sc * (p[..., 1] - center[1])) * SS], -1)


def downsample(img):
    h, w = img.shape[:2]
    return img.reshape(h // SS, SS, w // SS, SS, -1).mean((1, 3))


def finish(img):
    """Supersampled premultiplied RGBA -> 64x64 straight RGBA with the bakery's hard edge and 1px outline."""
    small = downsample(img)
    a = small[..., 3]
    rgb = np.where(a[..., None] > 0, small[..., :3] / np.maximum(a[..., None], 1e-6), 0)
    drawn = a >= 0.5  # the GL bake has no edge antialiasing
    out = np.zeros((CELL, CELL, 4))
    out[drawn, :3] = np.clip(rgb[drawn], 0, 1); out[drawn, 3] = 1
    edge = np.zeros_like(drawn)  # IconTextureBakery.outlineCell
    edge[1:] |= drawn[:-1]; edge[:-1] |= drawn[1:]; edge[:, 1:] |= drawn[:, :-1]; edge[:, :-1] |= drawn[:, 1:]
    out[edge & ~drawn] = (0, 0, 0, 1)
    return out


# ---------------------------------------------------------------- cubes
def render_cube(faces):
    """faces: 6 (rgba_tile, nrm_tile) pairs in block side order FRONT, BACK, TOP, BOTTOM, RIGHT, LEFT."""
    W = CELL * SS
    ys, xs = np.mgrid[0:W, 0:W] + 0.5
    img = np.zeros((W, W, 4))
    for f, (side, orient) in FACE_MAP.items():
        n, u, v = FACES[f]
        o = to_screen(VIEW @ (n * .5 - u * .5 - v * .5))
        eu = to_screen(VIEW @ (n * .5 + u * .5 - v * .5)) - o
        ev = to_screen(VIEW @ (n * .5 - u * .5 + v * .5)) - o
        ab = np.stack([xs - o[0], ys - o[1]], -1) @ np.linalg.inv(np.array([eu, ev]).T).T
        inside = (ab[..., 0] >= 0) & (ab[..., 0] <= 1) & (ab[..., 1] >= 0) & (ab[..., 1] <= 1)
        a, b = DIHEDRAL[orient](ab[..., 0], ab[..., 1])
        a = INSET + a * (1 - 2 * INSET); b = INSET + b * (1 - 2 * INSET)
        tex, nrm = faces[side]
        t = sample(tex, a, b)
        e = np.clip((sample(nrm[..., 3:4], a, b) - 0.5) * 2, 0, 1)
        la, lk = LIGHT[f]
        col = (la + lk * t[..., :3]) * (1 - e) + GLOW * t[..., :3] * e
        img[inside, :3] = col[inside] * t[inside, 3:4]
        img[inside, 3] = t[inside, 3]
    return finish(img)


# ---------------------------------------------------------------- models
def load_mesh(path):
    """Ogre mesh.xml -> list of (positions, normals, uvs, faces)."""
    s = open(path, encoding='utf-8', errors='ignore').read()
    out = []
    for sm in re.finditer(r'<submesh [^>]*>(.*?)</submesh>', s, re.S):
        body = sm.group(1)
        F = np.array(re.findall(r'<face v1="(\d+)" v2="(\d+)" v3="(\d+)"', body), int)
        P = np.array(re.findall(r'<position x="([^"]+)" y="([^"]+)" z="([^"]+)"', body), float)
        N = np.array(re.findall(r'<normal x="([^"]+)" y="([^"]+)" z="([^"]+)"', body), float)
        T = np.array(re.findall(r'<texcoord u="([^"]+)" v="([^"]+)"', body), float)
        out.append((P, N, T, F))
    return out


def rasterize(xy, z, attrs, size):
    """Z-buffered triangles: xy (T,3,2) px, z (T,3) larger = nearer, attrs (T,3,K) -> (image, coverage)."""
    zbuf = np.full((size, size), -np.inf)
    img = np.zeros((size, size, attrs.shape[2]))
    for t in range(len(xy)):
        (ax, ay), (bx, by), (cx, cy) = xy[t]
        den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(den) < 1e-12:
            continue
        x0, y0 = np.floor(xy[t].min(0)).astype(int).clip(0, size - 1)
        x1, y1 = np.ceil(xy[t].max(0)).astype(int).clip(0, size - 1)
        ys, xs = np.mgrid[y0:y1 + 1, x0:x1 + 1] + 0.5
        w0 = ((by - cy) * (xs - cx) + (cx - bx) * (ys - cy)) / den
        w1 = ((cy - ay) * (xs - cx) + (ax - cx) * (ys - cy)) / den
        w2 = 1 - w0 - w1
        zz = w0 * z[t, 0] + w1 * z[t, 1] + w2 * z[t, 2]
        sub = zbuf[y0:y1 + 1, x0:x1 + 1]
        upd = (w0 >= 0) & (w1 >= 0) & (w2 >= 0) & (zz > sub)
        if upd.any():
            sub[upd] = zz[upd]
            a = attrs[t]
            img[y0:y1 + 1, x0:x1 + 1][upd] = (w0[..., None] * a[0] + w1[..., None] * a[1] + w2[..., None] * a[2])[upd]
    return img, np.isfinite(zbuf)


def face_light(n):
    """Cube face light blended by view-space normal (squared-cosine weights over the three lit faces)."""
    w, a, k = [], 0, 0
    for f in FACE_MAP:
        d = VIEW @ FACES[f][0]
        w.append(np.clip(n @ d, 0, None) ** 2)
    w = np.stack(w, -1); w = w / np.maximum(w.sum(-1, keepdims=True), 1e-6)
    for i, f in enumerate(FACE_MAP):
        a = a + w[..., i] * LIGHT[f][0]; k = k + w[..., i] * LIGHT[f][1]
    k = np.where(w.sum(-1) > 0, k, min(v[1] for v in LIGHT.values()))
    return a, k


def render_model(mesh_xml, tex, emissive=None, front_yaw=180.0):
    """front_yaw turns the mesh so its front faces the icon's front-left (cube FRONT) face. The model is
    centred and scaled to fill the cell like a full block."""
    subs = load_mesh(mesh_xml)
    M = VIEW @ ry(front_yaw)
    P = np.concatenate([s[0] for s in subs])
    p_all = (M @ P.T).T
    lo, hi = p_all[:, :2].min(0), p_all[:, :2].max(0)
    centre = (lo + hi) / 2
    cube = (VIEW @ (np.array(np.meshgrid([-.5, .5], [-.5, .5], [-.5, .5])).reshape(3, -1))).T
    fill = (cube[:, :2].max(0) - cube[:, :2].min(0)).max() / (hi - lo).max()
    xy, zz, at = [], [], []
    for P, N, T, F in subs:
        p = (M @ P.T).T; n = (M @ N.T).T
        xy.append(to_screen(p, CAM['scale'] * fill, centre)[F]); zz.append(p[:, 2][F])
        at.append(np.concatenate([T, n], 1)[F])
    W = CELL * SS
    att, cov = rasterize(np.concatenate(xy), np.concatenate(zz), np.concatenate(at), W)
    t = sample(tex, att[..., 0], att[..., 1])
    n = att[..., 2:5]; n = n / np.maximum(np.linalg.norm(n, axis=-1, keepdims=True), 1e-6)
    la, lk = face_light(n)
    lit = la[..., None] + lk[..., None] * t[..., :3]
    if emissive is not None:  # lodcube.frag: max(emissive * tex, lit)
        e = sample(emissive[..., :1], att[..., 0], att[..., 1])
        lit = np.maximum(e * t[..., :3] * GLOW, lit)
    img = np.zeros((W, W, 4))
    img[cov, :3] = lit[cov]; img[cov, 3] = 1
    return finish(img)


# ---------------------------------------------------------------- texture sources
def load_rgba(path):
    return np.asarray(Image.open(path).convert('RGBA')).astype(np.float32) / 255


class Sheets:
    def __init__(self, starmade_data):
        self.data = starmade_data
        self.cache = {}

    def _sheet(self, path):
        if path not in self.cache:
            self.cache[path] = load_rgba(path)
        return self.cache[path]

    def vanilla(self, tid):
        base = os.path.join(self.data, 'textures', 'block', 'Default', '256', 't%03d' % (tid // 256))
        return self._tile(self._sheet(base + '.png'), tid % 256), self._tile(self._sheet(base + '_NRM.png'), tid % 256)

    def mod(self, slot):
        base = os.path.join(RES, 'textures', 'textures_0')
        return self._tile(self._sheet(base + '.png'), slot), self._tile(self._sheet(base + '_NRM.png'), slot)

    @staticmethod
    def _tile(sheet, i):
        t = sheet.shape[0] // 16
        return sheet[i // 16 * t:(i // 16 + 1) * t, i % 16 * t:(i % 16 + 1) * t]


# ---------------------------------------------------------------- LuaMade blocks
V = lambda tid: ('v', tid)  # vanilla texture id; plain ints are textures_0 slots
CASING = V(377)
# Block icon order in icons/blocks_0.png (packed, no holes); must match ResourceManager.BlockIcons.
# Cube faces mirror ResourceManager.Textures (FRONT, BACK, TOP, BOTTOM, RIGHT, LEFT).
LUAMADE = [
    ('COMPUTER', 'model', 'Computer', 0.0),
    ('MODEM', 'model', 'Modem', 180.0),
    ('DISK_DRIVE', 'cube', [0, 1, 2, CASING, 3, 4]),
    ('REMOTE_ACCESS_POINT', 'model', 'RemoteAccessPoint', 180.0),
    ('DATA_STORE', 'cube', [6, 6, 5, CASING, 6, 6]),
    ('NETWORKED_DATA_STORE', 'cube', [8, 8, 7, CASING, 8, 8]),
    ('PASSWORD_PERMISSION_MODULE', 'cube', [9] * 6),
    ('VAULT', 'cube', [10, 11, 12, CASING, 11, 11]),
    ('PROJECTOR', 'model', 'Projector', 180.0),
]


def render_luamade(sheets):
    out = []
    for entry in LUAMADE:
        if entry[1] == 'cube':
            faces = [sheets.vanilla(f[1]) if isinstance(f, tuple) else sheets.mod(f) for f in entry[2]]
            out.append((entry[0], render_cube(faces)))
        else:
            name, yaw = entry[2], entry[3]
            d = os.path.join(RES, 'models', name)
            em = os.path.join(d, name + '_EM.png')
            out.append((entry[0], render_model(os.path.join(d, name + '.mesh.xml'), load_rgba(os.path.join(d, name + '.png')),
                                               load_rgba(em) if os.path.exists(em) else None, yaw)))
    return out


# ---------------------------------------------------------------- calibration check
CHECK = ['Tactical Scanner Computer', 'Cloaker', 'RadarJammer', 'BOBBY AI Module', 'Loot Box', 'Logic Beam', 'Astrotech Module']


def check(sheets):
    cfg = open(os.path.join(sheets.data, 'config', 'BlockConfig.xml'), encoding='utf-8', errors='ignore').read()
    worst = 0
    for name in CHECK:
        m = re.search(r'<Block icon="(\d+)" name="%s" textureId="([^"]+)"' % re.escape(name), cfg)
        icon, ids = int(m.group(1)), [int(t) for t in m.group(2).split(',')]
        ref = sheets._tile(sheets._sheet(os.path.join(sheets.data, 'image-resource', 'build-icons-%02d-16x16-gui-.png' % (icon // 256))), icon % 256)
        mine = render_cube([sheets.vanilla(t) for t in ids])
        both = (ref[..., 3] > 0.5) & (mine[..., 3] > 0.5)
        iou = both.sum() / ((ref[..., 3] > 0.5) | (mine[..., 3] > 0.5)).sum()
        rmse = float(np.sqrt(((ref[both, :3] - mine[both, :3]) ** 2).mean()))
        worst = max(worst, rmse)
        print('%-28s silhouette IoU %.3f  colour RMSE %.4f' % (name, iou, rmse))
    assert worst < 0.12, 'renderer drifted from the vanilla bake (worst RMSE %.3f)' % worst
    print('check ok (worst RMSE %.4f)' % worst)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--starmade-master', default=os.path.join(HERE, '..', '..', 'StarMade-Master'))
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--preview', help='also write an upscaled preview PNG here')
    args = ap.parse_args()
    sheets = Sheets(os.path.join(args.starmade_master, 'src', 'main', 'resources', 'data'))
    if args.check:
        check(sheets)
        return
    icons = render_luamade(sheets)
    sheet = np.zeros((CELL * 16, CELL * 16, 4))
    for i, (_, im) in enumerate(icons):
        sheet[i // 16 * CELL:(i // 16 + 1) * CELL, i % 16 * CELL:(i % 16 + 1) * CELL] = im
    os.makedirs(os.path.join(RES, 'icons'), exist_ok=True)
    out = os.path.join(RES, 'icons', 'blocks_0.png')
    Image.fromarray((sheet * 255 + 0.5).astype(np.uint8), 'RGBA').save(out)
    print('wrote', out, '(' + ', '.join(n for n, _ in icons) + ')')
    if args.preview:
        strip = np.concatenate([im for _, im in icons], 1)
        bg = strip[..., :3] * strip[..., 3:] + 0.16 * (1 - strip[..., 3:])
        Image.fromarray((bg * 255).astype(np.uint8)).resize((strip.shape[1] * 3, CELL * 3), Image.NEAREST).save(args.preview)


if __name__ == '__main__':
    main()
