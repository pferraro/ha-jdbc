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
package io.github.hajdbc.cache.lazy;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.DatabaseProperties;
import io.github.hajdbc.cache.DatabaseMetaDataCache;
import io.github.hajdbc.dialect.Dialect;


/**
 * DatabaseMetaDataCache implementation that lazily caches data when requested.
 * Used when a compromise between memory usage and performance is desired.
 * Caches DatabaseProperties using a soft reference to prevent <code>OutOfMemoryError</code>s.
 * 
 * @author Paul Ferraro
 * @since 2.0
 */
public class SharedLazyDatabaseMetaDataCache<Z, D extends Database<Z>> implements DatabaseMetaDataCache<Z, D>
{
	private final DatabaseCluster<Z, D> cluster;
	
	private volatile Reference<Map.Entry<DatabaseProperties, LazyDatabaseMetaDataProvider>> entryRef = new SoftReference<>(null);
	
	public SharedLazyDatabaseMetaDataCache(DatabaseCluster<Z, D> cluster)
	{
		this.cluster = cluster;
	}
	
	@Override
	public synchronized void flush()
	{
		this.entryRef.clear();
	}

	@Override
	public DatabaseProperties getDatabaseProperties(D database, Connection connection) throws SQLException
	{
		Map.Entry<DatabaseProperties, LazyDatabaseMetaDataProvider> entry = this.entryRef.get();
		
		if (entry == null)
		{
			DatabaseMetaData metaData = connection.getMetaData();
			Dialect dialect = this.cluster.getDialect();
			LazyDatabaseMetaDataProvider provider = new LazyDatabaseMetaDataProvider(metaData);
			DatabaseProperties properties = new LazyDatabaseProperties(provider, dialect);
			
			entry = new AbstractMap.SimpleImmutableEntry<>(properties, provider);
		
			this.entryRef = new SoftReference<>(entry);
		}
		else
		{
			entry.getValue().setConnection(connection);
		}
		
		return entry.getKey();
	}
}
