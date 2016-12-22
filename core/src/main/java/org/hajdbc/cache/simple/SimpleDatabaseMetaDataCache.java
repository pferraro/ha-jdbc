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
package org.hajdbc.cache.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.DatabaseProperties;
import org.hajdbc.cache.DatabaseMetaDataCache;
import org.hajdbc.cache.lazy.LazyDatabaseProperties;
import org.hajdbc.dialect.Dialect;

/**
 * DatabaseMetaDataCache implementation that does not cache data.
 * To be used when memory usage is more of a concern than performance.
 * 
 * @author Paul Ferraro
 * @since 2.0
 */
public class SimpleDatabaseMetaDataCache<Z, D extends Database<Z>> implements DatabaseMetaDataCache<Z, D>
{
	private final DatabaseCluster<Z, D> cluster;

	public SimpleDatabaseMetaDataCache(DatabaseCluster<Z, D> cluster)
	{
		this.cluster = cluster;
	}
	
	/**
	 * @see org.hajdbc.cache.DatabaseMetaDataCache#flush()
	 */
	@Override
	public void flush()
	{
		// Nothing to flush
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.cache.DatabaseMetaDataCache#getDatabaseProperties(org.hajdbc.Database, java.sql.Connection)
	 */
	@Override
	public DatabaseProperties getDatabaseProperties(D database, Connection connection) throws SQLException
	{
		DatabaseMetaData metaData = connection.getMetaData();
		Dialect dialect = this.cluster.getDialect();
		return new LazyDatabaseProperties(new SimpleDatabaseMetaDataProvider(metaData), dialect);
	}
}
