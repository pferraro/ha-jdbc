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
package io.github.hajdbc.sql;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import io.github.hajdbc.Credentials;
import io.github.hajdbc.Locality;
import io.github.hajdbc.codec.Decoder;
import io.github.hajdbc.management.Description;
import io.github.hajdbc.management.MBean;


/**
 * A database described by a {@link DataSource}.
 * @author  Paul Ferraro
 */
@MBean
@Description("Database accessed via a DataSource")
public class DataSourceDatabase extends CommonDataSourceDatabase<DataSource>
{
	public DataSourceDatabase(String id, DataSource dataSource, Credentials credentials, int weight, Locality locality)
	{
		super(id, dataSource, credentials, weight, locality);
	}

	@Override
	public Connection connect(Decoder decoder) throws SQLException
	{
		DataSource dataSource = this.getConnectionSource();
		Credentials credentials = this.getCredentials();
		return (credentials != null) ? dataSource.getConnection(credentials.getUser(), credentials.decodePassword(decoder)) : dataSource.getConnection();
	}
}
