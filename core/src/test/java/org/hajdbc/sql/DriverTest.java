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

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import org.hajdbc.DatabaseCluster;
import org.hajdbc.DatabaseClusterConfigurationFactory;
import org.hajdbc.DatabaseClusterFactory;
import org.hajdbc.MockDriver;
import org.hajdbc.balancer.Balancer;
import org.hajdbc.durability.Durability;
import org.hajdbc.lock.LockManager;
import org.hajdbc.sql.ConnectionInvocationHandler;
import org.hajdbc.sql.Driver;
import org.hajdbc.sql.DriverDatabase;
import org.hajdbc.sql.DriverDatabaseBuilder;
import org.hajdbc.sql.DriverDatabaseClusterConfigurationBuilder;
import org.hajdbc.tx.TransactionIdentifierFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Paul Ferraro
 */
public class DriverTest
{
	private DatabaseClusterConfigurationFactory<java.sql.Driver, DriverDatabase> configurationFactory = mock(DatabaseClusterConfigurationFactory.class);
	private DatabaseClusterFactory<java.sql.Driver, DriverDatabase> factory = mock(DatabaseClusterFactory.class);
	private Connection connection = mock(Connection.class);
	
	private java.sql.Driver mockDriver = new MockDriver(this.connection);

	@Before
	public void before() throws SQLException
	{
		DriverManager.setLogWriter(new java.io.PrintWriter(System.out));
		DriverManager.registerDriver(this.mockDriver);
		Driver.setFactory(this.factory);
	}
	
	@After
	public void after() throws SQLException
	{
		DriverManager.deregisterDriver(this.mockDriver);
		DriverManager.setLogWriter(null);
	}
	
	@Test
	public void acceptsURL()
	{
		Driver driver = new Driver();
		
		Assert.assertTrue(driver.acceptsURL("jdbc:ha-jdbc:cluster"));
		Assert.assertTrue(driver.acceptsURL("jdbc:ha-jdbc://cluster"));
		Assert.assertTrue(driver.acceptsURL("jdbc:ha-jdbc://cluster/database"));
		Assert.assertFalse(driver.acceptsURL("jdbc:postgresql:database"));
		Assert.assertFalse(driver.acceptsURL("jdbc:postgresql://server"));
		Assert.assertFalse(driver.acceptsURL("jdbc:postgresql://server/database"));
	}
	
	@Test
	public void connect() throws Exception
	{
		this.connect("jdbc:ha-jdbc:cluster1", "cluster1");
		this.connect("jdbc:ha-jdbc://cluster2", "cluster2");
		this.connect("jdbc:ha-jdbc://cluster3/dummy", "cluster3");
	}
	
	private void connect(String url, String id) throws Exception
	{
		DatabaseCluster<java.sql.Driver, DriverDatabase> cluster = mock(DatabaseCluster.class);
		Balancer<java.sql.Driver, DriverDatabase> balancer = mock(Balancer.class);
		LockManager lockManager = mock(LockManager.class);
		
		DriverDatabase database = new DriverDatabaseBuilder("db1").driver(this.mockDriver).url("jdbc:mock:test").build();
		
		Driver driver = new Driver();
		
		Driver.setConfigurationFactory(id, this.configurationFactory);
		
		try
		{
//			when(this.connection.isValid(anyInt())).thenReturn(true);
			when(this.factory.createDatabaseCluster(eq(id), same(this.configurationFactory), any(DriverDatabaseClusterConfigurationBuilder.class))).thenReturn(cluster);
			
			when(cluster.isActive()).thenReturn(true);
			when(cluster.getBalancer()).thenReturn(balancer);
			when(balancer.iterator()).thenReturn(Collections.singleton(database).iterator());
			when(cluster.getBalancer()).thenReturn(balancer);
			when(balancer.contains(database)).thenReturn(true);
			when(balancer.isEmpty()).thenReturn(false);
			when(balancer.size()).thenReturn(1);
			when(balancer.iterator()).thenReturn(Collections.singleton(database).iterator());
			when(balancer.next()).thenReturn(database);
			when(cluster.getExecutor()).thenReturn(Executors.newCachedThreadPool());
			when(cluster.getLockManager()).thenReturn(lockManager);
			when(lockManager.readLock(null)).thenReturn(mock(Lock.class));
			when(cluster.getDurability()).thenReturn(mock(Durability.class));
			when(cluster.getTransactionIdentifierFactory()).thenReturn(mock(TransactionIdentifierFactory.class));
			
			try (Connection result = driver.connect(url, null))
			{
				Assert.assertTrue(result.getClass().getName(), Proxy.isProxyClass(result.getClass()));
				ConnectionInvocationHandler<java.sql.Driver, DriverDatabase, java.sql.Driver> handler = (ConnectionInvocationHandler<java.sql.Driver, DriverDatabase, java.sql.Driver>) Proxy.getInvocationHandler(result);
				Assert.assertSame(this.connection, handler.getProxyFactory().get(database));
			}
		}
		finally
		{
			Driver.close(id);
		}
	}
}
