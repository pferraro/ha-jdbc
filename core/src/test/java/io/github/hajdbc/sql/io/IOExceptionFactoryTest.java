package io.github.hajdbc.sql.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;

import io.github.hajdbc.ExceptionFactory;
import io.github.hajdbc.ExceptionType;
import io.github.hajdbc.dialect.Dialect;
import io.github.hajdbc.durability.Durability.Phase;
import io.github.hajdbc.sql.io.IOExceptionFactory;

public class IOExceptionFactoryTest
{
	private final ExceptionFactory<IOException> factory = new IOExceptionFactory();
	
	@Test
	public void getTargetClass()
	{
		assertSame(IOException.class, this.factory.getTargetClass());
	}
	
	@Test
	public void createExceptionFromMessage()
	{
		String message = "message";
		
		IOException result = this.factory.createException(message);
		
		assertSame(message, result.getMessage());
		assertNull(result.getCause());
	}
	
	@Test
	public void createExceptionFromException()
	{
		String message = "message";
		Exception exception = new Exception(message);
		
		IOException result = this.factory.createException(exception);
		
		assertNotNull(result.getMessage());
		assertSame(exception.getMessage(), result.getMessage());
		assertSame(exception, result.getCause());
	}
	
	@Test
	public void createExceptionFromSQLException()
	{
		IOException exception = new IOException();
		
		IOException result = this.factory.createException(exception);
		
		assertSame(exception, result);
	}
	
	@Test
	public void getType()
	{
		ExceptionType result = this.factory.getType();
		
		assertSame(ExceptionType.IO, result);
	}
	
	@Test
	public void equals()
	{
		assertTrue(this.factory.equals(new IOException(), new IOException()));
		assertFalse(this.factory.equals(new IOException("message"), new IOException()));
		assertTrue(this.factory.equals(new IOException("message"), new IOException("message")));
		assertFalse(this.factory.equals(new IOException("message1"), new IOException("message2")));
	}
	
	@Test
	public void correctHeuristic()
	{
		assertFalse(this.factory.correctHeuristic(new IOException(), Phase.COMMIT));
		assertFalse(this.factory.correctHeuristic(new IOException(), Phase.FORGET));
		assertFalse(this.factory.correctHeuristic(new IOException(), Phase.PREPARE));
		assertFalse(this.factory.correctHeuristic(new IOException(), Phase.ROLLBACK));
	}
	
	@Test
	public void indicatesFailure()
	{
		Dialect dialect = mock(Dialect.class);
		
		assertFalse(this.factory.indicatesFailure(new IOException(), dialect));

		Mockito.verifyNoMoreInteractions(dialect);
	}
}
