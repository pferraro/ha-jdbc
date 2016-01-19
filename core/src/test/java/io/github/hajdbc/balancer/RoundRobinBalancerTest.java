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
package io.github.hajdbc.balancer;

import static org.junit.Assert.*;

import io.github.hajdbc.MockDatabase;
import io.github.hajdbc.balancer.Balancer;
import io.github.hajdbc.balancer.roundrobin.RoundRobinBalancerFactory;


/**
 * @author Paul Ferraro
 */
public class RoundRobinBalancerTest extends AbstractBalancerTest
{
	public RoundRobinBalancerTest()
	{
		super(new RoundRobinBalancerFactory());
	}
	
	@Override
	public void next(Balancer<Void, MockDatabase> balancer)
	{
		int[] expected = new int[] { 1, 2, 2 };
		
		for (int i = 0; i < 100; ++i)
		{
			assertSame(this.databases[expected[i % 3]], balancer.next());
		}
	}
}
