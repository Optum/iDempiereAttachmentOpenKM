package org.optum.erp.dms.utils;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// TODO: Auto-generated Javadoc
/**
 * The Class Documents.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Documents {

	
	/** The document. */
	private ArrayList<Document> document;

	/**
	 * Gets the document.
	 *
	 * @return the document
	 */
	public ArrayList<Document> getDocument() {
		return document;
	}

	/**
	 * Sets the document.
	 *
	 * @param document the new document
	 */
	public void setDocument(ArrayList<Document> document) {
		this.document = document;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Documents [document=" + document + "]";
	}
	
	
}
