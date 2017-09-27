package org.peekit.finalizablereferencelist.list;

import static org.junit.Assert.assertEquals;

import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.peekit.finalizablereferencelist.demo.Util;

public class FinalizableReferenceListTest {

	// each big object takes ~600mb, I needed 3 to run out of memory
	private static final int BIG_OBJECTS_COUNT_FOR_OUT_OF_MEMORY = 3;
	
	private static class State {
		boolean flag = false;
	}

	private Object obj;
	private State state;
	
	@Before
	public void setup() {
		obj = new Object();
		state = new State();
	}
	
	@Test
	public void testListAddRemove() {
		FinalizableReferenceList<Object> list = FinalizableReferenceList.weak();

		list.add(obj, () -> {});
		assertEquals(1, list.size());
		assertEquals(obj, list.get(0).get());
		
		list.remove(0);
		assertEquals(0, list.size());
		
		list.add(obj, () -> {});
		list.remove(list.get(0));
		assertEquals(0, list.size());
	}
	
	@Test
	public void testListReferenceTypes() {
		FinalizableReferenceList<Object> list;
		
		list = new FinalizableReferenceList<Object>(WeakReference<Object>::new);
		list.add(obj, () -> {});
		assertEquals(WeakReference.class, list.get(0).getClass());
		
		list = FinalizableReferenceList.weak();
		list.add(obj, () -> {});
		assertEquals(WeakReference.class, list.get(0).getClass());
		
		list = FinalizableReferenceList.soft();
		list.add(obj, () -> {});
		assertEquals(SoftReference.class, list.get(0).getClass());
		
		list = FinalizableReferenceList.phantom();
		list.add(obj, () -> {});
		assertEquals(PhantomReference.class, list.get(0).getClass());
	}
	
	@Test
	public void testWeakReferenceList() {
		FinalizableReferenceList<Object> list = FinalizableReferenceList.weak();

		list.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		System.gc();
		this.<State>assertAwaiting(() -> state.flag == true, 3000);
	}
	
	@Test
	public void testPhantomReferenceList() {
		FinalizableReferenceList<Object> list = FinalizableReferenceList.phantom();
		
		list.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		System.gc();
		this.<State>assertAwaiting(() -> state.flag == true, 3000);
	}
	
	@Test
	public void testSoftReferenceIsCollectedWithNoFreeMemory() {
		FinalizableReferenceList<Object> list = FinalizableReferenceList.soft();
		
		list.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		try {
			List<Object> bigList = new ArrayList<>();
			for(int i = 0; i < BIG_OBJECTS_COUNT_FOR_OUT_OF_MEMORY; ++i) {
				bigList.add(Util.createBigObject());
			}
		} catch(OutOfMemoryError e) {
			waitMillis(3000);
			assertEquals(true, state.flag);
		}
	}
	
	@Test
	public void testSoftReferenceNotCollectedWhenOtherStuffCanBeCollected() {
		FinalizableReferenceList<Object> list = FinalizableReferenceList.soft();

		list.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		for(int i = 0; i < BIG_OBJECTS_COUNT_FOR_OUT_OF_MEMORY; ++i) {
			Util.createBigObject();
		}
		waitMillis(3000);
		assertEquals(false, state.flag);
	}
	
	@Test
	public void testSoftReferenceNotCollectedWithFreeMemory() {
		FinalizableReferenceList<Object> list = FinalizableReferenceList.soft();
		
		list.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		System.gc();
		this.<State>assertAwaiting(() -> state.flag == false, 3000);
	}
	
	private <T> void assertAwaiting(BooleanSupplier condition, long timeout) {
		long start = System.currentTimeMillis();
		do {
			waitMillis(1);
			if(condition.getAsBoolean())
				return;
		} while(System.currentTimeMillis() - start <= timeout);
		Assert.fail("Condition is never met");
	}
	
	private synchronized void waitMillis(long ms) {
		try {
			wait(ms);
		} catch (InterruptedException e) {}
	}
	
}
