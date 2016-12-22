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
package org.hajdbc.state.bdb;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hajdbc.DatabaseCluster;
import org.hajdbc.durability.DurabilityEvent;
import org.hajdbc.durability.DurabilityEventFactory;
import org.hajdbc.durability.InvocationEvent;
import org.hajdbc.durability.InvokerEvent;
import org.hajdbc.durability.InvokerResult;
import org.hajdbc.pool.CloseablePoolProvider;
import org.hajdbc.pool.Pool;
import org.hajdbc.pool.PoolFactory;
import org.hajdbc.state.DatabaseEvent;
import org.hajdbc.state.DurabilityListenerAdapter;
import org.hajdbc.state.SerializedDurabilityListener;
import org.hajdbc.state.StateManager;
import org.hajdbc.util.Objects;

import com.sleepycat.bind.ByteArrayBinding;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredKeySet;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;

/**
 * @author paul
 */
public class BerkeleyDBStateManager extends CloseablePoolProvider<Environment, DatabaseException> implements StateManager, SerializedDurabilityListener
{
	private static final String STATE = "state";
	private static final String INVOCATION = "invocation";
	private static final String INVOKER = "invoker";
	private static final EntryBinding<InvocationKey> INVOCATION_KEY_BINDING = new KeyBinding<>(InvocationKey.class);
	private static final EntryBinding<InvokerKey> INVOKER_KEY_BINDING = new KeyBinding<>(InvokerKey.class);
	private static final EntryBinding<byte[]> BLOB_BINDING = new ByteArrayBinding();
	static final byte[] NULL = new byte[0];
	
	private final File file;
	private final PoolFactory poolFactory;
	private final EnvironmentConfig config;
	final DurabilityEventFactory eventFactory;
	final DurabilityListenerAdapter listener;
	
	private volatile Pool<Environment, DatabaseException> pool;

	public BerkeleyDBStateManager(DatabaseCluster<?, ?> cluster, File file, EnvironmentConfig config, PoolFactory poolFactory)
	{
		super(Environment.class, DatabaseException.class);
		this.file = file;
		this.poolFactory = poolFactory;
		this.config = config;
		this.eventFactory = cluster.getDurability();
		this.listener = new DurabilityListenerAdapter(this, cluster.getTransactionIdentifierFactory(), this.eventFactory);
	}

	@Override
	public void start()
	{
		this.file.mkdirs();
		this.pool = this.poolFactory.createPool(this);
		Environment env = this.pool.take();
		try
		{
			for (String databaseName: Arrays.asList(STATE, INVOCATION, INVOKER))
			{
				try (Database database = env.openDatabase(null, databaseName, new DatabaseConfig().setAllowCreate(true).setTransactional(false)))
				{
					// Do nothing
				}
			}
		}
		finally
		{
			this.pool.release(env);
		}
	}

	@Override
	public void stop()
	{
		if (this.pool != null)
		{
			this.pool.close();
		}
	}

	@Override
	public Set<String> getActiveDatabases()
	{
		DatabaseQuery<Set<String>> query = new DatabaseQuery<Set<String>>(STATE)
		{
			@Override
			Set<String> execute(Database database)
			{
				return new TreeSet<>(createStateSet(database, true));
			}
		};
		return this.execute(query);
	}

	@Override
	public void setActiveDatabases(final Set<String> databases)
	{
		DatabaseOperation operation = new DatabaseOperation(STATE)
		{
			@Override
			void execute(Database database)
			{
				createStateSet(database, false).retainAll(databases);
			}
		};
		this.execute(operation);
	}

	@Override
	public void activated(final DatabaseEvent event)
	{
		DatabaseOperation operation = new DatabaseOperation(STATE)
		{
			@Override
			void execute(Database database)
			{
				createStateSet(database, false).add(event.getSource());
			}
		};
		this.execute(operation);
	}

	@Override
	public void deactivated(final DatabaseEvent event)
	{
		DatabaseOperation operation = new DatabaseOperation(STATE)
		{
			@Override
			void execute(Database database)
			{
				createStateSet(database, false).remove(event.getSource());
			}
		};
		this.execute(operation);
	}

