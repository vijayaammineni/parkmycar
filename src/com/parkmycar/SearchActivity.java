package com.parkmycar;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class SearchActivity extends Activity {

	private TextView txtQuery;

	@Override
	protected void onCreate(Bundle savedInstancesState) {
		super.onCreate(savedInstancesState);

		setContentView(R.layout.activity_search_results);
		txtQuery = (TextView) findViewById(R.id.txtQuery);
		handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	/** * Handling intent data */
	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			/**
			 * * Use this query to display search results like * 1. Getting the
			 * data from SQLite and showing in listview * 2. Making webrequest
			 * and displaying the data * For now we just display the query only
			 */
			txtQuery.setText("Search Query: " + query);
		}
	}
}
