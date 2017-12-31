package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.services.android.telemetry.MapboxTelemetry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.UriDeserializer;
import timber.log.Timber;

public class NearbyMapFragment extends android.support.v4.app.Fragment {

    private MapView mapView;
    private List<NearbyBaseMarker> baseMarkerOptions;
    private fr.free.nrw.commons.location.LatLng curLatLng;
    private View bottomSheetList;
    private View bottomSheetDetails;

    private BottomSheetBehavior bottomSheetListBehavior;
    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private LinearLayout wikipediaButton;
    private LinearLayout wikidataButton;
    private LinearLayout directionsButton;
    private LinearLayout commonsButton;
    private FloatingActionButton fabPlus;
    private FloatingActionButton fabCamera;
    private FloatingActionButton fabGallery;
    private View transparentView;
    private TextView description;
    private TextView title;
    private TextView distance;
    private ImageView icon;

    private TextView wikipediaButtonText;
    private TextView wikidataButtonText;
    private TextView commonsButtonText;
    private TextView directionsButtonText;

    private boolean isFabOpen=false;
    private Animation rotate_backward;
    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;

    private Place place;
    private Marker selected;

    public NearbyMapFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        initViews();
        setListeners();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriDeserializer())
                .create();
        if (bundle != null) {
            String gsonPlaceList = bundle.getString("PlaceList");
            String gsonLatLng = bundle.getString("CurLatLng");
            Type listType = new TypeToken<List<Place>>() {}.getType();
            List<Place> placeList = gson.fromJson(gsonPlaceList, listType);
            Type curLatLngType = new TypeToken<fr.free.nrw.commons.location.LatLng>() {}.getType();
            curLatLng = gson.fromJson(gsonLatLng, curLatLngType);
            baseMarkerOptions = NearbyController
                    .loadAttractionsFromLocationToBaseMarkerOptions(curLatLng,
                            placeList,
                            getActivity());
        }
        Mapbox.getInstance(getActivity(),
                getString(R.string.mapbox_commons_app_token));
        MapboxTelemetry.getInstance().setTelemetryEnabled(false);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (curLatLng != null) {
            setupMapView(savedInstanceState);
        }

        setHasOptionsMenu(false);

        return mapView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.getView().setFocusableInTouchMode(true);
        this.getView().requestFocus();
        this.getView().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if(bottomSheetDetailsBehavior.getState() == BottomSheetBehavior
                        .STATE_EXPANDED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return true;
                }
                else if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior
                        .STATE_COLLAPSED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    mapView.getMapAsync(MapboxMap::deselectMarkers);
                    selected=null;
                    return true;
                }
            }
            return false;
        });
    }

    private void initViews() {
        bottomSheetList = getActivity().findViewById(R.id.bottom_sheet);
        bottomSheetListBehavior = BottomSheetBehavior.from(bottomSheetList);
        bottomSheetDetails = getActivity().findViewById(R.id.bottom_sheet_details);
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        fabPlus = getActivity().findViewById(R.id.fab_plus);
        fabCamera = getActivity().findViewById(R.id.fab_camera);
        fabGallery = getActivity().findViewById(R.id.fab_galery);

        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(),R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate_backward);

        transparentView = getActivity().findViewById(R.id.transparentView);

        description = getActivity().findViewById(R.id.description);
        title = getActivity().findViewById(R.id.title);
        distance = getActivity().findViewById(R.id.category);
        icon = getActivity().findViewById(R.id.icon);

        wikidataButton = getActivity().findViewById(R.id.wikidataButton);
        wikipediaButton = getActivity().findViewById(R.id.wikipediaButton);
        directionsButton = getActivity().findViewById(R.id.directionsButton);
        commonsButton = getActivity().findViewById(R.id.commonsButton);

        wikidataButtonText = getActivity().findViewById(R.id.wikidataButtonText);
        wikipediaButtonText = getActivity().findViewById(R.id.wikipediaButtonText);
        directionsButtonText = getActivity().findViewById(R.id.directionsButtonText);
        commonsButtonText = getActivity().findViewById(R.id.commonsButtonText);

    }

    private void setListeners() {
        fabPlus.setOnClickListener(view -> animateFAB(isFabOpen));

        bottomSheetDetails.setOnClickListener(view -> {
            if(bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            else{
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        bottomSheetDetailsBehavior.setBottomSheetCallback(new BottomSheetBehavior
                .BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                prepareViewsForSheetPosition(newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= 0) {
                    transparentView.setAlpha(slideOffset);
                    if (slideOffset == 1) {
                        transparentView.setClickable(true);
                    } else if (slideOffset == 0){
                        transparentView.setClickable(false);
                    }
                }
            }
        });

        bottomSheetListBehavior.setBottomSheetCallback(new BottomSheetBehavior
                .BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        // Remove texts if it doesnt fit
        if (wikipediaButtonText.getLineCount() > 1
                || wikidataButtonText.getLineCount() > 1
                || commonsButtonText.getLineCount() > 1
                || directionsButtonText.getLineCount() > 1) {
            wikipediaButtonText.setVisibility(View.GONE);
            wikidataButtonText.setVisibility(View.GONE);
            commonsButtonText.setVisibility(View.GONE);
            directionsButtonText.setVisibility(View.GONE);
        }
    }

    private void setupMapView(Bundle savedInstanceState) {
        MapboxMapOptions options = new MapboxMapOptions()
                .styleUrl(Style.OUTDOORS)
                .logoEnabled(false)
                .attributionEnabled(false)
                .camera(new CameraPosition.Builder()
                        .target(new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude()))
                        .zoom(11)
                        .build());

        // create map
        mapView = new MapView(getActivity(), options);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            mapboxMap.addMarkers(baseMarkerOptions);

            mapboxMap.setOnInfoWindowCloseListener(marker -> {
                if (marker == selected){
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            });

            mapboxMap.setOnMarkerClickListener(marker -> {
                if (marker instanceof NearbyMarker) {
                    this.selected = marker;
                    NearbyMarker nearbyMarker = (NearbyMarker) marker;
                    Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
                    passInfoToSheet(place);
                    bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                return false;
            });

            addCurrentLocationMarker(mapboxMap);
        });

        mapView.setStyleUrl("asset://mapstyle.json");
    }

    /**
     * Adds a marker for the user's current position. Adds a
     * circle which uses the accuracy * 2, to draw a circle
     * which represents the user's position with an accuracy
     * of 95%.
     */
    private void addCurrentLocationMarker(MapboxMap mapboxMap) {
        MarkerOptions currentLocationMarker = new MarkerOptions()
                .position(new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude()));
        mapboxMap.addMarker(currentLocationMarker);

        List<LatLng> circle = createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                curLatLng.getAccuracy() * 2, 100);

        mapboxMap.addPolygon(
                new PolygonOptions()
                        .addAll(circle)
                        .strokeColor(Color.parseColor("#55000000"))
                        .fillColor(Color.parseColor("#11000000"))
        );
    }

    /**
     * Creates a series of points that create a circle on the map.
     * Takes the center latitude, center longitude of the circle,
     * the radius in meter and the number of nodes of the circle.
     *
     * @return List List of LatLng points of the circle.
     */
    private List<LatLng> createCircleArray(
            double centerLat, double centerLong, float radius, int nodes) {
        List<LatLng> circle = new ArrayList<>();
        float radiusKilometer = radius / 1000;
        double radiusLong = radiusKilometer
                / (111.320 * Math.cos(centerLat * Math.PI / 180));
        double radiusLat = radiusKilometer / 110.574;

        for (int i = 0; i < nodes; i++) {
            double theta = ((double) i / (double) nodes) * (2 * Math.PI);
            double nodeLongitude = centerLong + radiusLong * Math.cos(theta);
            double nodeLatitude = centerLat + radiusLat * Math.sin(theta);
            circle.add(new LatLng(nodeLatitude, nodeLongitude));
        }
        return circle;
    }

    public void prepareViewsForSheetPosition(int bottomSheetState) {

        switch (bottomSheetState) {
            case (BottomSheetBehavior.STATE_COLLAPSED):
                closeFabs(isFabOpen);
                if (!fabPlus.isShown()) showFAB();
                this.getView().requestFocus();
                break;
            case (BottomSheetBehavior.STATE_EXPANDED):
                this.getView().requestFocus();
                break;
            case (BottomSheetBehavior.STATE_HIDDEN):
                mapView.getMapAsync(MapboxMap::deselectMarkers);
                transparentView.setClickable(false);
                transparentView.setAlpha(0);
                closeFabs(isFabOpen);
                hideFAB();
                this.getView().requestFocus();
                break;
        }

    }

    private void hideFAB() {
        //get rid of anchors
        //Somehow this was the only way https://stackoverflow.com/questions/32732932/floatingactionbutton-visible-for-sometime-even-if-visibility-is-set-to-gone
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fabPlus
                .getLayoutParams();
        p.setAnchorId(View.NO_ID);
        fabPlus.setLayoutParams(p);
        fabPlus.hide();
        fabCamera.hide();
        fabGallery.hide();
    }

    private void showFAB() {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fabPlus.getLayoutParams();
        p.setAnchorId(getActivity().findViewById(R.id.bottom_sheet_details).getId());
        fabPlus.setLayoutParams(p);
        fabPlus.show();
    }

    private void passInfoToSheet(Place place) {
        this.place = place;
        wikipediaButton.setEnabled(place.hasWikipediaLink());
        wikipediaButton.setOnClickListener(view -> openWebView(place.siteLinks.getWikipediaLink()));

        wikidataButton.setEnabled(place.hasWikidataLink());
        wikidataButton.setOnClickListener(view -> openWebView(place.siteLinks.getWikidataLink()));

        directionsButton.setOnClickListener(view -> {
            //Open map app at given position
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, place.location.getGmmIntentUri());
            if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            }
        });

        commonsButton.setEnabled(place.hasCommonsLink());
        commonsButton.setOnClickListener(view -> openWebView(place.siteLinks.getCommonsLink()));

        icon.setImageResource(place.getLabel().getIcon());
        title.setText(place.name);
        distance.setText(place.distance);
        description.setText(place.getLongDescription());
        title.setText(place.name.toString());
        distance.setText(place.distance.toString());

        fabCamera.setOnClickListener(view -> {
            //TODO: Change this to activate camera upload (see ContributionsListFragment). Insert shared preference.
            Timber.d("Image title: " + place.getName() + "Image desc: " + place.getLongDescription());

            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("Title", place.getName());
            editor.putString("Desc", place.getLongDescription());
            editor.apply();

            //TODO: Shift this into title/desc screen after upload initiated
            sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            String imageTitle = sharedPref.getString("Title", "");
            String imageDesc = sharedPref.getString("Desc", "");

            Timber.d("After shared prefs, image title: " + imageTitle + " Image desc: " + imageDesc);

            openWebView(place.siteLinks.getWikidataLink());
        });

        fabGallery.setOnClickListener(view -> {
            //TODO: Change this to activate gallery upload (see ContributionsListFragment). Insert shared preference.
            Timber.d("Image title: " + place.getName() + "Image desc: " + place.getLongDescription());
            openWebView(place.siteLinks.getWikidataLink());
        });
    }

    private void openWebView(Uri link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, link);
        startActivity(browserIntent);
    }

    private void animateFAB(boolean isFabOpen) {

        if (isFabOpen) {

            fabPlus.startAnimation(rotate_backward);
            fabCamera.startAnimation(fab_close);
            fabGallery.startAnimation(fab_close);
            fabCamera.hide();
            fabGallery.hide();

        } else {

            fabPlus.startAnimation(rotate_forward);
            fabCamera.startAnimation(fab_open);
            fabGallery.startAnimation(fab_open);
            fabCamera.show();
            fabGallery.show();

        }

        this.isFabOpen=!isFabOpen;
    }

    private void closeFabs(boolean isFabOpen){
        if (isFabOpen) {
            fabPlus.startAnimation(rotate_backward);
            fabCamera.startAnimation(fab_close);
            fabGallery.startAnimation(fab_close);
            fabCamera.hide();
            fabGallery.hide();
            this.isFabOpen=!isFabOpen;
        }
    }

    @Override
    public void onStart() {
        if (mapView != null) {
            mapView.onStart();
        }
        super.onStart();
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (mapView != null) {
            mapView.onResume();
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroyView();
    }
}
