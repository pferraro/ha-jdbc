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
package org.hajdbc.durability.fine;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.durability.Durability;
import org.hajdbc.durability.DurabilityFactory;
import org.kohsuke.MetaInfServices;

/**
 * Factory for creating a {@link FineDurability}.
 * @author Paul Ferraro
 */
@MetaInfServices(DurabilityFactory.class)
public class FineDurabilityFactory implements DurabilityFactory
{
	private static final long serialVersionUID = 8493031235326848199L;

	@Override
	public String getId()
	{
		return "fine";
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.durability.DurabilityFactory#createDurability(org.hajdbc.DatabaseCluster)
	 */
	@Override
	public <Z, D extends Database<Z>> Durability<Z, D> createDurability(DatabaseCluster<Z, D> cluster)
	{
		return new FineDurability<>(cluster);
	}
}
