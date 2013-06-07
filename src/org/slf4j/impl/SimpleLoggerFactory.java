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

import org.grlea.log.SimpleLogger;
import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;
import org.slf4j.helpers.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * SimpleLoggerFactory is an implementation of {@link ILoggerFactory} returning
 * the appropriately named {@link SimpleLoggerAdapter} instance.
 * 
 * @author NagasawaXien
 */
public class SimpleLoggerFactory implements ILoggerFactory {
    private Map<String, Logger> loggerMap = new HashMap<String, Logger>();

    public synchronized Logger getLogger(String name) {
        Logger logger = loggerMap.get(name);
        if (logger == null) {
            try {
                Class<?> cls = Class.forName(name);
                SimpleLogger ilogger = new SimpleLogger(cls);
                logger = new SimpleLoggerAdapter(ilogger, name);
            } catch (Exception e) {
                Util.report("Failed to find class specified by name : " + name + ". Use " + Logger.class.getName());
                SimpleLogger ilogger = new SimpleLogger(Logger.class);
                logger = new SimpleLoggerAdapter(ilogger, name);
            }
            loggerMap.put(name, logger);
        }
        return logger;
    }
}
