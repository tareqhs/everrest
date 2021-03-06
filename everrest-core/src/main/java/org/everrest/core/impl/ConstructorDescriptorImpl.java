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
package org.everrest.core.impl;

import org.everrest.core.ApplicationContext;
import org.everrest.core.ConstructorDescriptor;
import org.everrest.core.ConstructorParameter;
import org.everrest.core.DependencySupplier;
import org.everrest.core.impl.method.ParameterHelper;
import org.everrest.core.impl.method.ParameterResolver;
import org.everrest.core.impl.method.ParameterResolverFactory;
import org.everrest.core.resource.ResourceDescriptorVisitor;
import org.everrest.core.util.Logger;
import org.everrest.core.util.Tracer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class ConstructorDescriptorImpl implements ConstructorDescriptor {
    /** Logger. */
    private static final Logger LOG = Logger.getLogger(ConstructorDescriptorImpl.class);

    /** ConstructorDescriptor comparator. */
    public static final Comparator<ConstructorDescriptor> CONSTRUCTOR_COMPARATOR = new ConstructorComparator();

    /** Compare two ConstructorDescriptor in number parameters order. */
    private static class ConstructorComparator implements Comparator<ConstructorDescriptor> {

        @Override
        public int compare(ConstructorDescriptor o1, ConstructorDescriptor o2) {
            int r = o2.getParameters().size() - o1.getParameters().size();
            if (r == 0) {
                LOG.debug("Two constructors with the same number of parameter found " + o1 + " and " + o2);
            }
            return r;
        }
    }

    /** Constructor. */
    private final Constructor<?> constructor;

    /** Collection of constructor's parameters. */
    private final List<ConstructorParameter> parameters;

    /** Resource class. */
    private final Class<?> resourceClass;

    /**
     * @param resourceClass
     *         resource class
     * @param constructor
     *         {@link Constructor}
     */
    public ConstructorDescriptorImpl(Class<?> resourceClass, Constructor<?> constructor) {
        this.resourceClass = resourceClass;
        this.constructor = constructor;

        Class<?>[] paramTypes = constructor.getParameterTypes();

        if (paramTypes.length == 0) {
            parameters = java.util.Collections.emptyList();
        } else {
            Type[] getParamTypes = constructor.getGenericParameterTypes();
            Annotation[][] annotations = constructor.getParameterAnnotations();
            List<ConstructorParameter> params = new ArrayList<ConstructorParameter>(paramTypes.length);
            boolean encodedClass = resourceClass.getAnnotation(Encoded.class) != null;

            for (int i = 0; i < paramTypes.length; i++) {
                String defaultValue = null;
                Annotation annotation = null;
                boolean encoded = false;

                // resource or provider ?
                final boolean provider = resourceClass.getAnnotation(Provider.class) != null;
                List<String> allowedAnnotation = provider
                                                 ? ParameterHelper.PROVIDER_CONSTRUCTOR_PARAMETER_ANNOTATIONS
                                                 : ParameterHelper.RESOURCE_CONSTRUCTOR_PARAMETER_ANNOTATIONS;

                for (Annotation a : annotations[i]) {
                    Class<?> ac = a.annotationType();
                    if (allowedAnnotation.contains(ac.getName())) {
                        if (annotation != null) {
                            throw new RuntimeException(
                                    "JAX-RS annotations on one of constructor parameters are equivocality. Annotations: "
                                    + annotation + " and " + a + " can't be applied to one parameter. ");
                        }
                        annotation = a;
                    } else if (!provider && ac == Encoded.class) {
                        // @Encoded has not sense for Provider. Provider may use only @Context annotation for constructor
                        // parameters
                        encoded = true;
                    } else if (!provider && ac == DefaultValue.class) {
                        // @Default has not sense for Provider. Provider may use only @Context annotation for constructor
                        // parameters
                        defaultValue = ((DefaultValue)a).value();
                    } else {
                        LOG.debug("Constructor parameter contains unknown or not valid JAX-RS annotation " + a
                                  + ". It will be ignored. ");
                    }
                }

                ConstructorParameter cp = new ConstructorParameterImpl(
                        annotation,
                        annotations[i],
                        paramTypes[i],
                        getParamTypes[i],
                        defaultValue,
                        encoded || encodedClass);

                params.add(cp);
            }

            parameters = java.util.Collections.unmodifiableList(params);
        }
    }


    @Override
    public void accept(ResourceDescriptorVisitor visitor) {
        visitor.visitConstructorInjector(this);
    }


    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }


    @Override
    public List<ConstructorParameter> getParameters() {
        return parameters;
    }


    @Override
    public Object createInstance(ApplicationContext context) {
        Object[] p = new Object[parameters.size()];
        int i = 0;
        for (ConstructorParameter cp : parameters) {
            Annotation a = cp.getAnnotation();
            if (a != null) {
                ParameterResolver<?> pr = ParameterResolverFactory.createParameterResolver(a);
                try {
                    p[i] = pr.resolve(cp, context);
                } catch (Exception e) {
                    Class<?> ac = a.annotationType();
                    if (ac == PathParam.class || ac == QueryParam.class || ac == MatrixParam.class) {
                        throw new WebApplicationException(e, Response.status(Response.Status.NOT_FOUND).build());
                    }
                    throw new WebApplicationException(e, Response.status(Response.Status.BAD_REQUEST).build());
                }
            } else {
                // If parameter not has not annotation then get constructor parameter
                // from DependencySupplier, this is out of scope JAX-RS specification.
                DependencySupplier dependencies = context.getDependencySupplier();
                if (dependencies == null) {
                    String msg = "Can't instantiate resource "
                                 + resourceClass.getName()
                                 + ". DependencySupplier not found, constructor's parameter of type "
                                 + cp.getGenericType()
                                 + " could not be injected. ";

                    LOG.error(msg);

                    if (Tracer.isTracingEnabled()) {
                        Tracer.trace(msg);
                    }

                    throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg)
                                                              .type(MediaType.TEXT_PLAIN).build());
                }

                Object tmp = dependencies.getComponent(cp);
                if (tmp == null) {
                    String msg = "Can't instantiate resource "
                                 + resourceClass.getName()
                                 + ". Constructor's parameter of type "
                                 + cp.getGenericType()
                                 + " could not be injected. ";

                    LOG.error(msg);

                    if (Tracer.isTracingEnabled()) {
                        Tracer.trace(msg);
                    }

                    throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg)
                                                              .type(MediaType.TEXT_PLAIN).build());
                }

                p[i] = tmp;
            }

            i++;
        }

        try {
            return constructor.newInstance(p);
        } catch (IllegalArgumentException argExc) {
            // should not be thrown, arguments already checked
            throw new InternalException(argExc);
        } catch (InstantiationException instExc) {
            // should not be thrown
            throw new InternalException(instExc);
        } catch (IllegalAccessException accessExc) {
            // should not be thrown
            throw new InternalException(accessExc);
        } catch (InvocationTargetException invExc) {
            // constructor may produce exceptions
            // get cause of exception that method produces
            Throwable cause = invExc.getCause();
            // if WebApplicationException than it may contain response
            if (WebApplicationException.class == cause.getClass()) {
                throw (WebApplicationException)cause;
            }

            throw new InternalException(cause);
        } catch (Exception e) {
            throw new InternalException(e);
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[ ConstructorInjectorImpl: ");
        sb.append("constructor: " + getConstructor().getName() + "; ");
        for (ConstructorParameter cp : getParameters()) {
            sb.append(cp.toString()).append(' ');
        }
        sb.append(" ]");
        return sb.toString();
    }
}
