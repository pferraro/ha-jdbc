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
package io.github.hajdbc.state.simple;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.state.StateManager;
import io.github.hajdbc.state.StateManagerFactory;

public class SimpleStateManagerFactory implements StateManagerFactory
{
	private static final long serialVersionUID = 4387061472532427110L;
	
	@Override
	public String getId()
	{
		return "simple";
	}

	/**
	 * {@inheritDoc}
	 * @see io.github.hajdbc.state.StateManagerFactory#createStateManager(io.github.hajdbc.DatabaseCluster)
	 */
	@Override
	public <Z, D extends Database<Z>> StateManager createStateManager(DatabaseCluster<Z, D> cluster)
	{
		return new SimpleStateManager();
	}
}
