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

import java.util.Arrays;

import com.t07m.console.Command;
import com.t07m.console.Console;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class ExampleStopCommand extends Command{
	
	public ExampleStopCommand() {
		super("stop");
		OptionParser op = new OptionParser();
		final String[] timeOptions = {
				"t",
				"time"
		};
		op.acceptsAll(Arrays.asList(timeOptions), "Time Delay (Seconds)").withRequiredArg();
		final String[] helpOptions = {
				"h",
				"help"
		};
		op.acceptsAll(Arrays.asList(helpOptions), "Display help/usage information").forHelp();
		this.setOptionParser(op);
	}

	public void process(OptionSet optionSet, Console console) {
		if(optionSet.has("help")) {
			printHelp(console);
		}else if(optionSet.has("time")) {
			Thread t = new Thread() {
				public void run() {
					long delay = Long.parseLong((String) optionSet.valueOf("time"));
					console.getLogger().info("Stopping application in " + delay + " seconds.");
					try {
						Thread.sleep(delay*1000);
						console.close();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			t.start();
		}else {
			console.getLogger().info("Stopping application.");
			console.close();
		}		
	}

}
