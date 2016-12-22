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
package org.hajdbc.state.distributed;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;
import org.hajdbc.distributed.Command;
import org.hajdbc.distributed.CommandDispatcher;
import org.hajdbc.distributed.CommandDispatcherFactory;
import org.hajdbc.distributed.CommandResponse;
import org.hajdbc.distributed.Member;
import org.hajdbc.distributed.MembershipListener;
import org.hajdbc.distributed.Remote;
import org.hajdbc.distributed.Stateful;
import org.hajdbc.durability.InvocationEvent;
import org.hajdbc.durability.InvokerEvent;
import org.hajdbc.logging.Level;
import org.hajdbc.logging.Logger;
import org.hajdbc.logging.LoggerFactory;
import org.hajdbc.messages.Messages;
import org.hajdbc.messages.MessagesFactory;
import org.hajdbc.state.DatabaseEvent;
import org.hajdbc.state.StateManager;

/**
 * @author Paul Ferraro
 */
public class DistributedStateManager<Z, D extends Database<Z>> implements StateManager, StateCommandContext<Z, D>, MembershipListener, Stateful, Remote
{
	private final Messages messages = MessagesFactory.getMessages();
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final DatabaseCluster<Z, D> cluster;
	private final StateManager stateManager;
	private final CommandDispatcher<StateCommandContext<Z, D>> dispatcher;
	private final ConcurrentMap<Member, Map<InvocationEvent, Map<String, InvokerEvent>>> remoteInvokerMap = new ConcurrentHashMap<>();
	
	public DistributedStateManager(DatabaseCluster<Z, D> cluster, CommandDispatcherFactory dispatcherFactory) throws Exception
	{
		this.cluster = cluster;
		this.stateManager = cluster.getStateManager();
		StateCommandContext<Z, D> context = this;
		this.dispatcher = dispatcherFactory.createCommandDispatcher(cluster.getId() + ".state", context, this, this);
	}

	@Override
	public Member getMember()
	{
		return this.dispatcher.getLocal();
	}

	@Override
	public Set<String> getActiveDatabases()
	{
		return this.stateManager.getActiveDatabases();
	}

	@Override
	public void setActiveDatabases(Set<String> databases)
	{
		this.stateManager.setActiveDatabases(databases);
	}

	@Override
	public void activated(DatabaseEvent event)
	{
		this.stateManager.activated(event);
		this.execute(new ActivationCommand<Z, D>(event));
	}

	@Override
	public void deactivated(DatabaseEvent event)
	{
		this.stateManager.deactivated(event);
		this.execute(new DeactivationCommand<Z, D>(event));
	}

	@Override
	public void afterInvocation(InvocationEvent event)
	{
		this.stateManager.afterInvocation(event);
		this.execute(new PostInvocationCommand<Z, D>(this.getRemoteDescriptor(event)));
	}

	@Override
	public void afterInvoker(InvokerEvent event)
	{
		this.stateManager.afterInvoker(event);
		this.execute(new InvokerCommand<Z, D>(this.getRemoteDescriptor(event)));
	}

	@Override
	public void beforeInvocation(InvocationEvent event)
	{
		this.stateManager.beforeInvocation(event);
		this.execute(new PreInvocationCommand<Z, D>(this.getRemoteDescriptor(event)));
	}

	@Override
	public void beforeInvoker(InvokerEvent event)
	{
		this.stateManager.beforeInvoker(event);
		this.execute(new InvokerCommand<Z, D>(this.getRemoteDescriptor(event)));
	}

	private <R> void execute(Command<R, StateCommandContext<Z, D>> command)
	{
		try
		{
			Map<Member, CommandResponse<R>> responses = this.dispatcher.executeAll(command, this.dispatcher.getLocal());
			
			for (Map.Entry<Member, CommandResponse<R>> entry: responses.entrySet())
			{
				Member member = entry.getKey();
				try
				{
					entry.getValue().get();
				}
				catch (Exception e)
				{
					this.logger.log(Level.WARN, e, "Failed to execute {0} on {1}", command, member);
				}
			}
		}
		catch (Exception e)
		{
			this.logger.log(Level.WARN, e, "Failed to send {0} to cluster", command);
		}
	}

