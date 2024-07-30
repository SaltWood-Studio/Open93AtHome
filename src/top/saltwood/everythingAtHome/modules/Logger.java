package top.saltwood.everythingAtHome.modules;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

public class Logger extends PrintStream {
    private Logger() {
        super(System.out);
    }
    public static final Logger logger = new Logger();
    private final Object _lock = new Object();

    public void log(Object message) {
        synchronized (_lock) {
            System.out.print(message);
        }
    }

    public void logLine(Object message) {
        synchronized (_lock) {
            System.out.println(message);
        }
    }

    public void logLine() {
        this.logLine("");
    }

    @Override
    public void print(boolean b) {
        this.log(b);
    }

    @Override
    public void print(char c) {
        this.log(c);
    }

    @Override
    public void print(int i) {
        this.log(i);
    }

    @Override
    public void print(long l) {
        this.log(l);
    }

    @Override
    public void print(float f) {
        this.log(f);
    }

    @Override
    public void print(double d) {
        this.log(d);
    }

    @Override
    public void print(@NotNull char[] s) {
        this.log(s);
    }

    @Override
    public void print(@Nullable String s) {
        this.log(s);
    }

    @Override
    public void print(@Nullable Object obj) {
        this.log(obj);
    }

    @Override
    public void println() {
        this.logLine();
    }

    @Override
    public void println(boolean x) {
        this.logLine(x);
    }

    @Override
    public void println(char x) {
        this.logLine(x);
    }

    @Override
    public void println(int x) {
        this.logLine(x);
    }

    @Override
    public void println(long x) {
        this.logLine(x);
    }

    @Override
    public void println(float x) {
        this.logLine(x);
    }

    @Override
    public void println(double x) {
        this.logLine(x);
    }

    @Override
    public void println(@NotNull char[] x) {
        this.logLine(x);
    }

    @Override
    public void println(@Nullable String x) {
        this.logLine(x);
    }

    @Override
    public void println(@Nullable Object x) {
        this.logLine(x);
    }
}
