package io.github.hajdbc.io;

import io.github.hajdbc.io.simple.SimpleInputSinkStrategy;

public class SimpleInputSinkStrategyTest extends InputSinkStrategyTest<byte[]>
{
	public SimpleInputSinkStrategyTest()
	{
		super(new SimpleInputSinkStrategy());
	}
}
