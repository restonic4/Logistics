package com.restonic4.logistics;

import com.restonic4.logistics.platform.Services;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Constants {
    public static final String MOD_ID = "logistics";
    public static final String MOD_NAME = "Logistics";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private static boolean debug = false;

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean enabled) {
        debug = enabled;

        Level targetLevel = enabled ? Level.DEBUG : Level.INFO;

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();

        LoggerConfig logCfg = cfg.getLoggerConfig(MOD_NAME);
        if (logCfg.getName().isEmpty()) {
            logCfg = new LoggerConfig(MOD_NAME, targetLevel, !enabled);
            cfg.addLogger(MOD_NAME, logCfg);
        } else {
            logCfg.setLevel(targetLevel);
            logCfg.setAdditive(!enabled);
        }

        String debugAppenderName = MOD_ID + "_DebugConsole";
        Map<String, Appender> appenders = logCfg.getAppenders();
        Appender existing = appenders.get(debugAppenderName);

        if (enabled && existing == null) {
            Layout<?> layout = null;

            String[] names = {"SysOut", "Console", "FMLSysOut", "SystemOut", "TerminalConsole"};
            for (String n : names) {
                Appender candidate = cfg.getAppender(n);
                if (candidate instanceof AbstractAppender) {
                    layout = candidate.getLayout();
                    if (layout != null) break;
                }
            }

            if (layout == null) {
                layout = PatternLayout.newBuilder()
                        .withPattern("%highlight{[%d{HH:mm:ss}] [%t/%level] (%logger) %msg%n}")
                        .withConfiguration(cfg)
                        .build();
            }

            ConsoleAppender appender = ConsoleAppender.newBuilder()
                    .setName(debugAppenderName)
                    .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                    .setLayout(layout)
                    .build();
            appender.start();

            logCfg.addAppender(appender, Level.DEBUG, null);
        } else if (!enabled && existing != null) {
            logCfg.removeAppender(debugAppenderName);
            existing.stop();
        }

        ctx.updateLoggers();
    }
}
