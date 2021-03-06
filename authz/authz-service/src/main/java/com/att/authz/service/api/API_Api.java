/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.authz.service.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.aft.dme2.internal.jetty.http.HttpStatus;
import com.att.authz.env.AuthzTrans;
import com.att.authz.facade.AuthzFacade;
import com.att.authz.layer.Result;
import com.att.authz.service.AuthAPI;
import com.att.authz.service.Code;
import com.att.authz.service.mapper.Mapper.API;
import com.att.cadi.Symm;
import com.att.cssa.rserv.HttpMethods;

/**
 * API Apis
 *
 */
public class API_Api {
	// Hide Public Constructor
	private API_Api() {}
	
	/**
	 * Normal Init level APIs
	 * 
	 * @param authzAPI
	 * @param facade
	 * @throws Exception
	 */
	public static void init(final AuthAPI authzAPI, AuthzFacade facade) throws Exception {
		////////
		// Overall APIs
		///////
		authzAPI.route(HttpMethods.GET,"/api",API.API,new Code(facade,"Document API", true) {
			@Override
			public void handle(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp) throws Exception {
				Result<Void> r = context.getAPI(trans,resp,authzAPI);
				if(r.isOK()) {
					resp.setStatus(HttpStatus.OK_200);
				} else {
					context.error(trans,resp,r);
				}
			}
		});

		////////
		// Overall Examples
		///////
		authzAPI.route(HttpMethods.GET,"/api/example/*",API.VOID,new Code(facade,"Document API", true) {
			@Override
			public void handle(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp) throws Exception {
				String pathInfo = req.getPathInfo();
				int question = pathInfo.lastIndexOf('?');
				
				pathInfo = pathInfo.substring(13, question<0?pathInfo.length():question);// IMPORTANT, this is size of "/api/example/"
				String nameOrContextType=Symm.base64noSplit.decode(pathInfo);
				Result<Void> r = context.getAPIExample(trans,resp,nameOrContextType,
						question>=0 && "optional=true".equalsIgnoreCase(req.getPathInfo().substring(question+1))
						);
				if(r.isOK()) {
					resp.setStatus(HttpStatus.OK_200);
				} else {
					context.error(trans,resp,r);
				}
			}
		});

	}
}
