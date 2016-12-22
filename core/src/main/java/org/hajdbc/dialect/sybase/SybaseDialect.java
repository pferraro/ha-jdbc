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
package org.hajdbc.dialect.sybase;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hajdbc.IdentityColumnSupport;
import org.hajdbc.dialect.StandardDialect;

/**
 * Dialect for Sybase (commercial).
 * @author Paul Ferraro
 */
public class SybaseDialect extends StandardDialect
{
	@Override
	public IdentityColumnSupport getIdentityColumnSupport()
	{
		return this;
	}

	@Override
	protected String vendorPattern()
	{
		return "sybase";
	}

	@Override
	protected String truncateTableFormat()
	{
		return "TRUNCATE TABLE {0}";
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
		return "(?<=\\W)CURRENT\\s+DATE(?=\\W)|(?<=\\W)TODAY\\s*\\(\\s*\\*\\s*\\)";
	}

	@Override
	protected String currentTimePattern()
	{
		return "(?<=\\W)CURRENT\\s+TIME(?=\\W)";
	}

	@Override
	protected String currentTimestampPattern()
	{
		return "(?<=\\W)CURRENT\\s+TIMESTAMP(?=\\W)|(?<=\\W)GETDATE\\s*\\(\\s*\\)|(?<=\\W)NOW\\s*\\(\\s*\\*\\s*\\)";
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

	@Override
	protected String randomPattern()
	{
		return "(?<=\\W)RAND\\s*\\(\\s*\\d*\\s*\\)";
	}

	@Override
	public boolean isValid(Connection connection) throws SQLException
	{
		// jTDS does not implement Connection.isValid(...)
		try (Statement statement = connection.createStatement())
		{
			statement.executeQuery("SELECT GETDATE()");
			return true;
		}
	}
}
