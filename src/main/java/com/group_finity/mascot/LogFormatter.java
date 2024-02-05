package com.group_finity.mascot;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Class to format logs.
 *
 * @author Yuki Yamada
 */
public class LogFormatter extends SimpleFormatter {

    private final Date dat = new Date();
    private static final String format = "{0,date} {0,time}";
    private MessageFormat formatter;

    private Object[] args = new Object[1];

    private String lineSeparator = System.lineSeparator();

    /**
     * Formats the given LogRecord.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public synchronized String format(final LogRecord record) {
        final StringBuilder sb = new StringBuilder();

        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;
        final StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(format);
        }
        formatter.format(args, text, null);
        sb.append(text);
        sb.append(" ");

        sb.append(record.getLevel().getLocalizedName());
        // sb.append(": ");
        sb.append(" ");


        if (record.getSourceClassName() != null) {
            sb.append(record.getSourceClassName());
        } else {
            sb.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) {
            sb.append(" ");
            sb.append(record.getSourceMethodName());
        }
        sb.append(" - ");

        final String message = formatMessage(record);
        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try (final StringWriter sw = new StringWriter(); final PrintWriter pw = new PrintWriter(sw)) {
                record.getThrown().printStackTrace(pw);
                sb.append(sw);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
