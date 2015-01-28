package com.parkmycar;

import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;

public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {
	public static final String AUTHORITY = SearchSuggestionProvider.class
			.getName();

	public static final int MODE = DATABASE_MODE_QUERIES;

	public SearchSuggestionProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor c = super.query(uri, projection, selection,
				selectionArgs, sortOrder);
		return c;
	}

}
