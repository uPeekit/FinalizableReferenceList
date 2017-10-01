package org.peekit.finalizablereferenceset.demo;

public class Util {

	public static class MyObject {
		private static int counter = 0;
		private String name;
		
		public MyObject() {
			this.name = "myobject" + counter++;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}

	private static final int REFERENCES_CREATE_COUNT = 3;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void createBigObject(Context context, boolean persistent) {
		Object obj = createBigObject();
		if(persistent)
			context.getBigList().add(obj);
	}
	
	public static Object createBigObject() {
		int count = 3 * 100_000_000;
		char[] ar = new char[count];
		for(int i = 0; i < count; ++i)
			ar[i] = (char)(Math.floor(Math.random() * 65000));
		return ar;
	}
	
	public static <T> void addMultipleReferences(Context<T> context) {
		T obj;
		for(int i = 0; i < REFERENCES_CREATE_COUNT; ++i) {
			obj = context.getGenerator().get();
			String objStr = obj.toString();
			context.getReferenceList().add(obj, () -> System.out.printf("%s is collected\n", objStr));
			context.getNormalList().add(obj);
		}
	}
	
}
