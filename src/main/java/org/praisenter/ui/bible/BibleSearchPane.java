package org.praisenter.ui.bible;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.praisenter.async.AsyncHelper;
import org.praisenter.data.PersistableComparator;
import org.praisenter.data.bible.Bible;
import org.praisenter.data.bible.BibleConfiguration;
import org.praisenter.data.bible.BibleSearchResult;
import org.praisenter.data.bible.BibleTextSearchCriteria;
import org.praisenter.data.bible.LocatedVerse;
import org.praisenter.data.bible.ReadOnlyBook;
import org.praisenter.data.search.SearchResult;
import org.praisenter.data.search.SearchTextMatch;
import org.praisenter.data.search.SearchType;
import org.praisenter.ui.GlobalContext;
import org.praisenter.ui.Option;
import org.praisenter.ui.controls.Dialogs;
import org.praisenter.ui.controls.AutoCompleteComboBox;
import org.praisenter.ui.controls.ProgressOverlay;
import org.praisenter.ui.translations.Translations;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyFloatWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

// FEATURE (M-M) add searching to the bible editor for finding and editing easily

public final class BibleSearchPane extends VBox {
	private static final String BIBLE_SEARCH_CSS = "p-bible-search";
	private static final String BIBLE_SEARCH_CRITERIA_CSS = "p-bible-search-criteria";
	
	private static final Logger LOGGER = LogManager.getLogger();

	private static final DecimalFormat SCORE_FORMAT = new DecimalFormat(Translations.get("search.score.format"));

	private final GlobalContext context;
	
	// data
	
	private final ObservableList<Bible> bibles;
	private final ObjectProperty<Bible> bible;
	private final ObservableList<ReadOnlyBook> books;
	private final ObjectProperty<ReadOnlyBook> book;
	private final ObjectProperty<Option<SearchType>> searchType;
	private final StringProperty terms;
	
	// value
	
	private final ObjectProperty<BibleSearchResult> value;
	private final BooleanProperty append;
	
