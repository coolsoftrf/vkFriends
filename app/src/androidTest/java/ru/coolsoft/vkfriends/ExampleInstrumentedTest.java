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

    //show user name and photo and friends in offline mode

    //change user - name, photo and friends should update

    //while friend list loading is in progress minimise and restore the app
    // - indicator should proceed from the last position
    // - when progress is finished the progress views should hide

    //while user photo loading is in progress minimise and restore the app
    // - when progress is finished the waiter should hide

    //select first user, rotate screen, select second friend
    // - friend list loading should pass
    // - common friends should get displayed (with at least the one currently logged in)

    //open friendlist, scroll couple pages down, mininize, restore
    // - images should correctly correspond to the users

    //on a clean installation perform login
    // - user photo should load an replace the user placeholder

    //end/right alignments (e. g. @id/counts)

    //on sub-user selection
    // - user image is updated
    // - whose field is updated
    // - after rotation both name and image stay correct
    // - after stack popping both name and image revert to previous user's ones

    //open picker
    //select sub-user
    //pick user
    //open picker
    //=user name and photo are correct

    //open picker
    //(+/-)scroll down
    //open sub-friends (go back to previous friends)
    // - the list is on top
    // - found amount indicatior is showing (not position indicator)

    //open picker
    //navigate 3 users deep
    //rotate screen
    //= backspace key must navigate users backwards in exact reverse order

    //bubble text changes dynamically on scroll - either fastscroll or list-bound
}
