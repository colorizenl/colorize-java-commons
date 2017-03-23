//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.google.common.base.Splitter;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

/**
 * Contains utility methods for encoding strings to and decoding them from a 
 * number of different formats.
 */
public final class Escape {
	
	private static final Splitter FORM_DATA_SPLITTER = Splitter.on("&");

	private Escape() {
	}
	
	/**
	 * Encodes a string using URL encoding.
	 */
	public static String urlEncode(String str, Charset charset) {
		try {
			return URLEncoder.encode(str, charset.displayName());
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Decodes a URL encoded string.
	 */
	public static String urlDecode(String encoded, Charset charset) {
		try {
			return URLDecoder.decode(encoded, charset.displayName());
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Encodes a number of key/value pairs using URL encoding, as used in the
	 * {@code application/x-www-form-urlencoded} media type.
	 */
	public static String formEncode(Map<String, String> data, Charset charset) {
		StringBuilder buffer = new StringBuilder();
		for (Map.Entry<String,String> entry : data.entrySet()) {
			if (buffer.length() > 0) {
				buffer.append('&');
			}
			buffer.append(Escape.urlEncode(entry.getKey(), charset));
			buffer.append('=');
			buffer.append(Escape.urlEncode(entry.getValue(), charset));
		}
		return buffer.toString();
	}
	
	/**
	 * Decodes a URL encoded collection of key/value pairs, as used in the
	 * {@code application/x-www-form-urlencoded} media type.
	 */
	public static Map<String, String> formDecode(String encoded, Charset charset) {
		if (encoded.startsWith("?")) {
			encoded = encoded.substring(1);
		}
		
		if (encoded.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, String> data = new HashMap<>();
		for (String param : FORM_DATA_SPLITTER.split(encoded)) {
			String paramName = param.substring(0, param.indexOf('='));
			String paramValue = param.substring(param.indexOf('=') + 1);
			data.put(Escape.urlDecode(paramName, charset), Escape.urlDecode(paramValue, charset));
		}
		return data;
	}
	
	public static String base64Encode(String str, Charset charset) {
		// Redirects to Google Guava, but adds convenience for String -> String encoding.
		return BaseEncoding.base64Url().encode(str.getBytes(charset));
	}
	
	public static String base64Decode(String str, Charset charset) {
		// Redirects to Google Guava, but adds convenience for String -> String decoding.
		return new String(BaseEncoding.base64Url().decode(str), charset);
	}
	
	/**
	 * Computes the MD5 hash for a string.
	 * @deprecated Use Google Guava's {@code Hashing} class instead.
	 */
	@Deprecated
	public static String hashMD5(String str, Charset charset) {
		return Hashing.md5().hashString(str, charset).toString();
	}
	
	/**
	 * Computes the SHA-1 hash for a string.
	 * @deprecated Use Google Guava's {@code Hashing} class instead.
	 */
	@Deprecated
	public static String hashSHA1(String str, Charset charset) {
		return Hashing.md5().hashString(str, charset).toString();
	}
	
	/**
	 * Computes the SHA-256 hash for a string.
	 * @deprecated Use Google Guava's {@code Hashing} class instead.
	 */
	@Deprecated
	public static String hashSHA256(String str, Charset charset) {
		return Hashing.sha256().hashString(str, charset).toString();
	}
	
	/**
	 * Computes the SHA-512 hash for a string.
	 * @deprecated Use Google Guava's {@code Hashing} class instead.
	 */
	@Deprecated
	public static String hashSHA512(String str, Charset charset) {
		return Hashing.sha512().hashString(str, charset).toString();
	}
	
	/**
	 * Computes a cryptographic hash for a string, using PBKDF2 with HMAC-SHA1.
	 * The hash will be salted using the value of {@code salt}, to protect the
	 * hash against dictionary attacks and rainbow table attacks.
	 * @throws UnsupportedOperationException if the JVM does not support PBKDF2.
	 */
	public static String hashPBKDF2(String str, String salt, Charset charset) {
		// This implementation is based on the approach described in the
		// article https://crackstation.net/hashing-security.htm
		PBEKeySpec keySpec = new PBEKeySpec(str.toCharArray(), salt.getBytes(charset), 1000, 24 * 8);
		try {
			SecretKey key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(keySpec);
			byte[] hash = key.getEncoded();
			return toHexString(hash);
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException("Invalid key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new UnsupportedOperationException("PBKDF2 not supported", e);
		}
	}
	
	/**
	 * Replaces all characters that are not allowed in CSV. Commas are replaced
	 * with spaces, double quotes are replaced with single quotes, and newlines
	 * and tabs are replaced with spaces. These characters are not escaped because 
	 * escape characters in CSV are not consistent between implementations. If
	 * you need more advanced behavior, use a deticated library instead.
	 */
	public static String escapeCSV(String str) {
		StringBuilder escaped = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (!isAllowedInCSV(c)) {
				escaped.append(' ');
			} else if (shouldBeEscapedInCSV(c)) {
				escaped.append('\'');
			} else {
				escaped.append(c);
			}
		}
		return escaped.toString();
	}

	private static boolean isAllowedInCSV(char c) {
		return c != ',' && c != ';' && c != '\n' && c != '\r' && c != '\t';
	}

	private static boolean shouldBeEscapedInCSV(char c) {
		return c == '\'' || c == '"';
	}
	
	/**
	 * Returns the hexcode for the specified byte array.
	 */
	public static String toHexString(byte[] bytes) {
		// Implementation based on
		// http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
		char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xFF;
			hexChars[i * 2] = hexArray[v >>> 4];
			hexChars[i * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}
