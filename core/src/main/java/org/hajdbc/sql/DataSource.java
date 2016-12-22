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
package org.hajdbc.sql;

import java.sql.Connection;
import java.sql.SQLException;

import org.hajdbc.DatabaseCluster;
import org.hajdbc.SimpleDatabaseClusterConfigurationFactory;

/**
 * @author Paul Ferraro
 */
public class DataSource extends CommonDataSource<javax.sql.DataSource, DataSourceDatabase, DataSourceDatabaseBuilder, DataSourceProxyFactory> implements javax.sql.DataSource
{
	private final DataSourceDatabaseClusterConfigurationBuilder builder = new DataSourceDatabaseClusterConfigurationBuilder();

	@Override
	public DataSourceDatabaseClusterConfigurationBuilder getConfigurationBuilder()
	{
		this.setConfigurationFactory(new SimpleDatabaseClusterConfigurationFactory<javax.sql.DataSource, DataSourceDatabase>());
		return this.builder;
	}

	@Override
	public DataSourceProxyFactory createProxyFactory(DatabaseCluster<javax.sql.DataSource, DataSourceDatabase> cluster)
	{
		return new DataSourceProxyFactory(cluster);
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		String user = this.getUser();
		return (user != null) ? this.getProxy().getConnection(user, this.getPassword()) : this.getProxy().getConnection();
	}

	@Override
	public Connection getConnection(String user, String password) throws SQLException
	{
		return this.getProxy().getConnection(user, password);
	}

	@Override
	public boolean isWrapperFor(Class<?> targetClass) throws SQLException
	{
		return this.getProxy().isWrapperFor(targetClass);
	}

	@Override
	public <T> T unwrap(Class<T> targetClass) throws SQLException
	{
		return this.getProxy().unwrap(targetClass);
	}
}
