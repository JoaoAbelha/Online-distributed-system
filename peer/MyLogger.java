package peer;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyLogger {

    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void bind(PeerSignature peer) {
        logger = Logger.getLogger(MyLogger.class.getName());
        logger.setUseParentHandlers(false);

        LogFormatter formatter = new LogFormatter(peer);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
    }


    public static void print(String content, Level level) {
        logger.log(level, content);
    }


}