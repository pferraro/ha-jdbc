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
package io.github.hajdbc.cache;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import io.github.hajdbc.AbstractNamed;
import io.github.hajdbc.ColumnProperties;
import io.github.hajdbc.QualifiedName;
import io.github.hajdbc.TableProperties;


/**
 * @author Paul Ferraro
 */
public abstract class AbstractTableProperties extends AbstractNamed<QualifiedName, TableProperties> implements TableProperties
{
	protected AbstractTableProperties(QualifiedName name) {
		super(name);
	}

	@Override
	public final Collection<String> getColumns() throws SQLException
	{
		return this.getColumnMap().keySet();
	}

	@Override
	public final ColumnProperties getColumnProperties(String column) throws SQLException
	{
		return this.getColumnMap().get(column);
	}

	protected abstract Map<String, ColumnProperties> getColumnMap() throws SQLException;
}
