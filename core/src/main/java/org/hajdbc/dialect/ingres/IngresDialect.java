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
package org.hajdbc.dialect.ingres;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.hajdbc.SequenceProperties;
import org.hajdbc.SequencePropertiesFactory;
import org.hajdbc.SequenceSupport;
import org.hajdbc.dialect.StandardDialect;

/**
 * Dialect for <a href="http://opensource.ingres.com/projects/ingres/">Ingres</a>.
 * 
 * @author Paul Ferraro
 */
public class IngresDialect extends StandardDialect
{
	private final Pattern legacySequencePattern = Pattern.compile("'?(\\w+)'?\\.(?:(?:CURR)|(?:NEXT))VAL", Pattern.CASE_INSENSITIVE);

	@Override
	protected String vendorPattern()
	{
		return "ingres";
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
			try (ResultSet resultSet = statement.executeQuery("SELECT seq_name FROM iisequence"))
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
	public String parseSequence(String sql)
	{
		String sequence = super.parseSequence(sql);
		
		return (sequence != null) ? sequence : this.parse(this.legacySequencePattern, sql);
	}

	@Override
	protected String sequencePattern()
	{
		return "(?:NEXT|CURRENT)\\s+VALUE\\s+FOR\\s+'?([^',\\s\\(\\)]+)";
	}

	@Override
	protected String currentDatePattern()
	{
		return "(?<=\\W)CURRENT_DATE(?=\\W)|(?<=\\W)DATE\\s*\\(\\s*'TODAY'\\s*\\)";
	}

	@Override
	protected String currentTimePattern()
	{
		return "(?<=\\W)CURRENT_TIME(?=\\W)|(?<=\\W)LOCAL_TIME(?=\\W)";
	}

	@Override
	protected String currentTimestampPattern()
	{
		return "(?<=\\W)CURRENT_TIMESTAMP(?=\\W)|(?<=\\W)LOCAL_TIMESTAMP(?=\\W)|(?<=\\W)DATE\\s*\\(\\s*'NOW'\\s*\\)";
	}

	@Override
	protected String randomPattern()
	{
		return "(?<=\\W)RANDOMF\\s*\\(\\s*\\)";
	}
}
