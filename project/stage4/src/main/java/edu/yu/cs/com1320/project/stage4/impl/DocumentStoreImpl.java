package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;


import java.util.*;
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
    private StackImpl<Undoable> myStack; //type undoable
    private HashTableImpl<URI, StackImpl<DocumentImpl>> uriToStack;

    private TrieImpl<Document> trie;
	protected String keyword; //For Comparator
	private boolean deleteAllACTIVATED;
	private CommandSet<URI> commandSetForDeleteAll;

	private MinHeapImpl<Document> minHeap;
	private boolean limitCheck;

	private int maxDocumentCount;
	private int maxDocumentBytes;
	private int currentAmountOfDocsInStore;
	private int currentMemoryOfBytes;

    public DocumentStoreImpl(){
    	this.table = new HashTableImpl<>();
    	this.myStack = new StackImpl<>();
    	this.uriToStack = new HashTableImpl<>();
    	this.trie = new TrieImpl<>();
    	this.keyword = null;
    	this.deleteAllACTIVATED = false;
    	this.commandSetForDeleteAll = new CommandSet<>();
		this.maxDocumentCount = Integer.MAX_VALUE;
    	this.maxDocumentBytes = Integer.MAX_VALUE;
    	this.minHeap = new MinHeapImpl<>();
    	this.currentAmountOfDocsInStore = 0;
    	this.currentMemoryOfBytes = 0;
    	this.limitCheck = false;


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
		//Put limitCheck() in here, and then write tests!

		if(uri == null || format == null){
			throw new IllegalArgumentException("Null uri or format = bad");
		}
		if(input == null){// delete
			if(this.getDocument(uri) != null){
				int hash = this.getDocument(uri).hashCode();
				this.deleteDocument(uri); //Command was pushed from deleteDocument method and other stack
				return hash;
			}else{
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
				this.heapReplace(document, newDoc);
				Document previousDoc = this.table.put(uri, newDoc);
				this.limitCheck();
				return previousDoc.hashCode();
			}else{
				this.myStack.push(new GenericCommand(uri, undoPutNew));
				this.heapInsert(newDoc);
				this.table.put(uri, newDoc);
				this.limitCheck();
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
				this.heapReplace(document, newDoc);
				Document previousDoc = this.table.put(uri, newDoc);
				this.limitCheck();
				return previousDoc.hashCode();
			}else{
				this.myStack.push(new GenericCommand(uri, undoPutNew));
				this.heapInsert(newDoc);
				this.table.put(uri, newDoc);
				this.limitCheck();
				return 0;
			}
		}
	}




	/**
     * @param uri the unique identifier of the document to get
     * @return the given document
     */
    public Document getDocument(URI uri){
		if(uri == null){
			throw new IllegalArgumentException("null URI");
		}
    	Document document = this.table.get(uri);
    	if(document == null){
    		return null;
		}
		this.reHeapMe(document);
		return document;
	}

	private void reHeapMe(Document document){
    	document.setLastUseTime(System.nanoTime());
    	minHeap.reHeapify(document);
	}
	private void heapInsert(Document document){
		DocumentImpl doc =(DocumentImpl) document;
    	doc.setLastUseTime(System.nanoTime());
    	minHeap.insert(doc);
    	this.updateNEWDOCCurrentMemoryOfBytesAndDocsInStore(doc);
	}
	private void heapReplace(Document docToBeErased, Document docToBeInserted){
    	docToBeErased.setLastUseTime(0);
    	minHeap.reHeapify(docToBeErased);
    	minHeap.remove();
    	docToBeInserted.setLastUseTime(System.nanoTime());
    	minHeap.insert(docToBeInserted);
    	DocumentImpl docToBeErasedd = (DocumentImpl) docToBeErased;
    	DocumentImpl docToBeInsertedd = (DocumentImpl) docToBeInserted;
    	this.updateREPLACEDDOCCurrentMemoryOfBytesAndDocsInStore(docToBeErasedd, docToBeInsertedd);
	}
	private void heapDelete(Document docToBeErased){
		docToBeErased.setLastUseTime(0);
		minHeap.reHeapify(docToBeErased);
		minHeap.remove();
		this.updateDELETEDOCCurrentMemoryOfBytesAndDocsInStore((DocumentImpl) docToBeErased);
	}

	private void limitCheck(){//Gonna have to do this after every put or undo
    	while((currentMemoryOfBytes > maxDocumentBytes) || (currentAmountOfDocsInStore > maxDocumentCount)){
			this.limitCheck = true;
    		DocumentImpl document = (DocumentImpl) minHeap.remove(); //Removed from Heap
			URI uri = document.getKey();
			this.deleteDocument(uri); // Removed from Trie and HashTable
			this.updateDELETEDOCCurrentMemoryOfBytesAndDocsInStore(document);
			this.myStack.pop(); //Will Take away the UNDO Command of this deleteDocument
			//Will null out all the documents that were ever at that URI
			StackImpl<DocumentImpl> stack = this.uriToStack.get(uri);
			StackImpl<DocumentImpl> documentStack = this.clearStackOfDoc(stack, document);
			this.uriToStack.put(uri, documentStack);
    		this.limitCheck = false;
		}
	}

	private StackImpl<DocumentImpl> clearStackOfDoc(StackImpl<DocumentImpl> firstStack, DocumentImpl documentToBeRemoved){
    	StackImpl<DocumentImpl> secondStack = new StackImpl<>();
    	while(firstStack.peek() != null){
    		DocumentImpl doc = firstStack.pop();
    		if(doc.equals(documentToBeRemoved)){
				//Do nothing, essentially deleting the doc from existence
			}else{
    			secondStack.push(doc);
			}
		}
    	while(secondStack.peek() != null){
    		DocumentImpl doc = secondStack.pop();
    		firstStack.push(doc);
		}
    	if(firstStack.peek() == null){
    		return null;
		}
    	return firstStack;
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

			if(!this.limitCheck){
				this.heapDelete(gotDoc);
			}


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
		this.heapDelete(poppedDoc);
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
		this.heapReplace(poppedDoc, peekedDoc);

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
		this.heapInsert(poppedDoc);
		this.table.put(uri, poppedDoc);
		return true;
	};

    private Function<URI, Boolean> noOp = uri -> true;

    private void updateNEWDOCCurrentMemoryOfBytesAndDocsInStore(DocumentImpl newDocument){
    	this.currentAmountOfDocsInStore++;
    	this.currentMemoryOfBytes = this.currentMemoryOfBytes + newDocument.getMemoryUsage();
	}

	private void updateREPLACEDDOCCurrentMemoryOfBytesAndDocsInStore(DocumentImpl docToBeOusted, DocumentImpl newDocument){
    	this.currentMemoryOfBytes = this.currentMemoryOfBytes - docToBeOusted.getMemoryUsage();
		this.currentMemoryOfBytes = this.currentMemoryOfBytes + newDocument.getMemoryUsage();
	}
	private void updateDELETEDOCCurrentMemoryOfBytesAndDocsInStore(DocumentImpl docToBeOusted){
    	this.currentAmountOfDocsInStore--;
    	this.currentMemoryOfBytes= this.currentMemoryOfBytes - docToBeOusted.getMemoryUsage();
	}

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
		this.limitCheck();
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
		this.limitCheck();
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
		List<Document> docs = this.trie.getAllSorted(this.keyword, new compareByKeyWord());
		for(Document d: docs){
			this.reHeapMe(d);
		}
		return docs;

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

		List<Document> docs = this.trie.getAllWithPrefixSorted(this.keyword, new compareByPrefix());

		for(Document d: docs){
			this.reHeapMe(d);
		}
		return docs;


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



	/**
	 * set maximum number of documents that may be stored
	 *
	 * @param limit
	 */
	@Override
	public void setMaxDocumentCount(int limit) {
		this.maxDocumentCount = limit;
		if((currentMemoryOfBytes > maxDocumentBytes) || (currentAmountOfDocsInStore > maxDocumentCount)){
			this.limitCheck();
		}
	}

	/**
	 * set maximum number of bytes of memory that may be used by all the documents in memory combined
	 *
	 * @param limit
	 */
	@Override
	public void setMaxDocumentBytes(int limit) {
		this.maxDocumentBytes = limit;
		if((currentMemoryOfBytes > maxDocumentBytes) || (currentAmountOfDocsInStore > maxDocumentCount)){
			this.limitCheck();
		}
	}


}











