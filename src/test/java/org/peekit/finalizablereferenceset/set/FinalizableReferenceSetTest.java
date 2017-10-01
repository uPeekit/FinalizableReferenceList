package org.peekit.finalizablereferenceset.set;

import static org.junit.Assert.assertEquals;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.peekit.finalizablereferenceset.demo.Util;

public class FinalizableReferenceSetTest {

	// each big object takes ~600mb, I needed 3 to run out of memory and trigger gc
	private static final int BIG_OBJECTS_COUNT_FOR_OUT_OF_MEMORY = 3;
	
	private static class State {
		boolean flag = false;
		volatile int i = 0;
		
		public synchronized void incr() {
			++i;
		}
	}

	private Object obj;
	private State state;
	private FinalizableReferenceSet<Object> set;
	
	@Before
	public void setup() {
		obj = new Object();
		state = new State();
	}
	
	@Test
	public void testSetItselfCollected() {
		set = FinalizableReferenceSet.phantom();
		set.add(obj, () -> state.incr());
		FinalizableReferenceSet<FinalizableReferenceSet<?>> callbackForSet = FinalizableReferenceSet.phantom();
		callbackForSet.add(set, () -> state.incr());
		
		obj = null;
		callGcAndAssertAwaiting(() -> state.i == 1, 3000);

		set.destroy();
		set = null;
		callGcAndAssertAwaiting(() -> state.i == 2, 3000);
	}
	
	@Test
	public void testAddAndRemove() {
		set = FinalizableReferenceSet.weak();
		boolean opResult;
		
		opResult = set.add(obj, () -> {});
		assertEquals(1, set.size());
		assertEquals(1, set.toArray().length);
		assertEquals(obj, set.toArray()[0]);
		assertEquals(true, opResult);
		
		opResult = set.add(obj, () -> {});
		assertEquals(2, set.size());
		assertEquals(1, set.toArray().length);
		assertEquals(false, opResult);
		
		opResult = set.remove(obj);
		assertEquals(0, set.size());
		assertEquals(true, opResult);
	}
	
	@Test
	public void testMultipleCallbacks() {
		set = FinalizableReferenceSet.weak();
		Object obj2 = new Object();
		
		set.add(obj, () -> state.incr());
		set.add(obj2, () -> state.incr());
		
		obj = null;
		obj2 = null;
		callGcAndAssertAwaiting(() -> state.i == 2, 3000);
	}
	
	@Test
	public void testMultipleCallbacksForSameObject() {
		set = FinalizableReferenceSet.weak();

		set.add(obj, () -> state.incr());
		set.add(obj, () -> state.incr());
		set.add(obj, () -> state.incr());
		assertEquals(3, set.size());
		
		obj = null;
		callGcAndAssertAwaiting(() -> state.i == 3, 3000);
	}
	
	@Test
	public void testListReferenceTypes() {
		set = FinalizableReferenceSet.weak();
		set.add(obj, () -> {});
		assertEquals(WeakReference.class, set.referenceType());
		
		set = FinalizableReferenceSet.soft();
		set.add(obj, () -> {});
		assertEquals(SoftReference.class, set.referenceType());
		
		set = FinalizableReferenceSet.phantom();
		set.add(obj, () -> {});
		assertEquals(PhantomReference.class, set.referenceType());

		class TestRef extends WeakReference<Object> {
			public TestRef(Object referent, ReferenceQueue<? super Object> q) { super(referent, q); }
		}
		set = new FinalizableReferenceSet<Object>(TestRef::new);
		set.add(obj, () -> {});
		assertEquals(TestRef.class, set.referenceType());
	}
	
	@Test
	public void testWeakReferenceList() {
		set = FinalizableReferenceSet.weak();

		set.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		callGcAndAssertAwaiting(() -> state.flag == true, 3000);
	}
	
	@Test
	public void testPhantomReferenceList() {
		set = FinalizableReferenceSet.phantom();
		
		set.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		callGcAndAssertAwaiting(() -> state.flag == true, 3000);
	}
	
	@Test
	public void testSoftReferenceIsCollectedWithNoFreeMemory() {
		set = FinalizableReferenceSet.soft();
		
		set.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		try {
			List<Object> bigList = new ArrayList<>();
			for(int i = 0; i < BIG_OBJECTS_COUNT_FOR_OUT_OF_MEMORY; ++i) {
				bigList.add(Util.createBigObject());
			}
		} catch(OutOfMemoryError e) {
			assertAwaiting(() -> state.flag == true, 3000);
		}
	}
	
	@Test
	public void testSoftReferenceNotCollectedWhenOtherStuffCanBeCollected() {
		set = FinalizableReferenceSet.soft();

		set.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		for(int i = 0; i < BIG_OBJECTS_COUNT_FOR_OUT_OF_MEMORY; ++i) {
			Util.createBigObject();
		}
		assertAwaiting(() -> state.flag == false, 3000);
	}
	
	@Test
	public void testSoftReferenceNotCollectedWithFreeMemory() {
		set = FinalizableReferenceSet.soft();
		
		set.add(obj, () -> state.flag = true);
		assertEquals(false, state.flag);
		
		obj = null;
		callGcAndAssertAwaiting(() -> state.flag == false, 3000);
	}
	
	@Test
	public void testPhantomValues() {
		set = FinalizableReferenceSet.phantom();
		boolean opResult;
		
		opResult = set.add(obj, () -> {});
		assertEquals(true, opResult);
		
		opResult = set.add(obj, () -> {});
		assertEquals(true, opResult);
		
		opResult = set.remove(obj);
		assertEquals(false, opResult);
		
		assertEquals(2, set.size());
		assertEquals(0, set.toArray().length);
	}
	
	@Test
	public void testIterator() {
		set = FinalizableReferenceSet.weak();
		Object obj2 = new Object();
		
		set.add(obj, () -> {});
		set.add(obj, () -> {});
		set.add(obj2, () -> {});
		
		Iterator<? extends Object> iter = set.iterator();
		int uniqueCount = 0;
		while(iter.hasNext()) {
			++uniqueCount;
			iter.next();
			iter.remove();
		}
		assertEquals(2, uniqueCount);
		assertEquals(0, set.size());
		try {
			iter.remove();
			Assert.fail("Remove should throw exception");
		} catch(IllegalStateException e) {}
		try {
			iter.next();
			Assert.fail("Next should throw exception");
		} catch(NoSuchElementException e) {}
	}
	
	private void callGcAndAssertAwaiting(BooleanSupplier condition, long timeout) {
		System.gc();
		assertAwaiting(condition, timeout);
	}
	
	private void assertAwaiting(BooleanSupplier condition, long timeout) {
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
