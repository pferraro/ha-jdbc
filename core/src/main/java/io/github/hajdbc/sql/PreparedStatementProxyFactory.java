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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import io.github.hajdbc.Database;
import io.github.hajdbc.invocation.Invoker;
import io.github.hajdbc.util.reflect.Proxies;

/**
 * 
 * @author Paul Ferraro
 */
public class PreparedStatementProxyFactory<Z, D extends Database<Z>> extends AbstractPreparedStatementProxyFactory<Z, D, PreparedStatement>
{
	protected PreparedStatementProxyFactory(Connection parent, ProxyFactory<Z, D, Connection, SQLException> parentFactory, Invoker<Z, D, Connection, PreparedStatement, SQLException> invoker, Map<D, PreparedStatement> map, TransactionContext<Z, D> context, List<Lock> locks, boolean selectForUpdate)
	{
		super(parent, parentFactory, invoker, map, context, locks, selectForUpdate);
	}

	@Override
	public PreparedStatement createProxy()
	{
		return Proxies.createProxy(PreparedStatement.class, new PreparedStatementInvocationHandler<>(this));
	}
}