	private RemoteInvocationDescriptor getRemoteDescriptor(InvocationEvent event)
	{
		return new RemoteInvocationDescriptorImpl(event, this.dispatcher.getLocal());
	}
	
	private RemoteInvokerDescriptor getRemoteDescriptor(InvokerEvent event)
	{
		return new RemoteInvokerDescriptorImpl(event, this.dispatcher.getLocal());
	}

	@Override
	public void start() throws SQLException
	{
		this.stateManager.start();
		this.dispatcher.start();
	}

	@Override
	public void stop()
	{
		this.dispatcher.stop();
		this.stateManager.stop();
	}

	@Override
	public boolean isEnabled()
	{
		return this.stateManager.isEnabled() && this.dispatcher.getLocal().equals(this.dispatcher.getCoordinator());
	}

	@Override
	public DatabaseCluster<Z, D> getDatabaseCluster()
	{
		return this.cluster;
	}

	@Override
	public StateManager getLocalStateManager()
	{
		return this.stateManager;
	}

	@Override
	public Map<InvocationEvent, Map<String, InvokerEvent>> getRemoteInvokers(Remote remote)
	{
		return this.remoteInvokerMap.get(remote.getMember());
	}

	@Override
	public void readState(ObjectInput input) throws IOException
	{
		if (input.available() > 0)
		{
			Set<String> databases = new TreeSet<>();
			
			int size = input.readInt();
			
			for (int i = 0; i < size; ++i)
			{
				databases.add(input.readUTF());
			}
			
			this.logger.log(Level.INFO, this.messages.initialClusterState(this.cluster, databases, this.dispatcher.getCoordinator()));
			
			this.stateManager.setActiveDatabases(databases);
		}
	}

	@Override
	public void writeState(ObjectOutput output) throws IOException
	{
		Set<D> databases = this.cluster.getBalancer();
		output.writeInt(databases.size());
		
		for (D database: databases)
		{
			output.writeUTF(database.getId());
		}
	}

	@Override
	public void added(Member member)
	{
		this.remoteInvokerMap.putIfAbsent(member, new HashMap<InvocationEvent, Map<String, InvokerEvent>>());
	}

	@Override
	public void removed(Member member)
	{
		if (this.dispatcher.getLocal().equals(this.dispatcher.getCoordinator()))
		{
			Map<InvocationEvent, Map<String, InvokerEvent>> invokers = this.remoteInvokerMap.remove(member);
			
			if (invokers != null)
			{
				this.cluster.getDurability().recover(invokers);
			}
		}
	}

	@Override
	public Map<InvocationEvent, Map<String, InvokerEvent>> recover()
	{
		return this.stateManager.recover();
	}

	private static class RemoteDescriptor implements Remote, Serializable
	{
		private static final long serialVersionUID = 3717630867671175936L;
		
		private final Member member;
		
		RemoteDescriptor(Member member)
		{
			this.member = member;
		}

		@Override
		public Member getMember()
		{
			return this.member;
		}
	}
	
	private static class RemoteInvocationDescriptorImpl extends RemoteDescriptor implements RemoteInvocationDescriptor
	{
		private static final long serialVersionUID = 7782082258670023082L;
		
		private final InvocationEvent event;
		
		RemoteInvocationDescriptorImpl(InvocationEvent event, Member member)
		{
			super(member);
			
			this.event = event;
		}
		
		@Override
		public InvocationEvent getEvent()
		{
			return this.event;
		}

		@Override
		public String toString()
		{
			return this.event.toString();
		}
	}
	
	private static class RemoteInvokerDescriptorImpl extends RemoteDescriptor implements RemoteInvokerDescriptor
	{
		private static final long serialVersionUID = 6991831573393882786L;
		
		private final InvokerEvent event;
		
		RemoteInvokerDescriptorImpl(InvokerEvent event, Member member)
		{
			super(member);
			
			this.event = event;
		}
		
		@Override
		public InvokerEvent getEvent()
		{
			return this.event;
		}

		@Override
		public String toString()
		{
			return this.event.toString();
		}
	}
}
