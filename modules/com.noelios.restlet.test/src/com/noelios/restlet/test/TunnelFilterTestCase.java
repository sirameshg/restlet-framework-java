/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package com.noelios.restlet.test;

import static org.restlet.service.TunnelService.REF_CUT_KEY;
import static org.restlet.service.TunnelService.REF_EXTENSIONS_KEY;
import static org.restlet.service.TunnelService.REF_ORIGINAL_KEY;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.Encoding;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Metadata;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.noelios.restlet.application.TunnelFilter;

/**
 * Tests cases for the tunnel filter.
 */
public class TunnelFilterTestCase extends TestCase {

    /**
     * 
     */
    private static final String EFFECTED = "http://example.org/adf.asdf/af.html";

    /**
     * 
     */
    private static final String START_REF_FOR_PATH_TEST = "http://www.example.com/abc/def/";

    /**
     * 
     */
    private static final String UNEFFECTED = "http://example.org/abc.def/af.ab";

    private List<Preference<CharacterSet>> accCharsets;

    private List<Preference<Encoding>> accEncodings;

    private List<Preference<Language>> accLanguages;

    private List<Preference<MediaType>> accMediaTypes;

    private String lastCreatedReference;

    private Request request;

    private Response response;

    private TunnelFilter tunnelFilter;

    void assertCharSets(CharacterSet... characterSets) {
        assertEqualSet(accCharsets, characterSets);
    }

    void assertEncodings(Encoding... encodings) {
        assertEqualSet(accEncodings, encodings);
    }

    <A extends Metadata> A assertEqualSet(List<? extends Preference<A>> actual,
            A... expected) {
        if (actual.size() != expected.length) {
            System.out.println("Is:     " + actual);
            System.out.println("Should: " + Arrays.asList(expected));
        }
        assertEquals(actual.size(), expected.length);
        boolean contained = false;
        for (Metadata exp : expected) {
            for (Preference<? extends Metadata> act : actual) {
                if (exp.equals(act.getMetadata())) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                String message = exp + " should be in, but is missing in "
                        + actual;
                fail(message);
            }
        }
        return null;
    }

    void assertLanguages(Language... languages) {
        assertEqualSet(accLanguages, languages);
    }

    void assertMediaTypes(MediaType... mediaTypes) {
        assertEqualSet(accMediaTypes, mediaTypes);
    }

    /**
     * @param expectedCut
     * @param expectedExtensions
     */
    private void check(String expectedCut, String expectedExtensions) {
        assertEquals(expectedCut, request.getResourceRef().toString());
        Map<String, Object> attributes = request.getAttributes();
        Reference reference = new Reference(this.lastCreatedReference);
        assertEquals(reference, attributes.get(REF_ORIGINAL_KEY));
        assertEquals(new Reference(expectedCut), attributes.get(REF_CUT_KEY));
        assertEquals(expectedExtensions, attributes.get(REF_EXTENSIONS_KEY));
    }

    /**
     * 
     * @param expectedSubPathCut
     *                if null, the same as subPathOrig
     * @param expectedExtension
     *                if null, then same as "" for this test
     */
    private void checkFromPath(String expectedSubPathCut,
            String expectedExtension) {
        if (expectedExtension == null)
            expectedExtension = "";
        if (expectedSubPathCut == null)
            check(this.lastCreatedReference, expectedExtension);
        else
            check(START_REF_FOR_PATH_TEST + expectedSubPathCut,
                    expectedExtension);
    }

    /**
     * @see #createGetFromPath(String)
     * @see #createRequest(Method, String)
     */
    void createGet(String reference) {
        createRequest(Method.GET, reference);
    }

    /**
     * 
     * @param subPathToCheck
     * @see #createGet(String)
     * @see #createRequest(Method, String)
     */
    private void createGetFromPath(String subPathToCheck) {
        createGet(START_REF_FOR_PATH_TEST + subPathToCheck);
    }

    /**
     * Creates a {@link Request} and put it into {@link #request}.<br>
     * To use the methods provided by the test case class use ever the provided
     * create methods to create a request.
     * 
     * @param method
     * @param reference
     * @see #createGet(String)
     * @see #createGetFromPath(String)
     */
    void createRequest(Method method, String reference) {
        this.request = new Request(method, reference);
        this.response = new Response(request);
        this.lastCreatedReference = reference;
        setPrefs();
    }

