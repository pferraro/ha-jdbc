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
package io.github.hajdbc.cache.eager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.DatabaseProperties;
import io.github.hajdbc.cache.DatabaseMetaDataCache;
import io.github.hajdbc.dialect.Dialect;
import io.github.hajdbc.messages.Messages;
import io.github.hajdbc.messages.MessagesFactory;

/**
 * DatabaseMetaDataCache implementation that eagerly caches data when first flushed.
 * To be used when performance more of a concern than memory usage.
 * 
 * @author Paul Ferraro
 * @since 2.0
 */
public class SharedEagerDatabaseMetaDataCache<Z, D extends Database<Z>> implements DatabaseMetaDataCache<Z, D>
{
	private static final Messages messages = MessagesFactory.getMessages();

	private final DatabaseCluster<Z, D> cluster;

	private volatile DatabaseProperties properties;
	
	public SharedEagerDatabaseMetaDataCache(DatabaseCluster<Z, D> cluster)
	{
		this.cluster = cluster;
	}
	
	@Override
	public void flush() throws SQLException
	{
		D database = this.cluster.getBalancer().next();
		
		if (database == null)
		{
			throw new SQLException(messages.noActiveDatabases(this.cluster));
		}
		
		this.setDatabaseProperties(database.connect(this.cluster.getDecoder()));
	}

	@Override
	public synchronized DatabaseProperties getDatabaseProperties(D database, Connection connection) throws SQLException
	{
		if (this.properties == null)
		{
			this.setDatabaseProperties(connection);
		}
		
		return this.properties;
	}
	
	private synchronized void setDatabaseProperties(Connection connection) throws SQLException
	{
		DatabaseMetaData metaData = connection.getMetaData();
		Dialect dialect = this.cluster.getDialect();
		this.properties = new EagerDatabaseProperties(metaData, dialect);
	}
}
