/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2012  Paul Ferraro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.hajdbc.logging.jboss;

import java.util.EnumMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.github.hajdbc.logging.AbstractLogger;
import io.github.hajdbc.logging.Level;

/**
 * @author Paul Ferraro
 */
public class JBossLogger extends AbstractLogger
{
	private static final Map<Level, org.jboss.logging.Logger.Level> levels = new EnumMap<>(Level.class);
	static
	{
		levels.put(Level.ERROR, org.jboss.logging.Logger.Level.ERROR);
		levels.put(Level.WARN, org.jboss.logging.Logger.Level.WARN);
		levels.put(Level.INFO, org.jboss.logging.Logger.Level.INFO);
		levels.put(Level.DEBUG, org.jboss.logging.Logger.Level.DEBUG);
		levels.put(Level.TRACE, org.jboss.logging.Logger.Level.TRACE);
	}
	
	private final Logger logger;
	
	public JBossLogger(Class<?> targetClass)
	{
		this.logger = Logger.getLogger(targetClass);
	}
	
	/**
	 * {@inheritDoc}
	 * @see io.github.hajdbc.logging.Logger#log(io.github.hajdbc.logging.Level, java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(Level level, Throwable e, String pattern, Object... args)
	{
		this.logger.logv(levels.get(level), e, pattern, args);
	}
}
