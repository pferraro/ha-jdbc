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
package org.hajdbc.logging.jboss;

import org.hajdbc.logging.Logger;
import org.hajdbc.logging.LoggingProvider;

/**
 * @author Paul Ferraro
 */
public class JBossLoggingProvider implements LoggingProvider
{
	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.logging.LoggingProvider#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		try
		{
			this.getClass().getClassLoader().loadClass("org.jboss.logging.Logger");
			return true;
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.logging.LoggingProvider#getLogger(java.lang.Class)
	 */
	@Override
	public Logger getLogger(Class<?> targetClass)
	{
		return new JBossLogger(targetClass);
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.logging.LoggingProvider#getName()
	 */
	@Override
	public String getName()
	{
		return "JBoss";
	}
}
