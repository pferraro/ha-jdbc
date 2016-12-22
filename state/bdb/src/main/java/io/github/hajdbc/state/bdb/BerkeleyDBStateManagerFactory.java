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
package io.github.hajdbc.state.bdb;

import java.io.File;
import java.text.MessageFormat;

import com.sleepycat.je.EnvironmentConfig;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;
import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;
import io.github.hajdbc.messages.Messages;
import io.github.hajdbc.messages.MessagesFactory;
import io.github.hajdbc.pool.generic.GenericObjectPoolConfiguration;
import io.github.hajdbc.pool.generic.GenericObjectPoolFactory;
import io.github.hajdbc.state.StateManager;
import io.github.hajdbc.state.StateManagerFactory;
import io.github.hajdbc.util.Strings;

public class BerkeleyDBStateManagerFactory extends GenericObjectPoolConfiguration implements StateManagerFactory
{
	private static final long serialVersionUID = 7138340006866127561L;
	
	private static final Messages messages = MessagesFactory.getMessages();
	private static final Logger logger = LoggerFactory.getLogger(BerkeleyDBStateManagerFactory.class);

	private String locationPattern = "{1}/{0}";
	
	@Override
	public String getId()
	{
		return "berkeleydb";
	}

	@Override
	public <Z, D extends Database<Z>> StateManager createStateManager(DatabaseCluster<Z, D> cluster)
	{
		String location = MessageFormat.format(this.locationPattern, cluster.getId(), Strings.HA_JDBC_HOME);
		EnvironmentConfig config = new EnvironmentConfig().setAllowCreate(true).setTransactional(true);
		
		logger.log(Level.INFO, messages.clusterStatePersistence(cluster, location));
		
		return new BerkeleyDBStateManager(cluster, new File(location), config, new GenericObjectPoolFactory(this));
	}
	
	public String getLocationPattern()
	{
		return this.locationPattern;
	}

	public void setLocationPattern(String pattern)
	{
		this.locationPattern = pattern;
	}
}
