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
package org.hajdbc.durability.none;

import java.util.Map;

import org.hajdbc.Database;
import org.hajdbc.ExceptionFactory;
import org.hajdbc.ExceptionType;
import org.hajdbc.durability.Durability;
import org.hajdbc.durability.DurabilityEvent;
import org.hajdbc.durability.DurabilityEventImpl;
import org.hajdbc.durability.InvocationEvent;
import org.hajdbc.durability.InvokerEvent;
import org.hajdbc.invocation.InvocationStrategy;
import org.hajdbc.invocation.Invoker;
import org.hajdbc.logging.Level;
import org.hajdbc.logging.Logger;
import org.hajdbc.logging.LoggerFactory;

/**
 * {@link Durability} implementation that does not track anything.
 * This durability level cannot detect, nor recover from mid-commit crashes.
 * @author Paul Ferraro
 */
public class NoDurability<Z, D extends Database<Z>> implements Durability<Z, D>
{
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.durability.Durability#getInvocationStrategy(org.hajdbc.invocation.InvocationStrategy, org.hajdbc.durability.Durability.Phase, java.lang.Object)
	 */
	@Override
	public InvocationStrategy getInvocationStrategy(InvocationStrategy strategy, Phase phase, Object transactionId)
	{
		return strategy;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.durability.Durability#getInvoker(org.hajdbc.invocation.Invoker, org.hajdbc.durability.Durability.Phase, java.lang.Object, org.hajdbc.ExceptionFactory)
	 */
	@Override
	public <T, R, E extends Exception> Invoker<Z, D, T, R, E> getInvoker(Invoker<Z, D, T, R, E> invoker, Phase phase, Object transactionId, ExceptionFactory<E> exceptionFactory)
	{
		return invoker;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.durability.Durability#recover(java.util.Map)
	 */
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
