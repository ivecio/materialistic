package io.github.hidroh.materialistic;

import android.accounts.Account;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowAccountManager;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.ShadowSupportDrawerLayout;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestListActivity;

import static org.assertj.android.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowSupportPreferenceManager.class, ShadowSupportDrawerLayout.class})
@RunWith(RobolectricGradleTestRunner.class)
public class DrawerActivityLoginTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;
    private TextView drawerAccount;
    private View drawerLogout;
    private View drawerUser;

    @Before
    public void setUp() {
        Preferences.sReleaseNotesSeen = true;
        controller = Robolectric.buildActivity(TestListActivity.class)
                .create()
                .postCreate(null)
                .start()
                .resume()
                .visible();
        activity = controller.get();
        drawerAccount = (TextView) activity.findViewById(R.id.drawer_account);
        drawerLogout = activity.findViewById(R.id.drawer_logout);
        drawerUser = activity.findViewById(R.id.drawer_user);
    }

    @Test
    public void testNoExistingAccount() {
        assertThat(drawerAccount).hasText(R.string.login);
        assertThat(drawerLogout).isNotVisible();
        assertThat(drawerUser).isNotVisible();
        Preferences.setUsername(activity, "username");
        assertThat(drawerAccount).hasText("username");
        assertThat(drawerLogout).isVisible();
        assertThat(drawerUser).isVisible();
        drawerLogout.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertThat(drawerAccount).hasText(R.string.login);
        assertThat(drawerLogout).isNotVisible();
    }

    @Test
    public void testOpenUserProfile() {
        Preferences.setUsername(activity, "username");
        drawerUser.performClick();
        ((ShadowSupportDrawerLayout) ShadowExtractor.extract(activity.findViewById(R.id.drawer_layout)))
                .getDrawerListeners().get(0)
                .onDrawerClosed(activity.findViewById(R.id.drawer));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, UserActivity.class)
                .hasExtra(UserActivity.EXTRA_USERNAME, "username");
    }

    @Test
    public void testExistingAccount() {
        ShadowAccountManager.get(activity).addAccountExplicitly(new Account("existing",
                BuildConfig.APPLICATION_ID), "password", null);
        drawerAccount.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertThat(alertDialog.getListView().getAdapter()).hasCount(2); // existing + add account
        shadowOf(alertDialog).clickOnItem(0);
        assertThat(alertDialog).isNotShowing();
        assertThat(drawerAccount).hasText("existing");
        assertThat(drawerLogout).isVisible();
        drawerAccount.performClick();
        alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(alertDialog.getListView().getAdapter()).hasCount(2); // existing + add account
    }

    @Test
    public void testAddAccount() {
        ShadowAccountManager.get(activity).addAccountExplicitly(new Account("existing",
                BuildConfig.APPLICATION_ID), "password", null);
        drawerAccount.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertThat(alertDialog.getListView().getAdapter()).hasCount(2); // existing + add account
        shadowOf(alertDialog).clickOnItem(1);
        assertThat(alertDialog).isNotShowing();
        ((ShadowSupportDrawerLayout) ShadowExtractor.extract(activity.findViewById(R.id.drawer_layout)))
                .getDrawerListeners().get(0)
                .onDrawerClosed(activity.findViewById(R.id.drawer));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @Test
    public void testMoreToggle() {
        activity.findViewById(R.id.drawer_more).performClick();
        assertThat(activity.findViewById(R.id.drawer_more_container)).isVisible();
        activity.findViewById(R.id.drawer_more).performClick();
        assertThat(activity.findViewById(R.id.drawer_more_container)).isNotVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
        Preferences.sReleaseNotesSeen = null;
    }
}
