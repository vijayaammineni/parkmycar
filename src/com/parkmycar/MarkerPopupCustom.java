package com.parkmycar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class MarkerPopupCustom implements InfoWindowAdapter
{
	private View popup=null;
	  private LayoutInflater inflater=null;

	  MarkerPopupCustom(LayoutInflater inflater) {
	    this.inflater=inflater;
	  }

	  @Override
	  public View getInfoWindow(Marker marker) {
	    return(null);
	  }

	  @SuppressLint("InflateParams")
	  @Override
	  public View getInfoContents(Marker marker) {
	    if (popup == null) {
	      popup=inflater.inflate(R.layout.popup, null);
	    }

	    TextView tv=(TextView)popup.findViewById(R.id.title);

	    tv.setText(marker.getTitle());
	    tv=(TextView)popup.findViewById(R.id.snippet);
	    tv.setText(marker.getSnippet());
	    
	    return(popup);
	  }

}
