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
package org.hajdbc.dialect.postgresql;

import org.hajdbc.dialect.Dialect;
import org.hajdbc.dialect.DialectFactory;

public class PostgreSQLDialectFactory implements DialectFactory
{
	private static final long serialVersionUID = -1228325684874648061L;

	@Override
	public String getId()
	{
		return "postgresql";
	}

	@Override
	public Dialect createDialect()
	{
		return new PostgreSQLDialect();
	}
}
