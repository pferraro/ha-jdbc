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
package io.github.hajdbc.state.distributed;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.state.DatabaseEvent;
import io.github.hajdbc.state.StateManager;

public class DeactivationCommand<Z, D extends Database<Z>> extends StateCommand<Z, D>
{
	private static final long serialVersionUID = -601538840572935794L;

	public DeactivationCommand(DatabaseEvent event)
	{
		super(event);
	}

	@Override
	protected boolean execute(DatabaseCluster<Z, D> cluster, StateManager stateManager, D database)
	{
		return cluster.deactivate(database, stateManager);
	}
}
