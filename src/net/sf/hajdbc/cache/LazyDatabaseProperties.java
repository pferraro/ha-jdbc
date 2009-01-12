/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.cache;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import net.sf.hajdbc.Dialect;

/**
 * @author Paul Ferraro
 *
 */
public class LazyDatabaseProperties extends AbstractLazyDatabaseProperties
{
	private final ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();
	
	public LazyDatabaseProperties(DatabaseMetaData metaData, Dialect dialect) throws SQLException
	{
		super(metaData, dialect);
		
		this.init(metaData);
	}
	
	protected LazyDatabaseProperties(DatabaseMetaData metaData, DatabaseMetaDataSupport support, Dialect dialect) throws SQLException
	{
		super(metaData, support, dialect);
		
		this.init(metaData);
	}

	private void init(DatabaseMetaData metaData) throws SQLException
	{
		this.setConnection(metaData.getConnection());
	}
	
	public void setConnection(Connection connection)
	{
		this.threadLocal.set(connection);
	}
	
	/**
	 * @see net.sf.hajdbc.cache.DatabaseMetaDataProvider#getDatabaseMetaData()
	 */
	@Override
	public DatabaseMetaData getDatabaseMetaData() throws SQLException
	{
		return this.threadLocal.get().getMetaData();
	}
}
