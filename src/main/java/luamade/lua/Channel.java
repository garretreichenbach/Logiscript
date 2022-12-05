package luamade.lua;

import luamade.luawrap.LuaCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.LuaManager;

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

	@LuaCallable
	public String getName() {
		return name;
	}

	@LuaCallable
	public String[] getMessages(String password) {
		if(password.equals(this.password)) return messages;
		else return new String[] {"Invalid password!"};
	}

	@LuaCallable
	public String getLatestMessage(String password) {
		if(password.equals(this.password)) return messages[messages.length - 1];
		else return "Invalid password!";
	}

	@LuaCallable
	public void sendMessage(String password, String message) {
		if(password.equals(this.password)) {
			//message = "[" + (new Date()) + "] " + message;
			String[] newMessages = new String[messages.length + 1];
			System.arraycopy(messages, 0, newMessages, 0, messages.length);
			newMessages[messages.length] = message;
			messages = newMessages;
			System.out.println("[" + name + "] " + message);
		}
	}

	@LuaCallable
	public void removeChannel(String password) {
		if(password.equals(this.password)) LuaManager.removeChannel(name);
	}
}
