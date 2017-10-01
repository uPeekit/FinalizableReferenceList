package org.peekit.finalizablereferenceset.set;

import static java.util.stream.Collectors.joining;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

public class FinalizableReferenceSet<T> {
	
	private String header = "";
	private BiFunction<T, ReferenceQueue<T>, Reference<T>> refGenerator;
	
	private Map<Reference<? extends T>, Runnable> callbacks = new HashMap<>();
	private ReferenceQueue<T> queue = new ReferenceQueue<>();
	
	private Thread cycleThread;
	private volatile boolean isActive = true;
	
	// generator defines reference type
	FinalizableReferenceSet(BiFunction<T, ReferenceQueue<T>, Reference<T>> refGenerator) {
		this.refGenerator = refGenerator;
		runCycle();
	}
	
	private void runCycle() {
		cycleThread = new Thread(() -> {
			Reference<? extends T> ref;
			while(isActive) {
				try {
					ref = queue.remove();
					callbacks.get(ref).run();
					callbacks.remove(ref);
				} catch (InterruptedException e) {
					continue;
				}
			}
		});
		cycleThread.start();
	}

	/**
	 * Should be called when set is not needed anymore to stop cycling thread and allow gc collect the set
	 * */
	public synchronized void destroy() {
		if(!isActive)
			return;
		isActive = false;
		cycleThread.interrupt();
	}
	
	public int size() {
		return callbacks.size();
	}

	/**
	 * Do not pass object itself to callback if you want reference to stay weak as Runnable object will keep strong reference to it<br>
	 * Notice that if references are phantom, result is always true
	 * @return true if element was not present in any of references
	 * */
	public synchronized boolean add(T element, Runnable callback) {
		if(element == null) {
			throw new NullPointerException();
		}
		boolean wasPresent = callbacks.keySet().stream().filter(ref -> ref.get() == element).count() > 0;

		Reference<T> reference = refGenerator.apply(element, queue);
		addHeaderIfNeccessary(reference);
		callbacks.put(reference, callback);
		
		return !wasPresent;
	}
	
	/**
	 * Notice that if references are phantom, result is always false
	 * @return true if element was present in any of references and have been removed
	 * */
	public boolean remove(T element) {
		return callbacks.keySet().removeIf(ref -> ref.get() == element);
	}
	
	public void clear() {
		callbacks.clear();
	}
	
	@SuppressWarnings("unchecked")
	public synchronized Reference<? extends T>[] getReferences() {
		Reference<? extends T>[] arr = new Reference[callbacks.size()];
		Iterator<Reference<? extends T>> iter = callbacks.keySet().iterator();
		int i = 0;
		while(iter.hasNext()) {
			arr[i++] = iter.next();
		}
		return arr;
	}
	
	/**
	 * Notice that if references are phantom, array will always be empty
	 * @return array of unique values present in the set
	 * */
	@SuppressWarnings("unchecked")
	public T[] toArray() {
		return (T[])callbacks.keySet().stream().map(Reference::get)
											   .distinct()
											   .filter(Objects::nonNull)
											   .toArray();
	}
	
	@Override
	public String toString() {
		String contents = callbacks.isEmpty() ? "set is empty" 
											  : callbacks.keySet().stream().map(ref -> Optional.ofNullable(ref.get())
													  										   .map(T::toString)
													  										   .orElse("some object"))
											  					  		   .collect(joining("\n"));
		return String.format("%s%s", header, contents);
	}

	private void addHeaderIfNeccessary(Reference<T> e) {
		if(header.isEmpty()) {
			header = String.format("%s<%s>%n", e.getClass().getName(), Optional.ofNullable(e.get())
																			   .map(T::getClass)
																			   .map(Class::getName)
																			   .orElse(Object.class.getName()));
		}
	}

	Class<?> referenceType() {
		return callbacks.keySet().toArray()[0].getClass();
	}

	/**
	 * Notice that if multiple callbacks associated with same object, they all will be removed
	 * @return iterator for unique values of set, the same that can be retrieved by toArray()
	 * */
	public Iterator<? extends T> iterator() {
		return new Iterator<T>() {
			private int i = 0;
			private T[] arr = toArray();
			private boolean increased = false;
			
			@Override
			public boolean hasNext() {
				increased = false;
				return i < (arr.length);
			}
	
			@Override
			public T next() {
				if(i >= arr.length)
					throw new NoSuchElementException();
				increased = true;
				return arr[i++];
			}
			
			@Override
			public void remove() {
				if(!increased)
					throw new IllegalStateException();
				callbacks.keySet().removeIf(ref -> ref.get() == arr[i-1]);
			}
		};
	}

	public static <T> FinalizableReferenceSet<T> weak() {
		return new FinalizableReferenceSet<T>(WeakReference<T>::new);
	}

	public static <T> FinalizableReferenceSet<T> soft() {
		return new FinalizableReferenceSet<T>(SoftReference<T>::new);
	}

	public static <T> FinalizableReferenceSet<T> phantom() {
		return new FinalizableReferenceSet<T>(PhantomReference<T>::new);
	}
	
}