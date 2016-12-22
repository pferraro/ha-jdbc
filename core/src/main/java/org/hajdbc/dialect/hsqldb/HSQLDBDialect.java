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
package org.hajdbc.dialect.hsqldb;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hajdbc.SequenceProperties;
import org.hajdbc.SequencePropertiesFactory;
import org.hajdbc.SequenceSupport;
import org.hajdbc.dialect.StandardDialect;

/**
 * Dialect for <a href="http://www.hsqldb.org">HSQLDB</a>.
 * 
 * @author  Paul Ferraro
 * @since   1.1
 */
public class HSQLDBDialect extends StandardDialect
{
	private static final Set<Integer> failureCodes = new HashSet<>(Arrays.asList(402, 407, 452, 460, 468));

	@Override
	protected String vendorPattern()
	{
		return "hsqldb";
	}

	@Override
	protected String executeFunctionFormat()
	{
		return "CALL {0}";
	}

	@Override
	public SequenceSupport getSequenceSupport()
	{
		return this;
	}

	@Override
	public List<String> getDefaultSchemas(DatabaseMetaData metaData)
	{
		return Collections.singletonList("PUBLIC");
	}

	@Override
	public Collection<SequenceProperties> getSequences(DatabaseMetaData metaData, SequencePropertiesFactory factory) throws SQLException
	{
		try (Statement statement = metaData.getConnection().createStatement())
		{
			try (ResultSet resultSet = statement.executeQuery("SELECT SEQUENCE_SCHEMA, SEQUENCE_NAME, INCREMENT FROM INFORMATION_SCHEMA.SYSTEM_SEQUENCES"))
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
	protected String createForeignKeyConstraintFormat()
	{
		// Deferrability clause is not supported.
		return "ALTER TABLE {1} ADD CONSTRAINT {0} FOREIGN KEY ({2}) REFERENCES {3} ({4}) ON DELETE {5,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT} ON UPDATE {6,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT}";
	}

	@Override
	protected String currentDatePattern()
	{
		return "(?<=\\W)CURRENT_DATE(?=\\W)|(?<=\\W)CURDATE\\s*\\(\\s*\\)";
	}

	@Override
	protected String currentTimePattern()
	{
		return "(?<=\\W)CURRENT_TIME(?=\\W)|(?<=\\W)CURTIME\\s*\\(\\s*\\)";
	}

	@Override
	protected String currentTimestampPattern()
	{
		return "(?<=\\W)CURRENT_TIMESTAMP(?=\\W)|(?<=\\W)NOW\\s*\\(\\s*\\)";
	}

	@Override
	protected boolean indicatesFailure(int code)
	{
		return failureCodes.contains(code);
	}
}
