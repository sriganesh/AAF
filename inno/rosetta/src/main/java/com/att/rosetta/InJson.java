/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.rosetta;

import java.io.IOException;
import java.io.Reader;

import com.att.inno.env.Env;
import com.att.inno.env.TimeTaken;
import com.att.rosetta.InJson.State;

public class InJson implements Parse<Reader, State> {
	public Parsed<State> parse(Reader r, Parsed<State> parsed) throws ParseException {
		// First things first, if there's a "leftover" event, process that immediately
		State state = (State)parsed.state;
		if(state.unsent > 0) {
			parsed.event = state.unsent;
			state.unsent = 0;
			return parsed;
		}
		
		int ch;
		char c;
		StringBuilder sb = parsed.sb;
		boolean inQuotes = false, escaped = false;
		boolean go = true;
		try {
			// Gather data from Reader, looking for special characters when not in Quotes
			while(go && (ch=r.read())>=0) {
				if(state.braces>=0 || ch==Parse.START_OBJ) { // ignore garbage/whitespace before content
					c=(char)ch;
					// Character is a quote.  
					if(c=='"') {
						if(inQuotes) {
							if(escaped) {  // if escaped Quote, add to data.
								sb.append(c);
								escaped = false;
							} else {
								inQuotes = false;
							}
						} else {
							parsed.isString=true;
							inQuotes = true;
						}
					} else { // Not a Quote
						if(inQuotes) {
							if(c=='\\') {
								if(escaped) {
									sb.append("\\\\");
									escaped = false;
								} else {
									escaped = true;
								}
							} else {
								sb.append(c);
							}
						} else {
							switch(c) {
								case ':':
									parsed.dataIsName();
									parsed.isString = false;
									break;
								case Parse.START_OBJ:
									if(state.braces++ == 0) {
										parsed.event = START_DOC;
										state.unsent = c;
									} else {
										parsed.event = c;
									}
									go = false;
									break;
								case Parse.END_OBJ:
									if(--state.braces == 0) {
										parsed.event = c;
										state.unsent = END_DOC;
									} else {
										parsed.event = c;
									}
									go = false;
									break;
								// These three end the data gathering, and send it along with the event that is ending the data gathering
								case Parse.NEXT:
									if(parsed.name.startsWith("__")) {
										parsed.event = Parse.ATTRIB;
										parsed.name = parsed.name.substring(2);
									} else {
										parsed.event = c;
									}
									go = false;
									break;
								case Parse.START_ARRAY:
								case Parse.END_ARRAY:
									parsed.event = c;
									go = false;
									break;
										
								// The Escape Sequence, for Quote marks within Quotes
								case '\\':
								// Ignore these, unless within quotes, at which point data-gather
								case ' ':
								case '\b':
								case '\f':
								case '\n':
								case '\r':
								case '\t':
									break;
								// Normal data... gather it
								default:
									sb.append(c);
							}
						}
					}
				}
			}
			return parsed;
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	public static class State {
		public int braces = 0;
		public char unsent = 0;
	}
	
//	@Override
	public Parsed<State> newParsed() {
		return new Parsed<State>(new State()); // no State needed
	}

//	@Override
	public TimeTaken start(Env env) {
		return env.start("Rosetta JSON In", Env.JSON);
	}
}
