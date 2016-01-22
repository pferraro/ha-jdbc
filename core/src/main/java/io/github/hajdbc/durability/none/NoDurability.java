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
package io.github.hajdbc.durability.none;

import java.util.Map;

import io.github.hajdbc.Database;
import io.github.hajdbc.ExceptionFactory;
import io.github.hajdbc.ExceptionType;
import io.github.hajdbc.durability.Durability;
import io.github.hajdbc.durability.DurabilityEvent;
import io.github.hajdbc.durability.DurabilityEventImpl;
import io.github.hajdbc.durability.InvocationEvent;
import io.github.hajdbc.durability.InvokerEvent;
import io.github.hajdbc.invocation.InvocationStrategy;
import io.github.hajdbc.invocation.Invoker;
import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;

/**
 * {@link Durability} implementation that does not track anything.
 * This durability level cannot detect, nor recover from mid-commit crashes.
 * @author Paul Ferraro
 */
public class NoDurability<Z, D extends Database<Z>> implements Durability<Z, D>
{
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public InvocationStrategy getInvocationStrategy(InvocationStrategy strategy, Phase phase, Object transactionId)
	{
		return strategy;
	}

	@Override
	public <T, R, E extends Exception> Invoker<Z, D, T, R, E> getInvoker(Invoker<Z, D, T, R, E> invoker, Phase phase, Object transactionId, ExceptionFactory<E> exceptionFactory)
	{
		return invoker;
	}

	@Override
	public void recover(Map<InvocationEvent, Map<String, InvokerEvent>> invokers)
	{
		this.logger.log(Level.WARN, invokers.toString());
	}

	@Override
	public DurabilityEvent createEvent(Object transactionId, Phase phase)
	{
		return new DurabilityEventImpl(transactionId, phase);
	}

	@Override
	public InvocationEvent createInvocationEvent(Object transactionId, Phase phase, ExceptionType exceptionType)
	{
		return null;
	}

	@Override
	public InvokerEvent createInvokerEvent(Object transactionId, Phase phase, String databaseId)
	{
		return null;
	}
}
