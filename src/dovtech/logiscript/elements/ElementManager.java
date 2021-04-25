package dovtech.logiscript.elements;

import api.config.BlockConfig;
import dovtech.logiscript.elements.blocks.Block;
import org.apache.commons.lang3.StringUtils;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.ArrayList;

/**
 * ElementManager
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/25/2021
 */
public class ElementManager {

    public enum FactoryType {NONE, CAPSULE_REFINERY, MICRO_ASSEMBLER, BASIC_FACTORY, STANDARD_FACTORY, ADVANCED_FACTORY}

    private static final ArrayList<Block> blockList = new ArrayList<>();

    public static void initializeBlocks() {
        for(Block blockElement : blockList) blockElement.initialize();
    }

    public static ArrayList<Block> getAllBlocks() {
        return blockList;
    }

    public static Block getBlock(short id) {
        for(Block blockElement : getAllBlocks()) if(blockElement.getBlockInfo().getId() == id) return blockElement;
        return null;
    }

    public static Block getBlock(String blockName) {
        for(Block block : getAllBlocks()) {
            if(block.getBlockInfo().getName().equalsIgnoreCase(blockName)) return block;
        }
        return null;
    }

    public static void addBlock(Block block) {
        blockList.add(block);
    }

    public static ElementCategory getCategory(String path) {
        String[] split = path.split("\\.");
        ElementCategory category = ElementKeyMap.getCategoryHirarchy();
        for(String s : split) {
            boolean createNew = false;
            if(category.hasChildren()) {
                for(ElementCategory child : category.getChildren()) {
                    if(child.getCategory().equalsIgnoreCase(s)) {
                        category = child;
                        break;
                    }
                    createNew = true;
                }
            } else createNew = true;
            if(createNew) category = BlockConfig.newElementCategory(category, StringUtils.capitalize(s));
        }
        return category;
    }

    public static ElementInformation getInfo(String name) {
        Block block = getBlock(name);
        if(block != null) return block.getBlockInfo();
        else {
            for(ElementInformation elementInfo : ElementKeyMap.getInfoArray()) {
                if(elementInfo.getName().equalsIgnoreCase(name)) return elementInfo;
            }
        }
        return null;
    }
}
