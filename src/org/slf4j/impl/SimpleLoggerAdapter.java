/*
 * Copyright (c) 2012-2013 NagasawaXien
 *
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute, and/or sell copies of  the Software, and to permit persons
 * to whom  the Software is furnished  to do so, provided  that the above
 * copyright notice(s) and this permission notice appear in all copies of
 * the  Software and  that both  the above  copyright notice(s)  and this
 * permission notice appear in supporting documentation.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR  A PARTICULAR PURPOSE AND NONINFRINGEMENT
 * OF  THIRD PARTY  RIGHTS. IN  NO EVENT  SHALL THE  COPYRIGHT  HOLDER OR
 * HOLDERS  INCLUDED IN  THIS  NOTICE BE  LIABLE  FOR ANY  CLAIM, OR  ANY
 * SPECIAL INDIRECT  OR CONSEQUENTIAL DAMAGES, OR  ANY DAMAGES WHATSOEVER
 * RESULTING FROM LOSS  OF USE, DATA OR PROFITS, WHETHER  IN AN ACTION OF
 * CONTRACT, NEGLIGENCE  OR OTHER TORTIOUS  ACTION, ARISING OUT OF  OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * Except as  contained in  this notice, the  name of a  copyright holder
 * shall not be used in advertising or otherwise to promote the sale, use
 * or other dealings in this Software without prior written authorization
 * of the copyright holder.
 *
 */
package org.slf4j.impl;

import org.grlea.log.DebugLevel;
import org.grlea.log.SimpleLogger;
import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * A wrapper over {@link org.grlea.log.SimpleLogger org.grlea.log.SimpleLogger} in
 * conformity with the {@link Logger} interface. Note that the logging levels
 * mentioned in this class refer to those defined in the org.grlea.log.DebugLevel.
 */
public final class SimpleLoggerAdapter extends MarkerIgnoringBase {
    private static final long serialVersionUID = 3640289244648147856L;
    private final SimpleLogger logger;

    SimpleLoggerAdapter(SimpleLogger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.wouldLog(DebugLevel.L2_ERROR);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.wouldLog(DebugLevel.L3_WARN);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.wouldLog(DebugLevel.L4_INFO);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.wouldLog(DebugLevel.L5_DEBUG);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.wouldLog(DebugLevel.L6_VERBOSE);
    }

    private void _log(DebugLevel level, String msg) {
        if (!logger.wouldLog(level) || msg == null || msg.isEmpty())
            return;

        logger.db(level, msg);
    }

    private void _log(DebugLevel level, FormattingTuple ft) {
        if (logger.wouldLog(level)) {
            logger.db(level, ft.getMessage());
            Throwable t = ft.getThrowable();
            if (t != null) {
                logger.dbe(level, t);
            }
        }
    }

    // ERROR
    @Override
    public void error(String arg0) {
        _log(DebugLevel.L2_ERROR, arg0);
    }

    @Override
    public void error(String arg0, Object arg1) {
        _log(DebugLevel.L2_ERROR, MessageFormatter.format(arg0, arg1));
    }

    @Override
    public void error(String arg0, Object arg1, Object arg2) {
        _log(DebugLevel.L2_ERROR, MessageFormatter.format(arg0, arg1, arg2));
    }

    @Override
    public void error(String arg0, Object... arg1) {
        _log(DebugLevel.L2_ERROR, MessageFormatter.arrayFormat(arg0, arg1));
    }

    @Override
    public void error(String arg0, Throwable arg1) {
        _log(DebugLevel.L2_ERROR, MessageFormatter.format(arg0, arg1));
    }

    // WARN
    @Override
    public void warn(String arg0) {
        _log(DebugLevel.L3_WARN, arg0);
    }

    @Override
    public void warn(String arg0, Object arg1) {
        _log(DebugLevel.L3_WARN, MessageFormatter.format(arg0, arg1));
    }

    @Override
    public void warn(String arg0, Object arg1, Object arg2) {
        _log(DebugLevel.L3_WARN, MessageFormatter.format(arg0, arg1, arg2));
    }

    @Override
    public void warn(String arg0, Object... arg1) {
        _log(DebugLevel.L3_WARN, MessageFormatter.arrayFormat(arg0, arg1));
    }

    @Override
    public void warn(String arg0, Throwable arg1) {
        _log(DebugLevel.L3_WARN, MessageFormatter.format(arg0, arg1));
    }

    // INFO

    @Override
    public void info(String arg0) {
        _log(DebugLevel.L4_INFO, arg0);
    }

    @Override
    public void info(String arg0, Object arg1) {
        _log(DebugLevel.L4_INFO, MessageFormatter.format(arg0, arg1));
    }

    @Override
    public void info(String arg0, Object arg1, Object arg2) {
        _log(DebugLevel.L4_INFO, MessageFormatter.format(arg0, arg1, arg2));
    }

    @Override
    public void info(String arg0, Object... arg1) {
        _log(DebugLevel.L4_INFO, MessageFormatter.arrayFormat(arg0, arg1));
    }

    @Override
    public void info(String arg0, Throwable arg1) {
        _log(DebugLevel.L4_INFO, MessageFormatter.format(arg0, arg1));
    }

    // DEBUG
    @Override
    public void debug(String arg0) {
        _log(DebugLevel.L5_DEBUG, arg0);
    }

    @Override
    public void debug(String arg0, Object arg1) {
        _log(DebugLevel.L5_DEBUG, MessageFormatter.format(arg0, arg1));
    }

    @Override
    public void debug(String arg0, Object arg1, Object arg2) {
        _log(DebugLevel.L5_DEBUG, MessageFormatter.format(arg0, arg1, arg2));
    }

    @Override
    public void debug(String arg0, Object... arg1) {
        _log(DebugLevel.L5_DEBUG, MessageFormatter.arrayFormat(arg0, arg1));
    }

    @Override
    public void debug(String arg0, Throwable arg1) {
        _log(DebugLevel.L5_DEBUG, MessageFormatter.format(arg0, arg1));
    }

    // TRACE
    @Override
    public void trace(String arg0) {
        _log(DebugLevel.L6_VERBOSE, arg0);
    }

    @Override
    public void trace(String arg0, Object arg1) {
        _log(DebugLevel.L6_VERBOSE, MessageFormatter.format(arg0, arg1));
    }

    @Override
    public void trace(String arg0, Object arg1, Object arg2) {
        _log(DebugLevel.L6_VERBOSE, MessageFormatter.format(arg0, arg1, arg2));
    }

    @Override
    public void trace(String arg0, Object... arg1) {
        _log(DebugLevel.L6_VERBOSE, MessageFormatter.arrayFormat(arg0, arg1));
    }

    @Override
    public void trace(String arg0, Throwable arg1) {
        _log(DebugLevel.L6_VERBOSE, MessageFormatter.format(arg0, arg1));
    }
}
