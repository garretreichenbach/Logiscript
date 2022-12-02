package thederpgamer.logiscript.api;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public @interface LuaCallable {

	String name();

	String description() default "";

	String[] args() default {};

	String returns() default "";
}
