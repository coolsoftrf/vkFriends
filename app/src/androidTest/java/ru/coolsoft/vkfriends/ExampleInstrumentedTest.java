package ru.coolsoft.vkfriends;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("ru.coolsoft.vkfriends", appContext.getPackageName());
    }

    @Test
    public void main(){
        //assert navigation view
        //assert photo @nav
        //assert name @nav
        //assert currently selected friends stay the same
    }
    @Test
    public void mainRotate(){
        //rotate screen

        //assert @main()
    }

    @Test
    public void fragment(){
        //open {first, second} contact by {photo, name}

        //assert photo
        //assert name gen
        //assert friend list
        //assert nav
    }
    @Test
    public void fragmentRotate(){
        //open fragment, rotate screen

        //assert @fragment()
    }
    @Test
    public void rotateFragment(){
        //rotate main activity
        //open fragment

        //assert @fragment()
    }
    @Test
    public void rotateFragmentRotate(){
        //rotate main activity
        //open fragment
        //rotate

        //assert @fragment()
    }

    @Test
    public void relaunch(){
        //exit by Back button
        //relaunch by app icon

        //assert full set
    }
    @Test
    public void minimize(){
        //exit by Home button
        //relaunch by app icon

        //assert full set
    }
    @Test
    public void fragmentMinimize(){
        //open fragment
        //exit by Home button
        //relaunch by app icon

        //assert full set
    }

    //waiters at rotation

    //stage progress at rotation
}
