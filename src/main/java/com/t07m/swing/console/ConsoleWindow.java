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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;

public class ConsoleWindow extends JFrame{

	private static final DateFormat defaultdateFormat = new SimpleDateFormat("[MM-dd-yy HH:mm:ss]");
	private static final int commandMemoryLimit = 100;
	private boolean asyncCommands = true;
	private DateFormat dateFormat = null;

	private JTextField textFieldInput;
	private JTextArea textOutput;
	private TextAreaOutputStream output;

	private Writer writer;

	private int commandMemoryIndex = -1;
	private ArrayList<Command> commands = new ArrayList<Command>();
	private ArrayList<String> commandMemory = new ArrayList<String>();

	private boolean firstLog = true;

	public ConsoleWindow(String title) {
		setTitle(title);
	}

	public void processText(String text) {
		synchronized(commands) {
			log(text, false);
			List<Command> possibles = new ArrayList<Command>();
			for(Command c : commands) {
				if(c.shouldProcess(text)) {
					possibles.add(c);
				}
			}
			Command longestPrefix = null;
			for(Command c : possibles) {
				if(longestPrefix == null)
					longestPrefix = c;
				else if (longestPrefix.getPrefix().length() < c.getPrefix().length())
					longestPrefix = c;
			}
			if(longestPrefix != null) {
				if(asyncCommands) {
					longestPrefix.submitAsync(text, this);
				}else {
					longestPrefix.submit(text, this);
				}
			}else {
				if(text.toLowerCase().contains("help") && commands.size() > 0) {
					log("Available Commands");
					Collections.sort(commands, new Comparator<Command>() {
						public int compare(Command o1, Command o2) {
							return o1.getPrefix().compareTo(o2.getPrefix());
						}
					});
					for(Command c : commands) {
						log(c.getPrefix());
					}
				}else if(text.toLowerCase().equals("clear") || text.toLowerCase().equals("cls")) {
					output.clear();
					firstLog = true;
				}else {
					log("Invalid Command");
				}
			}
		}
	}

	public void cleanup() {
		this.dispose();
	}

	public void closeRequested() {
		cleanup();
	}

	public void setDateFormat(DateFormat df) {
		dateFormat = df;
	}

	public DateFormat getDateFormat() {
		return dateFormat;
	}
	
	public boolean isAsyncCommands() {
		return asyncCommands;
	}
	
	public void setAsyncCommands(boolean async) {
		this.asyncCommands = async;
	}

	public void log(String message) {
		log(message, true);
	}

	private DateFormat getDateFormatOrDefault() {
		return dateFormat != null ? dateFormat : defaultdateFormat;
	}

	public Command getCommand(String prefix) {
		for(Command c : commands) {
			if(c.getPrefix().equalsIgnoreCase(prefix)) {
				return c;
			}
		}
		return null;
	}

	public void registerCommand(Command c) {
		synchronized(commands) {
			if(c != null && getCommand(c.getPrefix()) == null) {
				commands.add(c);
			}
		}
	}

	public void unregisterCommand(Command c) {
		synchronized(commands) {
			if(c != null) {
				commands.remove(c);
			}
		}
	}

	public void log(String message, boolean timestamp) {
		try {
			writer.write((firstLog ? "" : "\n") + (timestamp ? getDateFormatOrDefault().format(new Date())+" " : "") + message);
			if(firstLog)
				firstLog = false;
			writer.flush();
		} catch (IOException e) {
			System.out.println((timestamp ? getDateFormatOrDefault().format(new Date()) : "")  + " " + message);
		}
	}

	private void submitInput() {
		if(textFieldInput.getText().length() > 0) {
			String text = textFieldInput.getText();
			storeCommand(text);
			processText(text);
			textFieldInput.setText("");
		}
	}

	private void storeCommand(String command) {
		commandMemoryIndex = -1;
		commandMemory.add(0, command);
		if(commandMemory.size() > commandMemoryLimit) {
			commandMemory.remove(commandMemory.size()-1);
		}
	}

