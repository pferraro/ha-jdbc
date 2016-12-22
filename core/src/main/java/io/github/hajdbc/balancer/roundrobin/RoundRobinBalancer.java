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
package io.github.hajdbc.balancer.roundrobin;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import io.github.hajdbc.Database;
import io.github.hajdbc.balancer.AbstractSetBalancer;

/**
 * Balancer implementation whose {@link #next()} implementation uses a circular FIFO queue.
 * 
 * @author  Paul Ferraro
 * @param <D> either java.sql.Driver or javax.sql.DataSource
 */
public class RoundRobinBalancer<P, D extends Database<P>> extends AbstractSetBalancer<P, D>
{
	private Queue<D> databaseQueue = new LinkedList<>();

	/**
	 * Constructs a new RoundRobinBalancer
	 * @param databases
	 */
	public RoundRobinBalancer(Set<D> databases)
	{
		super(databases);
		
		for (D database: databases)
		{
			this.added(database);
		}
	}
	
	@Override
	protected void added(D database)
	{
		int weight = database.getWeight();
		
		for (int i = 0; i < weight; ++i)
		{
			this.databaseQueue.add(database);
		}
	}

	@Override
	protected void removed(D database)
	{
		int weight = database.getWeight();
		
		for (int i = 0; i < weight; ++i)
		{
			this.databaseQueue.remove(database);
		}
	}
	
	@Override
	public D next()
	{
		this.getLock().lock();
		
		try
		{
			if (this.databaseQueue.isEmpty())
			{
				return this.primary();
			}
			
			if (this.databaseQueue.size() == 1)
			{
				return this.databaseQueue.element();
			}
			
			D database = this.databaseQueue.remove();
			
			this.databaseQueue.add(database);
			
			return database;
		}
		finally
		{
			this.getLock().unlock();
		}
	}

	@Override
	protected void cleared()
	{
		this.databaseQueue.clear();
	}
}
