//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionTest {
    
    @Test
    public void testParseVersionString() { 
        Version v = Version.parse("1.6.0_31");
        assertEquals(1, v.getDigit(0));
        assertEquals(6, v.getDigit(1));
        assertEquals(0, v.getDigit(2));
        assertEquals(0, v.getDigit(3));
        assertEquals("1.6.0_31", v.toString());
        
        assertEquals("10.5.0", Version.parse("10.5.0").toString());
        assertEquals("1.6.0_10", Version.parse("1.6.0_10").toString());
        assertEquals("0.10.1", Version.parse("0.10.1").toString());
        assertEquals("0.10.20 b104", Version.parse("0.10.20 b104").toString());
        assertEquals("2014.2", Version.parse("2014.2").toString());
    }
    
    @Test
    public void testCannotParseVersionString() {
        assertThrows(IllegalArgumentException.class, () -> {
            Version.parse("nonsense");
        });
    }
    
    @Test
    public void testCompareVersions() {
        Version v1 = Version.parse("10.4.0");
        Version v2 = Version.parse("10.4.10");
        Version v3 = Version.parse("10.5.7");
        
        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v1.compareTo(v3) < 0);
        assertTrue(v2.compareTo(v3) < 0);
        assertTrue(v1.compareTo(v1) == 0);
        assertEquals(v1, Version.parse("10.4.0"));
        assertEquals(v1, Version.parse("10.4.0.0.0.0"));
    }
    
    @Test
    public void testCompareWithPrecision() {
        assertEquals(-1, Version.parse("1.0.0").compareTo(Version.parse("1.0.2"), 3));
        assertEquals(0, Version.parse("1.0.0").compareTo(Version.parse("1.0.2"), 2));
    }
    
    @Test
    public void testIsNewer() {
        assertFalse(Version.parse("1.0.0").isNewerThan(Version.parse("1.0.0")));
        assertFalse(Version.parse("1.0.0").isNewerThan(Version.parse("1.0.1")));
        assertFalse(Version.parse("1.0.0").isNewerThan(Version.parse("1.1.0")));
        assertFalse(Version.parse("1.0.0").isNewerThan(Version.parse("2.3.4")));
        assertFalse(Version.parse("1.0.1").isNewerThan(Version.parse("1.0.2")));
        assertFalse(Version.parse("1.0.1").isNewerThan(Version.parse("1.1.0")));
        
        assertTrue(Version.parse("1.1.0").isNewerThan(Version.parse("1.0.1")));        
        assertTrue(Version.parse("1.0.2").isNewerThan(Version.parse("1.0.1")));
        assertTrue(Version.parse("0.10.11").isNewerThan(Version.parse("0.10.1")));
    }
    
    @Test
    public void testVersionsWithDifferentPrecision() {
        assertEquals(Version.parse("1.0.0"), Version.parse("1.0"));
        assertEquals(Version.parse("1.0"), Version.parse("1.0.0"));
        assertFalse(Version.parse("1.0.0").isNewerThan(Version.parse("1.0")));
        assertTrue(Version.parse("1.0.1").isNewerThan(Version.parse("1.0")));
        assertFalse(Version.parse("1.0").isNewerThan(Version.parse("1.0.1")));
    }
    
    @Test
    public void testIgnoreSuffix() {
        assertTrue(Version.parse("1.0").equals(Version.parse("1.0")));
        assertTrue(Version.parse("1.0").equals(Version.parse("1.0.0")));
        assertTrue(Version.parse("1.0").equals(Version.parse("1.0 a")));
        assertTrue(Version.parse("1.0 a").equals(Version.parse("1.0 b")));
    }
    
    @Test
    public void testIsAtLeast() {
        assertTrue(Version.parse("1.0").isAtLeast(Version.parse("1.0")));
        assertTrue(Version.parse("1.0").isAtLeast(Version.parse("0.9")));
        assertFalse(Version.parse("1.0").isAtLeast(Version.parse("1.0.1")));
        assertFalse(Version.parse("1.0").isAtLeast(Version.parse("1.1")));
        assertTrue(Version.parse("10.10").isAtLeast(Version.parse("10.10.0")));
        assertTrue(Version.parse("10.10.0").isAtLeast(Version.parse("10.10")));
        assertTrue(Version.parse("9.0").isAtLeast(Version.parse("1.9")));
        assertTrue(Version.parse("10.11").isNewerThan(Version.parse("10.10.3")));
        assertFalse(Version.parse("10.11").isNewerThan(Version.parse("10.11.0")));
        assertEquals(Version.parse("10.11"), Version.parse("10.11.0"));
    }
    
    @Test
    public void testCanParse() {
        assertTrue(Version.canParse("1.0b"));
        assertFalse(Version.canParse("b1.0"));
        assertFalse(Version.canParse(null));
        assertFalse(Version.canParse(""));
    }
    
    @Test
    public void testTruncate() {
        assertEquals("1.2.3", Version.parse("1.2.3").truncate(3).toString());
        assertEquals("1.2", Version.parse("1.2.3").truncate(2).toString());
        assertEquals("1.2.3", Version.parse("1.2.3.beta").truncate(3).toString());
        assertEquals("1.2", Version.parse("1.2.3.beta").truncate(2).toString());
    }

    @Test
    public void testUnknown() {
        assertEquals("UNKNOWN", Version.UNKNOWN.toString());
        assertEquals(Version.UNKNOWN, Version.UNKNOWN);
        assertTrue(Version.parse("1.0.0").isNewerThan(Version.UNKNOWN));
        assertTrue(Version.parse("0.1.0").isNewerThan(Version.UNKNOWN));
    }

    @Test
    void isUnknown() {
        assertFalse(Version.parse("1.0.0").isUnknown());
        assertTrue(Version.UNKNOWN.isUnknown());
    }

    @Test
    void versionNumberWithLeadingZero() {
        List<Version> versions = new ArrayList<>();
        versions.add(Version.parse("3.11.0"));
        versions.add(Version.parse("3.11.09"));
        versions.add(Version.parse("3.11.1"));
        versions.add(Version.parse("3.11.10"));

        Collections.sort(versions);

        assertEquals("3.11.0", versions.get(0).toString());
        assertEquals("3.11.1", versions.get(1).toString());
        assertEquals("3.11.09", versions.get(2).toString());
        assertEquals("3.11.10", versions.get(3).toString());
    }

    @Test
    void ignoreLeadingV() {
        Version version = Version.parse("v1.2");

        assertEquals("v1.2", version.toString());
        assertEquals(Version.parse("1.2"), version);
    }

    @Test
    void parseUnknownVersion() {
        assertEquals(Version.UNKNOWN, Version.parse(""));
        assertEquals(Version.UNKNOWN, Version.parse("UNKNOWN"));
    }
}
