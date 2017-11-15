package com.jim.geofencedemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class AddVenueActivityUpdated extends AppCompatActivity implements OnMapReadyCallback,
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		View.OnClickListener, GoogleMap.OnMarkerDragListener,LocationListener, GoogleMap.OnMarkerClickListener {


	private GoogleApiClient mGoogleApiClient;
	private GoogleMap					googleMap;

	private Double						latitude	= 0d, longitude = 0d;
	private FloatingActionButton addLocation;
	private LocationRequest mLocationRequest;

	private static final String TAG = AddVenueActivityUpdated.class.getSimpleName();

	private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
	private String URL = "content://com.jim.geofencedemo/locations";
	Uri locations;
	private boolean isCurrentLocationDisplayed;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_add_venue);

		locations = Uri.parse(URL);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			checkLocationPermission();
		}
		else
		{
			Log.d(TAG,"Permission granted & device is below 6.0");
			Intent targetIntent = new Intent(this, LocationsUpdateServiceModified.class);
			startService(targetIntent);
		}

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);


		addLocation = (FloatingActionButton) findViewById(R.id.fab);
		addLocation.setOnClickListener(this);

	}

	public boolean checkLocationPermission(){
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {

			// Asking user if explanation is needed
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.ACCESS_FINE_LOCATION)) {

				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

				//Prompt the user once explanation has been shown
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
						MY_PERMISSIONS_REQUEST_LOCATION);


			} else {
				// No explanation needed, we can request the permission.
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
						MY_PERMISSIONS_REQUEST_LOCATION);
			}
			return false;
		} else {
			return true;
		}
	}

	public void onMapReady(GoogleMap googleMap) {
		this.googleMap = googleMap;
		googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


		//Initialize Google Play Services
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
					== PackageManager.PERMISSION_GRANTED) {

				buildGoogleApiClient();
			}
		}
		else {
			googleMap.setMyLocationEnabled(true);
			buildGoogleApiClient();
		}

		showAllSavedLocations();


	}


	protected synchronized void buildGoogleApiClient() {

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED) {
			googleMap.setMyLocationEnabled(true);

		}

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();
		mGoogleApiClient.connect();
	}


	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG,"onResume");
		if(googleMap != null) {
			showAllSavedLocations();
		}

	}

	public void showAllSavedLocations()
	{


		Cursor c = getContentResolver().query(locations, null, null, null, LocationsProvider.LOCATION_NAME);
		Log.d(TAG,"showAllSavedLocations  locations count"+c.getCount());
		if (c != null) {
			if(c.getCount() > 0) {

				while (c.moveToNext()) {

					LocationBean lb = new LocationBean();

					lb.rowId = c.getInt(c.getColumnIndex(LocationsProvider.ID));
					lb.lat = c.getDouble(c.getColumnIndex(LocationsProvider.LATITUDE));
					lb.log = c.getDouble(c.getColumnIndex(LocationsProvider.LONGITUDE));
					lb.rad = c.getInt(c.getColumnIndex(LocationsProvider.RADIUS));
					lb.locationname = c.getString(c.getColumnIndex(LocationsProvider.LOCATION_NAME));
					lb.isEntered = c.getInt(c.getColumnIndex(LocationsProvider.ENTER_EXIT));
					placeMarker(lb);

				}
			}
		}
		c.close();
	}

	public void placeMarker(LocationBean locationBean)
	{
		LatLng latLng = new LatLng(locationBean.getLat(),locationBean.getLog());

		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position(latLng);
		markerOptions.title(locationBean.getLocationname());
		markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
		googleMap.addMarker(markerOptions);


	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {

		Log.d(TAG,"onConnected called");
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(5000);
		mLocationRequest.setFastestInterval(5000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		if (ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED) {
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
		}

	}


	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

	}

	@Override
	public void onMarkerDragStart(Marker marker) {

		if (mGoogleApiClient != null) {
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
			mGoogleApiClient.disconnect();
		}

	}

	@Override
	public void onMarkerDrag(Marker marker) {

	}

	@Override
	public void onMarkerDragEnd(Marker marker) {

		LatLng lastMarker = marker.getPosition();
		latitude = lastMarker.latitude;
		longitude = lastMarker.longitude;
		Log.d("Custom Location :" ,"Latitude :"+latitude+" Longitude : "+longitude);


		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:

						Log.v(TAG,"Save this as venue");
						Intent i = new Intent(AddVenueActivityUpdated.this, AddZoneActivity.class);
						i.putExtra(AppConstants.KEY_LATITUDE, latitude);
						i.putExtra(AppConstants.KEY_LONGITUDE, longitude);
						startActivity(i);

						if (ContextCompat.checkSelfPermission(AddVenueActivityUpdated.this,
								android.Manifest.permission.ACCESS_FINE_LOCATION)
								== PackageManager.PERMISSION_GRANTED) {
							mGoogleApiClient.connect();
						}
						break;

					case DialogInterface.BUTTON_NEGATIVE:

						Log.v(TAG, "Don't save this venue");
						if (ContextCompat.checkSelfPermission(AddVenueActivityUpdated.this,
								android.Manifest.permission.ACCESS_FINE_LOCATION)
								== PackageManager.PERMISSION_GRANTED) {
							mGoogleApiClient.connect();
						}

						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getResources().getString(R.string.wanttosavethislocation))
				.setPositiveButton(getResources().getString(R.string.yes),
						dialogClickListener)
				.setNegativeButton(getResources().getString(R.string.no),
						dialogClickListener).show();

	}

	@Override
	public boolean onMarkerClick(Marker marker) {



		Log.d("Current Location :" ,"Latitude :"+latitude+" Longitude : "+longitude+" title "+marker.getTitle());

		if(marker.getSnippet() != null && marker.getSnippet().equalsIgnoreCase("Current Location"))
		{
			Intent i = new Intent(this, AddZoneActivity.class);
			i.putExtra(AppConstants.KEY_LATITUDE, latitude);
			i.putExtra(AppConstants.KEY_LONGITUDE, longitude);
			startActivity(i);
		}
		else
		{
			marker.showInfoWindow();

		}

		return true;
	}

	@Override
	public void onClick(View view) {

		switch (view.getId())
		{
			case R.id.fab:
				Intent i = new Intent(this, AddZoneActivity.class);
				i.putExtra(AppConstants.KEY_LATITUDE, latitude);
				i.putExtra(AppConstants.KEY_LONGITUDE, longitude);
				startActivity(i);
				break;
		}

	}


	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted. Do the
					// contacts-related task you need to do.
					if (ContextCompat.checkSelfPermission(this,
							Manifest.permission.ACCESS_FINE_LOCATION)
							== PackageManager.PERMISSION_GRANTED) {

						Intent targetIntent = new Intent(this, LocationsUpdateServiceModified.class);
						startService(targetIntent);
						if (mGoogleApiClient == null) {
							buildGoogleApiClient();
						}
					}

				} else {

					// Permission denied, Disable the functionality that depends on this permission.
					Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
					finish();
				}
				return;
			}

			// other 'case' lines to check for other permissions this app might request.
			// You can add here other case statements according to your requirement.
		}
	}

	@Override
	public void onLocationChanged(Location location) {

		//Place current location marker
		googleMap.clear();
		showAllSavedLocations();
		LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
		latitude= location.getLatitude();
		longitude = location.getLongitude();


		if(!isCurrentLocationDisplayed)
		{
			animateLocation(latLng);
		}

		Marker currentLocationMarker = googleMap.addMarker(new MarkerOptions().position(
							new LatLng(latitude, longitude)).icon(
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
		currentLocationMarker.setDraggable(true);
		currentLocationMarker.setSnippet("Current Location");
		googleMap.setOnMarkerClickListener(this);
		googleMap.setOnMarkerDragListener(this);

	}

	public void animateLocation(LatLng latLng)
	{
		googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
		isCurrentLocationDisplayed = true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mGoogleApiClient != null) {
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
		}

	}

}
