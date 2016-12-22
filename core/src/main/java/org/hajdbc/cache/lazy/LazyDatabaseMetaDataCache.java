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
package org.hajdbc.cache.lazy;

import java.lang.ref.Reference;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeMap;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.DatabaseProperties;
import org.hajdbc.cache.DatabaseMetaDataCache;
import org.hajdbc.dialect.Dialect;
import org.hajdbc.util.ref.ReferenceMap;
import org.hajdbc.util.ref.SoftReferenceFactory;

/**
 * Per-database {@link DatabaseMetaDataCache} implementation that populates itself lazily.
 * @author Paul Ferraro
 */
public class LazyDatabaseMetaDataCache<Z, D extends Database<Z>> implements DatabaseMetaDataCache<Z, D>
{
	private final Map<D, Map.Entry<DatabaseProperties, LazyDatabaseMetaDataProvider>> map = new ReferenceMap<>(new TreeMap<D, Reference<Map.Entry<DatabaseProperties, LazyDatabaseMetaDataProvider>>>(), SoftReferenceFactory.getInstance());
	private final DatabaseCluster<Z, D> cluster;

	public LazyDatabaseMetaDataCache(DatabaseCluster<Z, D> cluster)
	{
		this.cluster = cluster;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.cache.DatabaseMetaDataCache#flush()
	 */
	@Override
	public void flush()
	{
		synchronized (this.map)
		{
			this.map.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.cache.DatabaseMetaDataCache#getDatabaseProperties(org.hajdbc.Database, java.sql.Connection)
	 */
	@Override
	public DatabaseProperties getDatabaseProperties(D database, Connection connection) throws SQLException
	{
		synchronized (this.map)
		{
			Map.Entry<DatabaseProperties, LazyDatabaseMetaDataProvider> entry = this.map.get(database);
			
			if (entry == null)
			{
				DatabaseMetaData metaData = connection.getMetaData();
				Dialect dialect = this.cluster.getDialect();
				LazyDatabaseMetaDataProvider provider = new LazyDatabaseMetaDataProvider(metaData);
				DatabaseProperties properties = new LazyDatabaseProperties(provider, dialect);
				
				entry = new AbstractMap.SimpleImmutableEntry<>(properties, provider);

				this.map.put(database, entry);
			}
			else
			{
				entry.getValue().setConnection(connection);
			}
			
			return entry.getKey();
		}
	}
}
