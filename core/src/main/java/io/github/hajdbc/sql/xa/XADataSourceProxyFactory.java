/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2013  Paul Ferraro
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
package io.github.hajdbc.sql.xa;

import javax.sql.XADataSource;

import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.sql.CommonDataSourceProxyFactory;
import io.github.hajdbc.util.reflect.Proxies;

/**
 * 
 * @author Paul Ferraro
 */
public class XADataSourceProxyFactory extends CommonDataSourceProxyFactory<XADataSource, XADataSourceDatabase>
{
	public XADataSourceProxyFactory(DatabaseCluster<XADataSource, XADataSourceDatabase> cluster)
	{
		super(cluster);
	}

	@Override
	public XADataSource createProxy()
	{
		return Proxies.createProxy(XADataSource.class, new XADataSourceInvocationHandler(this));
	}
}
