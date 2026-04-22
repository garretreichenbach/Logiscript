package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;

public class BlockInfo extends LuaMadeUserdata {

    private final ElementInformation elementInfo;

    public BlockInfo(ElementInformation elementInfo) {
       this.elementInfo = elementInfo;
    }

    @LuaMadeCallable
    public String getName() {
        return elementInfo.getName();
    }

    @LuaMadeCallable
    public String getFullName() {
        return elementInfo.getFullName();
    }

    @LuaMadeCallable
    public String getDescription() {
        return elementInfo.getDescription();
    }

    @LuaMadeCallable
    public Short getId() {
        return elementInfo.getId();
    }

    /** Leaf category name, e.g. "Capsules" or "Cannon Barrels". */
    @LuaMadeCallable
    public String getCategory() {
        ElementCategory cat = elementInfo.getType();
        return cat == null ? null : cat.getCategory();
    }

    /** Full category path, e.g. "Resources > Basic Capsules > Capsules". */
    @LuaMadeCallable
    public String getCategoryPath() {
        ElementCategory cat = elementInfo.getType();
        return cat == null ? null : cat.getFullPathRecursive();
    }

    /** The MediaWiki-style category (used for in-game wiki docs). */
    @LuaMadeCallable
    public String getWikiCategory() {
        return elementInfo.getWikiCategory();
    }

    /** Inventory group string used by the stock UI. */
    @LuaMadeCallable
    public String getInventoryGroup() {
        return elementInfo.getInventoryGroup();
    }

    @LuaMadeCallable
    public Boolean isCapsule() {
        return elementInfo.isCapsule();
    }

    @LuaMadeCallable
    public Boolean isOre() {
        return elementInfo.isOre();
    }

    @LuaMadeCallable
    public Boolean isShoppable() {
        return elementInfo.isShoppable();
    }

    @LuaMadeCallable
    public Boolean isPlacable() {
        return elementInfo.isPlacable();
    }

    @LuaMadeCallable
    public Boolean isVanilla() {
        return elementInfo.isVanilla();
    }

    @LuaMadeCallable
    public Boolean isDeprecated() {
        return elementInfo.isDeprecated();
    }

    @LuaMadeCallable
    public Boolean isDoor() {
        return elementInfo.isDoor();
    }

    @LuaMadeCallable
    public Boolean isLightSource() {
        return elementInfo.isLightSource();
    }

    @LuaMadeCallable
    public Boolean isSignal() {
        return elementInfo.isSignal();
    }

    @LuaMadeCallable
    public Boolean hasInventory() {
        return elementInfo.isInventory();
    }

    @LuaMadeCallable
    public Integer getMaxHp() {
        return elementInfo.getMaxHitPointsFull();
    }
}
