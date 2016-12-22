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
package org.hajdbc.invocation;

import java.util.SortedMap;
import java.util.TreeMap;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.ExceptionFactory;
import org.hajdbc.balancer.Balancer;
import org.hajdbc.dialect.Dialect;
import org.hajdbc.logging.Level;
import org.hajdbc.logging.Logger;
import org.hajdbc.logging.LoggerFactory;
import org.hajdbc.messages.Messages;
import org.hajdbc.messages.MessagesFactory;
import org.hajdbc.sql.ProxyFactory;
import org.hajdbc.state.StateManager;

/**
 * @author paul
 *
 */
public class InvokeOnOneInvocationStrategy implements InvocationStrategy
{
	private static final Messages messages = MessagesFactory.getMessages();
	private static Logger logger = LoggerFactory.getLogger(InvokeOnOneInvocationStrategy.class);
	
	public static interface DatabaseSelector
	{
		<Z, D extends Database<Z>> D selectDatabase(Balancer<Z, D> balancer);
	}

	private final DatabaseSelector selector;
	
	public InvokeOnOneInvocationStrategy(DatabaseSelector selector)
	{
		this.selector = selector;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public <Z, D extends Database<Z>, T, R, E extends Exception> SortedMap<D, R> invoke(ProxyFactory<Z, D, T, E> factory, Invoker<Z, D, T, R, E> invoker) throws E
	{
		DatabaseCluster<Z, D> cluster = factory.getDatabaseCluster();
		ExceptionFactory<E> exceptionFactory = factory.getExceptionFactory();
		Balancer<Z, D> balancer = cluster.getBalancer();
		Dialect dialect = cluster.getDialect();
		StateManager stateManager = cluster.getStateManager();
		
		while (true)
		{
			D database = this.selector.selectDatabase(balancer);
			
			if (database == null)
			{
				throw exceptionFactory.createException(messages.noActiveDatabases(cluster));
			}
			
			T object = factory.get(database);
			
			try
			{
				R result = balancer.invoke(invoker, database, object);
				
				SortedMap<D, R> resultMap = new TreeMap<>();
				resultMap.put(database, result);
				return resultMap;
			}
			catch (Exception e)
			{
				// If this database was concurrently deactivated, just ignore the failure
				if (balancer.contains(database))
				{
					E exception = exceptionFactory.createException(e);
					
					if (exceptionFactory.indicatesFailure(exception, dialect))
					{
						if (cluster.deactivate(database, stateManager))
						{
							logger.log(Level.ERROR, exception, messages.deactivated(cluster, database));
						}
					}
					else
					{
						throw exception;
					}
				}
			}
		}
	}
}
