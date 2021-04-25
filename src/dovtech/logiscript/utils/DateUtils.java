package dovtech.logiscript.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DateUtils
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/25/2021
 */
public class DateUtils {

    public static int getAgeDays(Date date) {
        Date current = new Date(System.currentTimeMillis());
        long difference = Math.abs(current.getTime() - date.getTime());
        return (int) (difference / (1000 * 60 * 60 * 24));
    }

    public static int getAgeDays(long time) {
        return getAgeDays(new Date(time));
    }

    public static String getTimeFormatted(String format) {
        return getTimeFormatted(format, new Date());
    }

    public static String getTimeFormatted(String format, Date date) {
        return (new SimpleDateFormat(format)).format(date + " ");
    }

    public static String getTimeFormatted(Date date) {
        return getTimeFormatted("MM/dd/yyyy '-' hh:mm:ss z", date);
    }
}