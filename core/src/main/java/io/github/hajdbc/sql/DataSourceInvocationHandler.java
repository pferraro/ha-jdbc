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
package io.github.hajdbc.sql;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import io.github.hajdbc.invocation.InvocationStrategies;
import io.github.hajdbc.invocation.InvocationStrategy;
import io.github.hajdbc.util.reflect.Methods;

/**
 * @author Paul Ferraro
 *
 */
public class DataSourceInvocationHandler extends CommonDataSourceInvocationHandler<DataSource, DataSourceDatabase, DataSourceProxyFactory>
{
	private static final Set<Method> getConnectionMethods = Methods.findMethods(DataSource.class, "getConnection");
	
	public DataSourceInvocationHandler(DataSourceProxyFactory factory)
	{
		super(DataSource.class, factory);
	}

	/**
	 * {@inheritDoc}
	 * @see io.github.hajdbc.sql.CommonDataSourceInvocationHandler#getInvocationStrategy(javax.sql.CommonDataSource, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	protected InvocationStrategy getInvocationStrategy(DataSource dataSource, Method method, Object... parameters) throws SQLException
	{
		if (getConnectionMethods.contains(method))
		{
			return InvocationStrategies.TRANSACTION_INVOKE_ON_ALL;
		}
		return super.getInvocationStrategy(dataSource, method, parameters);
	}

	@Override
	protected ProxyFactoryFactory<DataSource, DataSourceDatabase, DataSource, SQLException, ?, ? extends Exception> getProxyFactoryFactory(DataSource object, Method method, Object... parameters) throws SQLException
	{
		if (getConnectionMethods.contains(method))
		{
			TransactionContext<DataSource, DataSourceDatabase> context = new LocalTransactionContext<>(this.getProxyFactory().getDatabaseCluster());
			
			return new ConnectionProxyFactoryFactory<>(context);
		}
		
		return super.getProxyFactoryFactory(object, method, parameters);
	}
}
