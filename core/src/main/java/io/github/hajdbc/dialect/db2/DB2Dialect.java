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
package io.github.hajdbc.dialect.db2;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import io.github.hajdbc.IdentityColumnSupport;
import io.github.hajdbc.SequenceProperties;
import io.github.hajdbc.SequencePropertiesFactory;
import io.github.hajdbc.SequenceSupport;
import io.github.hajdbc.dialect.StandardDialect;

/**
 * Dialect for DB2 (commercial).
 * @author  Paul Ferraro
 * @since   1.1
 */
@SuppressWarnings("nls")
public class DB2Dialect extends StandardDialect
{
	@Override
	protected String vendorPattern()
	{
		return "db2";
	}

	@Override
	protected String executeFunctionFormat()
	{
		return "VALUES {0}";
	}

	@Override
	public SequenceSupport getSequenceSupport()
	{
		return this;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport()
	{
		return this;
	}

	@Override
	public Collection<SequenceProperties> getSequences(DatabaseMetaData metaData, SequencePropertiesFactory factory) throws SQLException
	{
		try (Statement statement = metaData.getConnection().createStatement())
		{
			try (ResultSet resultSet = statement.executeQuery("SELECT SEQSCHEMA, SEQNAME, INCREMENT FROM SYSCAT.SEQUENCES"))
			{
				List<SequenceProperties> sequences = new LinkedList<>();
				
				while (resultSet.next())
				{
					sequences.add(factory.createSequenceProperties(resultSet.getString(1), resultSet.getString(2), resultSet.getInt(3)));
				}
				
				return sequences;
			}
		}
	}

	@Override
	protected String sequencePattern()
	{
		return "(?:NEXT|PREV)VAL\\s+FOR\\s+'?([^',\\s\\(\\)]+)";
	}

	@Override
	protected String nextSequenceValueFormat()
	{
		return "NEXTVAL FOR {0}";
	}

	@Override
	protected String dateLiteralFormat()
	{
		return this.timestampLiteralFormat();
	}

	@Override
	protected String timeLiteralFormat()
	{
		return this.timestampLiteralFormat();
	}

	@Override
	protected String timestampLiteralFormat()
	{
		return "''{0}''";
	}
}
