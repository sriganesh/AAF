/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.cssa.rserv;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.cadi.Access;
import com.att.cadi.CadiException;
import com.att.cadi.CadiWrap;
import com.att.cadi.Connector;
import com.att.cadi.Lur;
import com.att.cadi.TrustChecker;
import com.att.cadi.filter.CadiHTTPManip;
import com.att.cadi.taf.TafResp;
import com.att.cadi.taf.TafResp.RESP;
import com.att.inno.env.Env;
import com.att.inno.env.TimeTaken;
import com.att.inno.env.TransStore;

/**
 * Create a new Transaction Object for each and every incoming Transaction
 * 
 * Attach to Request.  User "FilterHolder" mechanism to retain single instance.
 * 
 * TransFilter includes CADIFilter as part of the package, so that it can
 * set User Data, etc, as necessary.
 * 
 *
 */
public abstract class TransFilter<TRANS extends TransStore> implements Filter {
	public static final String TRANS_TAG = "__TRANS__";
	
	private CadiHTTPManip cadi;
	
	public TransFilter(Access access, Connector con, TrustChecker tc, Object ... additionalTafLurs) throws CadiException {
		cadi = new CadiHTTPManip(access, con, tc, additionalTafLurs);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}
	
	protected Lur getLur() {
		return cadi.getLur();
	}

	protected abstract TRANS newTrans();
	protected abstract TimeTaken start(TRANS trans, ServletRequest request);
	protected abstract void authenticated(TRANS trans, Principal p);
	protected abstract void tallyHo(TRANS trans);
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		TRANS trans = newTrans();
		
		TimeTaken overall = start(trans,request);
		try {
			request.setAttribute(TRANS_TAG, trans);
			
			HttpServletRequest req = (HttpServletRequest)request;
			HttpServletResponse res = (HttpServletResponse)response;
			
			TimeTaken security = trans.start("CADI Security", Env.SUB);
//			TimeTaken ttvalid;
			TafResp resp;
			RESP r;
			CadiWrap cw = null;
			try {
				resp = cadi.validate(req,res);
				switch(r=resp.isAuthenticated()) {
					case IS_AUTHENTICATED:
						cw = new CadiWrap(req,resp,cadi.getLur());
						authenticated(trans, cw.getUserPrincipal());
						break;
					default:
						break;
				}
			} finally {
				security.done();
			}
			
			if(r==RESP.IS_AUTHENTICATED) {
				trans.checkpoint(resp.desc());
				chain.doFilter(cw, response);
			} else {
				//TODO this is a good place to check if too many checks recently
				// Would need Cached Counter objects that are cleaned up on 
				// use
				trans.checkpoint(resp.desc(),Env.ALWAYS);
				if(resp.isFailedAttempt())
						trans.audit().log(resp.desc());
			}
		} catch(Exception e) {
			trans.error().log(e);
			trans.checkpoint("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			throw new ServletException(e);
		} finally {
			overall.done();
			tallyHo(trans);
		}
	}

	@Override
	public void destroy() {
	};
}
