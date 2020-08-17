/*
 * Copyright (C) 2020 Matthew Rosato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.t07m.swing.console;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public abstract class Command {

	private final String prefix;
	private OptionParser optionParser = new OptionParser();

	public Command(String prefix) {
		this.prefix = prefix.toLowerCase();
	}

	protected void setOptionParser(OptionParser op) {
		final String[] helpOptions = {
				"h",
				"help"
		};
		op.acceptsAll(Arrays.asList(helpOptions), "Display help/usage information").forHelp();
		this.optionParser = op;
	}

	public String getPrefix() {
		return prefix;
	}

	public boolean shouldProcess(String value) {
		return value.toLowerCase().startsWith(prefix);
	}

	public void submitAsync(String text, ConsoleWindow console) {
		Thread t = new Thread() {
			public void run() {
				submit(text, console);
			}
		};
		t.start();
	}
	
	public void submit(String text, ConsoleWindow console) {
		try {
			if(text.length() > prefix.length()) {
				OptionSet optionSet = optionParser.parse(text.substring(prefix.length()).split("\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?"));
				if(optionSet != null) {
					if(optionSet.has("help")) {
						printHelp(console);
					}else {
						process(optionSet, console);
					}
					return;
				}
			}else if(optionParser.recognizedOptions().size() <= 1) {
				process(null, console);
				return;
			}
		}catch (OptionException e) {
			console.getLogger().info(e.getMessage());
		}
		try {
			OptionSet optionSet = optionParser.parse("");
			process(optionSet, console);
			return;
		}catch(OptionException e) {}
		if(optionParser.recognizedOptions().size() > 1) {
			printHelp(console);
		}
	}

	public void printHelp(ConsoleWindow console) {
		try(StringWriter writer = new StringWriter()){
			optionParser.printHelpOn(writer);
			for(String s : writer.toString().split("\n")) {
				console.getLogger().info(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}

	public abstract void process(OptionSet optionSet, ConsoleWindow console);

}
