/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.cssa.rserv.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.att.cssa.rserv.HttpCode;
import com.att.cssa.rserv.TypedCode;
import com.att.inno.env.TransJAXB;
import com.att.inno.env.impl.EnvFactory;


/**
 * Test the functioning of the "Content" class, which holds, and routes to the right code based on Accept values
 */
public class JU_Content {
	

	@Test
	public void test() throws Exception {
		final String BOOL = "Boolean";
		final String XML = "XML";
		TransJAXB trans = EnvFactory.newTrans();
		try {
		HttpCode<TransJAXB, String> cBool = new HttpCode<TransJAXB,String>(BOOL,"Standard String") {
			@Override
			public void handle(TransJAXB trans, HttpServletRequest req, HttpServletResponse resp) {
				try {
					resp.getOutputStream().write(context.getBytes());
				} catch (IOException e) {
				}
			}
		};

		HttpCode<TransJAXB,String> cXML = new HttpCode<TransJAXB,String>(XML, "Standard String") {
			@Override
			public void handle(TransJAXB trans, HttpServletRequest req, HttpServletResponse resp) {
				try {
					resp.getOutputStream().write(context.getBytes());
				} catch (IOException e) {
				}
			}
		};

		TypedCode<TransJAXB> ct = new TypedCode<TransJAXB>()
				.add(cBool,"application/" + Boolean.class.getName()+"+xml;charset=utf8;version=1.1")
				.add(cXML,"application/xml;q=.9");
		String expected = "application/java.lang.Boolean+xml;charset=utf8;version=1.1,application/xml;q=0.9";
		assertEquals(expected,ct.toString());

		//BogusReq req = new BogusReq();
		//expected = (expected);
		//HttpServletResponse resp = new BogusResp();
		
		assertNotNull("Same Content String and Accept String",ct.prep(trans,expected));

		//expects Null (not run)
		// A Boolean xml that must have charset utf8 and match version 1.2 or greater
		expected = ("application/java.lang.Boolean+xml;charset=utf8;version=1.2");
		assertNull("Accept Minor Version greater than Content Minor Version",ct.prep(trans,expected));

		// Same with (too many) spaces
		expected = (" application/java.lang.Boolean+xml ; charset = utf8 ; version = 1.2   ");
		assertNull("Accept Minor Version greater than Content Minor Version",ct.prep(trans,expected));

		//expects Null (not run)
		expected = ("application/java.lang.Boolean+xml;charset=utf8;version=2.1");
		assertNull("Major Versions not the same",ct.prep(trans,expected));

		expected = ("application/java.lang.Boolean+xml;charset=utf8;version=1.0");
		assertNotNull("Content Minor Version is greater than Accept Minor Version",ct.prep(trans,expected));

		expected = "application/java.lang.Squid+xml;charset=utf8;version=1.0,application/xml;q=.9";
		assertNotNull("2nd one will have to do...",ct.prep(trans,expected));

		expected = "application/java.lang.Boolean+xml;charset=UTF8;version=1.0";
		assertNotNull("Minor Charset in Caps acceptable",ct.prep(trans,expected));

		// expects no run 
		expected="application/java.lang.Boolean+xml;charset=MyType;version=1.0";
		assertNull("Unknown Minor Charset",ct.prep(trans,expected));

		expected="";
		assertNotNull("Blank Acceptance",ct.prep(trans,expected));
		
		expected=null;
		assertNotNull("Null Acceptance",ct.prep(trans,expected));	

		expected = ("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		assertNotNull("Matches application/xml, and other content not known",ct.prep(trans,expected));
		
		// No SemiColon
		expected = ("i/am/bogus,application/xml");
		assertNotNull("Match second entry, with no Semis",ct.prep(trans,expected));

 		} finally {	
			StringBuilder sb = new StringBuilder();
			trans.auditTrail(0, sb);
			System.out.println(sb);
		}
	}

}
