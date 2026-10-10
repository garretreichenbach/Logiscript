package luamade.lua.element.block;

import luamade.lua.vault.Vault;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.system.module.ComputerModule;
import org.schema.game.common.data.SegmentPiece;

@LuaMadeClass("VaultBlock")
public class VaultBlock extends Block {

	public VaultBlock(SegmentPiece piece, ComputerModule module) {
		super(piece, module);
	}

	@LuaMadeCallable
	public String getUuid() {
		return Vault.uuidOf(requireLiveSegmentPiece());
	}

	@LuaMadeCallable
	public Long getBalance() {
		return Vault.balanceOf(requireLiveSegmentPiece());
	}

	@LuaMadeCallable
	public String getAccessLevel() {
		return Vault.accessLevelOf(requireLiveSegmentPiece());
	}

	@LuaMadeCallable
	public Boolean requestPayment(Long amount, String reason) {
		return Vault.requestPayment(requireLiveSegmentPiece(), amount, reason);
	}

	@LuaMadeCallable
	public Boolean payout(Long amount, String reason) {
		return Vault.payout(requireLiveSegmentPiece(), amount);
	}
}
