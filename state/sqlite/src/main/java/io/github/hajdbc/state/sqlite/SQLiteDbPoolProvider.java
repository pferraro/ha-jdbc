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
package io.github.hajdbc.state.sqlite;

import java.io.File;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;
import io.github.hajdbc.pool.AbstractPoolProvider;

/**
 * <a href="http://sqljet.com/">SQLJet</a> is a java port of <a href="http://www.sqlite.org/">SQLite</a>.
 * 
 * @author paul
 */
public class SQLiteDbPoolProvider extends AbstractPoolProvider<SqlJetDb, SqlJetException>
{
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private final File file;

	public SQLiteDbPoolProvider(File file)
	{
		super(SqlJetDb.class, SqlJetException.class);
		this.file = file;
	}

	@Override
	public void close(SqlJetDb database)
	{
		try
		{
			database.close();
		}
		catch (SqlJetException e)
		{
			this.logger.log(Level.WARN, e, e.getMessage());
		}
	}

	@Override
	public synchronized SqlJetDb create() throws SqlJetException
	{
		boolean exists = this.file.exists();
		SqlJetDb db = SqlJetDb.open(this.file, true);
		if (!exists)
		{
			db.getOptions().setAutovacuum(true);
			db.getOptions().setIncrementalVacuum(true);
		}
		return db;
	}

	@Override
	public boolean isValid(SqlJetDb database)
	{
		return database.isOpen();
	}
}
