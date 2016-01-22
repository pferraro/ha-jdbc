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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import io.github.hajdbc.ColumnProperties;
import io.github.hajdbc.ForeignKeyConstraint;
import io.github.hajdbc.IdentifierNormalizer;
import io.github.hajdbc.QualifiedName;
import io.github.hajdbc.QualifiedNameFactory;
import io.github.hajdbc.UniqueConstraint;
import io.github.hajdbc.UniqueConstraintFactory;
import io.github.hajdbc.cache.AbstractTableProperties;
import io.github.hajdbc.dialect.Dialect;

/**
 * @author Paul Ferraro
 *
 */
public class EagerTableProperties extends AbstractTableProperties
{
	private Map<String, ColumnProperties> columnMap;
	private UniqueConstraint primaryKey;
	private Collection<UniqueConstraint> uniqueConstraints;
	private Collection<ForeignKeyConstraint> foreignKeyConstraints;
	private Collection<String> identityColumns;
	
	public EagerTableProperties(QualifiedName table, DatabaseMetaData metaData, Dialect dialect, QualifiedNameFactory factory) throws SQLException
	{
		super(table);
		
		IdentifierNormalizer normalizer = factory.getIdentifierNormalizer();
		this.columnMap = dialect.getColumns(metaData, table, dialect.createColumnPropertiesFactory(normalizer));
		UniqueConstraintFactory uniqueConstraintFactory = dialect.createUniqueConstraintFactory(normalizer);
		this.primaryKey = dialect.getPrimaryKey(metaData, table, uniqueConstraintFactory);
		this.uniqueConstraints = dialect.getUniqueConstraints(metaData, table, this.primaryKey, uniqueConstraintFactory);
		this.foreignKeyConstraints = dialect.getForeignKeyConstraints(metaData, table, dialect.createForeignKeyConstraintFactory(factory));
		this.identityColumns = dialect.getIdentityColumns(this.columnMap.values());
	}

	@Override
	protected Map<String, ColumnProperties> getColumnMap()
	{
		return this.columnMap;
	}

	@Override
	public UniqueConstraint getPrimaryKey()
	{
		return this.primaryKey;
	}

	@Override
	public Collection<ForeignKeyConstraint> getForeignKeyConstraints()
	{
		return this.foreignKeyConstraints;
	}

	@Override
	public Collection<UniqueConstraint> getUniqueConstraints()
	{
		return this.uniqueConstraints;
	}

	@Override
	public Collection<String> getIdentityColumns()
	{
		return this.identityColumns;
	}
}
