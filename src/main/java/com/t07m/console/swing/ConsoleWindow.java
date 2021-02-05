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
package com.t07m.console.swing;

import java.awt.Color;
import java.awt.Component;
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
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;

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

import com.t07m.console.AbstractConsole;

public class ConsoleWindow extends AbstractConsole{

	private JFrame frame;
	
	private JTextField textFieldInput;
	private JTextArea textOutput;
	private TextAreaOutputStream output;

	private Writer writer;

	private boolean firstLog = true;
	
	private int commandMemoryLimit = 100;
	protected int commandMemoryIndex = -1;
	protected ArrayList<String> commandMemory = new ArrayList<String>();

	public ConsoleWindow(String title) {
		frame = new JFrame();
		frame.setTitle(title);									
	}


	public void cleanup() {
		frame.setVisible(false);
		frame.dispose();
		commandMemory = null;
		output.close();
	}

	public void rawLog(String message) {
		try {
			writer.write((firstLog ? "" : "\n") + message);
			if(firstLog)
				firstLog = false;
			writer.flush();
		} catch (IOException e) {
			System.out.println(message);
		}
	}

	private void submitInput() {
		if(textFieldInput.getText().length() > 0) {
			String text = textFieldInput.getText();
			storeCommand(text);
			rawLog(text + "\n");
			processText(text);
			textFieldInput.setText("");
		}
	}

	public void setup() {
		try {
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {}
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent windowEvent) {
				close();
			}});
		frame.getContentPane().setBackground(Color.BLACK);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setSize(new Dimension(800, 600));
		frame.setMinimumSize(new Dimension(640, 480));
		SpringLayout springLayout = new SpringLayout();
		frame.getContentPane().setLayout(springLayout);

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
		springLayout.putConstraint(SpringLayout.NORTH, labelLineMark, -16, SpringLayout.SOUTH, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, labelLineMark, 0, SpringLayout.WEST, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, labelLineMark, 0, SpringLayout.SOUTH, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, labelLineMark, 10, SpringLayout.WEST, frame.getContentPane());
		frame.getContentPane().add(labelLineMark);
		springLayout.putConstraint(SpringLayout.NORTH, textFieldInput, 0, SpringLayout.NORTH, labelLineMark);
		springLayout.putConstraint(SpringLayout.WEST, textFieldInput, 0, SpringLayout.EAST, labelLineMark);
		springLayout.putConstraint(SpringLayout.SOUTH, textFieldInput, 0, SpringLayout.SOUTH, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, textFieldInput, 0, SpringLayout.EAST, frame.getContentPane());
		frame.getContentPane().add(textFieldInput);

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
		springLayout.putConstraint(SpringLayout.NORTH, sp, 0, SpringLayout.NORTH, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, sp, 0, SpringLayout.WEST, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, sp, 0, SpringLayout.EAST, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, sp, 0, SpringLayout.NORTH, textFieldInput);
		frame.getContentPane().add(sp);

		frame.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent e) {
				textFieldInput.requestFocus();
			}
			public void windowLostFocus(WindowEvent e) {
			}
		});
		output = new TextAreaOutputStream(textOutput);
		writer = new OutputStreamWriter(output);
		System.setOut(new PrintStream(output));
		System.setErr(new PrintStream(output));
	}

	public void clear() {
		output.clear();
		firstLog = true;
	}
	
	public void setLocationRelativeTo(Component c) {
		frame.setLocationRelativeTo(c);
	}
	
	public void setVisible(boolean visable) {
		frame.setVisible(true);
	}
	
	public void setState(int state) {
		frame.setState(state);
	}
	
	protected void storeCommand(String command) {
		commandMemoryIndex = -1;
		commandMemory.add(0, command);
		if(commandMemory.size() > commandMemoryLimit) {
			commandMemory.remove(commandMemory.size()-1);
		}
	}
	
}