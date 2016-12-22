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
package org.hajdbc.dialect.mckoi;

import org.hajdbc.SequenceSupport;
import org.hajdbc.dialect.StandardDialect;

/**
 * Dialect for <a href="http://mckoi.com">Mckoi</a>.
 * 
 * @author Paul Ferraro
 */
@SuppressWarnings("nls")
public class MckoiDialect extends StandardDialect
{
	@Override
	protected String vendorPattern()
	{
		return "mckoi";
	}

	@Override
	public SequenceSupport getSequenceSupport()
	{
		return this;
	}

	@Override
	protected String sequencePattern()
	{
		return "(?:CURR|NEXT)VAL\\s*\\(\\s*'([^']+)'\\s*\\)";
	}

	@Override
	protected String currentTimestampPattern()
	{
		return super.currentTimestampPattern() + "|(?<=\\W)DATEOB\\s*\\(\\s*\\)";
	}
}
