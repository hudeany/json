package de.soderer.yaml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class YamlDocumentList extends YamlValue implements List<YamlDocument> {
	private List<YamlDocument> documents = new ArrayList<>();

	@Override
	public int size() {
		return documents.size();
	}

	@Override
	public boolean isEmpty() {
		return documents.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return documents.contains(o);
	}

	@Override
	public Iterator<YamlDocument> iterator() {
		return documents.iterator();
	}

	@Override
	public Object[] toArray() {
		return documents.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return documents.toArray(a);
	}

	@Override
	public boolean add(YamlDocument e) {
		return documents.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return documents.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return documents.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends YamlDocument> c) {
		return documents.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends YamlDocument> c) {
		return documents.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return documents.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return documents.retainAll(c);
	}

	@Override
	public void clear() {
		documents.clear();
	}

	@Override
	public YamlDocument get(int index) {
		return documents.get(index);
	}

	@Override
	public YamlDocument set(int index, YamlDocument element) {
		return documents.set(index, element);
	}

	@Override
	public void add(int index, YamlDocument element) {
		documents.add(index, element);
	}

	@Override
	public YamlDocument remove(int index) {
		return documents.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return documents.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return documents.lastIndexOf(o);
	}

	@Override
	public ListIterator<YamlDocument> listIterator() {
		return documents.listIterator();
	}

	@Override
	public ListIterator<YamlDocument> listIterator(int index) {
		return documents.listIterator(index);
	}

	@Override
	public List<YamlDocument> subList(int fromIndex, int toIndex) {
		return documents.subList(fromIndex, toIndex);
	}
}
