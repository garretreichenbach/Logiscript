"""Generate textures_0_NRM.png for the LuaMade block atlas using StarMade's own block_normals generator.

Usage: python scripts/atlas_normals.py [--starmade-master PATH] [--atlas NAME]

Alpha (specular / emissive mask) from an existing _NRM.png is preserved per tile, so hand-tuned
tiles survive regeneration; only tiles with empty alpha get it auto-generated.
"""
import argparse
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEXTURES = os.path.join(HERE, '..', 'src', 'main', 'resources', 'textures')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--starmade-master', default=os.path.join(HERE, '..', '..', 'StarMade-Master'))
    ap.add_argument('--atlas', default='textures_0')
    args = ap.parse_args()

    sys.path.insert(0, os.path.join(args.starmade_master, 'assets', 'utils'))
    import block_normals as bn
    bn.selfcheck()

    diff = bn.load(os.path.join(TEXTURES, args.atlas + '.png'))
    if diff is None:
        sys.exit('missing ' + args.atlas + '.png - export it from textures.kra first')
    out = os.path.join(TEXTURES, args.atlas + '_NRM.png')
    old = Image.open(out) if os.path.exists(out) else None
    old_alpha = np.asarray(old.getchannel('A')).astype(np.float32) / 255 if old and old.mode == 'RGBA' else None

    xyz, alpha = bn.build(diff, old_alpha)
    img = np.dstack([xyz * 0.5 + 0.5, alpha])
    Image.fromarray(np.clip(img * 255 + 0.5, 0, 255).astype(np.uint8), 'RGBA').save(out)
    print('wrote', out)


if __name__ == '__main__':
    main()
