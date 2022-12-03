package luamade.lua;

import luamade.manager.LuaManager;

import java.util.Date;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Channel {

	private final String name;
	private final String password;
	private String[] messages = new String[0];

	public Channel(String name, String password) {
		this.name = name;
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public String[] getMessages(String password) {
		if(password.equals(this.password)) return messages;
		else return new String[] {"Invalid password!"};
	}

	public String[] getLatestMessage(String password) {
		if(password.equals(this.password)) return new String[] {messages[messages.length - 1]};
		else return new String[] {"Invalid password!"};
	}

	public void sendMessage(String password, String message) {
		if(password.equals(this.password)) {
			message = "[" + (new Date()) + "] " + message;
			String[] newMessages = new String[messages.length + 1];
			System.arraycopy(messages, 0, newMessages, 0, messages.length);
			newMessages[messages.length] = message;
			messages = newMessages;
		}
	}

	public void removeChannel(String password) {
		if(password.equals(this.password)) LuaManager.removeChannel(name);
	}
}
