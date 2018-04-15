//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * Custom JUnit test runner that ignores user interface tests when used in a
 * headless environment. In some server environments AWT/Swing/JavaFX are not
 * supported, causing user interface tests to fail. This test runner will skip
 * tests when used in such an environment.  
 */
public class HeadlessTestRunner extends BlockJUnit4ClassRunner {

    public HeadlessTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }
    
    /**
     * Ignores all tests when used in a headless environment.
     */
    @Override
    protected boolean isIgnored(FrameworkMethod child) {
        if (SwingUtils.isHeadlessEnvironment()) {
            return true;
        } else {
            return super.isIgnored(child);
        }
    }
}
