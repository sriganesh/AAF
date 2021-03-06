/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.rosetta.marshal;

import java.util.Iterator;
import java.util.List;

import com.att.rosetta.Ladder;
import com.att.rosetta.Marshal;
import com.att.rosetta.ParseException;
import com.att.rosetta.Parsed;


public abstract class FieldArray<T,S> extends Marshal<T> {
	private DataWriter<S> dataWriter;
	private String name;

	public FieldArray(String name, DataWriter<S> dw) {
		this.name = name;
		dataWriter = dw;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Parsed<State> parse(T t, Parsed<State> parsed) throws ParseException {
		Ladder<Iterator<?>> ladder = parsed.state.ladder;
		Iterator<?> iter = ladder.peek();
		if(iter==null) {
			List<S> list = data(t);
			if(list.isEmpty() && parsed.state.smallest) {
				ladder.push(DONE_ITERATOR);
			} else {
				ladder.push(new ListIterator<S>(list));
				parsed.event = START_ARRAY;
				parsed.name = name;
			}
		} else if (DONE_ITERATOR.equals(iter)) {
		} else {
			ladder.ascend(); // look at field info
				Iterator<?> memIter = ladder.peek();
				ListIterator<S> mems = (ListIterator<S>)iter;
				S mem;
				if(memIter==null) {
					mem=mems.next();
				} else if(!DONE_ITERATOR.equals(memIter)) {
					mem=mems.peek();
				} else if(iter.hasNext()) {
					mem=null;
					ladder.push(null);
				} else {
					mem=null;
				}
				
				if(mem!=null) {
					parsed.isString=dataWriter.write(mem, parsed.sb);
					parsed.event = NEXT;
				}
			ladder.descend();
			if(mem==null) {
				if(iter.hasNext()) {
					parsed.event = NEXT;
				} else {
					parsed.event = END_ARRAY;
					ladder.push(DONE_ITERATOR);
				}
			}
		}
		return parsed; // if unchanged, then it will end process
	}

	protected abstract List<S> data(T t);

}
