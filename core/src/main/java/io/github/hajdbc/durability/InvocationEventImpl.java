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
package io.github.hajdbc.durability;

import io.github.hajdbc.ExceptionType;

/**
 * @author Paul Ferraro
 */
public class InvocationEventImpl extends DurabilityEventImpl implements InvocationEvent
{	
	private static final long serialVersionUID = 3577057545595076359L;
	
	private final ExceptionType exceptionType;
	
	public InvocationEventImpl(Object transactionId, Durability.Phase phase, ExceptionType exceptionType)
	{
		super(transactionId, phase);
		
		this.exceptionType = exceptionType;
	}

	@Override
	public ExceptionType getExceptionType()
	{
		return this.exceptionType;
	}
}
