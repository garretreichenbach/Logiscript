package dovtech.logiscript.managers;

/**
 * ResourceManager
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/25/2021
 */
public interface ResourceManager {
    void initialize();
    Object getResource(String name);
}