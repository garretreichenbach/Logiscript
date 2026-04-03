package luamade.element.block;

import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.function.Predicate;

public class BlockUtils {

	/**
	 * Adds a controlling relationship between the given element and all elements that match the filter. The given element will be added to the controlling list of all matching elements, and all matching elements will be added to the controlledBy list of the given element.
	 * @param toControl The element that will control the matching elements.
	 * @param filter A predicate that determines which elements will be controlled by the given element.
	 */
	public static void addControlling(ElementInformation toControl, Predicate<ElementInformation> filter) {
		for(ElementInformation info : ElementKeyMap.getInfoArray()) {
			if(info != null && filter.test(info)) {
				info.controlling.add(toControl.id);
				toControl.controlledBy.add(info.id);
			}
		}
	}
}
