package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.Command;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage2.Document;
import edu.yu.cs.com1320.project.stage2.DocumentStore;

import java.util.function.Function;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;


public class DocumentStoreImpl implements DocumentStore {
	/**
     * the two document formats supported by this document store.
     * Note that TXT means plain text, i.e. a String.
     */

    private HashTableImpl<URI, Document> table;
    private StackImpl<Command> myStack;
    private HashTableImpl<URI, StackImpl<DocumentImpl>> uriToStack;

    public DocumentStoreImpl(){
    	this.table = new HashTableImpl<>();
    	this.myStack = new StackImpl<>();
    	this.uriToStack = new HashTableImpl<>();
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
		if(uri == null || format == null){
			throw new IllegalArgumentException("Null uri or format = bad");
		}
		if(input == null){// delete
			if(this.getDocument(uri) instanceof Document){
				int hash = this.getDocument(uri).hashCode();
				this.deleteDocument(uri); //Command was pushed from deleteDocument method and other stack
				return hash;
			}else{
				//Dont need to add to uriToStack bc theres no Document with the URI here
				this.myStack.push(new Command(uri, undoDelete));
				return 0;
			}

		}
		byte[] byteArray = input.readAllBytes();
		if(format.equals(DocumentFormat.BINARY) ){
			DocumentImpl newDoc = new DocumentImpl(uri, byteArray);
			StackImpl docStack = this.uriToStack.get(uri);
			if(docStack == null){
				docStack = new StackImpl<DocumentImpl>();
			}
			docStack.push(newDoc);
			this.uriToStack.put(uri, docStack);
			if(this.table.get(uri) instanceof Document){
				this.myStack.push(new Command(uri, undoPutReplace));
				return (this.table.put(uri, newDoc)).hashCode();
			}else{
				this.myStack.push(new Command(uri, undoPutNew));
				this.table.put(uri, newDoc);
				return 0;
			}
		}else{//when format is TXT
			String documentTXT = new String(byteArray);
			DocumentImpl newDoc = new DocumentImpl(uri, documentTXT);
			StackImpl docStack = this.uriToStack.get(uri);
			if(docStack == null){
				docStack = new StackImpl<DocumentImpl>();
			}
			docStack.push(newDoc);
			this.uriToStack.put(uri, docStack);

			if(this.table.get(uri) instanceof Document){
				this.myStack.push(new Command(uri, undoPutReplace));
				return (this.table.put(uri, newDoc)).hashCode();
			}else{
				this.myStack.push(new Command(uri, undoPutNew));
				this.table.put(uri, newDoc);
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

    	if(uri == null){
    		throw new IllegalArgumentException("null URI");
		}

    	if(this.table.get(uri) != null){
    		DocumentImpl gotDoc = (DocumentImpl) this.table.get(uri);
    		StackImpl gotDocsStack = this.uriToStack.get(uri);
    		gotDocsStack.push(gotDoc);
		}
    	if(this.table.put(uri, null) instanceof Document){
    		this.myStack.push(new Command(uri, undoDelete));
    		return true;
    	}else{
    		this.myStack.push(new Command(uri, undoDelete));
    		return false;
    	}
    }

    private Function<URI, Boolean> undoPutNew = uri -> {
		if(this.uriToStack.get(uri) == null) {
			return true;
		}
		DocumentImpl poppedDoc = this.uriToStack.get(uri).pop();
		this.table.put(uri, null);
		return true;
	};

    private Function<URI, Boolean> undoPutReplace = uri -> {
		if(this.uriToStack.get(uri) == null){
			return true;
		}
		DocumentImpl poppedDoc = this.uriToStack.get(uri).pop();
		DocumentImpl peekedDoc = this.uriToStack.get(uri).peek();
		this.table.put(uri, peekedDoc);
		return true;
	};

    private Function<URI, Boolean> undoDelete = uri -> {
    	if(this.uriToStack.get(uri) == null){
    		return true;
		}
		DocumentImpl poppedDoc = this.uriToStack.get(uri).pop();
		this.table.put(uri, poppedDoc);
		return true;
	};

    /**
     * undo the last put or delete command
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
	@Override
	public void undo() throws IllegalStateException {
		if(this.myStack.peek() == null){
			throw new IllegalStateException("Nothing to be undone!");
		}
		this.myStack.pop().undo();
	}

	
	/**
     * undo the last put or delete that was done with the given URI as its key
     * @param uri
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
	@Override
	public void undo(URI uri) throws IllegalStateException {
		if(this.myStack.peek() == null){
			throw new IllegalStateException("Nothing to be undone!");
		}
		StackImpl<Command> secondStack = new StackImpl<>();
		Command currentCommand = this.myStack.pop();
		while((!(currentCommand.getUri().equals(uri))) && (this.myStack.peek() != null)){
			secondStack.push(currentCommand);
			currentCommand = this.myStack.pop();
		}

		if((currentCommand.getUri().equals(uri))){
			while(secondStack.peek() != null){
				Command poppedCommand = secondStack.pop();
				this.myStack.push(poppedCommand);
			}
			currentCommand.undo();
		}else{//when URI was not found
			secondStack.push(currentCommand);
			while(secondStack.peek() != null){
				Command poppedCommand = secondStack.pop();
				this.myStack.push(poppedCommand);
			}
			throw new IllegalStateException("URI you've selected to be undone is not present in the stack");
		}
	}


}











