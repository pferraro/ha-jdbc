package org.hajdbc.io;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Random;

import org.hajdbc.io.InputSinkChannel;
import org.hajdbc.io.InputSinkStrategy;
import org.junit.Assert;
import org.junit.Test;

public abstract class InputSinkStrategyTest<S>
{
	private final Random random = new Random();
	private final InputSinkStrategy<S> strategy;

	protected InputSinkStrategyTest(InputSinkStrategy<S> strategy)
	{
		this.strategy = strategy;
	}
	
	@Test
	public void testInputStreamChannel() throws IOException
	{
		byte[] expected = new byte[Short.MAX_VALUE];
		
		this.random.nextBytes(expected);
		
		InputSinkChannel<InputStream, S> channel = this.strategy.createInputStreamChannel();
		
		S sink = channel.write(new ByteArrayInputStream(expected));
		
		try (InputStream input = channel.read(sink))
		{
			byte[] result = new byte[Short.MAX_VALUE];
			
			input.read(result);
			
			Assert.assertArrayEquals(expected, result);
		}
	}
	
	@Test
	public void testReaderChannel() throws IOException
	{
		int min = ' ';
		int max = '~';
		char[] expected = new char[Short.MAX_VALUE];
		for (int i = 0; i < expected.length; ++i)
		{
			expected[i] = (char) (this.random.nextInt(max - min) + min);
		}

		InputSinkChannel<Reader, S> channel = this.strategy.createReaderChannel();
		
		S sink = channel.write(new CharArrayReader(expected));
		
		try (Reader reader = channel.read(sink))
		{
			char[] result = new char[Short.MAX_VALUE];
			
			reader.read(result);
			
			Assert.assertArrayEquals(expected, result);
		}
	}
}
