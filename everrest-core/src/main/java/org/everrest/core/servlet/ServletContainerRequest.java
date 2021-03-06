/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.everrest.core.servlet;

import org.everrest.core.impl.ContainerRequest;
import org.everrest.core.impl.InputHeadersMap;
import org.everrest.core.impl.MultivaluedMapImpl;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.Enumeration;

/** @author andrew00x */
public final class ServletContainerRequest extends ContainerRequest {
    /**
     * @param req
     *         HttpServletRequest
     */
    public ServletContainerRequest(final HttpServletRequest req) {
        super(getMethod(req), getRequestUri(req), getBaseUri(req), getEntityStream(req), getHeader(req),
              new SecurityContext() {
                  @Override
                  public Principal getUserPrincipal() {
                      return req.getUserPrincipal();
                  }

                  @Override
                  public boolean isUserInRole(String role) {
                      return req.isUserInRole(role);
                  }

                  @Override
                  public boolean isSecure() {
                      return req.isSecure();
                  }

                  @Override
                  public String getAuthenticationScheme() {
                      return req.getAuthType();
                  }
              }
             );
    }

    /**
     * Extract HTTP method name from servlet request.
     *
     * @param servletRequest
     *         {@link HttpServletRequest}
     * @return HTTP method name
     * @see HttpServletRequest#getMethod()
     */
    private static String getMethod(HttpServletRequest servletRequest) {
        return servletRequest.getMethod();
    }

    /**
     * Constructs full request URI from {@link HttpServletRequest}, URI includes
     * query string and fragment.
     *
     * @param servletRequest
     *         {@link HttpServletRequest}
     * @return newly created URI
     */
    private static URI getRequestUri(HttpServletRequest servletRequest) {
        StringBuilder uri = new StringBuilder();
        String scheme = servletRequest.getScheme();
        uri.append(scheme);
        uri.append("://");
        uri.append(servletRequest.getServerName());
        int port = servletRequest.getServerPort();
        if (!(port == 80 || (port == 443 && "https".equals(scheme)))) {
            uri.append(':');
            uri.append(port);
        }
        uri.append(servletRequest.getRequestURI());
        String queryString = servletRequest.getQueryString();
        if (queryString != null) {
            uri.append('?');
            uri.append(queryString);
        }
        //System.out.println("REQ URI :  " + uri);
        return URI.create(uri.toString());
    }

    /**
     * Constructs base request URI from {@link HttpServletRequest} .
     *
     * @param servletRequest
     *         {@link HttpServletRequest}
     * @return newly created URI
     */
    private static URI getBaseUri(HttpServletRequest servletRequest) {
        StringBuilder uri = new StringBuilder();
        String scheme = servletRequest.getScheme();
        uri.append(scheme);
        uri.append("://");
        uri.append(servletRequest.getServerName());
        int port = servletRequest.getServerPort();
        if (!(port == 80 || (port == 443 && "https".equals(scheme)))) {
            uri.append(':');
            uri.append(port);
        }
        uri.append(servletRequest.getContextPath());
        uri.append(servletRequest.getServletPath());
        //System.out.println("BASE URI : " + uri);
        return URI.create(uri.toString());
    }

    /**
     * Get HTTP headers from {@link HttpServletRequest} .
     *
     * @param servletRequest
     *         {@link HttpServletRequest}
     * @return request headers
     */
    private static MultivaluedMap<String, String> getHeader(HttpServletRequest servletRequest) {
        MultivaluedMap<String, String> h = new MultivaluedMapImpl();
        Enumeration<String> headerNames = servletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            Enumeration<String> e = servletRequest.getHeaders(name);
            while (e.hasMoreElements()) {
                h.add(name, e.nextElement());
            }
        }
        return new InputHeadersMap(h);
    }

    /**
     * Get input stream from {@link HttpServletRequest} .
     *
     * @param servletRequest
     *         {@link HttpServletRequest}
     * @return request stream or null
     */
    private static InputStream getEntityStream(HttpServletRequest servletRequest) {
        try {
            return servletRequest.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
