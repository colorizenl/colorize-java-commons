//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyFrameTest {

    private static final double EPSILON = 0.001;

    @Test
    void sortKeyFramesByTime() {
        List<KeyFrame> keyFrames = new ArrayList<>();
        keyFrames.add(new KeyFrame(1f, 10f));
        keyFrames.add(new KeyFrame(2f, 20f));
        keyFrames.add(new KeyFrame(1.5f, 30f));
        Collections.sort(keyFrames);

        assertEquals(3, keyFrames.size());
        assertEquals(1.0, keyFrames.get(0).time(), EPSILON);
        assertEquals(1.5, keyFrames.get(1).time(), EPSILON);
        assertEquals(2.0, keyFrames.get(2).time(), EPSILON);
    }
}
