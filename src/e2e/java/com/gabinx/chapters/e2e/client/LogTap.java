package com.gabinx.chapters.e2e.client;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.LogEvent;

import java.util.ArrayList;
import java.util.List;

/** Collects the JEI ingredient lines and the Chapters recipe-viewer timing line. */
public final class LogTap extends AbstractAppender {
    private static LogTap instance;

    private final List<String> lines = new ArrayList<>();
    private int mark;

    private LogTap() {
        super("ChaptersE2e", null, null, true, Property.EMPTY_ARRAY);
    }

    public static void install() {
        if (instance != null) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().getLoggerConfig("com.gabinx.chapters.Chapters").setLevel(Level.DEBUG);
        context.updateLoggers();
        LogTap tap = new LogTap();
        tap.start();
        context.getConfiguration().getRootLogger().addAppender(tap, Level.DEBUG, null);
        context.updateLoggers();
        instance = tap;
    }

    public static void mark() {
        if (instance != null) {
            instance.mark = instance.lines.size();
        }
    }

    public static List<String> sinceMark() {
        if (instance == null) {
            return List.of();
        }
        return List.copyOf(instance.lines.subList(instance.mark, instance.lines.size()));
    }

    @Override
    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        if (message.contains("Ingredients are being") || message.contains("Recipe viewer refresh")) {
            lines.add(message);
        }
    }

    @Override
    public boolean ignoreExceptions() {
        return true;
    }

    /** Present so the class keeps a reference the compiler cannot treat as unused. */
    public static Appender appender() {
        return instance;
    }
}
