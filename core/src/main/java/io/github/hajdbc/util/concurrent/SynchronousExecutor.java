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
package io.github.hajdbc.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.github.hajdbc.util.Reversed;

/**
 * Executor service that executes tasks in the caller thread.
 * 
 * @author Paul Ferraro
 */
public class SynchronousExecutor extends AbstractExecutorService
{
	private final ExecutorService executor;
	private final boolean reverse;
	
	public SynchronousExecutor(ExecutorService executor)
	{
		this(executor, false);
	}
	
	public SynchronousExecutor(ExecutorService executor, boolean reverse)
	{
		this.executor = executor;
		this.reverse = reverse;
	}
	
	@Override
	public boolean awaitTermination(long time, TimeUnit unit) throws InterruptedException
	{
		return this.executor.awaitTermination(time, unit);
	}

	@Override
	public boolean isShutdown()
	{
		return this.executor.isShutdown();
	}

	@Override
	public boolean isTerminated()
	{
		return this.executor.isTerminated();
	}

	@Override
	public void shutdown()
	{
		this.executor.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow()
	{
		return this.executor.shutdownNow();
	}

	@Override
	public void execute(Runnable task)
	{
		task.run();
	}

	@Override
	public Future<?> submit(Runnable task)
	{
		return this.submit(Executors.callable(task));
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result)
	{
		return this.submit(Executors.callable(task, result));
	}

	@Override
	public <T> Future<T> submit(Callable<T> task)
	{
		return new EagerFuture<>(task);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException
	{
		return this.invokeAll(tasks, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	/**
	 * Executes the specified tasks serially, until the first successful result, then the remainder using the executor with which this executor was created.
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException
	{
		if (tasks.isEmpty()) return Collections.emptyList();

		long end = (timeout == Long.MAX_VALUE) ? 0 : System.currentTimeMillis() + unit.toMillis(timeout);
		boolean synchronous = !this.reverse;
		int remaining = tasks.size();
		LinkedList<Future<T>> futures = new LinkedList<>();
		
		for (Callable<T> task: this.reverse ? new Reversed<>(new ArrayList<>(tasks)) : tasks)
		{
			remaining -= 1;

			if (synchronous)
			{
				Future<T> future = this.reverse ? new LazyFuture<>(task) : new EagerFuture<>(task);
				
				if (this.reverse)
				{
					futures.addFirst(future);
				}
				else
				{
					futures.addLast(future);
				}
				
				// Execute remaining tasks in parallel, if there are multiple
				if (remaining > 1)
				{
					synchronous = false;
				}
			}
			else
			{
				Future<T> future = this.executor.submit(task);
				if (this.reverse)
				{
					futures.addFirst(future);
				}
				else
				{
					futures.addLast(future);
				}
				
				if (this.reverse && (remaining == 1))
				{
					synchronous = true;
				}
			}
		}
		
		try
		{
			// Wait until all tasks have finished
			for (Future<T> future: this.reverse ? new Reversed<>(futures) : futures)
			{
				if (!future.isDone())
				{
					try
					{
						if (end == 0)
						{
							future.get();
						}
						else
						{
							long now = System.currentTimeMillis();
							if (now < end)
							{
								future.get(end - now, TimeUnit.MILLISECONDS);
							}
						}
					}
					catch (ExecutionException e)
					{
						// Ignore
					}
					catch (CancellationException e)
					{
						// Ignore
					}
				}
			}
		}
		catch (TimeoutException e)
		{
			// Ignore
		}
		finally
		{
			// If interrupted, cancel any unfinished tasks
			for (Future<T> future: this.reverse ? new Reversed<>(futures) : futures)
			{
				if (!future.isDone())
				{
					future.cancel(true);
				}
			}
		}
		
		return futures;
	}
	
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
	{
		return this.getAny(this.invokeAll(tasks));
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException
	{
		return this.getAny(this.invokeAll(tasks, timeout, unit));
	}

	private <T> T getAny(List<Future<T>> futures) throws InterruptedException, ExecutionException
	{
		if (futures.isEmpty()) throw new IllegalArgumentException();
		
		return futures.get(this.reverse ? (futures.size() - 1) : 0).get();
	}
	
	/**
	 * Future that doesn't execute its task until get(...).
	 */
	private static class LazyFuture<T> implements Future<T>
	{
		private enum State
		{
			NEW, CANCELLED, DONE;
		}
		private final Callable<T> task;
		private volatile T result;
		private volatile ExecutionException exception;
		private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
		private final CountDownLatch latch = new CountDownLatch(1);
		
		LazyFuture(Callable<T> task)
		{
			this.task = task;
		}

		@Override
		public boolean cancel(boolean interrupt)
		{
			return this.state.compareAndSet(State.NEW, State.CANCELLED);
		}

		@Override
		public T get() throws ExecutionException, InterruptedException
		{
			return this.get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		}

		@Override
		public T get(long time, TimeUnit unit) throws ExecutionException, InterruptedException
		{
			if (this.state.compareAndSet(State.NEW, State.DONE))
			{
				try
				{
					this.result = this.task.call();
				}
				catch (Throwable e)
				{
					this.exception = new ExecutionException(e);
				}
				this.latch.countDown();
			}
			
			if (this.state.get() == State.CANCELLED)
			{
				throw new CancellationException();
			}
			
			if (time == Long.MAX_VALUE)
			{
				this.latch.await();
			}
			else
			{
				this.latch.await(time, unit);
			}
			
			if (this.exception != null)
			{
				throw this.exception;
			}
			
			return this.result;
		}

		@Override
		public boolean isCancelled()
		{
			return this.state.get() == State.CANCELLED;
		}

		@Override
		public boolean isDone()
		{
			return this.state.get() == State.DONE;
		}
	}

	/**
	 * Light-weight future implementation that executes its task on construction.
	 * @param <T>
	 */
	private static class EagerFuture<T> implements Future<T>
	{
		private T result;
		private ExecutionException exception;
		
		EagerFuture(Callable<T> task)
		{
			try
			{
				this.result = task.call();
			}
			catch (Throwable e)
			{
				this.exception = new ExecutionException(e);
			}
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning)
		{
			return false;
		}

		@Override
		public T get() throws ExecutionException
		{
			if (this.exception != null) throw this.exception;
			
			return this.result;
		}

		@Override
		public T get(long time, TimeUnit unit) throws ExecutionException
		{
			return this.get();
		}

		@Override
		public boolean isCancelled()
		{
			return false;
		}

		@Override
		public boolean isDone()
		{
			return true;
		}
	}
}
