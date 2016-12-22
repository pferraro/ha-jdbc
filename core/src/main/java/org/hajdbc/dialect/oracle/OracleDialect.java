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
package org.hajdbc.dialect.oracle;

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
 * Dialect for Oracle (commercial).
 * @author Paul Ferraro
 */
@SuppressWarnings("nls")
public class OracleDialect extends StandardDialect
{
	@Override
	protected String vendorPattern()
	{
		return "oracle";
	}

	@Override
	protected String dummyTable()
	{
		return "DUAL";
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
			try (ResultSet resultSet = statement.executeQuery("SELECT SEQUENCE_NAME, INCREMENT_BY FROM USER_SEQUENCES"))
			{
				List<SequenceProperties> sequences = new LinkedList<>();
				
				while (resultSet.next())
				{
					sequences.add(factory.createSequenceProperties(null, resultSet.getString(1), resultSet.getInt(2)));
				}
				
				return sequences;
			}
		}
	}

	@Override
	protected String schemaPattern(DatabaseMetaData metaData) throws SQLException
	{
		return metaData.getUserName();
	}

	@Override
	protected String truncateTableFormat()
	{
		return "TRUNCATE TABLE {0}";
	}

	@Override
	protected String createForeignKeyConstraintFormat()
	{
		// ON UPDATE and deferrability clauses are not supported.
		return "ALTER TABLE {1} ADD CONSTRAINT {0} FOREIGN KEY ({2}) REFERENCES {3} ({4}) ON DELETE {5,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT}";
	}

	@Override
	protected String sequencePattern()
	{
		return "'?(\\w+)'?\\.(?:CURR|NEXT)VAL";
	}

	@Override
	protected String nextSequenceValueFormat()
	{
		return "{0}.NEXTVAL";
	}

	@Override
	protected String alterSequenceFormat()
	{
		return "DROP SEQUENCE {0}; CREATE SEQUENCE {0} START WITH {1} INCREMENT BY {2}";
	}

	@Override
	protected boolean indicatesFailure(String sqlState)
	{
		// 66 class SQLStates indicate SQL*Net driver errors
		// 69 class SQLStates indicate SQL*Connect errors
		return super.indicatesFailure(sqlState) || sqlState.startsWith("66") || sqlState.startsWith("69");
	}
}
