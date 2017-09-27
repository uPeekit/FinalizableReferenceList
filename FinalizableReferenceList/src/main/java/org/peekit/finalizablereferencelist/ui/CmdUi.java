package org.peekit.finalizablereferencelist.ui;

import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.peekit.finalizablereferencelist.demo.Context;
import org.peekit.finalizablereferencelist.demo.Util;
import org.peekit.finalizablereferencelist.list.FinalizableReferenceList;

public class CmdUi<T> {
	private Context<T> context;
	
	private Scanner scanner;
	
	private Map<String, Runnable> actions = new HashMap<>();
	
	public CmdUi(Context<T> context) {
		this.context = context;
		
		scanner = new Scanner(System.in);
		
		defineActions();
		runInitializer();
		runInputReader();
	}

	private void defineActions() {
		actions.put("list", () -> 
						System.out.printf("%s%n%s", context.getReferenceList().toString(), waitForInputString()) );
		actions.put("normlist", () -> {
						String contents = context.getNormalList().isEmpty() ? "list is empty"
																			: context.getNormalList().stream().map(T::toString).collect(joining("\n"));
						System.out.printf("%s%n%s", contents, waitForInputString()); });
		actions.put("clear", () -> {
						context.getNormalList().clear();
						System.out.printf("normal list is cleared%n%s", waitForInputString()); });
		actions.put("gc", () -> {
						System.gc();
						System.out.printf("gc called%n%s", waitForInputString()); });
		actions.put("add", () -> {
						Util.addMultipleReferences(context);
						System.out.printf("new references created%n%s", waitForInputString()); });
		actions.put("big", () -> {
						Util.createBigObject(context, false);
						System.out.printf("big object created%n%s", waitForInputString()); });
		actions.put("bigpersist", () -> {
						try {
							Util.createBigObject(context, true);
							System.out.printf("big object created%n%s", waitForInputString());
						} catch(OutOfMemoryError e) {
							System.out.printf("error here: %s%n%s", e.getMessage(), waitForInputString());
						} });
	}

	private void runInitializer() {
		System.out.print("[weak/soft/phantom] > ");
		String input;
		while(true) {
			if(scanner.hasNextLine()) {
				input = scanner.nextLine();
				if(input.equals("weak")) {
					context.setReferenceList(FinalizableReferenceList.weak());
					break;
				} else if(input.equals("soft")) {
					context.setReferenceList(FinalizableReferenceList.soft());
					break;
				} else if(input.equals("phantom")) {
					context.setReferenceList(FinalizableReferenceList.phantom());
					break;
				}
			}
		}
		System.out.print(waitForInputString());
	}

	private void runInputReader() {
		new Thread(() -> {
			String input;
			while(true) {
				if(scanner.hasNextLine()) {
					input = scanner.nextLine();
					if(actions.containsKey(input))
						actions.get(input).run();
				}
			}
		}).start();
	}
	
	private String waitForInputString() {
		return String.format("[%s] > ", actions.keySet().stream().collect(joining("/")));
	}
}
