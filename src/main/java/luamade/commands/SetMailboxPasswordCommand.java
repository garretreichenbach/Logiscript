package luamade.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import luamade.LuaMade;
import luamade.data.misc.PlayerData;
import luamade.manager.LuaManager;
import org.schema.game.common.data.player.PlayerState;

import javax.annotation.Nullable;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class SetMailboxPasswordCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "set_mailbox_password";
	}

	@Override
	public String[] getAliases() {
		return new String[] {"set_mailbox_password"};
	}

	@Override
	public String getDescription() {
		return "Sets a password for your mailbox so that only those with the password can send you mail using a computer.\n" +
				"/%COMMAND% <password> : Sets the mailbox password to the specified password. Leave empty to remove the password.";
	}

	@Override
	public boolean isAdminOnly() {
		return false;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		PlayerData playerData = LuaManager.getPlayerData(sender.getName());
		if(args == null || args.length == 0) {
			playerData.setPassword("");
			PlayerUtils.sendMessage(sender, "Mailbox password removed.");
		} else {
			playerData.setPassword(args[0]);
			PlayerUtils.sendMessage(sender, "Mailbox password set to " + args[0] + ".");
		}
		LuaManager.savePlayerData(playerData);
		return true;
	}

	@Override
	public void serverAction(@Nullable PlayerState sender, String[] args) {

	}

	@Override
	public StarMod getMod() {
		return LuaMade.getInstance();
	}
}
