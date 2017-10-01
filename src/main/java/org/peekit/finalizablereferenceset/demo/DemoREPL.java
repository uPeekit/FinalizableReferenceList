package org.peekit.finalizablereferenceset.demo;

import org.peekit.finalizablereferenceset.demo.Util.MyObject;
import org.peekit.finalizablereferenceset.ui.CmdUi;

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
