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
package io.github.hajdbc.durability.coarse;

import java.util.Map;
import java.util.SortedMap;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.ExceptionType;
import io.github.hajdbc.durability.DurabilityListener;
import io.github.hajdbc.durability.InvocationEvent;
import io.github.hajdbc.durability.InvocationEventImpl;
import io.github.hajdbc.durability.InvokerEvent;
import io.github.hajdbc.durability.none.NoDurability;
import io.github.hajdbc.invocation.InvocationStrategy;
import io.github.hajdbc.invocation.Invoker;
import io.github.hajdbc.sql.ProxyFactory;
import io.github.hajdbc.state.StateManager;

/**
 * {@link io.github.hajdbc.durability.Durability} implementation that tracks invocations only, but not per-database invokers.
 * This durability level can detect, but not recover from, mid-commit crashes.
 * @author Paul Ferraro
 */
/**
 * @author Paul Ferraro
 * @param <Z>
 * @param <D>
 */
public class CoarseDurability<Z, D extends Database<Z>> extends NoDurability<Z, D>
{
	protected final DatabaseCluster<Z, D> cluster;

	public CoarseDurability(DatabaseCluster<Z, D> cluster)
	{
		this.cluster = cluster;
	}

	@Override
	public InvocationEvent createInvocationEvent(Object transactionId, Phase phase, ExceptionType exceptionType)
	{
		return new InvocationEventImpl(transactionId, phase, exceptionType);
	}

	@Override
	public InvocationStrategy getInvocationStrategy(final InvocationStrategy strategy, final Phase phase, final Object transactionId)
	{
		final DurabilityListener listener = this.cluster.getStateManager();

		return new InvocationStrategy()
		{
			@Override
			public <ZZ, DD extends Database<ZZ>, T, R, EE extends Exception> SortedMap<DD, R> invoke(ProxyFactory<ZZ, DD, T, EE> proxy, Invoker<ZZ, DD, T, R, EE> invoker) throws EE
			{
				InvocationEvent event = new InvocationEventImpl(transactionId, phase, proxy.getExceptionFactory().getType());

				listener.beforeInvocation(event);

				try
				{
					return strategy.invoke(proxy, invoker);
				}
				catch (Exception e)
				{
					throw proxy.getExceptionFactory().createException(e);
				}
				finally
				{
					listener.afterInvocation(event);
				}
			}
		};
	}

	@Override
	public void recover(Map<InvocationEvent, Map<String, InvokerEvent>> invokers)
	{
		StateManager stateManager = this.cluster.getStateManager();

		for (D database: this.cluster.getBalancer().backups())
		{
			this.cluster.deactivate(database, stateManager);
		}

		for (InvocationEvent event: invokers.keySet())
		{
			stateManager.afterInvocation(event);
		}
	}
}
