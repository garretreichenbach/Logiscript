package luamade.lua.galaxy;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

/**
 * One orbit ring of a star system, as drawn on the vanilla galaxy map. Rings are
 * circles centred on the system's star sector ({@link StarSystem#getSunSector()})
 * lying in the plane whose normal is {@link StarSystem#getAxis()}.
 *
 * <p>Obtained via {@link StarSystem#getOrbits()}.
 */
public class Orbit extends LuaMadeUserdata {

	private final int index;
	private final boolean planet;

	public Orbit(int index, boolean planet) {
		this.index = index;
		this.planet = planet;
	}

	/** Orbit slot, {@code 0} (innermost) to {@code 7}. */
	@LuaMadeCallable
	public Integer getIndex() {
		return index;
	}

	/**
	 * Ring radius in sectors, measured from the centre of the star's sector. Orbit 0 sits 1.5
	 * sectors out, each further slot one sector beyond (mirrors {@code SectorInformation.getOrbitRadius}).
	 */
	@LuaMadeCallable
	public Double getRadius() {
		return index + 1.5;
	}

	/** {@code "PLANET"} for a planet orbit, {@code "BELT"} for an asteroid belt. */
	@LuaMadeCallable
	public String getType() {
		return planet ? "PLANET" : "BELT";
	}
}
