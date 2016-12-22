/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2014  Paul Ferraro
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
package org.hajdbc.lock.distributed;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.hajdbc.lock.distributed.AcquireLockCommand;
import org.hajdbc.lock.distributed.LockCommandContext;
import org.hajdbc.lock.distributed.LockDescriptor;
import org.hajdbc.lock.distributed.RemoteLockDescriptor;
import org.junit.Test;

/**
 * Unit test for {@link AcquireLockCommand}.
 * @author Paul Ferraro
 */
public class AcquireLockCommandTestCase
{
	@Test
	public void execute() throws InterruptedException
	{
		RemoteLockDescriptor descriptor = mock(RemoteLockDescriptor.class);
		LockCommandContext context = mock(LockCommandContext.class);
		Lock lock = mock(Lock.class);
		long timeout = 10L;
		AcquireLockCommand command = new AcquireLockCommand(descriptor, timeout);
		Map<LockDescriptor, Lock> locks = mock(Map.class);
		
		// Successful lock
		when(context.getLock(descriptor)).thenReturn(lock);
		when(lock.tryLock(timeout, TimeUnit.MILLISECONDS)).thenReturn(true);
		when(context.getRemoteLocks(descriptor)).thenReturn(locks);
		
		Boolean result = command.execute(context);
		
		verify(locks).put(descriptor, lock);
		
		assertNotNull(result);
		assertTrue(result.booleanValue());
		
		// Unsuccessful lock
		when(lock.tryLock(timeout, TimeUnit.MILLISECONDS)).thenReturn(false);
		
		result = command.execute(context);
		
		verifyNoMoreInteractions(locks);
		
		assertNotNull(result);
		assertFalse(result.booleanValue());

		// Interrupted lock
		when(lock.tryLock(timeout, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException());

		result = command.execute(context);
		
		verifyNoMoreInteractions(locks);
		
		assertNotNull(result);
		assertFalse(result.booleanValue());
		assertTrue(Thread.currentThread().isInterrupted());
	}
}
