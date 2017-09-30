package org.peekit.finalizablereferencelist.demo;

import org.peekit.finalizablereferencelist.demo.Util.MyObject;
import org.peekit.finalizablereferencelist.ui.CmdUi;

public class DemoREPL {
	
	private static boolean gui = false;
	private static Context<MyObject> context = new Context<>(MyObject::new);
	
	public static void main(String[] args) {
		if(!gui) {
			new CmdUi<>(context);
		} else {
		}
	}

}
