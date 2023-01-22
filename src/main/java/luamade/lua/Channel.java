package luamade.lua;

import luamade.luawrap.LuaMadeCallable;
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
	private String[] messages;

	public Channel(String name, String password, String[] messages) {
		this.name = name;
		this.password = password;
		this.messages = messages;
	}

	@LuaMadeCallable
	public String getName() {
		return name;
	}

	@LuaMadeCallable
	public String[] getMessages(String password) {
		if(password.equals(this.password)) return messages;
		else return new String[] {"Invalid password!"};
	}

	@LuaMadeCallable
	public String getLatestMessage(String password) {
		if(password.equals(this.password)) return messages[messages.length - 1];
		else return "Invalid password!";
	}

	@LuaMadeCallable
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

	@LuaMadeCallable
	public void removeChannel(String password) {
		if(password.equals(this.password)) LuaManager.removeChannel(name);
	}

	public static class ChannelSerializable {

		public String name;
		public String password;
		public String[] messages;

		public ChannelSerializable(String name, String password, String[] messages) {
			this.name = name;
			this.password = password;
			this.messages = messages;
		}

		public ChannelSerializable(Channel channel) {
			this(channel.name, channel.password, channel.messages);
		}

		public Channel toChannel() {
			return new Channel(name, password, messages);
		}
	}
}
