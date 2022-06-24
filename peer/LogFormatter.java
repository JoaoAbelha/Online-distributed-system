package peer;

/**
 * taken from
 * https://stackoverflow.com/questions/53211694/change-color-and-format-of-java-util-logging-logger-output-in-eclipse
 */
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
    private PeerSignature peer;
    // ANSI escape code
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    LogFormatter(PeerSignature peer) {
        this.peer = peer;
    }

    // Here you can configure the format of the output and
    // its color by using the ANSI escape codes defined above.

    // format is called for every console log message
    @Override
    public String format(LogRecord record) {

        StringBuilder builder = new StringBuilder();
        builder.append(ANSI_BLUE);

        builder.append("[");
        builder.append(calcDate(record.getMillis()));
        builder.append("]");

        builder.append(" [");
        builder.append(this.peer.getId());
        builder.append("]");

        builder.append(" [");
        builder.append(this.peer.getPort() + ":" + this.peer.getAddress().getHostAddress());
        builder.append("]");

        builder.append(levelColor(record.getLevel()));
        builder.append(" [");
        builder.append(record.getLevel().getName());
        builder.append("]");

        builder.append(ANSI_WHITE);
        builder.append(" - ");
        builder.append(record.getMessage());

        builder.append(ANSI_RESET);
        builder.append("\n");
        return builder.toString();
    }

    private String calcDate(long millisecs) {
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date resultdate = new Date(millisecs);
        return date_format.format(resultdate);
    }

    private String levelColor(Level level) {
        if (level.equals(Level.INFO)) {
            return ANSI_BLUE;
        }

        if (level.equals(Level.WARNING)) {
            return ANSI_YELLOW;
        }

        if (level.equals(Level.FINE)) {
            return ANSI_GREEN;
        }

        if (level.equals(Level.SEVERE)) {
            return ANSI_RED;
        }

        return ANSI_WHITE;
    }

}