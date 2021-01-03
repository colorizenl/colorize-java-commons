//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EscapeTest {

    @Test
    public void testUrlEncode() {
        assertEquals("a%3D%5B0%5D", Escape.urlEncode("a=[0]", Charsets.UTF_8));
    }
    
    @Test
    public void testUrlDecode() {
        assertEquals("john doe", Escape.urlDecode("john%20doe", Charsets.UTF_8));
    }

    @Test
    public void testEscapeCSV() {
        assertEquals("colorize", Escape.escapeCSV("colorize"));
        assertEquals("colorize test", Escape.escapeCSV("colorize test"));
        assertEquals("colorize test test", Escape.escapeCSV("colorize test,test"));
        assertEquals("'colorize'", Escape.escapeCSV("\"colorize\""));
        assertEquals("colorize  test", Escape.escapeCSV("colorize\n\ttest"));
    }
    
    @Test
    public void testBase64EncodeAndDecode() throws Exception {
        assertEquals("MTIzNC01Njc4LTkwMDAtMTQwNg==",
                Escape.base64Encode("1234-5678-9000-1406", Charsets.UTF_8));
        assertEquals("", Escape.base64Encode("", Charsets.UTF_8));
        
        assertEquals("1234-5678-9000-1406",
                Escape.base64Decode("MTIzNC01Njc4LTkwMDAtMTQwNg==", Charsets.UTF_8));    
        assertEquals("", Escape.base64Decode("", Charsets.UTF_8));
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testMD5() {
        assertEquals("81dc9bdb52d04dc20036dbd8313ed055", Escape.hashMD5("1234", Charsets.UTF_8));
    }
    
    @Test
    public void testPBKDF2() {
        assertEquals("FF9B79938467ED7B64E9136B431652FBD1DF6FFC1BCA0D95", 
                Escape.hashPBKDF2("1234", "first", Charsets.UTF_8));
        assertEquals("E55900AB0335CD849B1C6DA666011FB9B842F9AC31424806", 
                Escape.hashPBKDF2("1234", "second", Charsets.UTF_8));
        assertEquals("DDE6C435B3ADCCDEA13528B92401B052E907205672B8EAAC", 
                Escape.hashPBKDF2("test", "first", Charsets.UTF_8));
    }
    
    @Test
    public void testToHexString() {
        assertEquals("", Escape.toHexString(new byte[0]));
        assertEquals("61", Escape.toHexString(new byte[] {'a'}));
        assertEquals("61313233", Escape.toHexString(new byte[] {'a', '1', '2', '3'}));
        assertEquals("6465616462656566", 
                Escape.toHexString(new byte[] {'d', 'e', 'a', 'd', 'b', 'e', 'e', 'f'}));
    }
}
