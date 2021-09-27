package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;

import java.util.*;
import java.util.function.Function;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

//The reason for all the errors is I haven't changed my StackImpl yet
public class DocumentStoreImpl implements DocumentStore {
	/**
     * the two document formats supported by this document store.
     * Note that TXT means plain text, i.e. a String.
     */

    private HashTableImpl<URI, Document> table;
    private StackImpl<Undoable> myStack; //type undoable
    private HashTableImpl<URI, StackImpl<DocumentImpl>> uriToStack;
    private TrieImpl<Document> trie;
	protected String keyword; //For Comparator
	private boolean deleteAllACTIVATED;
	private CommandSet<URI> commandSetForDeleteAll;
	private HashSet<String> setAlreadyDeletedInDeleteAll;

    public DocumentStoreImpl(){
    	this.table = new HashTableImpl<>();
    	this.myStack = new StackImpl<>();
    	this.uriToStack = new HashTableImpl<>();
    	this.trie = new TrieImpl<>();
    	this.keyword = null;
    	this.deleteAllACTIVATED = false;
    	this.commandSetForDeleteAll = new CommandSet<>();

    }
    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. 
     If there is a previous doc, return the hashCode of the previous doc.
     If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
	 * @throws IOException if there is an issue reading input
	 * @throws IllegalArgumentException if uri or format are null
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
			if(this.getDocument(uri) != null){
				int hash = this.getDocument(uri).hashCode();
				this.deleteDocument(uri); //Command was pushed from deleteDocument method and other stack
				return hash;
			}else{
				//Dont need to add to uriToStack bc theres no Document with the URI here
				this.myStack.push(new GenericCommand<>(uri, noOp));
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
			Document document = this.table.get(uri);
			if(document != null){
				Set<String> wordsInDeletedDoc = document.getWords();
				for(String s : wordsInDeletedDoc){
					this.trie.delete(s, document);
				}
				this.myStack.push(new GenericCommand(uri, undoPutReplace));
				return (this.table.put(uri, newDoc)).hashCode();
			}else{
				this.myStack.push(new GenericCommand(uri, undoPutNew));
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

			Set<String> wordsInDoc = newDoc.getWords();
			for(String s: wordsInDoc) {
				this.trie.put(s, newDoc);
			}
			Document document = this.table.get(uri);
			if(document != null){
				Set<String> wordsInDeletedDoc = document.getWords();
				for(String s : wordsInDeletedDoc){
					this.trie.delete(s, document);
				}
				this.myStack.push(new GenericCommand(uri, undoPutReplace));
				return (this.table.put(uri, newDoc)).hashCode();
			}else{
				this.myStack.push(new GenericCommand(uri, undoPutNew));
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

			Set<String> wordsInDoc = gotDoc.getWords();
			for(String s: wordsInDoc) {
				if(this.deleteAllACTIVATED){

				}else{
					this.trie.delete(s, gotDoc);
				}

			}

    		this.uriToStack.get(uri).push(gotDoc);

		}
    	if(this.table.put(uri, null) != null){
    		if(this.deleteAllACTIVATED){
    			this.commandSetForDeleteAll.addCommand(new GenericCommand(uri, undoDelete));
			}else{
				this.myStack.push(new GenericCommand(uri, undoDelete));
			}
			return true;
    	}else{
    		if(this.deleteAllACTIVATED){
    			this.commandSetForDeleteAll.addCommand(new GenericCommand(uri, noOp));
			}else{
				this.myStack.push(new GenericCommand(uri, noOp));
			}
    		return false;
    	}
    }

    private Function<URI, Boolean> undoPutNew = uri -> {
		if(this.uriToStack.get(uri) == null) {
			return true;
		}
		Document poppedDoc = this.uriToStack.get(uri).pop();

		Set<String> wordsInDoc = poppedDoc.getWords();
		for(String s: wordsInDoc) {
			this.trie.delete(s, poppedDoc);
		}

		this.table.put(uri, null);
		return true;
	};

    private Function<URI, Boolean> undoPutReplace = uri -> {
		if(this.uriToStack.get(uri) == null){
			return true;
		}
		DocumentImpl poppedDoc = this.uriToStack.get(uri).pop();
		DocumentImpl peekedDoc = this.uriToStack.get(uri).peek();

		Set<String> wordsInPoppedDoc = poppedDoc.getWords();
		for(String s : wordsInPoppedDoc){
			this.trie.delete(s, poppedDoc);
		}
		Set<String> wordsInPeekedDoc = peekedDoc.getWords();
		for(String s : wordsInPeekedDoc){
			this.trie.put(s,peekedDoc);
		}

		this.table.put(uri, peekedDoc);
		return true;
	};

    private Function<URI, Boolean> undoDelete = uri -> {
    	if(this.uriToStack.get(uri) == null){
    		return true;
		}
		DocumentImpl poppedDoc = this.uriToStack.get(uri).pop();
		Set<String> wordsInPoppedDoc = poppedDoc.getWords();
		for(String s : wordsInPoppedDoc){
			this.trie.put(s, poppedDoc);
		}
		this.table.put(uri, poppedDoc);
		return true;
	};

    private Function<URI, Boolean> noOp = uri -> true;

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
		boolean sizeWas1 = false;
		if(this.myStack.peek() == null){
			throw new IllegalStateException("Nothing to be undone!");
		}
		if(this.myStack.size() == 1){
			sizeWas1 = true;
		}
		StackImpl<Undoable> secondStack = new StackImpl<>();

		while(this.myStack.peek() != null){
			Undoable peekedCom = this.myStack.peek();
			if (peekedCom instanceof GenericCommand){
				GenericCommand genericCommand = (GenericCommand) this.myStack.pop();
				if(genericCommand.getTarget().equals(uri)){
					while(secondStack.peek() != null){
						Undoable poppedCommand = secondStack.pop();
						this.myStack.push(poppedCommand);
					}
					genericCommand.undo();
					break;
				}else{
					secondStack.push(genericCommand);
				}

			}else if(peekedCom instanceof CommandSet){
				CommandSet commandSet = (CommandSet) this.myStack.pop();
				if(commandSet.containsTarget(uri)){
					commandSet.undo(uri);
					if(commandSet.size() != 0){
						this.myStack.push(commandSet);
					}
					while(secondStack.peek() != null){
						Undoable poppedCommand = secondStack.pop();
						this.myStack.push(poppedCommand);
					}
					break;
				}else{
					secondStack.push(commandSet);
				}
			}
		}
		if(this.myStack.peek() == null) { // Lets say there's only one action to undo
			while(secondStack.peek() != null){
				Undoable poppedCommand = secondStack.pop();
				this.myStack.push(poppedCommand);
			}
			if(!sizeWas1){
				throw new IllegalStateException("URI you've selected to be undone is not present in the stack");
			}

		}


	}
	private String fixKeyWord(String str) {
		String fixedKeyWord = str.replaceAll("[^a-zA-Z0-9\\s]", "").toUpperCase();
		return fixedKeyWord;
	}

