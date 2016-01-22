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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.ExceptionFactory;
import io.github.hajdbc.dialect.Dialect;
import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;
import io.github.hajdbc.messages.Messages;
import io.github.hajdbc.messages.MessagesFactory;
import io.github.hajdbc.sql.ProxyFactory;
import io.github.hajdbc.state.StateManager;

/**
 * @author paul
 *
 */
public class InvokeOnManyInvocationStrategy implements InvocationStrategy
{
	private static final Messages messages = MessagesFactory.getMessages();
	private static final Logger logger = LoggerFactory.getLogger(InvokeOnManyInvocationStrategy.class);
	
	public static interface ResultsCollector
	{
		<Z, D extends Database<Z>, T, R, E extends Exception> Map.Entry<SortedMap<D, R>, SortedMap<D, E>> collectResults(ProxyFactory<Z, D, T, E> map, Invoker<Z, D, T, R, E> invoker);
	}

	private final ResultsCollector collector;
	
	public InvokeOnManyInvocationStrategy(ResultsCollector collector)
	{
		this.collector = collector;
	}

	@Override
	public <Z, D extends Database<Z>, T, R, E extends Exception> SortedMap<D, R> invoke(ProxyFactory<Z, D, T, E> factory, Invoker<Z, D, T, R, E> invoker) throws E
	{
		Map.Entry<SortedMap<D, R>, SortedMap<D, E>> results = this.collector.collectResults(factory, invoker);
		ExceptionFactory<E> exceptionFactory = factory.getExceptionFactory();
		SortedMap<D, R> resultMap = results.getKey();
		SortedMap<D, E> exceptionMap = results.getValue();
		
		if (!exceptionMap.isEmpty())
		{
			DatabaseCluster<Z, D> cluster = factory.getDatabaseCluster();
			Dialect dialect = cluster.getDialect();
			
			List<D> failedDatabases = new ArrayList<>(exceptionMap.size());
			
			// Determine which exceptions are due to failures
			for (Map.Entry<D, E> entry: exceptionMap.entrySet())
			{
				if (exceptionFactory.indicatesFailure(entry.getValue(), dialect))
				{
					failedDatabases.add(entry.getKey());
				}
			}

			StateManager stateManager = cluster.getStateManager();
			
			// Deactivate failed databases, unless all failed
			if (!resultMap.isEmpty() || (failedDatabases.size() < exceptionMap.size()))
			{
				for (D failedDatabase: failedDatabases)
				{
					E exception = exceptionMap.remove(failedDatabase);
					
					if (cluster.deactivate(failedDatabase, stateManager))
					{
						logger.log(Level.ERROR, exception, messages.deactivated(cluster, failedDatabase));
					}
				}
			}
			
			if (!exceptionMap.isEmpty())
			{
				// If primary database threw exception
				if (resultMap.isEmpty() || !exceptionMap.headMap(resultMap.firstKey()).isEmpty())
				{
					D primaryDatabase = exceptionMap.firstKey();
					E primaryException = exceptionMap.get(primaryDatabase);
					
					// Deactivate databases with non-matching exceptions
					for (Map.Entry<D, E> entry: exceptionMap.tailMap(primaryDatabase).entrySet())
					{
						E exception = entry.getValue();
						
						if (!exceptionFactory.equals(exception, primaryException))
						{
							D database = entry.getKey();
							
							if (cluster.deactivate(database, stateManager))
							{
								logger.log(Level.ERROR, exception, messages.inconsistent(cluster, database, primaryException, exception));
							}
						}
					}
	
					// Deactivate databases with results
					for (Map.Entry<D, R> entry: resultMap.entrySet())
					{
						D database = entry.getKey();
						
						if (cluster.deactivate(database, stateManager))
						{
							logger.log(Level.ERROR, messages.inconsistent(cluster, database, primaryException, entry.getValue()));
						}
					}
					
					throw primaryException;
				}
			}
			// Else primary was successful
			// Deactivate databases with exceptions
			for (Map.Entry<D, E> entry: exceptionMap.entrySet())
			{
				D database = entry.getKey();
				E exception = entry.getValue();
				
				if (cluster.deactivate(database, stateManager))
				{
					logger.log(Level.ERROR, exception, messages.deactivated(cluster, database));
				}
			}
		}
		
		return resultMap;
	}
}
