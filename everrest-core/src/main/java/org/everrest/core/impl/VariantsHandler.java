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

import org.everrest.core.header.QualityValue;
import org.everrest.core.impl.header.AcceptLanguage;
import org.everrest.core.impl.header.AcceptMediaType;
import org.everrest.core.impl.header.AcceptToken;
import org.everrest.core.impl.header.HeaderHelper;
import org.everrest.core.impl.header.Language;
import org.everrest.core.impl.header.Token;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public final class VariantsHandler {
    /**
     * For extract discovered parameter from variant and compare it with
     * parameter from one of accept headers, e. g. 'Accept', 'Accept-Language',
     * 'Accept-Charset', 'Accept-Encoding'.
     *
     * @param <T>
     *         accept parameter from headers
     * @param <V>
     *         parameter from variant
     */
    private static interface CompatibleChecker<T, V> {

        /**
         * Select discovered parameter from variant, e. g. media type, language,
         * character-set, encoding.
         *
         * @param variant
         *         the variant see {@link Variant}
         * @return discovered parameter from variant
         */
        V getValue(Variant variant);

        /**
         * Compare two parameters, one of it from variant other one from request
         * header.
         *
         * @param t
         *         acceptable parameter
         * @param v
         *         parameter from variant, see {@link #getValue(Variant)}
         * @return true if parameters is compatible false otherwise
         */
        boolean isCompatible(T t, V v);

    }

    /**
     * Checker for accept media type and media type from variant.
     *
     * @see CompatibleChecker
     */
    private static final CompatibleChecker<AcceptMediaType, MediaType> MEDIA_TYPE_CHECKER =
            new CompatibleChecker<AcceptMediaType, MediaType>() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public MediaType getValue(Variant variant) {
                    return variant.getMediaType();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isCompatible(AcceptMediaType accept, MediaType fromVariant) {
                    return accept.isCompatible(fromVariant);
                }

            };

    /**
     * Checker for accept language and language from variant.
     *
     * @see CompatibleChecker
     */
    private static final CompatibleChecker<AcceptLanguage, Locale> LANGUAGE_CHECKER =
            new CompatibleChecker<AcceptLanguage, Locale>() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Locale getValue(Variant variant) {
                    return variant.getLanguage();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isCompatible(AcceptLanguage accept, Locale fromVariant) {
                    return accept.isCompatible(new Language(fromVariant));
                }

            };

    /**
     * Checker for accept character-set and character-set from variant.
     *
     * @see CompatibleChecker
     */
    private static final CompatibleChecker<AcceptToken, String> CHARSET_CHECKER =
            new CompatibleChecker<AcceptToken, String>() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String getValue(Variant variant) {
                    MediaType m = variant.getMediaType();
                    return (m != null) ? m.getParameters().get("charset") : null;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isCompatible(AcceptToken accept, String fromVariant) {
                    return accept.isCompatible(new Token(fromVariant));
                }

            };

    /**
     * Checker for accept encoding and encoding from variant.
     *
     * @see CompatibleChecker
     */
    private static final CompatibleChecker<AcceptToken, String> ENCODING_CHECKER =
            new CompatibleChecker<AcceptToken, String>() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String getValue(Variant variant) {
                    return variant.getEncoding();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isCompatible(AcceptToken accept, String fromVariant) {
                    return accept.isCompatible(new Token(fromVariant));
                }

            };

    /**
     * Looking for most acceptable variant for given request.
     *
     * @param r
     *         see {@link ContainerRequest}
     * @param variants
     *         see {@link Variant} and {@link VariantListBuilderImpl}
     * @return variant or null
     */
    public static Variant handleVariants(ContainerRequest r, List<Variant> variants) {
        // create quality sorted list of acceptable media types
        List<AcceptMediaType> m =
                HeaderHelper.createAcceptedMediaTypeList(HeaderHelper.convertToString(r
                                                                                              .getRequestHeader(ContainerRequest.ACCEPT)));

        // create quality sorted list of accept encoding
        List<AcceptLanguage> l =
                HeaderHelper.createAcceptedLanguageList(HeaderHelper.convertToString(r
                                                                                             .getRequestHeader(
                                                                                                     ContainerRequest.ACCEPT_LANGUAGE)));

        // create quality sorted list of accept encoding
        List<AcceptToken> c =
                HeaderHelper.createAcceptedCharsetList(HeaderHelper.convertToString(r
                                                                                            .getRequestHeader(
                                                                                                    ContainerRequest.ACCEPT_CHARSET)));

        // create quality sorted list of accept encoding
        List<AcceptToken> e =
                HeaderHelper.createAcceptedEncodingList(HeaderHelper.convertToString(r
                                                                                             .getRequestHeader(
                                                                                                     ContainerRequest.ACCEPT_ENCODING)));

        List<Variant> myVariants = new ArrayList<Variant>(variants);

        handleVariants(m, myVariants, MEDIA_TYPE_CHECKER);
        handleVariants(l, myVariants, LANGUAGE_CHECKER);
        handleVariants(c, myVariants, CHARSET_CHECKER);
        handleVariants(e, myVariants, ENCODING_CHECKER);

        return myVariants.size() != 0 ? myVariants.get(0) : null;
    }

    /**
     * @param <T>
     *         type of acceptable parameters
     * @param <V>
     *         type of discovered parameter from variant
     * @param accept
     *         list of acceptable parameters
     * @param variants
     *         list of variants it will be modified
     * @param ch
     *         see {@link CompatibleChecker}
     */
    private static <T extends QualityValue, V> void handleVariants(List<T> accept, List<Variant> variants,
                                                                   CompatibleChecker<T, V> ch) {

        List<Variant> tmp = new ArrayList<Variant>();
        for (T a : accept) {
            for (Variant v : variants) {
                V p = ch.getValue(v);
                if (p != null) {
                    if (ch.isCompatible(a, p)) {
                        tmp.add(v);
                    }
                }
            }

        }
        // append elements without parameters, they must have lower priority
        for (Variant v : variants) {
            if (ch.getValue(v) == null) {
                tmp.add(v);
            }
        }
        // remove unacceptable
        variants.clear();
        variants.addAll(tmp);
    }

    /** Constructor. */
    private VariantsHandler() {
    }
}
