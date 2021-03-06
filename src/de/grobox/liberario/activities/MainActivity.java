/*    Liberario
 *    Copyright (C) 2013 Torsten Grote
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.grobox.liberario.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.BaseDrawerItem;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.mikepenz.materialdrawer.util.KeyboardUtil;

import java.util.LinkedList;

import de.cketti.library.changelog.ChangeLog;
import de.grobox.liberario.LiberarioApplication;
import de.grobox.liberario.Preferences;
import de.grobox.liberario.R;
import de.grobox.liberario.TransportNetwork;
import de.grobox.liberario.fragments.AboutMainFragment;
import de.grobox.liberario.fragments.DeparturesFragment;
import de.grobox.liberario.fragments.DirectionsFragment;
import de.grobox.liberario.fragments.FavTripsFragment;
import de.grobox.liberario.fragments.NearbyStationsFragment;
import de.grobox.liberario.fragments.PrefsFragment;
import de.grobox.liberario.utils.LiberarioUtils;
import de.schildbach.pte.NetworkProvider;

public class MainActivity extends AppCompatActivity implements TransportNetwork.Handler {
	static final public int CHANGED_NETWORK_PROVIDER = 1;
	static final public int CHANGED_HOME = 2;

	private Drawer drawer;
	private AccountHeader accountHeader;
	private Toolbar toolbar;

	private LinkedList<Integer> selectedItemStack = new LinkedList<>();

	@SuppressLint("MissingSuperCall")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(Preferences.darkThemeEnabled(this)) {
			setTheme(R.style.AppTheme);
		} else {
			setTheme(R.style.AppTheme_Light);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialize Application Context with all Transport Networks
		((LiberarioApplication) getApplicationContext()).initilize(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		final TransportNetwork network = Preferences.getTransportNetwork(this);

		// Handle Toolbar
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Accounts aka TransportNetworks
		accountHeader = new AccountHeaderBuilder()
             .withActivity(this)
             .withHeaderBackground(R.drawable.background_default)
             .withSelectionListEnabled(false)
			 .withThreeSmallProfileImages(true)
             .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
	             @Override
	             public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
		             if(!currentProfile && profile != null && profile instanceof ProfileDrawerItem) {
			             TransportNetwork network = (TransportNetwork) ((ProfileDrawerItem) profile).getTag();
			             if(network != null) {
				             // save new network
				             Preferences.setNetworkId(getContext(), network.getId());

				             // notify everybody of this change
				             onNetworkProviderChanged(network);
			             }
		             }
		             return false;
	             }
             })
			.withOnAccountHeaderSelectionViewClickListener(new AccountHeader.OnAccountHeaderSelectionViewClickListener() {
				@Override
				public boolean onClick(View view, IProfile profile) {
					Intent intent = new Intent(getContext(), PickNetworkProviderActivity.class);
					ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(getCurrentFocus(), 0, 0, 0, 0);
					ActivityCompat.startActivityForResult(MainActivity.this, intent, CHANGED_NETWORK_PROVIDER, options.toBundle());

					return true;
				}
			})
             .build();

		// Fragments

		final Fragment directionsFragment = new DirectionsFragment();
		final PrimaryDrawerItem directionsItem = new PrimaryDrawerItem()
				                                         .withName(R.string.tab_directions)
				                                         .withIcon(LiberarioUtils.getTintedDrawable(getContext(), android.R.drawable.ic_menu_directions));

		final Fragment favTripsFragment = new FavTripsFragment();
		final PrimaryDrawerItem favTripsItem = new PrimaryDrawerItem()
				                                       .withName(R.string.tab_fav_trips)
				                                       .withIcon(LiberarioUtils.getTintedDrawable(getContext(), R.drawable.ic_action_star));

		final Fragment departuresFragment = new DeparturesFragment();
		final PrimaryDrawerItem departuresItem = new PrimaryDrawerItem()
				                                         .withName(R.string.tab_departures)
				                                         .withIcon(LiberarioUtils.getTintedDrawable(getContext(), R.drawable.ic_action_departures));

		final Fragment nearbyStationsFragment = new NearbyStationsFragment();
		final PrimaryDrawerItem nearbyStationsItem = new PrimaryDrawerItem()
				                                             .withName(R.string.nearby_stations)
				                                             .withIcon(LiberarioUtils.getTintedDrawable(getContext(), R.drawable.ic_tab_stations));

		final Fragment prefsFragment = new PrefsFragment();
		final Fragment aboutFragment = new AboutMainFragment();

		// Drawer
		drawer = new DrawerBuilder()
            .withActivity(this)
            .withToolbar(toolbar)
            .withAccountHeader(accountHeader)
            .addDrawerItems(
                   directionsItem,
                   favTripsItem,
                   departuresItem,
                   nearbyStationsItem,
                   new DividerDrawerItem(),
                   new PrimaryDrawerItem().withName(R.string.action_settings).withIcon(LiberarioUtils.getTintedDrawable(getContext(), R.drawable.ic_action_settings)),
                   new PrimaryDrawerItem().withName(R.string.action_changelog).withIcon(LiberarioUtils.getTintedDrawable(getContext(), R.drawable.ic_action_changelog)),
                   new PrimaryDrawerItem().withName(R.string.action_about).withIcon(LiberarioUtils.getTintedDrawable(getContext(), R.drawable.ic_action_about))
            )
            .withOnDrawerListener(new Drawer.OnDrawerListener() {
                  @Override
                  public void onDrawerOpened(View drawerView) {
                      KeyboardUtil.hideKeyboard(MainActivity.this);
                  }

                  @Override
                  public void onDrawerClosed(View drawerView) {}

                  @Override
                  public void onDrawerSlide(View drawerView, float slideOffset) {}
              }
            )
            .withFireOnInitialOnClick(true)
            .withSavedInstance(savedInstanceState)
            .build();

		drawer.setOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
			@Override
			public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
				if(position == -1) {
					// adjust position to first item when -1 for some reason
					position = 0;
				}

				if(drawerItem != null && drawerItem instanceof Nameable) {
					switch(((Nameable) drawerItem).getNameRes()) {
						case R.string.tab_directions:
							switchItem(directionsFragment, position, drawerItem);
							break;
						case R.string.tab_fav_trips:
							switchItem(favTripsFragment, position, drawerItem);
							break;
						case R.string.tab_departures:
							switchItem(departuresFragment, position, drawerItem);
							break;
						case R.string.nearby_stations:
							switchItem(nearbyStationsFragment, position, drawerItem);
							break;
						case R.string.action_settings:
							switchItem(prefsFragment, position, drawerItem);
							break;
						case R.string.action_changelog:
							// don't select changelog item
							drawer.setSelection(selectedItemStack.peekLast());

							new HoloChangeLog(getContext()).getFullLogDialog().show();
							break;
						case R.string.action_about:
							switchItem(aboutFragment, position, drawerItem);
							break;
					}
				}
				return false;
			}
		});

		// select first item for start
		drawer.setSelection(0, false);
		if(network != null) {
			toolbar.setSubtitle(network.getName());
			updateDrawerItems(network);
		}

		checkFirstRun(network);

		addAccounts(network);

		// show Changelog
		HoloChangeLog cl = new HoloChangeLog(this);
		if(cl.isFirstRun() && !cl.isFirstRunEver()) {
			cl.getLogDialog().show();
		}
	}

	@Override
	public void onBackPressed() {
		// close the drawer first
		if(drawer != null && drawer.isDrawerOpen()) {
			drawer.closeDrawer();
		}
		// go back to previous item
		else if(selectedItemStack.size() > 1) {
			selectedItemStack.removeLast();
			drawer.setSelection(selectedItemStack.removeLast());
		}
		// if there is no back stack, close the activity
		else {
			super.onBackPressed();
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if(requestCode == CHANGED_NETWORK_PROVIDER && resultCode == RESULT_OK) {
			onNetworkProviderChanged(Preferences.getTransportNetwork(this));
		}

		// bounce home location change back to PrefsFragment since it uses animation to call the activity
		// and can't get the result itself at the moment
		if(requestCode == CHANGED_HOME && resultCode == RESULT_OK) {
			for(Fragment fragment : getSupportFragmentManager().getFragments()) {
				if(fragment instanceof PrefsFragment) {
					fragment.onActivityResult(requestCode, resultCode, intent);
				}
			}
		}

	}

	public void onNetworkProviderChanged(TransportNetwork network) {
		// get and set new network name for app bar
		toolbar.setSubtitle(network.getName());

		// update accounts the lazy way
		accountHeader.clear();
		addAccounts(network);

		// clear back stack
		selectedItemStack.clear();

		// update drawer items based on network capabilities
		updateDrawerItems(network);

		// notify the others of change, so call this method for each fragment
		if(getSupportFragmentManager().getFragments() != null) {
			for(Fragment fragment : getSupportFragmentManager().getFragments()) {
				if(fragment instanceof TransportNetwork.Handler) {
					((TransportNetwork.Handler) fragment).onNetworkProviderChanged(network);
				}
			}
		}
	}

	private void updateDrawerItems(TransportNetwork network) {
		// disable sections not supported by new network
		for(IDrawerItem i : drawer.getDrawerItems()) {
			if(i instanceof BaseDrawerItem) {
				BaseDrawerItem item = (BaseDrawerItem) i;

				switch(item.getNameRes()) {
					case R.string.tab_directions:
					case R.string.tab_fav_trips:
						item.setEnabled(network.getNetworkProvider().hasCapabilities(NetworkProvider.Capability.TRIPS));
						break;
					case R.string.tab_departures:
						item.setEnabled(network.getNetworkProvider().hasCapabilities(NetworkProvider.Capability.DEPARTURES));
						break;
					case R.string.nearby_stations:
						item.setEnabled(network.getNetworkProvider().hasCapabilities(NetworkProvider.Capability.NEARBY_LOCATIONS));
						break;
				}
			}
		}

		if(drawer.getDrawerItems().get(drawer.getCurrentSelection()).isEnabled()) {
			// this is somehow necessary to show enabled/disabled state
			drawer.setSelection(drawer.getCurrentSelection());
		} else {
			// select last section if this one is not supported by current network
			drawer.setSelection(drawer.getDrawerItems().size() - 1);
		}
	}

	private void switchItem(Fragment f, int position, IDrawerItem drawerItem) {
		// add new position to fragment stack
		selectedItemStack.add(position);

		// set fragment name in toolbar
		toolbar.setTitle(getString(((Nameable) drawerItem).getNameRes()));

		// set network name in toolbar
		if(f instanceof TransportNetwork.Handler && !(f instanceof PrefsFragment)) {
			TransportNetwork network = Preferences.getTransportNetwork(getContext());
			if(network != null) {
				toolbar.setSubtitle(network.getName());
			}
		} else {
			toolbar.setSubtitle(null);
		}

		// switch to fragment
		getSupportFragmentManager().beginTransaction()
		                           .replace(R.id.fragment_container, f)
		                           .commit();
	}

	private void checkFirstRun(TransportNetwork network) {
		// return if no network is set
		if(network == null) {
			Intent intent = new Intent(this, PickNetworkProviderActivity.class);

			// force choosing a network provider
			intent.putExtra("FirstRun", true);

			startActivityForResult(intent, CHANGED_NETWORK_PROVIDER);
		}
		else if(toolbar != null) {
			toolbar.setSubtitle(network.getName());
		}
	}

	private void addAccounts(TransportNetwork network) {
		if(network != null) {
			//noinspection deprecation
			ProfileDrawerItem item1 = new ProfileDrawerItem()
					                          .withName(network.getName())
					                          .withEmail(network.getDescription())
					                          .withIcon(getResources().getDrawable(network.getLogo()));
			item1.setTag(network);
			accountHeader.addProfile(item1, accountHeader.getProfiles().size());
		}

		TransportNetwork network2 = Preferences.getTransportNetwork(getContext(), 2);
		if(network2 != null) {
			//noinspection deprecation
			ProfileDrawerItem item2 = new ProfileDrawerItem()
					                          .withName(network2.getName())
					                          .withEmail(network2.getDescription())
					                          .withIcon(getResources().getDrawable(network2.getLogo()));
			item2.setTag(network2);
			accountHeader.addProfile(item2, accountHeader.getProfiles().size());
		}

		TransportNetwork network3 = Preferences.getTransportNetwork(getContext(), 3);
		if(network3 != null) {
			//noinspection deprecation
			ProfileDrawerItem item3 = new ProfileDrawerItem()
					                          .withName(network3.getName())
					                          .withEmail(network3.getDescription())
					                          .withIcon(getResources().getDrawable(network3.getLogo()));
			item3.setTag(network3);
			accountHeader.addProfile(item3, accountHeader.getProfiles().size());
		}
	}

	private Context getContext() {
		return this;
	}


	public static class HoloChangeLog extends ChangeLog {
		public static final String DARK_THEME_CSS =
				"body { color: #f3f3f3; font-size: 0.9em; background-color: #282828; } h1 { font-size: 1.3em; } ul { padding-left: 2em; }";

		public static final String MATERIAL_THEME_CSS =
				"body { color: #f3f3f3; font-size: 0.9em; background-color: #424242; } h1 { font-size: 1.3em; } ul { padding-left: 2em; }";

		public HoloChangeLog(Context context) {
			super(new ContextThemeWrapper(context, R.style.DialogTheme), theme());
		}

		private static String theme() {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				return MATERIAL_THEME_CSS;
			} else {
				return DARK_THEME_CSS;
			}
		}
	}
}
