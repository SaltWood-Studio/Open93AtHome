package top.saltwood.everythingAtHome.modules.cluster;

public class Logger {
    private Logger() {
    
    }
    public static final Logger logger = new Logger();
    private final Object _lock = new Object();
    
    public static Logger getInstance() { return logger; }
    
    public void log(String message) {
        synchronized (_lock) {
            System.out.println(message);
        }
    }
}
