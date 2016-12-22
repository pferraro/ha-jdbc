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
package org.hajdbc.sql.xa;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.hajdbc.Credentials;
import org.hajdbc.Locality;
import org.hajdbc.codec.Decoder;
import org.hajdbc.management.Description;
import org.hajdbc.management.MBean;
import org.hajdbc.sql.CommonDataSourceDatabase;

/**
 * A database described by an {@link XADataSource}.
 * @author Paul Ferraro
 */
@MBean
@Description("Database accessed via a server-side XADataSource")
public class XADataSourceDatabase extends CommonDataSourceDatabase<XADataSource>
{
	private boolean force2PC = false;
	
	public XADataSourceDatabase(String id, XADataSource dataSource, Credentials credentials, int weight, Locality locality, boolean force2PC)
	{
		super(id, dataSource, credentials, weight, locality);
		this.force2PC = force2PC;
	}

	@Override
	public Connection connect(Decoder decoder) throws SQLException
	{
		XADataSource dataSource = this.getConnectionSource();
		Credentials credentials = this.getCredentials();
		XAConnection connection = (credentials != null) ? dataSource.getXAConnection(credentials.getUser(), credentials.decodePassword(decoder)) : dataSource.getXAConnection();
		return connection.getConnection();
	}

	public boolean isForce2PC()
	{
		return this.force2PC;
	}
}
