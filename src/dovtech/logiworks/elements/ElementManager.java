package dovtech.logiworks.elements;

import api.config.BlockConfig;
import dovtech.logiworks.elements.blocks.Block;
import dovtech.logiworks.elements.blocks.Factory;
import dovtech.logiworks.elements.items.Item;
import org.apache.commons.lang3.StringUtils;
import org.schema.game.common.data.SegmentPiece;
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
    private static final ArrayList<Factory> factoryList = new ArrayList<>();
    private static final ArrayList<Item> itemList = new ArrayList<>();

    public static void initialize() {
        for(Block block : blockList) block.initialize();
        for(Factory factory : factoryList) factory.initialize();
        for(Item item : itemList) item.initialize();
    }

    public static ArrayList<Block> getAllBlocks() {
        return blockList;
    }

    public static ArrayList<Factory> getAllFactories() {
        return factoryList;
    }

    public static ArrayList<Item> getAllItems() {
        return itemList;
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

    public static Block getBlock(SegmentPiece segmentPiece) {
        for(Block block : getAllBlocks()) if(block.getId() == segmentPiece.getType()) return block;
        return null;
    }

    public static Factory getFactory(String factoryName) {
        for(Factory factory : getAllFactories()) {
            if(factory.getBlockInfo().getName().equalsIgnoreCase(factoryName)) return factory;
        }
        return null;
    }

    public static Factory getFactory(SegmentPiece segmentPiece) {
        for(Factory factory : getAllFactories()) if(factory.getId() == segmentPiece.getType()) return factory;
        return null;
    }

    public static Item getItem(String itemName) {
        for(Item item : getAllItems()) {
            if(item.getItemInfo().getName().equalsIgnoreCase(itemName)) return item;
        }
        return null;
    }

    public static void addBlock(Block block) {
        blockList.add(block);
    }

    public static void addFactory(Factory factory) {
        factoryList.add(factory);
    }

    public static void addItem(Item item) {
        itemList.add(item);
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
            Factory factory = getFactory(name);
            if(factory != null) return factory.getBlockInfo();
            else {
                Item item = getItem(name);
                if(item != null) return item.getItemInfo();
                else {
                    for(ElementInformation elementInfo : ElementKeyMap.getInfoArray()) {
                        if(elementInfo.getName().equalsIgnoreCase(name)) return elementInfo;
                    }
                }
            }
        }
        return null;
    }
}
