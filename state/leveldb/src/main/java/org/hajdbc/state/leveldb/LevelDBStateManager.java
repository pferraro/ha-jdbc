package org.hajdbc.state.leveldb;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hajdbc.DatabaseCluster;
import org.hajdbc.durability.DurabilityEvent;
import org.hajdbc.durability.DurabilityEventFactory;
import org.hajdbc.durability.InvocationEvent;
import org.hajdbc.durability.InvokerEvent;
import org.hajdbc.durability.InvokerResult;
import org.hajdbc.logging.Level;
import org.hajdbc.logging.Logger;
import org.hajdbc.logging.LoggerFactory;
import org.hajdbc.state.DatabaseEvent;
import org.hajdbc.state.DurabilityListenerAdapter;
import org.hajdbc.state.SerializedDurabilityListener;
import org.hajdbc.state.StateManager;
import org.hajdbc.util.Objects;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;

public class LevelDBStateManager implements StateManager, SerializedDurabilityListener
{
	private static final Logger logger = LoggerFactory.getLogger(LevelDBStateManager.class);
	
	private final DBFactory factory;
	private final File file;
	private final Options options;
	private final DurabilityEventFactory eventFactory;
	private final DurabilityListenerAdapter listener;
	private volatile DB stateDatabase;
	private volatile DB invokerDatabase;
	private volatile DB invocationDatabase;
	
	public LevelDBStateManager(DatabaseCluster<?, ?> cluster, DBFactory factory, File file, Options options)
	{
		this.factory = factory;
		this.file = file;
		this.options = options;
		this.eventFactory = cluster.getDurability();
		this.listener = new DurabilityListenerAdapter(this, cluster.getTransactionIdentifierFactory(), this.eventFactory);
	}

	@Override
	public void start() throws SQLException
	{
		try
		{
			this.stateDatabase = this.factory.open(this.file, this.options);
			this.invokerDatabase = this.factory.open(this.file, this.options);
			this.invocationDatabase = this.factory.open(this.file, this.options);
		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public void stop()
	{
		try
		{
			this.stateDatabase.close();
			this.invokerDatabase.close();
			this.invocationDatabase.close();
		}
		catch (IOException e)
		{
			logger.log(Level.WARN, e);
		}
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public void activated(DatabaseEvent event)
	{
		this.stateDatabase.put(event.getSource().getBytes(StandardCharsets.UTF_8), null);
	}

	@Override
	public void deactivated(DatabaseEvent event)
	{
		this.stateDatabase.delete(event.getSource().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public Set<String> getActiveDatabases()
	{
		try (DBIterator entries = this.stateDatabase.iterator())
		{
			Set<String> databases = new TreeSet<>();
			while (entries.hasNext())
			{
				databases.add(new String(entries.next().getKey(), StandardCharsets.UTF_8));
			}
			return databases;
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void setActiveDatabases(Set<String> databases)
	{
		try (WriteBatch batch = this.stateDatabase.createWriteBatch())
		{
			for (Map.Entry<byte[], byte[]> entry: this.stateDatabase)
			{
				batch.delete(entry.getKey());
			}
			for (String database: databases)
			{
				batch.put(database.getBytes(StandardCharsets.UTF_8), null);
			}
			this.stateDatabase.write(batch);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void beforeInvocation(byte[] transactionId, byte phase, byte exceptionType)
	{
		this.invocationDatabase.put(createKey(transactionId, phase), new byte[] { exceptionType });
	}

	@Override
	public void afterInvocation(byte[] transactionId, byte phase)
	{
		try (WriteBatch batch = this.invokerDatabase.createWriteBatch())
		{
			for (String database: new String[] { })
			{
				batch.delete(createKey(transactionId, phase, database));
			}
			this.invocationDatabase.delete(createKey(transactionId, phase));
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static byte[] createKey(byte[] transactionId, byte phase) {
		byte[] key = Arrays.copyOf(transactionId, transactionId.length + 1);
		key[transactionId.length] = phase;
		return key;
	}

	@Override
	public void beforeInvoker(byte[] transactionId, byte phase, String databaseId)
	{
		this.invokerDatabase.put(createKey(transactionId, phase, databaseId), null);
	}

	@Override
	public void afterInvoker(byte[] transactionId, byte phase, String databaseId, byte[] result)
	{
		this.invokerDatabase.put(createKey(transactionId, phase, databaseId), result);
	}

	private static byte[] createKey(byte[] transactionId, byte phase, String database) {
		byte[] databaseBytes = database.getBytes(StandardCharsets.UTF_8);
		byte[] key = new byte[transactionId.length + 2 + databaseBytes.length];
		key[0] = (byte) transactionId.length;
		for (int i = 0; i < transactionId.length; ++i)
		{
			key[1 + i] = databaseBytes[i];
		}
		key[transactionId.length + 1] = phase;
		for (int i = 0; i < databaseBytes.length; ++i)
		{
			key[transactionId.length + 2 + i] = databaseBytes[i];
		}
		return key;
	}

	@Override
	public Map<InvocationEvent, Map<String, InvokerEvent>> recover()
	{
		Map<InvocationEvent, Map<String, InvokerEvent>> map = new HashMap<>();
		
		try (DBIterator entries = this.invocationDatabase.iterator())
		{
			while (entries.hasNext())
			{
				Map.Entry<byte[], byte[]> entry = entries.next();
				byte[] key = entry.getKey();
				byte[] txId = Arrays.copyOf(key, key.length - 1);
				byte phase = key[key.length - 1];
				byte exceptionType = entry.getValue()[0];
				map.put(this.listener.createInvocationEvent(txId, phase, exceptionType), new HashMap<String, InvokerEvent>());
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}

		try (DBIterator entries = this.invokerDatabase.iterator())
		{
			while (entries.hasNext())
			{
				Map.Entry<byte[], byte[]> entry = entries.next();
				byte[] key = entry.getKey();
				byte[] txId = Arrays.copyOfRange(key, 1, key[0] + 1);
				byte phase = key[txId.length + 1];
				String databaseId = new String(Arrays.copyOfRange(key, txId.length + 2, key.length), StandardCharsets.UTF_8);
				
				DurabilityEvent event = this.listener.createEvent(txId, phase);
				Map<String, InvokerEvent> invokers = map.get(event);
				if (invokers != null)
				{
					InvokerEvent invokerEvent = this.eventFactory.createInvokerEvent(event.getTransactionId(), event.getPhase(), databaseId);
					byte[] value = entry.getValue();
					
					if (value != null)
					{
						invokerEvent.setResult(Objects.deserialize(value, InvokerResult.class));
					}
					
					invokers.put(databaseId, invokerEvent);
				}
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}

		return map;
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
}
