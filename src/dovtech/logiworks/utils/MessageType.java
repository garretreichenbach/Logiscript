package dovtech.logiworks.utils;

/**
 * MessageType
 * <Description>
 *
 * @author TheDerpGamer
 * @since 06/09/2021
 */
public enum MessageType {
    INFO("[INFO]: "),
    WARNING("[WARNING]: "),
    ERROR("[ERROR]: "),
    CRITICAL("[CRITICAL]: ");

    public String prefix;

    MessageType(String prefix) {
        this.prefix = prefix;
    }
}