    private void extensionTunnelOff() {
        Application application = tunnelFilter.getApplication();
        application.getTunnelService().setExtensionTunnel(false);
    }

    /**
     * Call this method to filter the current request
     */
    private void filter() {
        tunnelFilter.beforeHandle(request, response);
        setPrefs();
    }

    private void setPrefs() {
        this.accMediaTypes = request.getClientInfo().getAcceptedMediaTypes();
        this.accLanguages = request.getClientInfo().getAcceptedLanguages();
        this.accCharsets = request.getClientInfo().getAcceptedCharacterSets();
        this.accEncodings = request.getClientInfo().getAcceptedEncodings();
    }

    @Override
    public void setUp() {
        this.tunnelFilter = new TunnelFilter(new Application(new Context()));
        this.tunnelFilter.getApplication().getTunnelService()
                .setExtensionTunnel(true);
    }

    public void testExtMappingOff1() {
        extensionTunnelOff();
        createGet(UNEFFECTED);
        accLanguages.add(new Preference<Language>(Language.valueOf("ajh")));
        accMediaTypes.add(new Preference<MediaType>(
                MediaType.APPLICATION_STUFFIT));
        filter();
        assertEquals(UNEFFECTED, request.getResourceRef().toString());
        assertLanguages(Language.valueOf("ajh"));
        assertMediaTypes(MediaType.APPLICATION_STUFFIT);
        assertCharSets();
        assertEncodings();
    }

    public void testExtMappingOff2() {
        extensionTunnelOff();
        createGet(EFFECTED);
        accLanguages.add(new Preference<Language>(Language.valueOf("ajh")));
        accMediaTypes.add(new Preference<MediaType>(
                MediaType.APPLICATION_STUFFIT));
        filter();
        assertEquals(EFFECTED, request.getResourceRef().toString());
        assertLanguages(Language.valueOf("ajh"));
        assertMediaTypes(MediaType.APPLICATION_STUFFIT);
        assertCharSets();
        assertEncodings();
    }

    public void testExtMappingOn() {
        createGet(UNEFFECTED);
        filter();
        check(UNEFFECTED, "");
        assertLanguages();
        assertCharSets();
        assertCharSets();
        assertMediaTypes();

        createGet(EFFECTED);
        filter();
        check("http://example.org/adf.asdf/af", ".html");
        assertMediaTypes(MediaType.TEXT_HTML);
        assertLanguages();
        assertCharSets();
        assertCharSets();

        createGetFromPath("afhhh");
        filter();
        checkFromPath(null, null);
        assertEqualSet(accMediaTypes);
        assertLanguages();
        assertEncodings();
        assertCharSets();

        createGetFromPath("hksf.afsdf");
        filter();
        checkFromPath(null, null);
        assertMediaTypes();
        assertLanguages();
        assertEncodings();
        assertCharSets();

        createGetFromPath("hksf.afsdf.html");
        filter();
        checkFromPath("hksf.afsdf", ".html");
        assertMediaTypes(MediaType.TEXT_HTML);
        assertLanguages();
        assertEncodings();
        assertCharSets();

        createGetFromPath("hksf.afsdf.html.txt");
        filter();
        checkFromPath("hksf.afsdf", ".html.txt");
        assertMediaTypes(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN);
        assertLanguages();
        assertEncodings();
        assertCharSets();

        createGetFromPath("hksf.html.afsdf.txt");
        filter();
        checkFromPath("hksf.afsdf", ".html.txt");
        assertMediaTypes(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN);
        assertLanguages();
        assertEncodings();
        assertCharSets();

        createGetFromPath("hksf.html.afsdf.txt.en.fr");
        filter();
        checkFromPath("hksf.afsdf", ".html.txt.en.fr");
        assertMediaTypes(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN);
        assertLanguages(Language.ENGLISH, Language.FRENCH);
        assertEncodings();
        assertCharSets();

        createGet(START_REF_FOR_PATH_TEST);
        filter();
        checkFromPath(null, null);
        assertMediaTypes();
        assertLanguages();
        assertEncodings();
        assertCharSets();
    }
}
