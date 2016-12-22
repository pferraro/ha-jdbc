package org.hajdbc.io;

import java.io.File;

import org.hajdbc.io.file.FileInputSinkStrategy;

public class FileInputSinkStrategyTest extends InputSinkStrategyTest<File>
{
	public FileInputSinkStrategyTest()
	{
		super(new FileInputSinkStrategy());
	}
}
