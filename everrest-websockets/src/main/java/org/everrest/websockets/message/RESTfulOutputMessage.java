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
package org.everrest.websockets.message;

/**
 * RESTful output messages.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class RESTfulOutputMessage extends OutputMessage {
    private String method;
    private String path;
    private Pair[] headers;

    public RESTfulOutputMessage() {
    }

    /**
     * Get name of HTTP method specified for resource method, e.g. GET, POST, PUT, etc.
     *
     * @return name of HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set name of HTTP method specified for resource method, e.g. GET, POST, PUT, etc.
     *
     * @param method
     *         name of HTTP method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Get resource path.
     *
     * @return resource path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set resource path.
     *
     * @param path
     *         resource path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get HTTP headers.
     *
     * @return HTTP headers
     */
    public Pair[] getHeaders() {
        return headers;
    }

    /**
     * Set HTTP headers.
     *
     * @param headers
     *         HTTP headers
     */
    public void setHeaders(Pair[] headers) {
        this.headers = headers;
    }
}
