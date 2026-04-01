package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.system.module.ComputerModule;
import org.schema.game.common.controller.elements.FactoryAddOn;
import org.schema.game.common.controller.elements.FactoryAddOnInterface;
import org.schema.game.common.controller.elements.ManagerModuleCollection;
import org.schema.game.common.controller.elements.factory.FactoryCollectionManager;
import org.schema.game.common.controller.elements.factory.FactoryElementManager;
import org.schema.game.common.controller.elements.factory.FactoryUnit;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.meta.RecipeInterface;
import org.schema.game.common.data.player.inventory.StashInventory;

@LuaMadeClass("Factory")
public class FactoryBlock extends Block {

	public FactoryBlock(SegmentPiece piece, ComputerModule module) {
		super(piece, module);
	}

	private FactoryCollectionManager getCollectionManager() {
		SegmentPiece piece = requireLiveSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedSegmentController<?>)) return null;
		ManagedSegmentController<?> ctrl = (ManagedSegmentController<?>) piece.getSegmentController();
		if(!(ctrl.getManagerContainer() instanceof FactoryAddOnInterface)) return null;
		FactoryAddOn factoryAddOn = ((FactoryAddOnInterface) ctrl.getManagerContainer()).getFactory();
		ManagerModuleCollection<FactoryUnit, FactoryCollectionManager, FactoryElementManager> module
			= factoryAddOn.map.get(piece.getType());
		if(module == null) return null;
		return module.getCollectionManagersMap().get(piece.getAbsoluteIndex());
	}

	private StashInventory getStashInventory() {
		SegmentPiece piece = requireLiveSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedSegmentController<?>)) return null;
		ManagedSegmentController<?> ctrl = (ManagedSegmentController<?>) piece.getSegmentController();
		org.schema.game.common.data.player.inventory.Inventory inv
			= ctrl.getManagerContainer().getInventory(piece.getAbsoluteIndex());
		return inv instanceof StashInventory ? (StashInventory) inv : null;
	}

	/**
	 * Returns the factory sub-type: "basic", "standard", "advanced", "micro_assembler", or "capsule_assembler".
	 */
	@LuaMadeCallable
	public String getFactoryType() {
		short type = requireLiveSegmentPiece().getType();
		switch(type) {
			case ElementKeyMap.FACTORY_BASIC_ID: return "basic";
			case ElementKeyMap.FACTORY_STANDARD_ID: return "standard";
			case ElementKeyMap.FACTORY_ADVANCED_ID: return "advanced";
			case ElementKeyMap.FACTORY_MICRO_ASSEMBLER_ID: return "micro_assembler";
			case ElementKeyMap.FACTORY_CAPSULE_ASSEMBLER_ID: return "capsule_assembler";
			default: return "unknown";
		}
	}

	/**
	 * Returns true when this factory is actively consuming power to run a recipe.
	 */
	@LuaMadeCallable
	public Boolean isProducing() {
		FactoryCollectionManager mgr = getCollectionManager();
		return mgr != null && mgr.isPowerCharging(System.currentTimeMillis());
	}

	/**
	 * Returns the factory's capability level (1 + number of connected enhancer blocks).
	 * Higher capability means more items produced per cycle.
	 */
	@LuaMadeCallable
	public Integer getCapability() {
		FactoryCollectionManager mgr = getCollectionManager();
		return mgr == null ? 1 : mgr.getFactoryCapability();
	}

	/**
	 * Returns the current power charge level as a value between 0.0 and 1.0.
	 * This rises toward 1.0 over the course of each production cycle and
	 * resets to 0.0 after each step fires.
	 */
	@LuaMadeCallable
	public Float getPowerLevel() {
		FactoryCollectionManager mgr = getCollectionManager();
		return mgr == null ? 0f : mgr.getPowered();
	}

	/**
	 * Returns the time in milliseconds for one production cycle, accounting for capability modifiers.
	 */
	@LuaMadeCallable
	public Long getBakeTime() {
		FactoryCollectionManager mgr = getCollectionManager();
		return mgr == null ? FactoryCollectionManager.DEFAULT_BAKE_TIME : mgr.getBakeTime();
	}

	/**
	 * Returns production cycle progress as a value between 0.0 and 1.0,
	 * based on the factory's power charge level. Returns 0.0 when idle.
	 */
	@LuaMadeCallable
	public Double getProgress() {
		FactoryCollectionManager mgr = getCollectionManager();
		if(mgr == null || !mgr.isPowerCharging(System.currentTimeMillis())) return 0.0;
		return (double) mgr.getPowered();
	}

	/**
	 * Returns the current recipe loaded in this factory, or nil if none is active.
	 */
	@LuaMadeCallable
	public FactoryRecipe getRecipe() {
		FactoryCollectionManager mgr = getCollectionManager();
		if(mgr == null) return null;
		RecipeInterface recipe = mgr.getCurrentRecipe();
		return recipe == null ? null : new FactoryRecipe(recipe);
	}

	/**
	 * Sets the block type ID this factory should produce.
	 * For macro factories (basic/standard/advanced), this selects the recipe
	 * associated with that block type. Set to 0 to clear. Has no effect on
	 * micro assemblers or capsule assemblers, which have fixed recipes.
	 */
	@LuaMadeCallable
	public void setProductionTarget(int blockType) {
		StashInventory inv = getStashInventory();
		if(inv != null) {
			inv.setProduction((short) blockType);
		}
	}

	/**
	 * Returns the block type ID currently set as the production target, or nil if none is set.
	 */
	@LuaMadeCallable
	public Short getProductionTarget() {
		StashInventory inv = getStashInventory();
		if(inv == null) return null;
		short production = inv.getProduction();
		return production == 0 ? null : production;
	}

	/**
	 * Clears the production target, returning the factory to recipe-slot-driven mode.
	 */
	@LuaMadeCallable
	public void clearProductionTarget() {
		StashInventory inv = getStashInventory();
		if(inv != null) {
			inv.setProduction((short) 0);
		}
	}

	/**
	 * Sets a cap on how many items this factory will produce in total.
	 * Set to 0 for unlimited production.
	 */
	@LuaMadeCallable
	public void setProductionLimit(int limit) {
		StashInventory inv = getStashInventory();
		if(inv != null) {
			inv.setProductionLimit(Math.max(0, limit));
		}
	}

	/**
	 * Returns the current production limit, or 0 if production is unlimited.
	 */
	@LuaMadeCallable
	public Integer getProductionLimit() {
		StashInventory inv = getStashInventory();
		return inv == null ? 0 : inv.getProductionLimit();
	}
}
