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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Set;

import io.github.hajdbc.Database;
import io.github.hajdbc.invocation.InvocationStrategies;
import io.github.hajdbc.invocation.InvocationStrategy;
import io.github.hajdbc.util.reflect.Methods;

/**
 * @author Paul Ferraro
 *
 */
public class DatabaseMetaDataInvocationHandler<Z, D extends Database<Z>> extends ChildInvocationHandler<Z, D, Connection, SQLException, DatabaseMetaData, SQLException, DatabaseMetaDataProxyFactory<Z, D>>
{
	private static final Set<Method> databaseReadMethodSet = Methods.findMethods(DatabaseMetaData.class, "getAttributes", "getBestRowIdentifier", "getCatalogs", "getColumnPrivileges", "getColumns", "getCrossReference", "getExportedKeys", "getFunctionColumns", "getFunctions", "getImportedKeys", "getIndexInfo", "getPrimaryKeys", "getProcedureColumns", "getProcedures", "getPseudoColumns", "getSchemas", "getSuperTables", "getSuperTypes", "getTablePrivileges", "getTables", "getUDTs", "getVersionColumns");
	private static final Method getConnectionMethod = Methods.getMethod(DatabaseMetaData.class, "getConnection");
	
	
	public DatabaseMetaDataInvocationHandler(DatabaseMetaDataProxyFactory<Z, D> proxyFactory)
	{
		super(DatabaseMetaData.class, proxyFactory, getConnectionMethod);
	}

	@Override
	protected InvocationStrategy getInvocationStrategy(DatabaseMetaData metaData, Method method, Object... parameters)
	{
		if (databaseReadMethodSet.contains(method))
		{
			return InvocationStrategies.INVOKE_ON_NEXT;
		}
		
		return InvocationStrategies.INVOKE_ON_ANY;
	}
}
