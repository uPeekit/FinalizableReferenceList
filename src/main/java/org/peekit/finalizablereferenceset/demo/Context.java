package org.peekit.finalizablereferenceset.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.peekit.finalizablereferenceset.set.FinalizableReferenceSet;

public class Context<T> {

	private Supplier<T> generator;
	private List<T> normalList;
	private List<?> bigList;
	private FinalizableReferenceSet<T> finalizableReferenceSet;
	
	public Context(Supplier<T> generator) {
		this.generator = generator;
		
		normalList = new ArrayList<>();
		bigList = new ArrayList<>();
	}
	
	public List<T> getNormalList() {
		return normalList;
	}
	
	public FinalizableReferenceSet<T> getReferenceList() {
		return finalizableReferenceSet;
	}

	public List<?> getBigList() {
		return bigList;
	}
	
	public Supplier<T> getGenerator() {
		return generator;
	}
	
	public void setReferenceList(FinalizableReferenceSet<T> referenceList) {
		this.finalizableReferenceSet = referenceList;
	}

}
