package io.github.hajdbc.io;

import java.io.File;

import io.github.hajdbc.io.file.FileInputSinkStrategy;

public class FileInputSinkStrategyTest extends InputSinkStrategyTest<File>
{
	public FileInputSinkStrategyTest()
	{
		super(new FileInputSinkStrategy());
	}
}
