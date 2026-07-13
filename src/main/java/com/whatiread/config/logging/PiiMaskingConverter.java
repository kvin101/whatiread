package com.whatiread.config.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class PiiMaskingConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null) {
            return null;
        }
        return message
                .replaceAll("(?i)(Bearer\\s+)\\S+", "$1***")
                .replaceAll("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})", "***@$2")
                .replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*", "$1***");
    }
}
