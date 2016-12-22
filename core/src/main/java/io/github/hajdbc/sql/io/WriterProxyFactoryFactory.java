/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2013  Paul Ferraro
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
package io.github.hajdbc.sql.io;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Map;

import io.github.hajdbc.Database;
import io.github.hajdbc.invocation.Invoker;
import io.github.hajdbc.sql.ProxyFactory;
import io.github.hajdbc.sql.ProxyFactoryFactory;

/**
 * 
 * @author Paul Ferraro
 */
public class WriterProxyFactoryFactory<Z, D extends Database<Z>, P> implements ProxyFactoryFactory<Z, D, P, SQLException, Writer, IOException>
{
	@Override
	public ProxyFactory<Z, D, Writer, IOException> createProxyFactory(P parentProxy, ProxyFactory<Z, D, P, SQLException> parent, Invoker<Z, D, P, Writer, SQLException> invoker, Map<D, Writer> writers)
	{
		return new WriterProxyFactory<>(parentProxy, parent, invoker, writers);
	}
}
