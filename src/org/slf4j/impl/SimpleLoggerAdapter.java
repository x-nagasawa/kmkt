/*
 * Copyright (c) 2004-2005 SLF4J.ORG
 * Copyright (c) 2004-2005 QOS.ch
 * Copyright (c) 2012 NagasawaXien
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
    private static final long serialVersionUID = 3640289244648147855L;
    private final SimpleLogger logger;

    SimpleLoggerAdapter(SimpleLogger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    public boolean isErrorEnabled() {
        return logger.wouldLog(DebugLevel.L2_ERROR);
    }

    public boolean isWarnEnabled() {
        return logger.wouldLog(DebugLevel.L3_WARN);
    }

    public boolean isInfoEnabled() {
        return logger.wouldLog(DebugLevel.L4_INFO);
    }

    public boolean isDebugEnabled() {
        return logger.wouldLog(DebugLevel.L5_DEBUG);
    }

    public boolean isTraceEnabled() {
        return logger.wouldLog(DebugLevel.L6_VERBOSE);
    }


    public void error(String arg0) {
        logger.error(arg0);
    }

    public void error(String arg0, Object arg1) {
        if (isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1);
            if (ft.getThrowable() == null) {
                error(ft.getMessage());
            } else {
                error(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void error(String arg0, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1, arg2);
            if (ft.getThrowable() == null) {
                error(ft.getMessage());
            } else {
                error(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void error(String arg0, Object[] arg1) {
        if (isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(arg0, arg1);
            if (ft.getThrowable() == null) {
                error(ft.getMessage());
            } else {
                error(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void error(String arg0, Throwable arg1) {
        logger.error(arg0);
        logger.errorException(arg1);
    }

    public void warn(String arg0) {
        logger.warn(arg0);
    }

    public void warn(String arg0, Object arg1) {
        if (isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1);
            if (ft.getThrowable() == null) {
                warn(ft.getMessage());
            } else {
                warn(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void warn(String arg0, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1, arg2);
            if (ft.getThrowable() == null) {
                warn(ft.getMessage());
            } else {
                warn(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void warn(String arg0, Object[] arg1) {
        if (isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(arg0, arg1);
            if (ft.getThrowable() == null) {
                warn(ft.getMessage());
            } else {
                warn(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void warn(String arg0, Throwable arg1) {
        logger.warn(arg0);
        logger.warnException(arg1);
    }

    public void info(String arg0) {
        logger.info(arg0);
    }

    public void info(String arg0, Object arg1) {
        if (isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1);
            if (ft.getThrowable() == null) {
                info(ft.getMessage());
            } else {
                info(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void info(String arg0, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1, arg2);
            if (ft.getThrowable() == null) {
                info(ft.getMessage());
            } else {
                info(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void info(String arg0, Object[] arg1) {
        if (isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(arg0, arg1);
            if (ft.getThrowable() == null) {
                info(ft.getMessage());
            } else {
                info(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void info(String arg0, Throwable arg1) {
        logger.info(arg0);
        logger.dbe(DebugLevel.L4_INFO, arg1);
    }

    public void debug(String arg0) {
        logger.debug(arg0);
    }

    public void debug(String arg0, Object arg1) {
        if (isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1);
            if (ft.getThrowable() == null) {
                debug(ft.getMessage());
            } else {
                debug(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void debug(String arg0, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1, arg2);
            if (ft.getThrowable() == null) {
                debug(ft.getMessage());
            } else {
                debug(ft.getMessage(), ft.getThrowable());
            }
        }
    }


    public void debug(String arg0, Object[] arg1) {
        if (isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(arg0, arg1);
            if (ft.getThrowable() == null) {
                debug(ft.getMessage());
            } else {
                debug(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void debug(String arg0, Throwable arg1) {
        logger.debug(arg0);
        logger.dbe(DebugLevel.L5_DEBUG, arg1);
    }

    public void trace(String arg0) {
        logger.verbose(arg0);
    }

    public void trace(String arg0, Object arg1) {
        if (isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1);
            if (ft.getThrowable() == null) {
                trace(ft.getMessage());
            } else {
                trace(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void trace(String arg0, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(arg0, arg1, arg2);
            if (ft.getThrowable() == null) {
                trace(ft.getMessage());
            } else {
                trace(ft.getMessage(), ft.getThrowable());
            }
        }
    }


    public void trace(String arg0, Object[] arg1) {
        if (isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(arg0, arg1);
            if (ft.getThrowable() == null) {
                trace(ft.getMessage());
            } else {
                trace(ft.getMessage(), ft.getThrowable());
            }
        }
    }

    public void trace(String arg0, Throwable arg1) {
        logger.verbose(arg0);
        logger.dbe(DebugLevel.L6_VERBOSE, arg1);
    }
}
