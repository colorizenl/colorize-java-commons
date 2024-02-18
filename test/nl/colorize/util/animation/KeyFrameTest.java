//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyFrameTest {

    private static final float EPSILON = 0.001f;

    @Test
    void sortKeyFramesByTime() {
        List<KeyFrame> keyFrames = new ArrayList<>();
        keyFrames.add(new KeyFrame(1f, 10f));
        keyFrames.add(new KeyFrame(2f, 20f));
        keyFrames.add(new KeyFrame(1.5f, 30f));
        Collections.sort(keyFrames);

        assertEquals(3, keyFrames.size());
        assertEquals(1f, keyFrames.get(0).time(), EPSILON);
        assertEquals(1.5f, keyFrames.get(1).time(), EPSILON);
        assertEquals(2f, keyFrames.get(2).time(), EPSILON);
    }

    @Test
    void stringForm() {
        assertEquals("1.0: 10.0", new KeyFrame(1, 10).toString());
        assertEquals("0.25: 10.1", new KeyFrame(0.25f, 10.1f).toString());
    }
}
