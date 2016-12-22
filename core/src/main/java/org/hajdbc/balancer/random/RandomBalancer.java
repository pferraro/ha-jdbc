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
package org.hajdbc.balancer.random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.hajdbc.Database;
import org.hajdbc.balancer.AbstractSetBalancer;

/**
 * Balancer implementation whose {@link #next()} implementation returns a random database.
 * The probability that a given database will be returned is: <em>weight / total-weight</em>.
 * 
 * @author  Paul Ferraro
 * @param <D> either java.sql.Driver or javax.sql.DataSource
 */
public class RandomBalancer<P, D extends Database<P>> extends AbstractSetBalancer<P, D>
{
	private volatile List<D> databaseList;

	private Random random = new Random();

	/**
	 * Constructs a new RandomBalancer
	 * @param databases
	 */
	public RandomBalancer(Set<D> databases)
	{
		super(databases);
		
		int total = 0;
		
		for (D database: databases)
		{
			total += database.getWeight();
		}
		
		List<D> list = new ArrayList<>(total);
		
		for (D database: databases)
		{
			int weight = database.getWeight();
			for (int i = 0; i < weight; ++i)
			{
				list.add(database);
			}
		}
		
		this.databaseList = list;
	}

	@Override
	public D next()
	{
		List<D> list = this.databaseList;
		
		return !list.isEmpty() ? list.get(this.random.nextInt(list.size())) : this.primary();
	}

	@Override
	protected void added(D database)
	{
		int weight = database.getWeight();
		
		if (weight > 0)
		{
			List<D> list = new ArrayList<>(this.databaseList.size() + weight);
			
			list.addAll(this.databaseList);
			
			for (int i = 0; i < weight; ++i)
			{
				list.add(database);
			}
			
			this.databaseList = list;
		}
	}

	@Override
	protected void removed(D database)
	{
		int weight = database.getWeight();

		if (weight > 0)
		{
			List<D> list = new ArrayList<>(this.databaseList.size() - weight);
			
			int index = this.databaseList.indexOf(database);
			
			list.addAll(this.databaseList.subList(0, index));
			list.addAll(this.databaseList.subList(index + weight, this.databaseList.size()));
			
			this.databaseList = list;
		}
	}

	@Override
	protected void cleared()
	{
		this.databaseList = Collections.emptyList();
	}
}
