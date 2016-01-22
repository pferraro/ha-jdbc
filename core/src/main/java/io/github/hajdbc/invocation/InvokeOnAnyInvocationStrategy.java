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
package io.github.hajdbc.invocation;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.ExceptionFactory;
import io.github.hajdbc.balancer.Balancer;
import io.github.hajdbc.dialect.Dialect;
import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;
import io.github.hajdbc.messages.Messages;
import io.github.hajdbc.messages.MessagesFactory;
import io.github.hajdbc.sql.ProxyFactory;
import io.github.hajdbc.state.StateManager;

/**
 * @author Paul Ferraro
 */
public class InvokeOnAnyInvocationStrategy implements InvocationStrategy
{
	private static final Messages messages = MessagesFactory.getMessages();
	private static final Logger logger = LoggerFactory.getLogger(ExistingResultsCollector.class);

	private final InvocationStrategy strategy;
	
	public InvokeOnAnyInvocationStrategy(InvocationStrategy strategy)
	{
		this.strategy = strategy;
	}

	@Override
	public <Z, D extends Database<Z>, T, R, E extends Exception> SortedMap<D, R> invoke(ProxyFactory<Z, D, T, E> factory, Invoker<Z, D, T, R, E> invoker) throws E
	{
		DatabaseCluster<Z, D> cluster = factory.getDatabaseCluster();
		Balancer<Z, D> balancer = cluster.getBalancer();
		Dialect dialect = cluster.getDialect();
		StateManager stateManager = cluster.getStateManager();

		for (Map.Entry<D, T> entry: factory.entries())
		{
			D database = entry.getKey();
			
			// If this database is no longer active, just skip it.
			if (balancer.contains(database))
			{
				try
				{
					R result = invoker.invoke(database, entry.getValue());
					
					SortedMap<D, R> resultMap = new TreeMap<>();
					resultMap.put(database, result);
					return resultMap;
				}
				catch (Exception e)
				{
					// If this database was concurrently deactivated, just ignore the failure
					if (cluster.getBalancer().contains(database))
					{
						ExceptionFactory<E> exceptionFactory = factory.getExceptionFactory();
						E exception = exceptionFactory.createException(e);
						
						if (exceptionFactory.indicatesFailure(exception, dialect) && (cluster.getBalancer().size() > 1))
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
		
		// If no existing databases could handle the request, delegate to another invocation strategy
		return this.strategy.invoke(factory, invoker);
	}
}
