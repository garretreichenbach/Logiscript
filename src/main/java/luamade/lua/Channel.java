package luamade.lua;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.LuaManager;
import org.luaj.vm2.LuaString;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Channel extends LuaMadeUserdata {

	private final String name;
	private final String password;
	private String[] messages = new String[0];

	public Channel(String name, String password) {
		this.name = name;
		this.password = password;
	}

	@LuaMadeCallable
	public LuaString getName() {
		return LuaString.valueOf(name);
	}

	@LuaMadeCallable
	public LuaString[] getMessages(LuaString password) {
		if(password.tojstring().equals(this.password)) {
			LuaString[] messages = new LuaString[this.messages.length];
			for(int i = 0; i < this.messages.length; i ++) messages[i] = LuaString.valueOf(this.messages[i]);
			return messages;
		} else return new LuaString[] {LuaString.valueOf("Invalid password!")};
	}

	@LuaMadeCallable
	public LuaString getLatestMessage(LuaString password) {
		if(password.tojstring().equals(this.password)) return getMessages(password)[messages.length - 1];
		else return LuaString.valueOf("Invalid password!");
	}

	@LuaMadeCallable
	public void sendMessage(LuaString password, LuaString message) {
		if(password.tojstring().equals(this.password)) {
			//message = "[" + (new Date()) + "] " + message;
			String[] newMessages = new String[messages.length + 1];
			System.arraycopy(messages, 0, newMessages, 0, messages.length);
			newMessages[messages.length] = message.tojstring();
			messages = newMessages;
			System.out.println("[" + name + "] " + message);
		}
	}

	@LuaMadeCallable
	public void removeChannel(LuaString password) {
		if(password.tojstring().equals(this.password)) LuaManager.removeChannel(name);
	}
}
