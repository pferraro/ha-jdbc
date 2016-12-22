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
package org.hajdbc.sync;

import java.io.File;
import java.sql.SQLException;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.DumpRestoreSupport;
import org.hajdbc.ExceptionType;
import org.hajdbc.SynchronizationStrategy;
import org.hajdbc.codec.Decoder;
import org.hajdbc.dialect.Dialect;
import org.hajdbc.messages.Messages;
import org.hajdbc.messages.MessagesFactory;
import org.hajdbc.util.Files;

/**
 * A synchronization strategy that uses dump/restore procedures.
 * @author Paul Ferraro
 */
public class DumpRestoreSynchronizationStrategy implements SynchronizationStrategy
{
	private static final Messages messages = MessagesFactory.getMessages();

	private static final long serialVersionUID = 5743532034969216540L;
	private static final String DUMP_FILE_SUFFIX = ".dump";

	private boolean dataOnly = false;

	@Override
	public String getId()
	{
		return "dump-restore";
	}

	public boolean isDataOnly()
	{
		return this.dataOnly;
	}

	public void setDataOnly(boolean dataOnly)
	{
		this.dataOnly = dataOnly;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.SynchronizationStrategy#init(org.hajdbc.DatabaseCluster)
	 */
	@Override
	public <Z, D extends Database<Z>> void init(DatabaseCluster<Z, D> cluster)
	{
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.SynchronizationStrategy#destroy(org.hajdbc.DatabaseCluster)
	 */
	@Override
	public <Z, D extends Database<Z>> void destroy(DatabaseCluster<Z, D> cluster)
	{
	}
	
	@Override
	public <Z, D extends Database<Z>> void synchronize(SynchronizationContext<Z, D> context) throws SQLException
	{
		Dialect dialect = context.getDialect();
		Decoder decoder = context.getDecoder();
		DumpRestoreSupport support = dialect.getDumpRestoreSupport();
		
		if (support == null)
		{
			throw new SQLException(messages.dumpRestoreNotSupported(dialect));
		}
		
		try
		{
			File file = Files.createTempFile(DUMP_FILE_SUFFIX);
			
			try
			{
				support.dump(context.getSourceDatabase(), decoder, file, this.dataOnly);
				support.restore(context.getTargetDatabase(), decoder, file, this.dataOnly);
			}
			finally
			{
				Files.delete(file);
			}
		}
		catch (Exception e)
		{
			throw ExceptionType.SQL.<SQLException>getExceptionFactory().createException(e);
		}
	}
}
