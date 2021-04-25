package dovtech.logiscript.elements.blocks;

import api.config.BlockConfig;
import dovtech.logiscript.LogiScript;
import dovtech.logiscript.elements.ElementManager;
import dovtech.logiscript.managers.TextureManager;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;

/**
 * Block
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/25/2021
 */
public abstract class Block {

    protected ElementInformation blockInfo;

    public Block(String name, ElementCategory category, String... sideNames) {
        short[] textureIds = new short[6];
        String replace = name.toLowerCase().trim().replace(" ", "-");
        int i;
        for(i = 0; i < textureIds.length && i < sideNames.length; i ++) {
            String sideName = sideNames[i].toLowerCase().trim().replace(" ", "-");
            String textureName = replace + "-" + sideName;
            textureIds[i] = (short) TextureManager.getTexture(textureName).getTextureId();
        }
        if(i < 5) {
            for(int j = 0; i < textureIds.length && j < sideNames.length; i ++) {
                String sideName = sideNames[j].toLowerCase().trim().replace(" ", "-");
                String textureName = replace + "-" + sideName;
                textureIds[i] = (short) TextureManager.getTexture(textureName).getTextureId();
                j ++;
            }
        }

        blockInfo = BlockConfig.newElement(LogiScript.getInstance(), name, textureIds);
        BlockConfig.setElementCategory(blockInfo, category);
        ElementManager.addBlock(this);
    }

    public final ElementInformation getBlockInfo() {
        return blockInfo;
    }

    public final short getId() {
        return blockInfo.getId();
    }

    public abstract void initialize();
}