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
@SuppressWarnings("nls")
public class SybaseDialect extends StandardDialect
{
	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.dialect.StandardDialect#getIdentityColumnSupport()
	 */
	@Override
	public IdentityColumnSupport getIdentityColumnSupport()
	{
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.dialect.StandardDialect#vendorPattern()
	 */
	@Override
	protected String vendorPattern()
	{
		return "sybase";
	}

	/**
	 * @see org.hajdbc.dialect.StandardDialect#truncateTableFormat()
	 */
	@Override
	protected String truncateTableFormat()
	{
		return "TRUNCATE TABLE {0}";
	}
	
	/**
	 * Deferrability clause is not supported.
	 * @see org.hajdbc.dialect.StandardDialect#createForeignKeyConstraintFormat()
	 */
	@Override
	protected String createForeignKeyConstraintFormat()
	{
		return "ALTER TABLE {1} ADD CONSTRAINT {0} FOREIGN KEY ({2}) REFERENCES {3} ({4}) ON DELETE {5,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT} ON UPDATE {6,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT}";
	}

	/**
	 * @see org.hajdbc.dialect.StandardDialect#currentDatePattern()
	 */
	@Override
	protected String currentDatePattern()
	{
		return "(?<=\\W)CURRENT\\s+DATE(?=\\W)|(?<=\\W)TODAY\\s*\\(\\s*\\*\\s*\\)";
	}

	/**
	 * @see org.hajdbc.dialect.StandardDialect#currentTimePattern()
	 */
	@Override
	protected String currentTimePattern()
	{
		return "(?<=\\W)CURRENT\\s+TIME(?=\\W)";
	}

	/**
	 * @see org.hajdbc.dialect.StandardDialect#currentTimestampPattern()
	 */
	@Override
	protected String currentTimestampPattern()
	{
		return "(?<=\\W)CURRENT\\s+TIMESTAMP(?=\\W)|(?<=\\W)GETDATE\\s*\\(\\s*\\)|(?<=\\W)NOW\\s*\\(\\s*\\*\\s*\\)";
	}

	/**
	 * @see org.hajdbc.dialect.StandardDialect#dateLiteralFormat()
	 */
	@Override
	protected String dateLiteralFormat()
	{
		return this.timestampLiteralFormat();
	}

	/**
	 * @see org.hajdbc.dialect.StandardDialect#timeLiteralFormat()
	 */
	@Override
	protected String timeLiteralFormat()
	{
		return this.timestampLiteralFormat();
	}

	/**
	 * @see org.hajdbc.dialect.StandardDialect#timestampLiteralFormat()
	 */
	@Override
	protected String timestampLiteralFormat()
	{
		return "''{0}''";
	}
	
	/**
	 * @see org.hajdbc.dialect.StandardDialect#randomPattern()
	 */
	@Override
	protected String randomPattern()
	{
		return "(?<=\\W)RAND\\s*\\(\\s*\\d*\\s*\\)";
	}

	/**
	 * jTDS does not implement Connection.isValid(...)
	 */
	@Override
	public boolean isValid(Connection connection) throws SQLException
	{
		try (Statement statement = connection.createStatement())
		{
			statement.executeQuery("SELECT GETDATE()");
			return true;
		}
	}
}
