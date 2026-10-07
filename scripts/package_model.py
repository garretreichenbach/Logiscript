"""Package an exported model as src/main/resources/models/<Name>.zip in the layout MeshLoader.loadModMesh expects.

Usage: python scripts/package_model.py <Name> [exported .mesh.xml]

Expects models/<Name>/<Name>.png, <Name>_EM.png and <Name>_NRM.png. The mesh's submesh material must be named <Name>.
The .material and .scene are written in vanilla's OgreMax format, which is what the game's parser reads.
"""
import os
import shutil
import sys
import zipfile

MODELS = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'models')

MATERIAL = """material {n}
{{
	technique
	{{
		pass
		{{
			ambient 0.588 0.588 0.588 1
			diffuse 0.588 0.588 0.588 1
			specular 0 0 0 1 10

			texture_unit
			{{
				texture {n}.png
			}}

			texture_unit
			{{
				texture {n}_NRM.png
			}}

			texture_unit
			{{
				texture {n}_EM.png
				colour_op_ex add src_texture src_current
				colour_op_multipass_fallback one one
			}}
		}}
	}}
}}
"""

SCENE = """<scene formatVersion="1.0" minOgreVersion="1.8" ogreMaxVersion="2.6.1" unitType="/10" unitsPerMeter="3.93701" upAxis="y">
    <environment>
        <colourAmbient b="0.333333" g="0.333333" r="0.333333"/>
        <colourBackground b="0" g="0" r="0"/>
        <clipping far="10000" near="0"/>
    </environment>
    <nodes>
        <node name="{n}">
            <position x="0" y="0" z="0"/>
            <scale x="1" y="1" z="1"/>
            <rotation qw="1" qx="0" qy="0" qz="0"/>
            <entity castShadows="true" meshFile="{n}.mesh" name="{n}" receiveShadows="true">
                <subentities>
                    <subentity index="0" materialName="{n}"/>
                </subentities>
            </entity>
        </node>
    </nodes>
</scene>
"""


def main():
    name = sys.argv[1]
    folder = os.path.join(MODELS, name)
    if len(sys.argv) > 2:
        shutil.copy(sys.argv[2], os.path.join(folder, name + '.mesh.xml'))
    mesh = open(os.path.join(folder, name + '.mesh.xml'), encoding='utf-8').read()
    if 'material="%s"' % name not in mesh:
        sys.exit('submesh material must be named ' + name)
    open(os.path.join(folder, name + '.material'), 'w', newline='\n').write(MATERIAL.format(n=name))
    open(os.path.join(folder, name + '.scene'), 'w', newline='\n').write(SCENE.format(n=name))
    files = [name + s for s in ('.mesh.xml', '.material', '.scene', '.png', '_EM.png', '_NRM.png')]
    with zipfile.ZipFile(os.path.join(MODELS, name + '.zip'), 'w', zipfile.ZIP_DEFLATED) as z:
        for f in files:
            z.write(os.path.join(folder, f), f)
    print('wrote models/%s.zip: %s' % (name, ', '.join(files)))


if __name__ == '__main__':
    main()
