package org.praisenter.data.bible;

import org.praisenter.Watchable;

import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class Verse implements ReadOnlyVerse, Comparable<Verse> {
	private final StringProperty text;
	private final StringProperty number;

	public Verse() {
		this.text = new SimpleStringProperty();
		this.number = new SimpleStringProperty();
	}

	public Verse(String number, String text) {
		this();
		this.number.set(number);
		this.text.set(text);
	}

	@Override
	public String toString() {
		return String.valueOf(this.number.get()) + " " + this.text.get();
	}

	@Override
	public int compareTo(Verse o) {
		return this.getNumber().compareTo(o.getNumber());
	}

	@Override
	public Verse copy() {
		Verse v = new Verse();
		v.setNumber(this.getNumber());
		v.setText(this.getText());
		return v;
	}

	@Override
	@JsonProperty
	public String getNumber() {
		return this.number.get();
	}

	@JsonProperty
	public void setNumber(String number) {
		this.number.set(number);
	}

	@Override
	@Watchable(name = "number")
	public StringProperty numberProperty() {
		return this.number;
	}

	@Override
	@JsonProperty
	public String getText() {
		return this.text.get();
	}

	@JsonProperty
	public void setText(String text) {
		this.text.set(text);
	}

	@Override
	@Watchable(name = "text")
	public StringProperty textProperty() {
		return this.text;
	}
}
