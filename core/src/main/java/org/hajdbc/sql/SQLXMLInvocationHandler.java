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
package org.hajdbc.sql;

import java.lang.reflect.Method;
import java.sql.SQLXML;
import java.util.Set;

import org.hajdbc.Database;
import org.hajdbc.util.reflect.Methods;

public class SQLXMLInvocationHandler<Z, D extends Database<Z>, P> extends LocatorInvocationHandler<Z, D, P, SQLXML, SQLXMLProxyFactory<Z, D, P>>
{
	private static final Set<Method> READ_METHODS = Methods.findMethods(java.sql.SQLXML.class, "getBinaryStream", "getCharacterStream", "getSource", "getString");
	private static final Set<Method> WRITE_METHODS = Methods.findMethods(java.sql.SQLXML.class, "setBinaryStream", "setCharacterStream", "setResult", "setString");

	public SQLXMLInvocationHandler(SQLXMLProxyFactory<Z, D, P> proxyFactory)
	{
		super(SQLXML.class, proxyFactory, READ_METHODS, WRITE_METHODS);
	}
}
