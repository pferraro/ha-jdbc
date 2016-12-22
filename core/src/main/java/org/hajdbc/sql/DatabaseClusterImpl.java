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
package org.hajdbc.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;

import javax.management.JMException;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.DatabaseClusterConfiguration;
import org.hajdbc.DatabaseClusterConfigurationListener;
import org.hajdbc.DatabaseClusterListener;
import org.hajdbc.Locality;
import org.hajdbc.SynchronizationListener;
import org.hajdbc.SynchronizationStrategy;
import org.hajdbc.TransactionMode;
import org.hajdbc.Version;
import org.hajdbc.balancer.Balancer;
import org.hajdbc.cache.DatabaseMetaDataCache;
import org.hajdbc.codec.Decoder;
import org.hajdbc.dialect.Dialect;
import org.hajdbc.distributed.CommandDispatcherFactory;
import org.hajdbc.durability.Durability;
import org.hajdbc.durability.InvocationEvent;
import org.hajdbc.durability.InvokerEvent;
import org.hajdbc.io.InputSinkStrategy;
import org.hajdbc.lock.LockManager;
import org.hajdbc.lock.distributed.DistributedLockManager;
import org.hajdbc.logging.Level;
import org.hajdbc.logging.Logger;
import org.hajdbc.logging.LoggerFactory;
import org.hajdbc.management.Description;
import org.hajdbc.management.MBean;
import org.hajdbc.management.MBeanRegistrar;
import org.hajdbc.management.ManagedAttribute;
import org.hajdbc.management.ManagedOperation;
import org.hajdbc.messages.Messages;
import org.hajdbc.messages.MessagesFactory;
import org.hajdbc.state.DatabaseEvent;
import org.hajdbc.state.StateManager;
import org.hajdbc.state.distributed.DistributedStateManager;
import org.hajdbc.sync.SynchronizationContext;
import org.hajdbc.sync.SynchronizationContextImpl;
import org.hajdbc.tx.SimpleTransactionIdentifierFactory;
import org.hajdbc.tx.TransactionIdentifierFactory;
import org.hajdbc.tx.UUIDTransactionIdentifierFactory;
import org.hajdbc.util.concurrent.cron.CronExpression;
import org.hajdbc.util.concurrent.cron.CronThreadPoolExecutor;

/**
 * @author Paul Ferraro
 */
@MBean
public class DatabaseClusterImpl<Z, D extends Database<Z>> implements DatabaseCluster<Z, D>
{
	static final Messages messages = MessagesFactory.getMessages();
	static final Logger logger = LoggerFactory.getLogger(DatabaseClusterImpl.class);
	
	private final String id;
	
	final DatabaseClusterConfiguration<Z, D> configuration;
	
	private Balancer<Z, D> balancer;
	private Dialect dialect;
	private Durability<Z, D> durability;
	private DatabaseMetaDataCache<Z, D> databaseMetaDataCache;
	private ExecutorService executor;
	private Decoder decoder;
	private CronThreadPoolExecutor cronExecutor;
	private LockManager lockManager;
	private StateManager stateManager;
	private InputSinkStrategy<? extends Object> sinkSourceFactory;
	private TransactionIdentifierFactory<? extends Object> txIdentifierFactory;
	private MBeanRegistrar<Z, D> registrar;
	
	private boolean active = false;
	
	private final List<DatabaseClusterConfigurationListener<Z, D>> configurationListeners = new CopyOnWriteArrayList<>();	
	private final List<DatabaseClusterListener> clusterListeners = new CopyOnWriteArrayList<>();
	private final List<SynchronizationListener> synchronizationListeners = new CopyOnWriteArrayList<>();
	
	public DatabaseClusterImpl(String id, DatabaseClusterConfiguration<Z, D> configuration)
	{
		this(id, configuration, null);
	}
	
	public DatabaseClusterImpl(String id, DatabaseClusterConfiguration<Z, D> configuration, DatabaseClusterConfigurationListener<Z, D> listener)
	{
		this.id = id;
		this.configuration = configuration;
		
		if (listener != null)
		{
			this.configurationListeners.add(listener);
		}
	}

