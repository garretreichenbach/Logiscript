package dovtech.logiscript.elements.items.components;

import api.config.BlockConfig;
import dovtech.logiscript.elements.ElementManager;
import dovtech.logiscript.elements.items.Item;
import org.schema.game.common.data.element.FactoryResource;

/**
 * CPU
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/26/2021
 */
public class CPU {

    public static class Basic extends Item {

        public Basic() {
            super("Basic CPU", ElementManager.getCategory("General.Components"));
        }

        @Override
        public void initialize() {
            itemInfo.setDescription("A basic CPU made for processing simple tasks." +
                    "\nMax Speed: TODO" +
                    "\nMax Threads: 1");
            itemInfo.setShoppable(true);
            itemInfo.setPrice(1000);

            BlockConfig.addRecipe(itemInfo, BlockConfig.customFactories.get(ElementManager.getBlock("Circuit Fabricator").getId()), 15,
                    new FactoryResource(3, (short) 993),
                    new FactoryResource(1, (short) 399),
                    new FactoryResource(3, (short) 407),
                    new FactoryResource(1, (short) 408),
                    new FactoryResource(1, (short) 409),
                    new FactoryResource(3, (short) 410),
                    new FactoryResource(15, (short) 546),
                    new FactoryResource(15, (short) 547)
            );
            BlockConfig.add(itemInfo);
        }
    }
}
