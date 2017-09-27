package org.peekit.finalizablereferencelist.list;

import static java.util.stream.Collectors.joining;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class FinalizableReferenceList<T> {
	private String header = "";
	
	private BiFunction<T, ReferenceQueue<T>, Reference<T>> refGenerator;
	
	private List<Reference<T>> list = new ArrayList<>();
	private Map<Reference<? extends T>, Runnable> callbacks = new HashMap<>();
	private ReferenceQueue<T> queue = new ReferenceQueue<>();
	
	public static <T> FinalizableReferenceList<T> weak() {
		return new FinalizableReferenceList<T>(WeakReference<T>::new);
	}
	
	public static <T> FinalizableReferenceList<T> soft() {
		return new FinalizableReferenceList<T>(SoftReference<T>::new);
	}
	
	public static <T> FinalizableReferenceList<T> phantom() {
		return new FinalizableReferenceList<T>(PhantomReference<T>::new);
	}
	
	// generator defines reference type
	public FinalizableReferenceList(BiFunction<T, ReferenceQueue<T>, Reference<T>> refGenerator) {
		this.refGenerator = refGenerator;
		runCycle();
	}
	
	private void runCycle() {
		new Thread(() -> {
			Reference<? extends T> ref;
			while(true) {
				try {
					ref = queue.remove();
					FinalizableReferenceList.this.callbacks.get(ref).run();
					FinalizableReferenceList.this.callbacks.remove(ref);
					FinalizableReferenceList.this.list.remove(ref);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public int size() {
		return list.size();
	}

	public Reference<T> get(int index) {
		return list.get(index);
	}

	// do not pass object itself to callback if you want reference to stay weak as Runnable object will keep strong reference to it
	public boolean add(T element, Runnable callback) {
		Reference<T> reference = refGenerator.apply(element, queue);
		addHeaderIfNeccessary(reference);
		callbacks.put(reference, callback);
		return list.add(reference);
	}
	
	public void add(int index, T element, Runnable callback) {
		Reference<T> reference = refGenerator.apply(element, queue);
		addHeaderIfNeccessary(reference);
		callbacks.put(reference, callback);
		list.add(index, reference);
	}
	
	public Reference<T> set(int index, T element, Runnable callback) {
		Reference<T> reference = refGenerator.apply(element, queue);
		callbacks.put(reference, callback);
		return list.set(index, reference);
	}
	
	public boolean remove(Reference<T> ref) {
		callbacks.remove(ref);
		return list.remove(ref);
	}
	
	public Reference<T> remove(int index) {
		Reference<T> reference = list.get(index);
		callbacks.remove(reference);
		list.remove(reference);
		return reference;
	}
	
	public Stream<Reference<T>> stream() {
		return list.stream();
	}
	
	public void clear() {
		list.clear();
	}
	
	private void addHeaderIfNeccessary(Reference<T> e) {
		if(header.isEmpty()) {
			header = String.format("%s<%s>%n", e.getClass().getName(), Optional.ofNullable(e.get())
																			   .map(T::getClass)
																			   .map(Class::getName)
																			   .orElse(Object.class.getName()));
		}
	}
	
	ReferenceQueue<T> queue() {
		return queue;
	}
	
	@Override
	public String toString() {
		String contents = list.isEmpty() ? "list is empty" 
										 : list.stream().map(ref -> Optional.ofNullable(ref.get())
																			.map(T::toString)
																			.orElse("some object"))
														.collect(joining("\n"));
		return String.format("%s%s", header, contents);
	}
	
}