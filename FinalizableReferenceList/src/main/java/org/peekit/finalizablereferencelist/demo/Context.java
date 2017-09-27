package org.peekit.finalizablereferencelist.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.peekit.finalizablereferencelist.list.FinalizableReferenceList;

public class Context<T> {

	private Supplier<T> generator;
	private List<T> normalList;
	private List<?> bigList;
	private FinalizableReferenceList<T> finalizableReferenceList;
	
	public Context(Supplier<T> generator) {
		this.generator = generator;
		
		normalList = new ArrayList<>();
		bigList = new ArrayList<>();
	}
	
	public List<T> getNormalList() {
		return normalList;
	}
	
	public FinalizableReferenceList<T> getReferenceList() {
		return finalizableReferenceList;
	}

	public List<?> getBigList() {
		return bigList;
	}
	
	public Supplier<T> getGenerator() {
		return generator;
	}
	
	public void setReferenceList(FinalizableReferenceList<T> referenceList) {
		this.finalizableReferenceList = referenceList;
	}

}
