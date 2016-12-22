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
import java.sql.DriverAction;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.hajdbc.DatabaseCluster;
import org.hajdbc.DatabaseClusterConfigurationFactory;
import org.hajdbc.DatabaseClusterFactory;
import org.hajdbc.invocation.InvocationStrategies;
import org.hajdbc.invocation.Invoker;
import org.hajdbc.logging.Level;
import org.hajdbc.logging.Logger;
import org.hajdbc.logging.LoggerFactory;
import org.hajdbc.messages.Messages;
import org.hajdbc.messages.MessagesFactory;
import org.hajdbc.xml.XMLDatabaseClusterConfigurationFactory;
import org.kohsuke.MetaInfServices;

/**
 * @author  Paul Ferraro
 */
@MetaInfServices(java.sql.Driver.class)
public class Driver extends AbstractDriver implements DriverAction
{
	private static final Pattern URL_PATTERN = Pattern.compile("jdbc:ha-jdbc:(?://)?([^/]+)(?:/.+)?");
	private static final String CONFIG = "config";

	private static final Messages messages = MessagesFactory.getMessages();
	static final Logger logger = LoggerFactory.getLogger(Driver.class);

	static volatile Duration timeout = Duration.ofSeconds(10);
	static volatile DatabaseClusterFactory<java.sql.Driver, DriverDatabase> factory = new DatabaseClusterFactoryImpl<>();
	
	static final Map<String, DatabaseClusterConfigurationFactory<java.sql.Driver, DriverDatabase>> configurationFactories = new ConcurrentHashMap<>();
	static final ConcurrentMap<String, Map.Entry<DriverProxyFactory, java.sql.Driver>> proxies = new ConcurrentHashMap<>();

	static
	{
		try
		{
			Driver driver = new Driver();
			DriverManager.registerDriver(driver, driver);
		}
		catch (SQLException e)
		{
			logger.log(Level.ERROR, messages.registerDriverFailed(Driver.class), e);
		}
	}
	
	public static void close(String id)
	{
		Map.Entry<DriverProxyFactory, java.sql.Driver> entry = proxies.remove(id);
		if (entry != null)
		{
			try (DriverProxyFactory factory = entry.getKey())
			{
				factory.getDatabaseCluster().stop();
			}
		}
	}

	public static void setFactory(DatabaseClusterFactory<java.sql.Driver, DriverDatabase> factory)
	{
		Driver.factory = factory;
	}

	public static void setConfigurationFactory(String id, DatabaseClusterConfigurationFactory<java.sql.Driver, DriverDatabase> configurationFactory)
	{
		configurationFactories.put(id,  configurationFactory);
	}

	private static Map.Entry<DriverProxyFactory, java.sql.Driver> getProxyEntry(String id, Properties properties)
	{
		Function<String, Map.Entry<DriverProxyFactory, java.sql.Driver>> function = (String key) ->
		{
			DatabaseClusterConfigurationFactory<java.sql.Driver, DriverDatabase> configurationFactory = configurationFactories.get(key);
			
			if (configurationFactory == null)
			{
				String config = (properties != null) ? properties.getProperty(CONFIG) : null;
				configurationFactory = new XMLDatabaseClusterConfigurationFactory<>(id, config);
			}
			try
			{
				DatabaseCluster<java.sql.Driver, DriverDatabase> cluster = factory.createDatabaseCluster(key, configurationFactory, new DriverDatabaseClusterConfigurationBuilder());
				DriverProxyFactory factory = new DriverProxyFactory(cluster);
				cluster.start();
				return new AbstractMap.SimpleImmutableEntry<>(factory, factory.createProxy());
			}
			catch (SQLException e)
			{
				throw new IllegalStateException(e);
			}
		};
		return proxies.computeIfAbsent(id, function);
	}

	public Driver()
	{
		super(URL_PATTERN);
	}

	@Override
	public void deregister()
	{
		// When the driver instance that was registered with the DriverManager is deregistered, close any clusters
		for (String id : proxies.keySet())
		{
			try
			{
				close(id);
			}
			catch (Throwable e)
			{
				e.printStackTrace(DriverManager.getLogWriter());
			}
		}
	}

	@Override
	public Connection connect(String url, final Properties properties) throws SQLException
	{
		String id = this.parse(url);
		
		// JDBC spec compliance
		if (id == null) return null;
		
		Map.Entry<DriverProxyFactory, java.sql.Driver> entry = getProxyEntry(id, properties);
		TransactionContext<java.sql.Driver, DriverDatabase> context = new LocalTransactionContext<>(entry.getKey().getDatabaseCluster());

		ConnectionProxyFactoryFactory<java.sql.Driver, DriverDatabase, java.sql.Driver> factory = new ConnectionProxyFactoryFactory<>(context);
		DriverInvoker<Connection> invoker = (DriverDatabase database, java.sql.Driver driver) -> driver.connect(database.getLocation(), properties);
		return factory.createProxyFactory(entry.getValue(), entry.getKey(), invoker, InvocationStrategies.INVOKE_ON_ALL.invoke(entry.getKey(), invoker)).createProxy();
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, final Properties properties) throws SQLException
	{
		String id = this.parse(url);
		
		// JDBC spec compliance
		if (id == null) return null;
		
		Map.Entry<DriverProxyFactory, java.sql.Driver> entry = getProxyEntry(id, properties);
		DriverInvoker<DriverPropertyInfo[]> invoker = (DriverDatabase database, java.sql.Driver driver) -> driver.getPropertyInfo(database.getLocation(), properties);
		SortedMap<DriverDatabase, DriverPropertyInfo[]> results = InvocationStrategies.INVOKE_ON_ANY.invoke(entry.getKey(), invoker);
		return results.get(results.firstKey());
	}

	@Override
	public java.util.logging.Logger getParentLogger()
	{
		return java.util.logging.Logger.getGlobal();
	}

	private interface DriverInvoker<R> extends Invoker<java.sql.Driver, DriverDatabase, java.sql.Driver, R, SQLException>
	{
	}
}
