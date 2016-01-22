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
package io.github.hajdbc.dialect.derby;

import io.github.hajdbc.IdentityColumnSupport;
import io.github.hajdbc.SequenceSupport;
import io.github.hajdbc.dialect.StandardDialect;

/**
 * Dialect for <a href="http://db.apache.org/derby">Apache Derby</a>.
 * 
 * @author Paul Ferraro
 * @since 1.1
 */
@SuppressWarnings("nls")
public class DerbyDialect extends StandardDialect
{
	@Override
	public IdentityColumnSupport getIdentityColumnSupport()
	{
		return this;
	}

	@Override
	public SequenceSupport getSequenceSupport()
	{
		// Sequence support was added to 10.6.1.0
		return this.meetsRequirement(10, 6) ? this : null;
	}

	@Override
	protected String vendorPattern()
	{
		return "derby";
	}

	@Override
	protected String executeFunctionFormat()
	{
		return "VALUES {0}";
	}

	/**
	 * Deferrability clause is not supported.
	 */
	@Override
	protected String createForeignKeyConstraintFormat()
	{
		return "ALTER TABLE {1} ADD CONSTRAINT {0} FOREIGN KEY ({2}) REFERENCES {3} ({4}) ON DELETE {5,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT} ON UPDATE {6,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT}";
	}

	@Override
	protected String currentDatePattern()
	{
		return super.currentDatePattern() + "|(?<=\\W)CURRENT\\s+DATE(?=\\W)";
	}

	@Override
	protected String currentTimePattern()
	{
		return super.currentTimePattern() + "|(?<=\\W)CURRENT\\s+TIME(?=\\W)";
	}

	@Override
	protected String currentTimestampPattern()
	{
		return super.currentTimestampPattern() + "|(?<=\\W)CURRENT\\s+TIMESTAMP(?=\\W)";
	}

	@Override
	protected String dateLiteralFormat()
	{
		return "DATE(''{0}'')";
	}

	@Override
	protected String timeLiteralFormat()
	{
		return "TIME(''{0}'')";
	}

	@Override
	protected String timestampLiteralFormat()
	{
		return "TIMESTAMP(''{0}'')";
	}
}
