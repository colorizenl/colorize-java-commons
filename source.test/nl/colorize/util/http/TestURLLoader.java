//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import nl.colorize.util.LoadUtils;
import nl.colorize.util.swing.Utils2D;
import nl.colorize.util.testutil.SimpleHttpServer;

/**
 * Unit test for the {@code URLLoader} and associated {@code HttpResponse} classes. 
 */
public class TestURLLoader {
	
	private static SimpleHttpServer server;
	private static String testURL;
	
	@BeforeClass
	public static void before() {
		server = new SimpleHttpServer();
		server.start(9090);
		testURL = "http://localhost:9090";
	}
	
	@AfterClass
	public static void after() {
		server.stop(true);
	}
	
	// Implementation note:
	// To make sure reading URLs and sending parameters actually works, some of
	// these tests send read requests to real websites. This is obviously not 
	// very robust, since the tests will break if the site changes.
	
	@Test
	public void testLoadTextResponse() throws IOException {
		String response = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8)
				.sendRequest().getBody();
		assertTrue(response.contains("<base href=\"http://www.colorize.nl/\" />"));
		assertTrue(response.contains("</html>"));
	}
	
	@Test
	public void testLoadBinaryResponse() throws IOException {
		HttpResponse response = URLLoader.get("http://www.colorize.nl/images/logo.png", 
				Charsets.UTF_8).sendRequest();
		assertNotNull(Utils2D.loadImage(response.openStream()));
	}
	
	@Test
	public void testPostRequest() throws IOException {
		URLLoader urlLoader = URLLoader.post(testURL, Charsets.UTF_8);
		urlLoader.addParam("a", "2");
		HttpResponse response = urlLoader.sendRequest();
		
		assertEquals(HttpStatus.OK, response.getStatus());
		assertTrue(response.getBody().contains("POST /"));
		assertTrue(response.getBody().contains("Content-Type: application/x-www-form-urlencoded"));
		assertTrue(response.getBody().contains("a=2"));
	}
	
	@Test(expected=IOException.class)
	public void test404() throws IOException {
		URLLoader loader = URLLoader.get("http://www.colorize.nl/nothing.jpg", Charsets.UTF_8);
		loader.sendRequest();
	}
	
	@Test
	public void testRequestMethod() throws IOException {
		URLLoader get = new URLLoader("http://www.colorize.nl", Method.GET, Charsets.UTF_8);
		assertEquals("GET", ((HttpURLConnection) get.openConnection()).getRequestMethod());
		
		URLLoader post = URLLoader.post("http://www.colorize.nl", Charsets.UTF_8);
		post.addParam("b", "c");
		assertEquals("POST", ((HttpURLConnection) post.openConnection()).getRequestMethod());
		assertEquals("http://www.colorize.nl", post.toString());
		
		URLLoader delete = new URLLoader("http://www.a.com", Method.DELETE, Charsets.UTF_8);
		delete.addParam("b", "c");
		assertEquals("http://www.a.com?b=c", delete.toString());
	}
	
	@Test
	public void testGetVariables() throws IOException {
		URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		loader.addParam("page", "contact");
		assertEquals("contact", loader.getParamValue("page"));
		String response = loader.sendRequest().getBody();
		assertTrue(response.contains("<h1>Contact"));
	}

	@Test
	public void testUrlEncodeParameters() {
		URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		loader.addParam("a", "b c");
		assertEquals("http://www.colorize.nl?a=b+c", loader.toString());
		assertEquals("b c", loader.getParamValue("a"));
	}
	
	@Test(expected=NullPointerException.class)
	public void testDisallowNullParameters() {
		URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		loader.addParam(null, null);
	}
	
	@Test
	public void testToURL() {
		URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		loader.addParam("page", "contact");
		loader.addParam("x", "y z");
		assertEquals("http://www.colorize.nl?page=contact&x=y+z", loader.toString());
		assertEquals("http://www.colorize.nl?page=contact&x=y+z", loader.getURL().toString());
	}
	
	@Test
	public void testLoadLocalFile() throws Exception {
		String html = "<html><head><title>Test</title></head></html>";
		URL tempFileURL = LoadUtils.createTempFile(html, Charsets.UTF_8).toURI().toURL();
		assertTrue(tempFileURL.toString().startsWith("file:/"));
		URLLoader loader = new URLLoader(tempFileURL, Method.GET, Charsets.UTF_8);
		assertEquals(html, loader.sendRequest().getBody());
	}
	
	@Test(expected = ProtocolException.class)
	public void testCannotSendPostToFileURL() throws Exception {
		String html = "<html><head><title>Test</title></head></html>";
		URL tempFileURL = LoadUtils.createTempFile(html, Charsets.UTF_8).toURI().toURL();
		URLLoader loader = new URLLoader(tempFileURL, Method.POST, Charsets.UTF_8);
		loader.sendRequest();
	}
	
	@Test
	public void testResponseHttpStatus() throws Exception {
		URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		assertEquals(HttpStatus.OK, loader.sendRequest().getStatus());
	}
	
	@Test
	public void testHttps() throws Exception {
		String response = URLLoader.get("https://www.bitbucket.org", Charsets.UTF_8)
				.sendRequest().getBody();
		assertTrue(response.contains("Bitbucket"));
	}
	
	@Test
	public void testContentType() throws Exception {
		assertEquals("text/html", toResponse(HttpStatus.OK, 
				"text/html", "").getContentType(null).toString());
		assertEquals("text/html; charset=utf-8", toResponse(HttpStatus.OK, 
				"text/html; charset=UTF-8", "").getContentType(null).toString());
		assertEquals("text/html", toResponse(HttpStatus.OK, 
				"text/html; charset=UTF-8", "").getContentType(null).withoutParameters().toString());
		assertNull(toResponse(HttpStatus.OK, "", "").getContentType(null));
	}
	
	@Test
	public void testCharset() throws Exception {
		assertEquals(Charsets.ISO_8859_1, toResponse(HttpStatus.OK, 
				"text/html;charset=ISO-8859-1", "").getCharset());
		assertEquals(Charsets.UTF_8, toResponse(HttpStatus.OK, 
				"text/html", "").getCharset());
		assertEquals(Charsets.UTF_8, new HttpResponse(HttpStatus.OK).getCharset());
	}
	
	@Test
	public void testHeaderNamesAreCaseInsensitive() throws Exception {
		Map<String, String> headers = ImmutableMap.of("header-name", "test");
		HttpResponse response = new HttpResponse(HttpStatus.OK, headers);
		assertEquals("test", response.getHeader("header-name"));
		assertEquals("test", response.getHeader("Header-Name"));
		assertEquals(ImmutableSet.of("header-name"), response.getHeaders().keySet());
	}
	
	@Test(expected=NullPointerException.class)
	public void testDisallowNullHeaders() {
		URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		loader.setRequestHeader(null, null);
	}
	
	@Test
	public void testDownloadResponseHeaders() throws Exception {
		URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		HttpResponse response = loader.sendRequest();
		Map<String, String> responseHeaders = response.getHeaders();
		assertEquals("HTTP/1.1 200 OK", responseHeaders.get(""));
		assertEquals("text/html; charset=UTF-8", responseHeaders.get("Content-Type"));
		assertEquals(12, response.getHeaders().size());
	}
	
	@Test
	public void testHttpToHttpsRedirect() throws Exception {
		HttpResponse response = URLLoader.get("http://www.dennisbijlsma.com/temp/test_https_redirect.php", 
				Charsets.UTF_8).sendRequest();
		assertEquals(HttpStatus.OK, response.getStatus());
		assertEquals("Registered", response.getHeader("X-Magnolia-Registration"));
		assertTrue(response.getBody().contains("Bitbucket"));
	}
	
	@Test
	public void testMalformedContentType() throws Exception {
		HttpResponse response = new HttpResponse(HttpStatus.OK, 
				ImmutableMap.of(HttpHeaders.CONTENT_TYPE, "text/html;; charset=US-ASCII"), "");
		assertNull(response.getContentType(null));
	}
	
	@Test
	public void testMalformedCharset() throws Exception {
		HttpResponse response = new HttpResponse(HttpStatus.OK, 
				ImmutableMap.of(HttpHeaders.CONTENT_TYPE, "text/html;; charset=US-ASCII"),"");
		assertEquals(Charsets.UTF_8, response.getCharset());
	}
	
	@Test
	public void testUnsupportedCharset() throws Exception {
		HttpResponse response = new HttpResponse(HttpStatus.OK, 
				ImmutableMap.of(HttpHeaders.CONTENT_TYPE, "text/html; charset=@$!"),"");
		assertEquals(Charsets.UTF_8, response.getCharset());
	}
	
	@Test
	public void testAddParams() {
		URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		loader.addParam("a", "2");
		loader.addParams(ImmutableMap.of("b", "3", "c", "4"));
		assertEquals("http://www.colorize.nl?a=2&b=3&c=4", loader.toString());
	}
	
	@Test
	public void testSameHeaderMultipleTimes() throws Exception {
		HttpResponse response = URLLoader.get("https://www.linkedin.com", Charsets.UTF_8).sendRequest();
		String cookies = response.getHeader(HttpHeaders.SET_COOKIE);
		assertTrue(cookies.split("\n").length >= 3);
	}
	
	@Test
	public void testDisableCertificateVerification() throws Exception {
		HttpResponse response = URLLoader.get("https://html5.validator.nu", Charsets.UTF_8)
				.disableCertificateVerification()
				.sendRequest();
		assertEquals(HttpStatus.OK, response.getStatus());
	}
	
	@Test
	public void testSetBasicAuthentication() throws Exception {
		URLLoader request = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		request.setBasicAuthentication("Aladdin", "open sesame");
		assertEquals("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==", 
				request.getRequestHeader(HttpHeaders.AUTHORIZATION));
	}
	
	@Test
	public void testHttpStatus() throws Exception {
		HttpStatus status = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8)
				.sendRequest().getStatus();
		assertEquals(HttpStatus.OK, status);
		assertEquals(200, status.getCode());
		assertEquals("200 OK", status.toString());
		assertFalse(status.isClientError());
		assertFalse(status.isServerError());
	}
	
	@Test
	public void testMultipleHeadersWithTheSameName() {
		URLLoader request = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		request.addRequestHeader(HttpHeaders.COOKIE, "name=value", true);
		request.addRequestHeader(HttpHeaders.COOKIE, "name2=value2", true);
		
		assertEquals(3, request.getRequestHeaders().size());
		assertEquals("Cookie", request.getRequestHeaders().get(1).getLeft());
		assertEquals("name=value", request.getRequestHeaders().get(1).getRight());
		assertEquals("Cookie", request.getRequestHeaders().get(2).getLeft());
		assertEquals("name2=value2", request.getRequestHeaders().get(2).getRight());
	}
	
	@Test
	public void testHeadRequestsOnlyDownloadHeaders() throws IOException {
		HttpResponse getResponse = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8)
				.sendRequest();
		assertEquals(HttpStatus.OK, getResponse.getStatus());
		assertFalse(getResponse.getBody().isEmpty());
		
		HttpResponse headResponse = new URLLoader("http://www.colorize.nl", Method.HEAD, Charsets.UTF_8)
				.sendRequest();
		assertEquals(HttpStatus.OK, headResponse.getStatus());
		assertTrue(headResponse.getBody().isEmpty());
	}
	
	@Test
	public void testReplaceHeadersWithTheSameName() {
		URLLoader request = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
		request.addRequestHeader(HttpHeaders.COOKIE, "name=value", false);
		request.addRequestHeader(HttpHeaders.COOKIE, "name2=value2", false);
		assertEquals("name2=value2", request.getRequestHeader(HttpHeaders.COOKIE));
	}
	
	@Test
	public void testRequestMethodsWithRequestBody() {
		List<Method> supported = new ArrayList<Method>();
		for (Method method : Method.values()) {
			if (method.hasRequestBody()) {
				supported.add(method);
			}
		}
		
		assertEquals(ImmutableList.of(Method.POST, Method.PUT), supported);
	}
	
	@Test
	public void testRequestMethodsWithHeadersOnly() {
		List<Method> supported = new ArrayList<Method>();
		for (Method method : Method.values()) {
			if (method.isResponseHeadersOnly()) {
				supported.add(method);
			}
		}
		
		assertEquals(ImmutableList.of(Method.HEAD), supported);
	}
	
	@Test
	public void testFormEncodedPostData() {
		URLLoader loader = URLLoader.post("http://www.colorize.nl", Charsets.UTF_8);
		loader.addParam("a", "2");
		loader.addParam("b", "3<4");
		
		assertEquals("application/x-www-form-urlencoded;charset=UTF-8", 
				loader.getRequestHeader(HttpHeaders.CONTENT_TYPE));
		assertEquals("a=2&b=3%3C4", loader.getRequestBody());
	}
	
	@Test
	public void testJsonPostData() {
		URLLoader loader = URLLoader.post("http://www.colorize.nl", Charsets.UTF_8);
		loader.setRequestBody("{\"a\":2,\"b\":3}", "application/json");
		
		assertEquals("application/json", loader.getRequestHeader(HttpHeaders.CONTENT_TYPE));
		assertEquals("{\"a\":2,\"b\":3}", loader.getRequestBody());
	}
	
	@Test
	public void testPreventEmptyResponseDueToPrematureClose() throws IOException {
		URLLoader urlLoader = URLLoader.get("https://www.whoscored.com/Players/97752", Charsets.UTF_8);
		urlLoader.setRequestHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel " +
				"Mac OS X 10_11_3) AppleWebKit/601.4.4 (KHTML, like Gecko) Version/9.0.3 Safari/601.4.4");
		HttpResponse response = urlLoader.sendRequest();
		assertFalse(response.getBody().isEmpty());
	}
	
	@Test
	public void testParameterAlreadyPresentInURL() {
		URLLoader urlLoader = URLLoader.get("http://www.colorize.nl?a=2", Charsets.UTF_8);
		urlLoader.addParam("b", "3");
		
		assertEquals(2, urlLoader.getParams().size());
		assertEquals(ImmutableMap.of("a", "2", "b", "3"), urlLoader.getParams());
		assertEquals("http://www.colorize.nl?a=2&b=3", urlLoader.getURL().toString());
	}
	
	private HttpResponse toResponse(HttpStatus status, String contentType, String body) {
		Map<String, String> headers = ImmutableMap.of(HttpHeaders.CONTENT_TYPE, contentType);
		return new HttpResponse(status, headers, body);
	}
}
