package org.praisenter.data.bible;

import org.praisenter.data.Copyable;

import javafx.beans.property.ReadOnlyStringProperty;

public interface ReadOnlyVerse extends Copyable {
	public String getNumber();
	public String getText();
	
	public ReadOnlyStringProperty numberProperty();
	public ReadOnlyStringProperty textProperty();
}
