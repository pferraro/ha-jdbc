/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2013  Paul Ferraro
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
package org.hajdbc.sql;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.DatabaseClusterListener;
import org.hajdbc.state.DatabaseEvent;

/**
 * 
 * @author Paul Ferraro
 */
public abstract class AbstractRootProxyFactory<Z, D extends Database<Z>> extends AbstractProxyFactory<Z, D, SQLException, Z, SQLException> implements RootProxyFactory<Z, D>, DatabaseClusterListener
{
	protected AbstractRootProxyFactory(DatabaseCluster<Z, D> cluster)
	{
		super(cluster, createInitialMap(cluster), SQLException.class);
		
		cluster.addListener(this);
	}

	private static <Z, D extends Database<Z>> Map<D, Z> createInitialMap(DatabaseCluster<Z, D> cluster)
	{
		Map<D, Z> map = new TreeMap<>();
		for (D database: cluster.getBalancer())
		{
			map.put(database, database.getConnectionSource());
		}
		return map;
	}
	
	@Override
	protected Z create(D database)
	{
		return database.getConnectionSource();
	}

	@Override
	public void activated(DatabaseEvent event)
	{
		// Do nothing
	}

	@Override
	public synchronized void deactivated(DatabaseEvent event)
	{
		D database = this.getDatabaseCluster().getDatabase(event.getSource());
		
		for (ChildProxyFactory<Z, D, Z, SQLException, ?, ? extends Exception> child: this.children())
		{
			child.close(database);
		}
	}

	@Override
	public void close()
	{
		this.getDatabaseCluster().removeListener(this);
	}
}
