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
package org.hajdbc;

import java.sql.SQLException;

/**
 * @author Paul Ferraro
 */
public class SimpleDatabaseClusterConfigurationFactory<Z, D extends Database<Z>> implements DatabaseClusterConfigurationFactory<Z, D>
{
	private static final long serialVersionUID = -6882420729056764462L;
	
	@Override
	public <B extends DatabaseBuilder<Z, D>> DatabaseClusterConfiguration<Z, D> createConfiguration(DatabaseClusterConfigurationBuilder<Z, D, B> builder) throws SQLException
	{
		return builder.build();
	}

	@Override
	public void added(D database, DatabaseClusterConfiguration<Z, D> configuration)
	{
	}

	@Override
	public void removed(D database, DatabaseClusterConfiguration<Z, D> configuration)
	{
	}
}
