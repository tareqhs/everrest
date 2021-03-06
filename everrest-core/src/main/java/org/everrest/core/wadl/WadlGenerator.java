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
package org.everrest.core.wadl;

import org.everrest.core.method.MethodParameter;
import org.everrest.core.resource.AbstractResourceDescriptor;
import org.everrest.core.resource.ResourceMethodDescriptor;
import org.everrest.core.wadl.research.Application;
import org.everrest.core.wadl.research.Param;
import org.everrest.core.wadl.research.RepresentationType;
import org.everrest.core.wadl.research.Resources;

import javax.ws.rs.core.MediaType;

/**
 * A WadGenerator creates structure that can be reflected to WADL
 * representation.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public interface WadlGenerator {

    /** @return {@link Application} instance, it is root element in WADL */
    Application createApplication();

    /**
     * @return {@link Resources} instance. Element <i>resources</i> in WADL
     * document is container for the descriptions of resources provided
     * by application
     */
    Resources createResources();

    /**
     * @param rd
     *         See {@link AbstractResourceDescriptor}
     * @return {@link org.everrest.core.wadl.research.Resource.Resource}
     * describes application resource, each resource identified by a URI
     */
    org.everrest.core.wadl.research.Resource createResource(AbstractResourceDescriptor rd);

    /**
     * @param path
     *         resource relative path
     * @return {@link org.everrest.core.wadl.research.Resource.Resource}
     * describes application resource, each resource identified by a URI
     */
    org.everrest.core.wadl.research.Resource createResource(String path);

    /**
     * @param md
     *         See {@link ResourceMethodDescriptor}
     * @return {@link org.everrest.core.rest.wadl.research.Method} describes the
     * input to and output from an HTTP protocol method they may be
     * applied to a resource
     */
    org.everrest.core.wadl.research.Method createMethod(ResourceMethodDescriptor md);

    /**
     * @return {@link org.everrest.core.wadl.research.Request} describes the
     * input to be included when applying an HTTP method to a resource
     * @see {@link org.everrest.core.wadl.research.Method}
     */
    org.everrest.core.wadl.research.Request createRequest();

    /**
     * @return {@link org.everrest.core.wadl.research.Response} describes the
     * output that result from performing an HTTP method on a resource
     * @see {@link org.everrest.core.wadl.research.Method}
     */
    org.everrest.core.wadl.research.Response createResponse();

    /**
     * @param mediaType
     *         one of media type that resource can consume
     * @return {@link RepresentationType} describes a representation of
     * resource's state
     */
    RepresentationType createRequestRepresentation(MediaType mediaType);

    /**
     * @param mediaType
     *         one of media type that resource can produce
     * @return {@link RepresentationType} describes a representation of
     * resource's state
     */
    RepresentationType createResponseRepresentation(MediaType mediaType);

    /**
     * @param methodParameter
     *         See {@link MethodParameter}
     * @return {@link Param} describes a parameterized component of its parent
     * element resource, request, response
     * @see org.everrest.core.wadl.research.Resource
     * @see org.everrest.core.wadl.research.Request
     * @see org.everrest.core.wadl.research.Response
     */
    Param createParam(MethodParameter methodParameter);

}