	/**
	 * Deactivates the specified database.
	 * @param databaseId a database identifier
	 * @throws IllegalArgumentException if no database exists with the specified identifier.
	 */
	@ManagedOperation
	public void deactivate(String databaseId)
	{
		this.deactivate(this.getDatabase(databaseId), this.stateManager);
	}

	/**
	 * Synchronizes, using the default strategy, and reactivates the specified database.
	 * @param databaseId a database identifier
	 * @throws IllegalArgumentException if no database exists with the specified identifier.
	 * @throws IllegalStateException if synchronization fails.
	 */
	@ManagedOperation
	public void activate(String databaseId)
	{
		this.activate(databaseId, this.configuration.getDefaultSynchronizationStrategy());
	}

	/**
	 * Synchronizes, using the specified strategy, and reactivates the specified database.
	 * @param databaseId a database identifier
	 * @param strategyId the identifer of a synchronization strategy
	 * @throws IllegalArgumentException if no database exists with the specified identifier, or no synchronization strategy exists with the specified identifier.
	 * @throws IllegalStateException if synchronization fails.
	 */
	@ManagedOperation
	public void activate(String databaseId, String strategyId)
	{
		SynchronizationStrategy strategy = this.configuration.getSynchronizationStrategyMap().get(strategyId);
		
		if (strategy == null)
		{
			throw new IllegalArgumentException(messages.invalidSyncStrategy(strategyId, this.configuration.getSynchronizationStrategyMap().keySet()));
		}
		
		D database = this.getDatabase(databaseId);
		try
		{
			if (this.activate(database, strategy))
			{
				logger.log(Level.INFO, messages.activated(this, database));
			}
		}
		catch (SQLException e)
		{
			logger.log(Level.WARN, e, messages.activationFailed(this, database));
			
			SQLException exception = e.getNextException();
			
			while (exception != null)
			{
				logger.log(Level.ERROR, exception);
				
				exception = exception.getNextException();
			}

			throw new IllegalStateException(e.toString());
		}
		catch (InterruptedException e)
		{
			logger.log(Level.WARN, e);
			
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Determines whether or not the specified database is responsive
	 * @param databaseId a database identifier
	 * @return true, if the database is alive, false otherwise
	 * @throws IllegalArgumentException if no database exists with the specified identifier.
	 */
	@ManagedOperation
	public boolean isAlive(String databaseId)
	{
		return this.isAlive(this.getDatabase(databaseId), Level.WARN);
	}
	
	/**
	 * Returns a collection of active databases in this cluster.
	 * @return a list of database identifiers
	 */
	@ManagedAttribute
	public Set<String> getActiveDatabases()
	{
		Set<String> databases = new TreeSet<>();
		
		for (D database: this.balancer)
		{
			databases.add(database.getId());
		}
		
		return databases;
	}
	
	/**
	 * Returns a collection of inactive databases in this cluster.
	 * @return a collection of database identifiers
	 */
	@ManagedAttribute
	public Set<String> getInactiveDatabases()
	{
		Set<String> databases = new TreeSet<>(this.configuration.getDatabaseMap().keySet());
		
		for (D database: this.balancer)
		{
			databases.remove(database.getId());
		}
		
		return databases;
	}
	
	/**
	 * Return the current HA-JDBC version
	 * @return the current version
	 */
	@ManagedAttribute
	public String getVersion()
	{
		return Version.CURRENT.toString();
	}

	/**
	 * Removes the specified database from the cluster.
	 * @param databaseId a database identifier
	 * @throws JMException 
	 * @throws IllegalArgumentException if database already exists.
	 *
	@ManagedOperation
	public void add(String databaseId) throws JMException
	{
		D database = this.configuration.getDatabaseFactory().createDatabase(databaseId);
		
		if (this.configuration.getDatabaseMap().putIfAbsent(databaseId, database) != null)
		{
			throw new IllegalArgumentException(Messages.DATABASE_ALREADY_EXISTS.getMessage(databaseId, this));
		}
		
		this.registrar.register(this, database);
		
		for (DatabaseClusterConfigurationListener<Z, D> listener: this.configurationListeners)
		{
			listener.added(database, this.configuration);
		}
	}
*/	
	/**
	 * Removes the specified database from the cluster.
	 * @param databaseId a database identifier
	 * @throws IllegalStateException if database is still active.
	 */
	@ManagedOperation
	public void remove(String databaseId)
	{
		D database = this.getDatabase(databaseId);
		
		if (this.balancer.contains(database))
		{
			throw new IllegalStateException(messages.stillActive(this, database));
		}

		this.registrar.unregister(this, database);
		
		this.configuration.getDatabaseMap().remove(databaseId);

		for (DatabaseClusterConfigurationListener<Z, D> listener: this.configurationListeners)
		{
			listener.removed(database, this.configuration);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getId()
	 */
	@ManagedAttribute
	@Override
	public String getId()
	{
		return this.id;
	}

	@Override
	public String toString()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#isActive()
	 */
	@ManagedAttribute
	@Override
	public boolean isActive()
	{
		return this.active;
	}
	
	/**
	 * Returns the set of synchronization strategies available to this cluster.
	 * @return a set of synchronization strategy identifiers
	 */
	@ManagedAttribute
	public Set<String> getSynchronizationStrategies()
	{
		return new TreeSet<>(this.configuration.getSynchronizationStrategyMap().keySet());
	}
	
	/**
	 * Returns the default synchronization strategy used by this cluster.
	 * @return a synchronization strategy identifier
	 */
	@ManagedAttribute
	public String getDefaultSynchronizationStrategy()
	{
		return this.configuration.getDefaultSynchronizationStrategy();
	}

	/**
	 * Flushes this cluster's cache of DatabaseMetaData.
	 */
	@ManagedOperation
	@Description("Flushes this cluster's cache of database meta data")
	public void flushMetaDataCache()
	{
		try
		{
			this.databaseMetaDataCache.flush();
		}
		catch (SQLException e)
		{
			throw new IllegalStateException(e.toString(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#addConfigurationListener(org.hajdbc.DatabaseClusterConfigurationListener)
	 */
	@ManagedOperation
	@Override
	public void addConfigurationListener(DatabaseClusterConfigurationListener<Z, D> listener)
	{
		this.configurationListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#addListener(org.hajdbc.DatabaseClusterListener)
	 */
	@ManagedOperation
	@Override
	public void addListener(DatabaseClusterListener listener)
	{
		this.clusterListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#addSynchronizationListener(org.hajdbc.SynchronizationListener)
	 */
	@ManagedOperation
	@Override
	public void addSynchronizationListener(SynchronizationListener listener)
	{
		this.synchronizationListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#removeConfigurationListener(org.hajdbc.DatabaseClusterConfigurationListener)
	 */
	@ManagedOperation
	@Override
	public void removeConfigurationListener(DatabaseClusterConfigurationListener<Z, D> listener)
	{
		this.configurationListeners.remove(listener);
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#removeListener(org.hajdbc.DatabaseClusterListener)
	 */
	@ManagedOperation
	@Override
	public void removeListener(DatabaseClusterListener listener)
	{
		this.clusterListeners.remove(listener);
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#removeSynchronizationListener(org.hajdbc.SynchronizationListener)
	 */
	@ManagedOperation
	@Override
	public void removeSynchronizationListener(SynchronizationListener listener)
	{
		this.synchronizationListeners.remove(listener);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#activate(org.hajdbc.Database, org.hajdbc.state.StateManager)
	 */
	@Override
	public boolean activate(D database, StateManager manager)
	{
		boolean added = this.balancer.add(database);
		
		if (added)
		{
/*
			database.setActive(true);
			
			if (database.isDirty())
			{
				database.clean();
			}
*/
			DatabaseEvent event = new DatabaseEvent(database);

			manager.activated(event);
			
			for (DatabaseClusterListener listener: this.clusterListeners)
			{
				listener.activated(event);
			}
		}
		
		return added;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#deactivate(org.hajdbc.Database, org.hajdbc.state.StateManager)
	 */
	@Override
	public boolean deactivate(D database, StateManager manager)
	{
		boolean removed = this.balancer.remove(database);
		
		if (removed)
		{
//			database.setActive(false);
			
			DatabaseEvent event = new DatabaseEvent(database);

			manager.deactivated(event);
			
			for (DatabaseClusterListener listener: this.clusterListeners)
			{
				listener.deactivated(event);
			}
		}
		
		return removed;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getBalancer()
	 */
	@Override
	public Balancer<Z, D> getBalancer()
	{
		return this.balancer;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getDatabase(java.lang.String)
	 */
	@Override
	public D getDatabase(String id)
	{
		D database = this.configuration.getDatabaseMap().get(id);
		
		if (database == null)
		{
			throw new IllegalArgumentException(messages.invalidDatabase(this, id));
		}
		
		return database;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getDatabaseMetaDataCache()
	 */
	@Override
	public DatabaseMetaDataCache<Z, D> getDatabaseMetaDataCache()
	{
		return this.databaseMetaDataCache;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getDialect()
	 */
	@Override
	public Dialect getDialect()
	{
		return this.dialect;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getDurability()
	 */
	@Override
	public Durability<Z, D> getDurability()
	{
		return this.durability;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getLockManager()
	 */
	@Override
	public LockManager getLockManager()
	{
		return this.lockManager;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getExecutor()
	 */
	@Override
	public ExecutorService getExecutor()
	{
		return this.executor;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getTransactionMode()
	 */
	@Override
	public TransactionMode getTransactionMode()
	{
		return this.configuration.getTransactionMode();
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getStateManager()
	 */
	@Override
	public StateManager getStateManager()
	{
		return this.stateManager;
	}

	@Override
	public ThreadFactory getThreadFactory()
	{
		return this.configuration.getThreadFactory();
	}

	@Override
	public Decoder getDecoder()
	{
		return this.decoder;
	}

	@Override
	public InputSinkStrategy<? extends Object> getInputSinkStrategy()
	{
		return this.sinkSourceFactory;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#getTransactionIdentifierFactory()
	 */
	@Override
	public TransactionIdentifierFactory<? extends Object> getTransactionIdentifierFactory()
	{
		return this.txIdentifierFactory;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#isCurrentDateEvaluationEnabled()
	 */
	@Override
	public boolean isCurrentDateEvaluationEnabled()
	{
		return this.configuration.isCurrentDateEvaluationEnabled();
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#isCurrentTimeEvaluationEnabled()
	 */
	@Override
	public boolean isCurrentTimeEvaluationEnabled()
	{
		return this.configuration.isCurrentTimeEvaluationEnabled();
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#isCurrentTimestampEvaluationEnabled()
	 */
	@Override
	public boolean isCurrentTimestampEvaluationEnabled()
	{
		return this.configuration.isCurrentTimestampEvaluationEnabled();
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#isIdentityColumnDetectionEnabled()
	 */
	@Override
	public boolean isIdentityColumnDetectionEnabled()
	{
		return this.configuration.isIdentityColumnDetectionEnabled();
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#isRandEvaluationEnabled()
	 */
	@Override
	public boolean isRandEvaluationEnabled()
	{
		return this.configuration.isRandEvaluationEnabled();
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.DatabaseCluster#isSequenceDetectionEnabled()
	 */
	@Override
	public boolean isSequenceDetectionEnabled()
	{
		return this.configuration.isSequenceDetectionEnabled();
	}

	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.Lifecycle#start()
	 */
	@Override
	public synchronized void start() throws SQLException
	{
		if (this.active) return;
		
		this.decoder = this.configuration.getDecoderFactory().createDecoder(this.id);
		this.lockManager = this.configuration.getLockManagerFactory().createLockManager();
		this.stateManager = this.configuration.getStateManagerFactory().createStateManager(this);
		
		CommandDispatcherFactory dispatcherFactory = this.configuration.getDispatcherFactory();
		
		if (dispatcherFactory != null)
		{
			try
			{
				this.lockManager = new DistributedLockManager(this, dispatcherFactory);
				this.stateManager = new DistributedStateManager<>(this, dispatcherFactory);
			}
			catch (Exception e)
			{
				throw new SQLException(e);
			}
		}
		
		this.balancer = this.configuration.getBalancerFactory().createBalancer(new TreeSet<D>());
		this.dialect = this.configuration.getDialectFactory().createDialect();
		this.durability = this.configuration.getDurabilityFactory().createDurability(this);
		this.executor = this.configuration.getExecutorProvider().getExecutor(this.configuration.getThreadFactory());
		this.sinkSourceFactory = this.configuration.getInputSinkProvider().createInputSinkStrategy();
		this.txIdentifierFactory = (dispatcherFactory != null) ? new UUIDTransactionIdentifierFactory() : new SimpleTransactionIdentifierFactory();

		this.lockManager.start();
		this.stateManager.start();
		
		Set<String> databases = this.stateManager.getActiveDatabases();
		
		if (!databases.isEmpty())
		{
			messages.initialClusterState(this, databases);
			
			for (String databaseId: databases)
			{
				D database = this.getDatabase(databaseId);
				
				this.balancer.add(database);
			}
		}
		else
		{
			messages.initialClusterStateEmpty(this);
			
			for (D database: this.configuration.getDatabaseMap().values())
			{
				if (this.isAlive(database, Level.WARN))
				{
					this.activate(database, this.stateManager);
//					database.setActive(true);
				}
			}
		}

		Map<InvocationEvent, Map<String, InvokerEvent>> invokers = this.stateManager.recover();
		if (!invokers.isEmpty())
		{
			this.durability.recover(invokers);
		}
		
		this.databaseMetaDataCache = this.configuration.getDatabaseMetaDataCacheFactory().createCache(this);
		
		try
		{
			this.flushMetaDataCache();
		}
		catch (IllegalStateException e)
		{
			// Ignore - cache will initialize lazily.
		}
		
		CronExpression failureDetectionExpression = this.configuration.getFailureDetectionExpression();
		CronExpression autoActivationExpression = this.configuration.getAutoActivationExpression();
		int threads = requiredThreads(failureDetectionExpression) + requiredThreads(autoActivationExpression);
		
		if (threads > 0)
		{
			this.cronExecutor = new CronThreadPoolExecutor(threads, this.configuration.getThreadFactory());
			
			if (failureDetectionExpression != null)
			{
				this.cronExecutor.schedule(new FailureDetectionTask(), failureDetectionExpression);
			}
						
			if (autoActivationExpression != null)
			{
				this.cronExecutor.schedule(new AutoActivationTask(), autoActivationExpression);
			}
		}
		
		this.registrar = this.configuration.getMBeanRegistrarFactory().createMBeanRegistrar();

		try
		{
			this.registrar.register(this);
			
			for (D database: this.configuration.getDatabaseMap().values())
			{
				this.registrar.register(this, database);
			}
		}
		catch (JMException e)
		{
			throw new SQLException(e);
		}

		this.active = true;
		
		logger.log(Level.INFO, messages.start(this));
	}

	private static int requiredThreads(CronExpression expression)
	{
		return (expression != null) ? 1 : 0;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.hajdbc.Lifecycle#stop()
	 */
	@Override
	public synchronized void stop()
	{
		logger.log(Level.INFO, messages.stop(this));
		
		this.active = false;
		
		if (this.balancer != null)
		{
			// Proactively deactivate any embedded databases
			for (D database: this.balancer)
			{
				if (database.getLocality() == Locality.EMBEDDED)
				{
					this.deactivate(database, this.stateManager);
				}
			}
		}
		
		if (this.registrar != null)
		{
			this.registrar.unregister(this);
			
			for (D database: this.configuration.getDatabaseMap().values())
			{
				this.registrar.unregister(this, database);
			}
		}
		
		if (this.cronExecutor != null)
		{
			this.cronExecutor.shutdownNow();
		}
		
		if (this.stateManager != null)
		{
			this.stateManager.stop();
		}
		
		if (this.lockManager != null)
		{
			this.lockManager.stop();
		}

		if (this.executor != null)
		{
			this.executor.shutdownNow();
		}

		if (this.balancer != null)
		{
			this.balancer.clear();
		}
	}

	boolean isAlive(D database, Level level)
	{
		try (Connection connection = database.connect(this.decoder))
		{
			return this.dialect.isValid(connection);
		}
		catch (SQLException e)
		{
			logger.log(level, e);
			return false;
		}
	}

	boolean activate(D database, SynchronizationStrategy strategy) throws SQLException, InterruptedException
	{
		if (!this.isAlive(database, Level.DEBUG)) return false;
		
		Lock lock = this.lockManager.writeLock(null);
		
		lock.lockInterruptibly();
		
		try
		{
			if (this.balancer.contains(database)) return false;
			
			if (!this.balancer.isEmpty())
			{
				try (SynchronizationContext<Z, D> context = new SynchronizationContextImpl<>(this, database))
				{
					DatabaseEvent event = new DatabaseEvent(database);
					
					logger.log(Level.INFO, messages.synchronizationBegin(this, database, strategy));
					
					for (SynchronizationListener listener: this.synchronizationListeners)
					{
						listener.beforeSynchronization(event);
					}
					
					strategy.synchronize(context);
	
					logger.log(Level.INFO, messages.synchronizationEnd(this, database, strategy));
					
					for (SynchronizationListener listener: this.synchronizationListeners)
					{
						listener.afterSynchronization(event);
					}
				}
			}
			
			return this.activate(database, this.stateManager);
		}
		finally
		{
			lock.unlock();
		}
	}

	class FailureDetectionTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!DatabaseClusterImpl.this.getStateManager().isEnabled()) return;
			
			Set<D> databases = DatabaseClusterImpl.this.getBalancer();
			
			int size = databases.size();
			
			if ((size > 1) || DatabaseClusterImpl.this.configuration.isEmptyClusterAllowed())
			{
				List<D> deadList = new ArrayList<>(size);
				
				for (D database: databases)
				{
					if (!DatabaseClusterImpl.this.isAlive(database, Level.WARN))
					{
						deadList.add(database);
					}
				}

				if ((deadList.size() < size) || DatabaseClusterImpl.this.configuration.isEmptyClusterAllowed())
				{
					for (D database: deadList)
					{
						if (DatabaseClusterImpl.this.deactivate(database, DatabaseClusterImpl.this.getStateManager()))
						{
							logger.log(Level.ERROR, messages.deactivated(DatabaseClusterImpl.this, database));
						}
					}
				}
			}
		}
	}	
	
	class AutoActivationTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!DatabaseClusterImpl.this.getStateManager().isEnabled()) return;
			
			try
			{
				Set<D> activeDatabases = DatabaseClusterImpl.this.getBalancer();
				
				if (!activeDatabases.isEmpty())
				{
					for (D database: DatabaseClusterImpl.this.configuration.getDatabaseMap().values())
					{
						if (!activeDatabases.contains(database))
						{
							try
							{
								if (DatabaseClusterImpl.this.activate(database, DatabaseClusterImpl.this.configuration.getSynchronizationStrategyMap().get(DatabaseClusterImpl.this.configuration.getDefaultSynchronizationStrategy())))
								{
									logger.log(Level.INFO, messages.activated(DatabaseClusterImpl.this, database));
								}
							}
							catch (SQLException e)
							{
								logger.log(Level.DEBUG, e);
							}
						}
					}
				}
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}
}
