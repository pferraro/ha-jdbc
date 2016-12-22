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
package org.hajdbc.dialect.firebird;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hajdbc.SequenceProperties;
import org.hajdbc.SequencePropertiesFactory;
import org.hajdbc.SequenceSupport;
import org.hajdbc.dialect.StandardDialect;

/**
 * Dialect for <a href="firebird.sourceforge.net">Firebird</a>.
 * @author Paul Ferraro
 */
@SuppressWarnings("nls")
public class FirebirdDialect extends StandardDialect
{
	@Override
	protected String vendorPattern()
	{
		return "firebird";
	}

	@Override
	protected String dummyTable()
	{
		return "RDB$DATABASE";
	}

	@Override
	protected String alterSequenceFormat()
	{
		// Firebird 2.0 will support standard syntax.  Until then...
		return "SET GENERATOR {0} TO {1}";
	}

	@Override
	public SequenceSupport getSequenceSupport()
	{
		return this;
	}

	@Override
	public Collection<SequenceProperties> getSequences(DatabaseMetaData metaData, SequencePropertiesFactory factory) throws SQLException
	{
		try (Statement statement = metaData.getConnection().createStatement())
		{
			try (ResultSet resultSet = statement.executeQuery("SELECT RDB$GENERATOR_NAME FROM RDB$GENERATORS"))
			{
				List<SequenceProperties> sequences = new LinkedList<>();
				
				while (resultSet.next())
				{
					sequences.add(factory.createSequenceProperties(null, resultSet.getString(1), 1));
				}
				
				return sequences;
			}
		}
	}

	@Override
	protected String sequencePattern()
	{
		// Firebird 2.0 will support standard syntax.  Until then...
		return "GEN_ID\\s*\\(\\s*([^\\s,]+)\\s*,\\s*\\d+\\s*\\)";
	}

	@Override
	protected String selectForUpdatePattern()
	{
		return "SELECT\\s+.+\\s+WITH\\s+LOCK";
	}

	@Override
	protected String nextSequenceValueFormat()
	{
		// Firebird 2.0 will support standard syntax.  Until then...
		return "GEN_ID({0}, 1)";
	}
}
