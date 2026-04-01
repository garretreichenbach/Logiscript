package luamade.lua.element.block;

import luamade.lua.element.inventory.ItemStack;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.element.meta.RecipeInterface;
import org.schema.game.common.data.element.meta.RecipeProductInterface;

public class FactoryRecipe extends LuaMadeUserdata {
	private final RecipeInterface recipe;

	public FactoryRecipe(RecipeInterface recipe) {
		this.recipe = recipe;
	}

	@LuaMadeCallable
	public Double getBakeTime() {
		return (double) recipe.getBakeTime();
	}

	@LuaMadeCallable
	public Integer getProductCount() {
		RecipeProductInterface[] products = recipe.getRecipeProduct();
		return products == null ? 0 : products.length;
	}

	@LuaMadeCallable
	public ItemStack[] getInputs() {
		return getInputsForChain(0);
	}

	@LuaMadeCallable
	public ItemStack[] getInputs(int chainIndex) {
		return getInputsForChain(chainIndex);
	}

	@LuaMadeCallable
	public ItemStack[] getOutputs() {
		return getOutputsForChain(0);
	}

	@LuaMadeCallable
	public ItemStack[] getOutputs(int chainIndex) {
		return getOutputsForChain(chainIndex);
	}

	private ItemStack[] getInputsForChain(int chainIndex) {
		RecipeProductInterface[] products = recipe.getRecipeProduct();
		if(products == null || chainIndex < 0 || chainIndex >= products.length) return null;
		FactoryResource[] resources = products[chainIndex].getInputResource();
		return resourcesToItemStacks(resources);
	}

	private ItemStack[] getOutputsForChain(int chainIndex) {
		RecipeProductInterface[] products = recipe.getRecipeProduct();
		if(products == null || chainIndex < 0 || chainIndex >= products.length) return null;
		FactoryResource[] resources = products[chainIndex].getOutputResource();
		return resourcesToItemStacks(resources);
	}

	private ItemStack[] resourcesToItemStacks(FactoryResource[] resources) {
		if(resources == null) return new ItemStack[0];
		ItemStack[] result = new ItemStack[resources.length];
		for(int i = 0; i < resources.length; i++) {
			result[i] = new ItemStack(resources[i].type, resources[i].count);
		}
		return result;
	}
}
