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

import java.util.Set;

import org.hajdbc.Database;
import org.hajdbc.balancer.Balancer;
import org.hajdbc.balancer.BalancerFactory;
import org.kohsuke.MetaInfServices;

/**
 * Factory for creating a {@link RandomBalancer}
 * @author Paul Ferraro
 */
@MetaInfServices(BalancerFactory.class)
public class RandomBalancerFactory implements BalancerFactory
{
	private static final long serialVersionUID = -910276247314814572L;

	@Override
	public String getId()
	{
		return "random";
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.balancer.BalancerFactory#createBalancer(java.util.Set)
	 */
	@Override
	public <Z, D extends Database<Z>> Balancer<Z, D> createBalancer(Set<D> databases)
	{
		return new RandomBalancer<>(databases);
	}
}
