package org.praisenter.data.bible;

import org.praisenter.data.Copyable;
import org.praisenter.data.Identifiable;
import org.praisenter.data.Localized;
import org.praisenter.data.Persistable;
import org.praisenter.data.search.Indexable;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;

public interface ReadOnlyBible extends Indexable, Persistable, Copyable, Identifiable, Localized {
	public String getLanguage();
	public String getCopyright();
	public String getSource();
	public String getNotes();
	
	public ReadOnlyStringProperty languageProperty();
	public ReadOnlyStringProperty copyrightProperty();
	public ReadOnlyStringProperty sourceProperty();
	public ReadOnlyStringProperty notesProperty();
	
	public ObservableList<? extends ReadOnlyBook> getBooksUnmodifiable();
	
	public int getVerseCount();
	public int getBookCount();
	public LocatedVerse getVerse(int bookNumber, int chapterNumber, String verseNumber);
	public LocatedVerse getNextVerse(int bookNumber, int chapterNumber, String verseNumber);
	public LocatedVerse getPreviousVerse(int bookNumber, int chapterNumber, String verseNumber);
	public LocatedVerseTriplet getTriplet(int bookNumber, int chapterNumber, String verseNumber);
	public LocatedVerseTriplet getNextTriplet(int bookNumber, int chapterNumber, String verseNumber);
	public LocatedVerseTriplet getPreviousTriplet(int bookNumber, int chapterNumber, String verseNumber);
	public LocatedVerseTriplet getMatchingTriplet(LocatedVerseTriplet triplet);
	public Book getLastBook();
	public int getMaxBookNumber();
	public Book getMatchingBook(ReadOnlyBook book);
}
