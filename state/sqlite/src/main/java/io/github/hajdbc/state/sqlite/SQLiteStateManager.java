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
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.schema.ISqlJetSchema;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.durability.DurabilityEvent;
import io.github.hajdbc.durability.DurabilityEventFactory;
import io.github.hajdbc.durability.InvocationEvent;
import io.github.hajdbc.durability.InvokerEvent;
import io.github.hajdbc.durability.InvokerResult;
import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;
import io.github.hajdbc.pool.Pool;
import io.github.hajdbc.pool.PoolFactory;
import io.github.hajdbc.state.DatabaseEvent;
import io.github.hajdbc.state.DurabilityListenerAdapter;
import io.github.hajdbc.state.SerializedDurabilityListener;
import io.github.hajdbc.state.StateManager;
import io.github.hajdbc.util.Objects;

/**
 * @author Paul Ferraro
 */
public class SQLiteStateManager<Z, D extends Database<Z>> implements StateManager, SerializedDurabilityListener
{
	// SQLite has minimal concurrency support - and only supports a single writer per-database
	// So, mitigate this by using separate databases per table.
	private enum DB { STATE, INVOCATION }
	
	private static final Logger logger = LoggerFactory.getLogger(SQLiteStateManager.class);
	private static final String STATE_TABLE = "cluster_state";
	private static final String DATABASE_COLUMN = "database_id";

	private static final String INVOCATION_TABLE = "cluster_invocation";
	private static final String INVOKER_TABLE = "cluster_invoker";
	private static final String INVOKER_TABLE_INDEX = "cluster_invoker_index";
	private static final String TRANSACTION_COLUMN = "tx_id";
	private static final String PHASE_COLUMN = "phase_id";
	private static final String EXCEPTION_COLUMN = "exception_id";
	private static final String RESULT_COLUMN = "result";

	static final String CREATE_INVOCATION_SQL = MessageFormat.format("CREATE TABLE {0} ({1} BLOB NOT NULL, {2} INTEGER NOT NULL, {3} INTEGER NOT NULL, PRIMARY KEY ({1}, {2}))", INVOCATION_TABLE, TRANSACTION_COLUMN, PHASE_COLUMN, EXCEPTION_COLUMN);
	static final String CREATE_INVOKER_SQL = MessageFormat.format("CREATE TABLE {0} ({1} BLOB NOT NULL, {2} INTEGER NOT NULL, {3} TEXT NOT NULL, {4} BLOB, PRIMARY KEY ({1}, {2}, {3}))", INVOKER_TABLE, TRANSACTION_COLUMN, PHASE_COLUMN, DATABASE_COLUMN, RESULT_COLUMN);
	static final String CREATE_INVOKER_INDEX = MessageFormat.format("CREATE INDEX {0} ON {1} ({2}, {3})", INVOKER_TABLE_INDEX, INVOKER_TABLE, TRANSACTION_COLUMN, PHASE_COLUMN);
	static final String CREATE_STATE_SQL = MessageFormat.format("CREATE TABLE {0} ({1} TEXT NOT NULL, PRIMARY KEY ({1}))", STATE_TABLE, DATABASE_COLUMN);

	final DurabilityListenerAdapter listener;
	final DurabilityEventFactory eventFactory;
	private final File file;
	private final PoolFactory poolFactory;
	
	// Control concurrency ourselves, instead of relying of sqljet lock polling.
	private final Map<DB, ReadWriteLock> locks = new EnumMap<>(DB.class);
	private final Map<DB, Pool<SqlJetDb, SqlJetException>> pools = new EnumMap<>(DB.class);

	public SQLiteStateManager(DatabaseCluster<Z, D> cluster, File file, PoolFactory poolFactory)
	{
		this.file = file;
		this.poolFactory = poolFactory;
		this.eventFactory = cluster.getDurability();
		this.listener = new DurabilityListenerAdapter(this, cluster.getTransactionIdentifierFactory(), this.eventFactory);
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}

