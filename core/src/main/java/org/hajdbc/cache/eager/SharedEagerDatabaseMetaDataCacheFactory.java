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
package org.hajdbc.cache.eager;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.cache.DatabaseMetaDataCache;
import org.hajdbc.cache.DatabaseMetaDataCacheFactory;
import org.kohsuke.MetaInfServices;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DatabaseMetaDataCacheFactory.class)
public class SharedEagerDatabaseMetaDataCacheFactory implements DatabaseMetaDataCacheFactory
{
	private static final long serialVersionUID = -7042032576675428976L;

	@Override
	public String getId()
	{
		return "shared-eager";
	}

	@Override
	public <Z, D extends Database<Z>> DatabaseMetaDataCache<Z, D> createCache(DatabaseCluster<Z, D> cluster)
	{
		return new SharedEagerDatabaseMetaDataCache<>(cluster);
	}
}
