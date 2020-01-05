//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.tool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CopyrightUpdateToolTest {

    @Test
    public void testUpdateCopyrightYear() {
        CopyrightUpdateTool tool = new CopyrightUpdateTool("2020", "2020", null);

        assertEquals("Copyright 2020 Colorize", tool.processLine("Copyright 2020 Colorize"));
        assertEquals("Copyright 2020 Colorize", tool.processLine("Copyright 2019 Colorize"));
    }

    @Test
    public void testUpdateCopyrightMultiYearLeaveStart() {
        CopyrightUpdateTool tool = new CopyrightUpdateTool("leave", "2020", null);

        assertEquals("Copyright 2019-2020 Colorize", tool.processLine("Copyright 2019-2020 Colorize"));
        assertEquals("Copyright 2008-2020 Colorize", tool.processLine("Copyright 2008-2020 Colorize"));
        assertEquals("Copyright 2008-2020 Colorize", tool.processLine("Copyright 2008-2019 Colorize"));
        assertEquals("Copyright 2007, 2020 Colorize", tool.processLine("Copyright 2007, 2020 Colorize"));
        assertEquals("Copyright 2007, 2020 Colorize", tool.processLine("Copyright 2007, 2019 Colorize"));
    }

    @Test
    public void testUpdateCopyrightMultiYearReplaceStart() {
        CopyrightUpdateTool tool = new CopyrightUpdateTool("2007", "2020", null);

        assertEquals("Copyright 2007-2020 Colorize", tool.processLine("Copyright 2010-2020 Colorize"));
        assertEquals("Copyright 2007, 2020 Colorize", tool.processLine("Copyright 2011, 2019 Colorize"));
    }

    @Test
    public void testRewriteLicenseURL() {
        CopyrightUpdateTool tool = new CopyrightUpdateTool("2007", "2020", "test");

        assertEquals("// test", tool.processLine("// Apache license"));
    }
}
