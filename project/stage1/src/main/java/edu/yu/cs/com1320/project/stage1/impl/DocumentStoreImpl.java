package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage1.Document;
import edu.yu.cs.com1320.project.stage1.DocumentStore;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;


public class DocumentStoreImpl implements DocumentStore {
	/**
     * the two document formats supported by this document store.
     * Note that TXT means plain text, i.e. a String.
     */

    private HashTableImpl<URI, Document> table;

    public DocumentStoreImpl(){
    	this.table = new HashTableImpl<>();
    }
    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. 
     If there is a previous doc, return the hashCode of the previous doc. 
     If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     */
     /*
	Your code will receive documents as an InputStream and the document's key as an instance of URI. 
	When a document is added to your document store, you must do the following:

	1. Read the entire contents of the document from the InputStream into a byte[]

	2. Create an instance of DocumentImpl with the URI and the String or byte[]that was passed to you.

	3. Insert the Document object into the hash table with URI as the key and the Document object as the value

	4. Return the hashCode of the previous document that was stored in the hashTable at that URI, or zero if there was none
     */
	@Override
	public int putDocument(InputStream input, URI uri, DocumentFormat format) throws IOException {
		if(uri == null){
			throw new IllegalArgumentException("Null URI");
		}
		if(input == null){
			try{
				Document replacedDoc = this.table.put(uri, null);
				return replacedDoc.hashCode();
			}catch(NullPointerException e){
				return 0;
			}
		}
		byte[] byteArray = input.readAllBytes();
		if(format.equals(DocumentFormat.BINARY) ){
			DocumentImpl newDoc = new DocumentImpl(uri, byteArray);
			try{
				return (this.table.put(uri, newDoc)).hashCode();
			}catch(NullPointerException e){
				return 0;
			}
		}else{//when format is TXT
			String documentTXT = new String(byteArray);
			DocumentImpl newDoc = new DocumentImpl(uri, documentTXT);
			try{
				return (this.table.put(uri, newDoc)).hashCode();
			}catch(NullPointerException e){
				return 0;
			}
		}





	}


	/**
     * @param uri the unique identifier of the document to get
     * @return the given document
     */
    public Document getDocument(URI uri){
    	return this.table.get(uri);
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    public boolean deleteDocument(URI uri){
    	if(this.table.put(uri, null) instanceof Document){
    		return true;
    	}else{
    		return false;
    	}
    }


}