	@Override
	public void activated(final DatabaseEvent event)
	{
		Transaction transaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb db) throws SqlJetException
			{
				db.getTable(STATE_TABLE).insert(event.getSource());
			}
		};
		try
		{
			this.execute(transaction, DB.STATE);
		}
		catch (SqlJetException e)
		{
			logger.log(Level.ERROR, e);
		}
	}

	@Override
	public void deactivated(final DatabaseEvent event)
	{
		Transaction transaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb db) throws SqlJetException
			{
				ISqlJetTable table = db.getTable(STATE_TABLE);
				ISqlJetCursor cursor = table.lookup(table.getPrimaryKeyIndexName(), event.getSource());
				try
				{
					if (!cursor.eof())
					{
						cursor.delete();
					}
				}
				finally
				{
					close(cursor);
				}
			}
		};
		try
		{
			this.execute(transaction, DB.STATE);
		}
		catch (SqlJetException e)
		{
			logger.log(Level.ERROR, e);
		}
	}

	@Override
	public void beforeInvocation(InvocationEvent event)
	{
		this.listener.beforeInvocation(event);
	}

	@Override
	public void afterInvocation(InvocationEvent event)
	{
		this.listener.afterInvocation(event);
	}

	@Override
	public void beforeInvoker(InvokerEvent event)
	{
		this.listener.beforeInvoker(event);
	}

	@Override
	public void afterInvoker(InvokerEvent event)
	{
		this.listener.afterInvoker(event);
	}

	@Override
	public void start() throws SQLException
	{
		for (DB db: DB.values())
		{
			this.locks.put(db, new ReentrantReadWriteLock());
			this.pools.put(db, this.poolFactory.createPool(new SQLiteDbPoolProvider(new File(this.file.toURI().resolve(db.name().toLowerCase())))));
		}
		
		Transaction stateTransaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb database) throws SqlJetException
			{
				ISqlJetSchema schema = database.getSchema();
				if (schema.getTable(STATE_TABLE) == null)
				{
					database.createTable(CREATE_STATE_SQL);
				}
				else if (Boolean.getBoolean(StateManager.CLEAR_LOCAL_STATE))
				{
					database.getTable(STATE_TABLE).clear();
				}
			}
		};
		Transaction invocationTransaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb database) throws SqlJetException
			{
				ISqlJetSchema schema = database.getSchema();
				if (schema.getTable(INVOCATION_TABLE) == null)
				{
					database.createTable(CREATE_INVOCATION_SQL);
				}
				if (schema.getTable(INVOKER_TABLE) == null)
				{
					database.createTable(CREATE_INVOKER_SQL);
					database.createIndex(CREATE_INVOKER_INDEX);
				}
			}
		};
		
		try
		{
			this.execute(stateTransaction, DB.STATE);
			this.execute(invocationTransaction, DB.INVOCATION);
		}
		catch (SqlJetException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public void stop()
	{
		for (Pool<SqlJetDb, SqlJetException> pool: this.pools.values())
		{
			pool.close();
		}
		this.pools.clear();
	}

	@Override
	public Set<String> getActiveDatabases()
	{
		Query<Set<String>> query = new Query<Set<String>>()
		{
			@Override
			public Set<String> execute(SqlJetDb database) throws SqlJetException
			{
				Set<String> set = new TreeSet<>();
				ISqlJetTable table = database.getTable(STATE_TABLE);
				ISqlJetCursor cursor = table.lookup(table.getPrimaryKeyIndexName());
				try
				{
					if (!cursor.eof())
					{
						do
						{
							set.add(cursor.getString(DATABASE_COLUMN));
						}
						while (cursor.next());
					}
					return set;
				}
				finally
				{
					close(cursor);
				}
			}
		};
		
		try
		{
			return this.execute(query, DB.STATE);
		}
		catch (SqlJetException e)
		{
			logger.log(Level.ERROR, e);
			return Collections.emptySet();
		}
	}

	@Override
	public void setActiveDatabases(final Set<String> databases)
	{
		Transaction transaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb db) throws SqlJetException
			{
				ISqlJetTable table = db.getTable(STATE_TABLE);
				table.clear();
				for (String database: databases)
				{
					table.insert(database);
				}
			}
		};
		try
		{
			this.execute(transaction, DB.STATE);
		}
		catch (SqlJetException e)
		{
			logger.log(Level.ERROR, e);
		}
	}

	@Override
	public Map<InvocationEvent, Map<String, InvokerEvent>> recover()
	{
		Query<Map<InvocationEvent, Map<String, InvokerEvent>>> invocationQuery = new Query<Map<InvocationEvent, Map<String, InvokerEvent>>>()
		{
			@Override
			public Map<InvocationEvent, Map<String, InvokerEvent>> execute(SqlJetDb database) throws SqlJetException
			{
				Map<InvocationEvent, Map<String, InvokerEvent>> map = new HashMap<>();
				ISqlJetCursor cursor = database.getTable(INVOCATION_TABLE).open();
				try
				{
					if (!cursor.eof())
					{
						do
						{
							byte[] transactionId = cursor.getBlobAsArray(TRANSACTION_COLUMN);
							byte phase = (byte) cursor.getInteger(PHASE_COLUMN);
							byte exceptionType = (byte) cursor.getInteger(EXCEPTION_COLUMN);
							map.put(SQLiteStateManager.this.listener.createInvocationEvent(transactionId, phase, exceptionType), new HashMap<String, InvokerEvent>());
						}
						while (cursor.next());
					}
				}
				finally
				{
					cursor.close();
				}
				cursor = database.getTable(INVOKER_TABLE).open();
				try
				{
					if (!cursor.eof())
					{
						do
						{
							byte[] transactionId = cursor.getBlobAsArray(TRANSACTION_COLUMN);
							byte phase = (byte) cursor.getInteger(PHASE_COLUMN);
							DurabilityEvent event = SQLiteStateManager.this.listener.createEvent(transactionId, phase);
							Map<String, InvokerEvent> invokers = map.get(event);
							if (invokers != null)
							{
								String databaseId = cursor.getString(DATABASE_COLUMN);
								InvokerEvent invokerEvent = SQLiteStateManager.this.eventFactory.createInvokerEvent(event.getTransactionId(), event.getPhase(), databaseId);
								
								if (!cursor.isNull(RESULT_COLUMN))
								{
									byte[] result = cursor.getBlobAsArray(RESULT_COLUMN);
									invokerEvent.setResult(Objects.deserialize(result, InvokerResult.class));
								}
								
								invokers.put(databaseId, invokerEvent);
							}
						}
						while (cursor.next());
					}
				}
				finally
				{
					cursor.close();
				}
				return map;
			}
		};
		try
		{
			return this.execute(invocationQuery, DB.INVOCATION);
		}
		catch (SqlJetException e)
		{
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void beforeInvocation(final byte[] transactionId, final byte phase, final byte exceptionType)
	{
		Transaction transaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb db) throws SqlJetException
			{
				db.getTable(INVOCATION_TABLE).insert(transactionId, phase, exceptionType);
			}
		};
		try
		{
			this.execute(transaction, DB.INVOCATION);
		}
		catch (SqlJetException e)
		{
			logger.log(Level.ERROR, e);
		}
	}

	@Override
	public void afterInvocation(final byte[] transactionId, final byte phase)
	{
		Transaction transaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb db) throws SqlJetException
			{
				ISqlJetTable table = db.getTable(INVOCATION_TABLE);
				ISqlJetCursor cursor = table.lookup(table.getPrimaryKeyIndexName(), transactionId, phase);
				try
				{
					if (!cursor.eof())
					{
						cursor.delete();
					}
				}
				finally
				{
					close(cursor);
				}
				table = db.getTable(INVOKER_TABLE);
				cursor = table.lookup(INVOKER_TABLE_INDEX, transactionId, phase);
				try
				{
					if (!cursor.eof())
					{
						do
						{
							cursor.delete();
						}
						while (cursor.next());
					}
				}
				finally
				{
					close(cursor);
				}
			}
		};
		try
		{
			this.execute(transaction, DB.INVOCATION);
		}
		catch (SqlJetException e)
		{
			logger.log(Level.ERROR, e);
		}
	}

	@Override
	public void beforeInvoker(final byte[] transactionId, final byte phase, final String databaseId)
	{
		Transaction transaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb db) throws SqlJetException
			{
				db.getTable(INVOKER_TABLE).insert(transactionId, phase, databaseId);
			}
		};
		try
		{
			this.execute(transaction, DB.INVOCATION);
		}
		catch (SqlJetException e)
		{
			logger.log(Level.ERROR, e);
		}
	}

	@Override
	public void afterInvoker(final byte[] transactionId, final byte phase, final String databaseId, final byte[] result)
	{
		Transaction transaction = new Transaction()
		{
			@Override
			public void execute(SqlJetDb db) throws SqlJetException
			{
				ISqlJetTable table = db.getTable(INVOKER_TABLE);
				ISqlJetCursor cursor = table.lookup(table.getPrimaryKeyIndexName(), transactionId, phase, databaseId);
				try
				{
					if (!cursor.eof())
					{
						cursor.updateByFieldNames(Collections.<String, Object>singletonMap(RESULT_COLUMN, result));
					}
				}
				finally
				{
					close(cursor);
				}
			}
		};
		try
		{
			this.execute(transaction, DB.INVOCATION);
		}
		catch (SqlJetException e)
		{
			logger.log(Level.ERROR, e);
		}
	}

	static void close(ISqlJetCursor cursor)
	{
		try
		{
			cursor.close();
		}
		catch (SqlJetException e)
		{
			logger.log(Level.WARN, e);
		}
	}

	private void execute(Transaction transaction, DB db) throws SqlJetException
	{
		Pool<SqlJetDb, SqlJetException> pool = this.pools.get(db);
		Lock lock = this.locks.get(db).writeLock();
		
		SqlJetDb database = pool.take();
		
		lock.lock();
		
		try
		{
			database.beginTransaction(SqlJetTransactionMode.WRITE);
			
			try
			{
				transaction.execute(database);
				
				database.commit();
			}
			catch (SqlJetException e)
			{
				try
				{
					database.rollback();
				}
				catch (SqlJetException ex)
				{
					logger.log(Level.WARN, ex);
				}
				throw e;
			}
		}
		finally
		{
			lock.unlock();
			pool.release(database);
		}
	}

	private <T> T execute(Query<T> query, DB db) throws SqlJetException
	{
		Pool<SqlJetDb, SqlJetException> pool = this.pools.get(db);
		Lock lock = this.locks.get(db).readLock();
		
		SqlJetDb database = pool.take();
		
		lock.lock();
		
		try
		{
			database.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			
			try
			{
				return query.execute(database);
			}
			finally
			{
				database.commit();
			}
		}
		finally
		{
			lock.unlock();
			
			pool.release(database);
		}
	}

	interface Query<T>
	{
		T execute(SqlJetDb database) throws SqlJetException;
	}

	interface Transaction
	{
		void execute(SqlJetDb database) throws SqlJetException;
	}
}
