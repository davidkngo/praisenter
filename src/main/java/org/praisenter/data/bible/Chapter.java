package org.praisenter.data.bible;

import java.util.ArrayList;
import java.util.List;

import org.praisenter.Watchable;
import org.praisenter.data.Copyable;

import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class Chapter implements ReadOnlyChapter, Comparable<Chapter> {
	private final IntegerProperty number;
	private final ObservableList<Verse> verses;
	private final ObservableList<Verse> versesReadOnly;

	public Chapter() {
		this.number = new SimpleIntegerProperty();
		this.verses = FXCollections.observableArrayList();
		this.versesReadOnly = FXCollections.unmodifiableObservableList(this.verses);
	}

	public Chapter(int number) {
		this();
		this.number.set(number);
	}

	@Override
	public String toString() {
		return String.valueOf(this.number.get());
	}

	@Override
	public int compareTo(Chapter o) {
		return this.getNumber() - o.getNumber();
	}

	@Override
	public Chapter copy() {
		Chapter c = new Chapter();
		c.setNumber(this.getNumber());
		for (Verse verse : this.verses) {
			c.verses.add(verse.copy());
		}
		return c;
	}

	public void renumber() {
		int n = 1;
		for (Verse verse : this.verses) {
			if (verse.getNumber().contains("-")) {
				String[] parts = verse.getNumber().split("-");

				ArrayList<CharSequence> v = new ArrayList<CharSequence>();
				for (String part : parts) {
					v.add(String.valueOf(n++));
				}

				verse.setNumber(String.join("-", v));
			}
			else {
				verse.setNumber(String.valueOf(n++));
			}
		}
	}

	public void reorder() {
		FXCollections.sort(this.verses);
	}

	public int getMaxVerseNumber() {
		int max = -Integer.MAX_VALUE;
		for (Verse verse : this.verses) {

			if (verse.getNumber().contains("-")) {
				String[] parts = verse.getNumber().split("-");

				for (String part : parts) {

					int n = Integer.parseInt(part);
					if (n > max) {
						max = n;
					}

				}

			}

			int n = Integer.parseInt(verse.getNumber());
			if (n > max) {
				max = n;
			}
		}
		return max >= 0 ? max : 1;
	}

	@Override
	public Verse getVerse(String verse) {
		for (Verse v : this.verses) {
			if (v.getNumber().equalsIgnoreCase(verse)) {
				return v;
			}
		}
		return null;
	}

	@JsonProperty
	public int getNumber() {
		return this.number.get();
	}

	@JsonProperty
	public void setNumber(int number) {
		this.number.set(number);
	}

	@Override
	@Watchable(name = "number")
	public IntegerProperty numberProperty() {
		return this.number;
	}

	@JsonProperty
	public void setVerses(List<Verse> verses) {
		this.verses.setAll(verses);
	}

	@JsonProperty
	@Watchable(name = "verses")
	public ObservableList<Verse> getVerses() {
		return this.verses;
	}

	@Override
	public ObservableList<? extends ReadOnlyVerse> getVersesUnmodifiable() {
		return this.versesReadOnly;
	}
}
