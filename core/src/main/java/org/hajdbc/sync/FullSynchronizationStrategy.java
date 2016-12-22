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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.ExceptionType;
import org.hajdbc.SynchronizationStrategy;
import org.hajdbc.TableProperties;
import org.hajdbc.logging.Level;
import org.hajdbc.logging.Logger;
import org.hajdbc.logging.LoggerFactory;
import org.hajdbc.messages.Messages;
import org.hajdbc.messages.MessagesFactory;
import org.hajdbc.util.Strings;
import org.kohsuke.MetaInfServices;

/**
 * Database-independent synchronization strategy that does full record transfer between two databases.
 * This strategy is best used when there are <em>many</em> differences between the active database and the inactive database (i.e. very much out of sync).
 * The following algorithm is used:
 * <ol>
 *  <li>Drop the foreign keys on the inactive database (to avoid integrity constraint violations)</li>
 *  <li>For each database table:
 *   <ol>
 *    <li>Delete all rows in the inactive database table</li>
 *    <li>Query all rows on the active database table</li>
 *    <li>For each row in active database table:
 *     <ol>
 *      <li>Insert new row into inactive database table</li>
 *     </ol>
 *    </li>
 *   </ol>
 *  </li>
 *  <li>Re-create the foreign keys on the inactive database</li>
 *  <li>Synchronize sequences</li>
 * </ol>
 * @author  Paul Ferraro
 */
@MetaInfServices(SynchronizationStrategy.class)
public class FullSynchronizationStrategy implements SynchronizationStrategy, TableSynchronizationStrategy
{
	private static final long serialVersionUID = 9190347092842178162L;

	static Messages messages = MessagesFactory.getMessages();
	static Logger logger = LoggerFactory.getLogger(FullSynchronizationStrategy.class);

	private SynchronizationStrategy strategy = new PerTableSynchronizationStrategy(this);
	private int maxBatchSize = 100;
	private int fetchSize = 0;

	@Override
	public String getId()
	{
		return "full";
	}

	@Override
	public <Z, D extends Database<Z>> void init(DatabaseCluster<Z, D> cluster)
	{
		this.strategy.init(cluster);
	}

	@Override
	public <Z, D extends Database<Z>> void synchronize(SynchronizationContext<Z, D> context) throws SQLException
	{
		this.strategy.synchronize(context);
	}

	@Override
	public <Z, D extends Database<Z>> void destroy(DatabaseCluster<Z, D> cluster)
	{
		this.strategy.destroy(cluster);
	}

	@Override
	public <Z, D extends Database<Z>> void synchronize(SynchronizationContext<Z, D> context, TableProperties table) throws SQLException
	{
		final String tableName = table.getName().getDMLName();
		final Collection<String> columns = table.getColumns();
		
		final String commaDelimitedColumns = Strings.join(columns, Strings.PADDED_COMMA);
		
		final String selectSQL = String.format("SELECT %s FROM %s", commaDelimitedColumns, tableName);
		final String deleteSQL = context.getDialect().getTruncateTableSQL(table);
		final String insertSQL = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, commaDelimitedColumns, Strings.join(Collections.nCopies(columns.size(), Strings.QUESTION), Strings.PADDED_COMMA));
		
		Connection sourceConnection = context.getConnection(context.getSourceDatabase());
		Connection targetConnection = context.getConnection(context.getTargetDatabase());
		
		try (final Statement selectStatement = sourceConnection.createStatement())
		{
			selectStatement.setFetchSize(this.fetchSize);
			
			Callable<ResultSet> callable = new Callable<ResultSet>()
			{
				@Override
				public ResultSet call() throws SQLException
				{
					logger.log(Level.DEBUG, selectSQL);
					return selectStatement.executeQuery(selectSQL);
				}
			};
			
			Future<ResultSet> future = context.getExecutor().submit(callable);
			
			try (Statement deleteStatement = targetConnection.createStatement())
			{
				logger.log(Level.DEBUG, deleteSQL);
				int deletedRows = deleteStatement.executeUpdate(deleteSQL);
		
				logger.log(Level.INFO, messages.deleteCount(table, deletedRows));
			}
			
			logger.log(Level.DEBUG, insertSQL);
			
			try (PreparedStatement insertStatement = targetConnection.prepareStatement(insertSQL))
			{
				int statementCount = 0;
				
				try (ResultSet resultSet = future.get())
				{
					while (resultSet.next())
					{
						int index = 0;
						
						for (String column: table.getColumns())
						{
							index += 1;
							
							int type = context.getDialect().getColumnType(table.getColumnProperties(column));
							
							Object object = context.getSynchronizationSupport().getObject(resultSet, index, type);
							
							if (resultSet.wasNull())
							{
								insertStatement.setNull(index, type);
							}
							else
							{
								insertStatement.setObject(index, object, type);
							}
						}
						
						insertStatement.addBatch();
						statementCount += 1;
						
						if ((statementCount % this.maxBatchSize) == 0)
						{
							insertStatement.executeBatch();
							insertStatement.clearBatch();
						}
						
						insertStatement.clearParameters();
					}
				}
				
				if ((statementCount % this.maxBatchSize) > 0)
				{
					insertStatement.executeBatch();
				}
		
				logger.log(Level.INFO, messages.insertCount(table, statementCount));
			}
			catch (ExecutionException e)
			{
				throw ExceptionType.SQL.<SQLException>getExceptionFactory().createException(e.getCause());
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new SQLException(e);
			}
		}
	}
	
	@Override
	public <Z, D extends Database<Z>> void dropConstraints(SynchronizationContext<Z, D> context) throws SQLException
	{
		context.getSynchronizationSupport().dropForeignKeys();
	}

	@Override
	public <Z, D extends Database<Z>> void restoreConstraints(SynchronizationContext<Z, D> context) throws SQLException
	{
		context.getSynchronizationSupport().restoreForeignKeys();
	}

	/**
	 * @return the fetchSize.
	 */
	public int getFetchSize()
	{
		return this.fetchSize;
	}

	/**
	 * @param fetchSize the fetchSize to set.
	 */
	public void setFetchSize(int fetchSize)
	{
		this.fetchSize = fetchSize;
	}
	
	/**
	 * @return the maxBatchSize.
	 */
	public int getMaxBatchSize()
	{
		return this.maxBatchSize;
	}

	/**
	 * @param maxBatchSize the maxBatchSize to set.
	 */
	public void setMaxBatchSize(int maxBatchSize)
	{
		this.maxBatchSize = maxBatchSize;
	}
}