	public void setup() {
		try {
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {}
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent windowEvent) {
				closeRequested();
			}});
		getContentPane().setBackground(Color.BLACK);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setSize(new Dimension(800, 600));
		this.setMinimumSize(new Dimension(640, 480));
		SpringLayout springLayout = new SpringLayout();
		getContentPane().setLayout(springLayout);

		textFieldInput = new JTextField();
		textFieldInput.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		textFieldInput.setForeground(Color.WHITE);
		textFieldInput.setCaretColor(Color.WHITE);
		textFieldInput.setBackground(Color.BLACK);
		textFieldInput.setFont(new Font("Consolas", Font.PLAIN, 12));
		textFieldInput.setColumns(10);
		textFieldInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				submitInput();
			}
		});
		textFieldInput.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_UP) {
					if(commandMemory.size() > commandMemoryIndex+1) {
						String text = textFieldInput.getText();
						if(commandMemoryIndex == -1 && text.length() > 0) {
							if(!(commandMemory.size() > 0 && commandMemory.get(0).equals(text))) {
								storeCommand(text);
								commandMemoryIndex = 0;
							}
						}
						commandMemoryIndex++;
						textFieldInput.setText(commandMemory.get(commandMemoryIndex));
					}
				}else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
					if(commandMemoryIndex > 0) {
						commandMemoryIndex--;
						textFieldInput.setText(commandMemory.get(commandMemoryIndex));
					}else {
						commandMemoryIndex = -1;
						textFieldInput.setText("");
					}
				}
			}
			public void keyReleased(KeyEvent e) {
			}
		});

		JLabel labelLineMark = new JLabel(">");
		labelLineMark.setFont(new Font("Consolas", Font.PLAIN, 13));
		labelLineMark.setForeground(Color.WHITE);
		labelLineMark.setBackground(Color.BLACK);
		labelLineMark.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				textFieldInput.requestFocus();
			}
			public void focusLost(FocusEvent e) {
			}
		});
		springLayout.putConstraint(SpringLayout.NORTH, labelLineMark, -16, SpringLayout.SOUTH, getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, labelLineMark, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, labelLineMark, 0, SpringLayout.SOUTH, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, labelLineMark, 10, SpringLayout.WEST, getContentPane());
		getContentPane().add(labelLineMark);
		springLayout.putConstraint(SpringLayout.NORTH, textFieldInput, 0, SpringLayout.NORTH, labelLineMark);
		springLayout.putConstraint(SpringLayout.WEST, textFieldInput, 0, SpringLayout.EAST, labelLineMark);
		springLayout.putConstraint(SpringLayout.SOUTH, textFieldInput, 0, SpringLayout.SOUTH, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, textFieldInput, 0, SpringLayout.EAST, getContentPane());
		getContentPane().add(textFieldInput);

		textOutput = new JTextArea();
		textOutput.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				textFieldInput.dispatchEvent(e);
				textFieldInput.requestFocus();
			}
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					textFieldInput.requestFocus();
					submitInput();
				}
			}			
			public void keyReleased(KeyEvent e) {}
		});
		textOutput.setEditable(false);
		textOutput.setBorder(new LineBorder(new Color(0, 0, 0), 1));
		textOutput.setForeground(Color.LIGHT_GRAY);
		textOutput.setBackground(Color.BLACK);
		textOutput.setFont(new Font("Consolas", Font.BOLD, 12));
		textOutput.setLineWrap(true);
		JScrollPane sp = new JScrollPane( //
				textOutput, //
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, //
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER //
				);
		sp.setBorder(new LineBorder(new Color(0, 0, 0), 0));
		springLayout.putConstraint(SpringLayout.NORTH, sp, 0, SpringLayout.NORTH, getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, sp, 0, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, sp, 0, SpringLayout.EAST, getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, sp, 0, SpringLayout.NORTH, textFieldInput);
		getContentPane().add(sp);

		this.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent e) {
				textFieldInput.requestFocus();
			}
			public void windowLostFocus(WindowEvent e) {
			}
		});
		output = new TextAreaOutputStream(textOutput);
		writer = new OutputStreamWriter(output);
	}
}