	private class compareByKeyWord implements Comparator<Document> {
		String keyword = DocumentStoreImpl.this.keyword;

		@Override
		public int compare(Document doc1, Document doc2) {
			int doc1Num = doc1.wordCount(keyword);
			int doc2Num = doc2.wordCount(keyword);

			return doc2Num - doc1Num;


		}

	}

	private class compareByPrefix implements Comparator<Document>{
		String keyword = DocumentStoreImpl.this.keyword;

		@Override
		public int compare(Document doc1, Document doc2) {
			int doc1Num = 0;
			int doc2Num = 0;
			if (doc1 instanceof DocumentImpl) {
				doc1Num = ((DocumentImpl) doc1).prefixWordCount(keyword);
			}
			if(doc2 instanceof  DocumentImpl){
				doc2Num = ((DocumentImpl) doc2).prefixWordCount(keyword);
			}


			return doc2Num-doc1Num;
		}




	}

	/**
	 * Retrieve all documents whose text contains the given keyword.
	 * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
	 * Search is CASE INSENSITIVE.
	 *
	 * @param keyword
	 * @return a List of the matches. If there are no matches, return an empty list.
	 */
	@Override
	public List<Document> search(String keyword) {
		List<Document> searchedList = new ArrayList<>();
		if(keyword == null){
			return searchedList;
		}
		this.keyword = this.fixKeyWord(keyword);

		return this.trie.getAllSorted(this.keyword, new compareByKeyWord());

	}

	/**
	 * Retrieve all documents whose text starts with the given prefix
	 * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
	 * Search is CASE INSENSITIVE.
	 *
	 * @param keywordPrefix
	 * @return a List of the matches. If there are no matches, return an empty list.
	 */
	@Override
	public List<Document> searchByPrefix(String keywordPrefix) {
		List<Document> searchedList = new ArrayList<>();
		if(keywordPrefix == null){
			return searchedList;
		}
		this.keyword = this.fixKeyWord(keywordPrefix);

		return this.trie.getAllWithPrefixSorted(this.keyword, new compareByPrefix());


	}

	/**
	 * Completely remove any trace of any document which contains the given keyword
	 *
	 * @param keyword
	 * @return a Set of URIs of the documents that were deleted.
	 */
	@Override
	public Set<URI> deleteAll(String keyword) {
		Set<URI> setOfDeletedURIs = new HashSet<>();
		if(keyword == null){
			this.myStack.push(new GenericCommand(null, noOp));
			return setOfDeletedURIs;
		}

		Set<Document> deletedDocs = this.trie.deleteAll(keyword);
		if(deletedDocs.isEmpty()){
			this.myStack.push(new GenericCommand(null, noOp));
			return setOfDeletedURIs;
		}
		this.deleteAllACTIVATED = true;
		for(Document d : deletedDocs){
			URI uri = d.getKey();
			setOfDeletedURIs.add(uri);
			this.deleteDocument(uri);
		}
		this.deleteAllACTIVATED = false;
		this.myStack.push(this.commandSetForDeleteAll);
		this.commandSetForDeleteAll = new CommandSet<>();

		return setOfDeletedURIs;

	}

	/**
	 * Completely remove any trace of any document which contains a word that has the given prefix
	 * Search is CASE INSENSITIVE.
	 *
	 * @param keywordPrefix
	 * @return a Set of URIs of the documents that were deleted.
	 */
	@Override
	public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
		Set<URI> setOfDeletedURIs = new HashSet<>();
		if(keywordPrefix == null){
			this.myStack.push(new GenericCommand(null, noOp));
			return setOfDeletedURIs;
		}
		Set<Document> deletedDocs = this.trie.deleteAllWithPrefix(keywordPrefix);
		if(deletedDocs.isEmpty()){
			this.myStack.push(new GenericCommand(null, noOp));
			return setOfDeletedURIs;
		}
		this.deleteAllACTIVATED = true;

		for(Document d : deletedDocs){
			URI uri = d.getKey();
			setOfDeletedURIs.add(uri);
			this.deleteDocument(uri);
		}
		this.deleteAllACTIVATED = false;
		this.myStack.push(this.commandSetForDeleteAll);
		this.commandSetForDeleteAll = new CommandSet<>();

		return setOfDeletedURIs;

	}


}