	public BibleSearchPane(GlobalContext context, BibleConfiguration configuration) {
		this.getStyleClass().add(BIBLE_SEARCH_CSS);
		
		this.context = context;
		
		this.bibles = FXCollections.observableArrayList();
		this.bible = new SimpleObjectProperty<Bible>();
		this.books = FXCollections.observableArrayList();
		this.book = new SimpleObjectProperty<ReadOnlyBook>();
		this.searchType = new SimpleObjectProperty<Option<SearchType>>();
		this.terms = new SimpleStringProperty();
		
		this.value = new SimpleObjectProperty<BibleSearchResult>();
		this.append = new SimpleBooleanProperty(false);
		
		ObservableList<Option<SearchType>> types = FXCollections.observableArrayList();
		types.add(new Option<SearchType>(Translations.get("search.type.phrase"), SearchType.PHRASE));
		types.add(new Option<SearchType>(Translations.get("search.type.allwords"), SearchType.ALL_WORDS));
		types.add(new Option<SearchType>(Translations.get("search.type.anyword"), SearchType.ANY_WORD));
		this.searchType.setValue(types.get(0));
		
		ObservableList<Bible> bibles = context.getWorkspaceManager().getItemsUnmodifiable(Bible.class).sorted(new PersistableComparator<Bible>());
		Bindings.bindContent(this.bibles, bibles);
		
		this.bible.addListener((obs, ov, nv) -> {
			ReadOnlyBook book = this.book.get();
			if (ov != null) {
				Bindings.unbindContent(this.books, ov.getBooks());
			}
			if (nv != null) {
				Bindings.bindContent(this.books, nv.getBooks());
				ReadOnlyBook newBook = nv.getMatchingBook(book);
				if (newBook != null) {
					this.book.set(newBook);
				}
			}
		});
		
		Bible backupBible = null;
		if (bibles != null && bibles.size() > 0) {
			backupBible = bibles.get(0);
		}
		
		UUID primaryId = configuration.getPrimaryBibleId();
		
		Bible primaryBible = null;
		if (primaryId != null) {
			primaryBible = context.getWorkspaceManager().getItem(Bible.class, primaryId);
		}
		
		if (primaryBible == null) {
			primaryBible = backupBible;
		}
		
		this.bible.set(primaryBible);
		
		TextField txtSearch = new TextField();
		txtSearch.setPromptText(Translations.get("search.terms.placeholder"));
		txtSearch.textProperty().bindBidirectional(this.terms);
		
		ComboBox<Option<SearchType>> cmbSearchType = new ComboBox<Option<SearchType>>(types);
		cmbSearchType.setValue(types.get(0));
		cmbSearchType.valueProperty().bindBidirectional(this.searchType);
		
		ComboBox<Bible> cmbBible = new ComboBox<Bible>(bibles);
		cmbBible.valueProperty().bindBidirectional(this.bible);
		
		ComboBox<ReadOnlyBook> cmbBook = new AutoCompleteComboBox<ReadOnlyBook>(this.books, (typedText, book) -> {
			Pattern pattern = Pattern.compile("^" + Pattern.quote(typedText) + ".*", Pattern.CASE_INSENSITIVE);
			if (pattern.matcher(book.getName()).matches()) {
				return true;
			}
			return false;
		});
		cmbBook.valueProperty().bindBidirectional(this.book);
		cmbBook.setPromptText(Translations.get("bible.book.placeholder"));
		
		Button btnSearch = new Button(Translations.get("search.button"));
		
		GridPane top = new GridPane();
		
		top.add(txtSearch, 0, 0);
		top.add(cmbBible, 1, 0);
		top.add(cmbBook, 2, 0);
		top.add(cmbSearchType, 3, 0);
		top.add(btnSearch, 4, 0);
		
		txtSearch.setMaxWidth(Double.MAX_VALUE);
		cmbBible.setMaxWidth(Double.MAX_VALUE);
		cmbBook.setMaxWidth(Double.MAX_VALUE);
		cmbSearchType.setMaxWidth(Double.MAX_VALUE);
		btnSearch.setMaxWidth(Double.MAX_VALUE);
		
		final int[] widths = new int[] { 30, 25, 20, 15, 10 };
		for (int i = 0; i < widths.length; i++) {
			ColumnConstraints cc = new ColumnConstraints();
			cc.setPercentWidth(widths[i]);
			top.getColumnConstraints().add(cc);
		}
		
		top.getStyleClass().add(BIBLE_SEARCH_CRITERIA_CSS);
		
		///////////////////////////
		
		TextArea txtVerse = new TextArea();
		txtVerse.setWrapText(true);
		txtVerse.setEditable(false);
		txtVerse.setPromptText(Translations.get("bible.search.results.placeholder"));

		TableView<BibleSearchResult> table = new TableView<BibleSearchResult>();
		
		// columns
		TableColumn<BibleSearchResult, Number> score = new TableColumn<BibleSearchResult, Number>(Translations.get("search.score"));
		TableColumn<BibleSearchResult, BibleSearchResult> reference = new TableColumn<BibleSearchResult, BibleSearchResult>(Translations.get("bible.search.results.reference"));
		TableColumn<BibleSearchResult, BibleSearchResult> verseText = new TableColumn<BibleSearchResult, BibleSearchResult>(Translations.get("bible.search.results.text"));
		
		score.setCellValueFactory(p -> new ReadOnlyFloatWrapper(p.getValue().getScore()));
		reference.setCellValueFactory(p -> new ReadOnlyObjectWrapper<BibleSearchResult>(p.getValue()));
		verseText.setCellValueFactory(p -> new ReadOnlyObjectWrapper<BibleSearchResult>(p.getValue()));
		
		score.setCellFactory(p -> new TableCell<BibleSearchResult, Number>() {
			{
				setAlignment(Pos.CENTER_RIGHT);
			}
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null || empty) {
					setText(null);
				} else {
					setText(SCORE_FORMAT.format(item));
				}
			}
		});
		reference.setCellFactory(p -> new TableCell<BibleSearchResult, BibleSearchResult>() {
			@Override
			protected void updateItem(BibleSearchResult item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null || empty) {
					setText(null);
				} else {
					setText(MessageFormat.format("{0} {1}:{2}", 
							item.getBook().getName(),
							item.getChapter().getNumber(),
							item.getVerse().getNumber()));
				}
			}
		});
		verseText.setCellFactory(p -> new TableCell<BibleSearchResult, BibleSearchResult>() {
			@Override
			protected void updateItem(BibleSearchResult item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null || empty) {
					setGraphic(null);
				} else {
					List<SearchTextMatch> matches = item.getMatches();
					SearchTextMatch match = null;
					if (matches != null && matches.size() > 0) {
						match = matches.get(0);
					}
					
					if (match == null) {
						setGraphic(new Text(item.getVerse().getText()));
						return;
					}
					
					// get the matched text
					String highlighted = match.getMatchedText();
					HBox text = new HBox();
					
					// format the match text from Lucene to show what we matched on
					String[] mparts = highlighted.replaceAll("\n\r?", " ").split("<B>");
					for (String mpart : mparts) {
						if (mpart.contains("</B>")) {
							String[] nparts = mpart.split("</B>");
							Text temp = new Text(nparts[0]);
							temp.getStyleClass().add("p-search-highlight");
							text.getChildren().add(temp);
							// it's possible mpart could be "blah</B>" which would only give us one part
							if (nparts.length > 1) {
								text.getChildren().add(new Text(nparts[1]));
							}
						} else {
							text.getChildren().add(new Text(mpart));
						}
					}
					
					setGraphic(text);
				}
			}
		});
		
		score.setPrefWidth(75);
		reference.setPrefWidth(150);
		verseText.setPrefWidth(600);
		
		table.getColumns().add(score);
		table.getColumns().add(reference);
		table.getColumns().add(verseText);
		table.setPlaceholder(new Label(Translations.get("bible.search.results.none")));
		
		table.setRowFactory(tv -> {
		    TableRow<BibleSearchResult> row = new TableRow<BibleSearchResult>();
		    row.setOnMouseClicked(event -> {
		    	if (!row.isEmpty()) {
		    		BibleSearchResult rowData = row.getItem();
			        if (event.getClickCount() == 2) {
			            // set the current value
			        	this.append.set(event.isShortcutDown());
			        	this.value.set(null);
			        	this.value.set(rowData);
			        }
		    	}
		    });
		    return row ;
		});
		
		table.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
			if (nv != null) {
				txtVerse.setText(nv.getVerse().getText());
			} else {
				txtVerse.setText(null);
			}
		});
		
		ProgressOverlay overlay = new ProgressOverlay();
		overlay.setVisible(false);
		
		StackPane stack = new StackPane(table, overlay);
		
		Label lblResults = new Label();
		
		SplitPane splt = new SplitPane(stack, txtVerse);
		splt.setOrientation(Orientation.VERTICAL);
		splt.setDividerPosition(0, 0.8);
		SplitPane.setResizableWithParent(txtVerse, false);
		
		VBox.setVgrow(splt, Priority.ALWAYS);
		
		this.getChildren().addAll(top, splt, lblResults);
		
		EventHandler<ActionEvent> handler = e -> {
			Bible bible = this.bible.get();
			ReadOnlyBook book = this.book.get();
			String text = this.terms.get();
			Option<SearchType> type = this.searchType.get();
			
			if (text != null && text.length() != 0 && type != null) {
				overlay.setVisible(true);
				
				final int maxResults = 100;
				BibleTextSearchCriteria criteria = new BibleTextSearchCriteria(
						text,
						type.getValue(),
						maxResults,
						bible != null ? bible.getId() : null, 
						book != null ? book.getNumber() : -1);
				
				context.getWorkspaceManager().search(criteria).thenCompose(AsyncHelper.onJavaFXThreadAndWait((result) -> {
					table.setItems(FXCollections.observableArrayList(this.getSearchResults(result.getResults())));
					lblResults.setText(MessageFormat.format(Translations.get("bible.search.results.output"), result.hasMore() ? maxResults + "+" : result.getNumberOfResults()));
					overlay.setVisible(false);
				})).exceptionally(t -> {
					LOGGER.error("Failed to search bibles using terms '" + text + "' due to: " + t.getMessage(), t);
					Platform.runLater(() -> {
						Alert alert = Dialogs.exception(this.context.getStage(), t);
						alert.show();
					});
					return null;
				});
			}
		};
		
		// update the search results when things are changed, removed, added, etc.
		context.getWorkspaceManager().getItemsUnmodifiable(Bible.class).addListener((Change<? extends Bible> c) -> {
			handler.handle(null);
		});
		
		txtSearch.setOnAction(handler);
		btnSearch.setOnAction(handler);
	}
	
	private List<BibleSearchResult> getSearchResults(List<SearchResult> results) {
		List<BibleSearchResult> output = new ArrayList<BibleSearchResult>();
		for (SearchResult result : results) {
			Document document = result.getDocument();
			Bible bible = this.context.getWorkspaceManager().getItem(Bible.class, UUID.fromString(document.get(Bible.FIELD_ID)));
			if (bible == null) {
				LOGGER.warn("Unable to find bible '{}'. A re-index might fix this problem.", document.get(Bible.FIELD_ID));
				continue;
			}
			
			// get the details
			int bookNumber = document.getField(Bible.FIELD_BOOK_NUMBER).numericValue().intValue();
			int chapterNumber = document.getField(Bible.FIELD_VERSE_CHAPTER).numericValue().intValue();
			String verseNumber = document.getField(Bible.FIELD_VERSE_NUMBER).stringValue();
			
			LocatedVerse verse = null;
			if (bible != null) {
				verse = bible.getVerse(bookNumber, chapterNumber, verseNumber);
			}
			
			// just continue if its not found
			if (verse == null) {
				LOGGER.warn("Unable to find {} {}:{} in '{}'. A re-index might fix this problem.", bookNumber, chapterNumber, verseNumber, bible != null ? bible.getName() : "null");
				continue;
			}
			
			output.add(new BibleSearchResult(
					verse.getBible(),
					verse.getBook(), 
					verse.getChapter(), 
					verse.getVerse(), 
					result.getMatches(), 
					result.getScore()));
		}
		return output;
	}
	
	public BibleSearchResult getValue() {
		return this.value.get();
	}
	
	public ReadOnlyObjectProperty<BibleSearchResult> valueProperty() {
		return this.value;
	}
	
	public boolean isAppendEnabled() {
		return this.append.get();
	}
	
	public ReadOnlyBooleanProperty appendProperty() {
		return this.append;
	}
	
	public String getSearchTerms() {
		return this.terms.get();
	}
	
	public void setSearchTerms(String terms) {
		this.terms.set(terms);
	}
	
	public StringProperty searchTermsProperty() {
		return this.terms;
	}
}