	Set<String> createStateSet(Database database, boolean readOnly)
	{
		return new StoredKeySet<>(database, TupleBinding.getPrimitiveBinding(String.class), !readOnly);
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
	public void beforeInvocation(final byte[] transactionId, final byte phase, final byte exceptionType)
	{
		DatabaseOperation operation = new DatabaseOperation(INVOCATION)
		{
			@Override
			void execute(Database database)
			{
				createInvocationMap(database, false).put(new InvocationKey(transactionId, phase), exceptionType);
			}
		};
		this.execute(operation);
	}

	@Override
	public void afterInvocation(final byte[] transactionId, final byte phase)
	{
		DatabaseOperation invokerperation = new DatabaseOperation(INVOKER)
		{
			@Override
			void execute(Database database)
			{
				Iterator<InvokerKey> keys = createInvokerMap(database, false).keySet().iterator();
				while (keys.hasNext())
				{
					InvokerKey key = keys.next();
					if ((key.getPhase() == phase) && Arrays.equals(key.getTransactionId(), transactionId))
					{
						keys.remove();
					}
				}
			}
		};
		DatabaseOperation invocationOperation = new DatabaseOperation(INVOCATION)
		{
			@Override
			void execute(Database database)
			{
				createInvocationMap(database, false).remove(new InvocationKey(transactionId, phase));
			}
		};
		this.execute(invokerperation, invocationOperation);
	}

	@Override
	public void beforeInvoker(final byte[] transactionId, final byte phase, final String databaseId)
	{
		DatabaseOperation operation = new DatabaseOperation(INVOKER)
		{
			@Override
			void execute(Database database)
			{
				createInvokerMap(database, false).put(new InvokerKey(transactionId, phase, databaseId), NULL);
			}
		};
		this.execute(operation);
	}

	@Override
	public void afterInvoker(final byte[] transactionId, final byte phase, final String databaseId, final byte[] result)
	{
		DatabaseOperation operation = new DatabaseOperation(INVOKER)
		{
			@Override
			void execute(Database database)
			{
				createInvokerMap(database, false).put(new InvokerKey(transactionId, phase, databaseId), result);
			}
		};
		this.execute(operation);
	}

	@Override
	public Map<InvocationEvent, Map<String, InvokerEvent>> recover()
	{
		final Map<InvocationEvent, Map<String, InvokerEvent>> result = new HashMap<>();
		DatabaseQuery<Void> query = new DatabaseQuery<Void>(INVOCATION)
		{
			@Override
			Void execute(Database database)
			{
				for (Map.Entry<InvocationKey, Byte> entry: createInvocationMap(database, true).entrySet())
				{
					InvocationKey key = entry.getKey();
					result.put(BerkeleyDBStateManager.this.listener.createInvocationEvent(key.getTransactionId(), key.getPhase(), entry.getValue()), new HashMap<String, InvokerEvent>());
				}
				return null;
			}
		};
		this.execute(query);
		query = new DatabaseQuery<Void>(INVOKER)
		{
			@Override
			Void execute(Database database)
			{
				for (Map.Entry<InvokerKey, byte[]> entry: createInvokerMap(database, true).entrySet())
				{
					InvokerKey key = entry.getKey();
					DurabilityEvent event = BerkeleyDBStateManager.this.listener.createEvent(key.getTransactionId(), key.getPhase());
					Map<String, InvokerEvent> invokers = result.get(event);
					if (invokers != null)
					{
						String databaseId = key.getDatabaseId();
						InvokerEvent invokerEvent = BerkeleyDBStateManager.this.eventFactory.createInvokerEvent(event.getTransactionId(), event.getPhase(), databaseId);
						byte[] value = entry.getValue();
						if (value.length > 0)
						{
							invokerEvent.setResult(Objects.deserialize(value, InvokerResult.class));
						}
						invokers.put(databaseId, invokerEvent);
					}
				}
				return null;
			}
		};
		this.execute(query);
		return result;
	}
	
	private static class InvocationKey implements Serializable
	{
		private static final long serialVersionUID = -9033714764207519351L;
		private final byte[] transactionId;
		private final byte phase;
		
		InvocationKey(byte[] transactionId, byte phase)
		{
			this.transactionId = transactionId;
			this.phase = phase;
		}
		
		byte[] getTransactionId()
		{
			return this.transactionId;
		}
		
		byte getPhase()
		{
			return this.phase;
		}
	}

	private static class InvokerKey extends InvocationKey
	{
		private static final long serialVersionUID = 400751577923581135L;
		private final String databaseId;
		
		InvokerKey(byte[] transactionId, byte phase, String databaseId)
		{
			super(transactionId, phase);
			this.databaseId = databaseId;
		}
		
		String getDatabaseId()
		{
			return this.databaseId;
		}
	}
	
	Map<InvocationKey, Byte> createInvocationMap(Database database, boolean readOnly)
	{
		return new StoredMap<>(database, INVOCATION_KEY_BINDING, TupleBinding.getPrimitiveBinding(Byte.class), !readOnly);
	}
	
	Map<InvokerKey, byte[]> createInvokerMap(Database database, boolean readOnly)
	{
		return new StoredMap<>(database, INVOKER_KEY_BINDING, BLOB_BINDING, !readOnly);
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}

	@Override
	public Environment create() throws DatabaseException
	{
		return new Environment(this.file, this.config);
	}

	@Override
	public boolean isValid(Environment environment)
	{
		try
		{
			environment.checkHandleIsValid();
			
			return true;
		}
		catch (DatabaseException e)
		{
			return false;
		}
	}

	private static abstract class DatabaseOperation
	{
		private final String databaseName;

		DatabaseOperation(String databaseName)
		{
			this.databaseName = databaseName;
		}

		String getDatabaseName()
		{
			return this.databaseName;
		}

		abstract void execute(Database database);
	}
	
	private void execute(DatabaseOperation... dbOperations)
	{
		Operation[] operations = new Operation[dbOperations.length];
		for (int i = 0; i < dbOperations.length; ++i)
		{
			final DatabaseOperation operation = dbOperations[i];
			operations[i] = new Operation()
			{
				@Override
				public void execute(Environment env, Transaction transaction)
				{
					try (Database database = env.openDatabase(transaction, operation.getDatabaseName(), new DatabaseConfig().setTransactional(true)))
					{
						operation.execute(database);
					}
				}
			};
		}
		this.execute(operations);
	}

	private abstract static class DatabaseQuery<T>
	{
		private final String databaseName;
		
		DatabaseQuery(String databaseName)
		{
			this.databaseName = databaseName;
		}
		
		String getDatabaseName()
		{
			return this.databaseName;
		}
		
		abstract T execute(Database database);
	}
	
	private <T> T execute(final DatabaseQuery<T> dbQuery)
	{
		Query<T> query = new Query<T>()
		{
			@Override
			public T execute(Environment env)
			{
				try (Database database = env.openDatabase(null, dbQuery.getDatabaseName(), new DatabaseConfig().setReadOnly(true)))
				{
					return dbQuery.execute(database);
				}
			}
		};
		return this.execute(query);
	}
	
	private static interface Operation
	{
		void execute(Environment env, Transaction transaction);
	}
	
	private void execute(Operation... operations)
	{
		Environment env = this.pool.take();
		try
		{
			Transaction transaction = env.beginTransaction(null, null);
			try
			{
				for (Operation operation: operations)
				{
					operation.execute(env, transaction);
				}
				transaction.commit();
			}
			catch (RuntimeException e)
			{
				transaction.abort();
				throw e;
			}
		}
		finally
		{
			this.pool.release(env);
		}
	}
	
	private static interface Query<T>
	{
		T execute(Environment env);
	}
	
	private <T> T execute(Query<T> query)
	{
		Environment env = this.pool.take();
		try
		{
			return query.execute(env);
		}
		finally
		{
			this.pool.release(env);
		}
	}
	
	static class KeyBinding<T> implements EntryBinding<T>
	{
		private final Class<T> targetClass;

		KeyBinding(Class<T> targetClass)
		{
			this.targetClass = targetClass;
		}
		
		@Override
		public T entryToObject(DatabaseEntry entry)
		{
			return Objects.deserialize(entry.getData(), this.targetClass);
		}

		@Override
		public void objectToEntry(T object, DatabaseEntry entry)
		{
			entry.setData(Objects.serialize(object));
		}
	}
}
