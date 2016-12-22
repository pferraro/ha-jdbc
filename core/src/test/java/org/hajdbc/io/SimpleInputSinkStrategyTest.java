package org.hajdbc.io;

import org.hajdbc.io.simple.SimpleInputSinkStrategy;

public class SimpleInputSinkStrategyTest extends InputSinkStrategyTest<byte[]>
{
	public SimpleInputSinkStrategyTest()
	{
		super(new SimpleInputSinkStrategy());
	}
}
