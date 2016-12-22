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
package org.hajdbc.sync;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.SynchronizationStrategy;

/**
 * Work in progress...
 * @author Paul Ferraro
 */
public class FastDifferentialSynchronizationStrategy implements SynchronizationStrategy
{
	private static final long serialVersionUID = 2556031934309008750L;

	@Override
	public String getId()
	{
		return "delta";
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.SynchronizationStrategy#init(org.hajdbc.DatabaseCluster)
	 */
	@Override
	public <Z, D extends Database<Z>> void init(DatabaseCluster<Z, D> cluster)
	{
//		"UPDATE changes.table SET new_flag = $1 WHERE id = NEW.id;";
//		"INSERT INTO changes.table (id, new_flag) SELECT NEW.id, $1 WHERE NOT EXISTS (SELECT 1 FROM changes.table WHERE id = NEW.id);";
	}

	@Override
	public <Z, D extends Database<Z>> void synchronize(SynchronizationContext<Z, D> context)
	{
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.SynchronizationStrategy#destroy(org.hajdbc.DatabaseCluster)
	 */
	@Override
	public <Z, D extends Database<Z>> void destroy(DatabaseCluster<Z, D> cluster)
	{
	}
}
