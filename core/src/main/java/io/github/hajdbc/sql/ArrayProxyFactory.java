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
package io.github.hajdbc.sql;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Map;

import io.github.hajdbc.Database;
import io.github.hajdbc.invocation.Invoker;
import io.github.hajdbc.util.reflect.Proxies;

/**
 * 
 * @author Paul Ferraro
 */
public class ArrayProxyFactory<Z, D extends Database<Z>, P> extends LocatorProxyFactory<Z, D, P, Array>
{
	public ArrayProxyFactory(P parentProxy, ProxyFactory<Z, D, P, SQLException> parent, Invoker<Z, D, P, Array, SQLException> invoker, Map<D, Array> locators, boolean locatorsUpdateCopy)
	{
		super(parentProxy, parent, invoker, locators, locatorsUpdateCopy);
	}

	@Override
	public void close(D database, Array array) throws SQLException
	{
		array.free();
	}

	@Override
	public Array createProxy()
	{
		return Proxies.createProxy(Array.class, new ArrayInvocationHandler<>(this));
	}
}
