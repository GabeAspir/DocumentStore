package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.impl.BTreeImpl;

import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;


import java.io.File;
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

    private BTreeImpl<URI, Document> Btree;
    private StackImpl<Undoable> myStack; //type undoable
    private HashMap<URI, StackImpl<DocumentImpl>> uriToStack;

    private TrieImpl<URI> trie;
	protected String keyword; //For Comparator
	private boolean deleteAllACTIVATED;
	private CommandSet<URI> commandSetForDeleteAll;

	private MinHeapImpl<DocImposter> minHeap;

	private int maxDocumentCount;
	private int maxDocumentBytes;
	private int currentAmountOfDocsInStore;
	private int currentMemoryOfBytes;

	private File baseDir;

    public DocumentStoreImpl(){
    	this.Btree = new BTreeImpl<>();
    	this.myStack = new StackImpl<>();
    	this.uriToStack = new HashMap<>();
    	this.trie = new TrieImpl<>();
    	this.keyword = null;
    	this.deleteAllACTIVATED = false;
    	this.commandSetForDeleteAll = new CommandSet<>();
		this.maxDocumentCount = Integer.MAX_VALUE;
    	this.maxDocumentBytes = Integer.MAX_VALUE;
    	this.minHeap = new MinHeapImpl<>();
    	this.currentAmountOfDocsInStore = 0;
    	this.currentMemoryOfBytes = 0;
		this.baseDir = new File(System.getProperty("user.dir"));
		Btree.setPersistenceManager(new DocumentPersistenceManager(this.baseDir));

    }

    public DocumentStoreImpl(File baseDir){
		this.Btree = new BTreeImpl<>();
		this.myStack = new StackImpl<>();
		this.uriToStack = new HashMap<>();
		this.trie = new TrieImpl<>();
		this.keyword = null;
		this.deleteAllACTIVATED = false;
		this.commandSetForDeleteAll = new CommandSet<>();
		this.maxDocumentCount = Integer.MAX_VALUE;
		this.maxDocumentBytes = Integer.MAX_VALUE;
		this.minHeap = new MinHeapImpl<>();
		this.currentAmountOfDocsInStore = 0;
		this.currentMemoryOfBytes = 0;
		this.baseDir = baseDir;
		Btree.setPersistenceManager(new DocumentPersistenceManager(baseDir));

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
			Document document = this.Btree.get(uri);


			if(document != null){
				Set<String> wordsInDeletedDoc = document.getWords();
				for(String s : wordsInDeletedDoc){
					this.trie.delete(s, uri);
				}

				try{ // able to reheap, then it was in mem
					this.reHeapMe(new DocImposter(document));
					this.heapReplace(new DocImposter(document), new DocImposter(newDoc));
					this.myStack.push(new GenericCommand(uri, undoPutReplace));
				}catch(NullPointerException e){ // if it came from disk, it'll throw a NPE
					this.heapInsert(new DocImposter(newDoc));
					this.myStack.push(new GenericCommand(uri, undoPutReplaceFromDisk));
				}


				Document previousDoc = this.Btree.put(uri, newDoc);
				this.limitCheck();
				return previousDoc.hashCode();
			}else{
				this.myStack.push(new GenericCommand(uri, undoPutNew));
				this.heapInsert(new DocImposter(newDoc));
				this.Btree.put(uri, newDoc);
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
				this.trie.put(s, uri);
			}
			Document document = this.Btree.get(uri);
			if(document != null){
				Set<String> wordsInDeletedDoc = document.getWords();
				for(String s : wordsInDeletedDoc){
					this.trie.delete(s, uri);
				}
				try{ // able to reheap, then it was in mem
					this.reHeapMe(new DocImposter(document));
					this.heapReplace(new DocImposter(document), new DocImposter(newDoc));
					this.myStack.push(new GenericCommand(uri, undoPutReplace));
				}catch(NullPointerException e){ // if it came from disk, it'll throw a NPE
					this.heapInsert(new DocImposter(newDoc));
					this.myStack.push(new GenericCommand(uri, undoPutReplaceFromDisk));
				}
				Document previousDoc = this.Btree.put(uri, newDoc);
				this.limitCheck();
				return previousDoc.hashCode();
			}else{
				this.myStack.push(new GenericCommand(uri, undoPutNew));
				this.heapInsert(new DocImposter(newDoc));
				this.Btree.put(uri, newDoc);
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
    	Document document = this.Btree.get(uri);
    	if(document == null){
    		return null;
		}
    	//Doc could have been from disk, or it could've been from mem.
		/*
		If it came from mem, just reHeapMe works fine
		if it came from disk, the heap won't have it. And therefore,
		have to Heap Insert and then limitCheck
		 */
		try{ //if it came from mem
			this.reHeapMe(new DocImposter(document));
		}catch(NullPointerException e){ // if it came from disk, it'll throw a NPE
			this.heapInsert(new DocImposter(document));
			this.limitCheck();
			//No undo statement needed
		}

		return document;
	}

	private class DocImposter implements Comparable<DocImposter>{

		private Document document;

		private DocImposter(Document document){
    		this.document = document;
		}

		@Override
		public int compareTo(DocImposter o) {
			Document doc1 = this.getDocumentofImposter();
			Document doc2 = o.getDocumentofImposter();
			return doc1.compareTo(doc2); //Will Compare it based on doc's compareTo
		}

		@Override
		public boolean equals(Object o) {
			if(this == o){
				return true;
			}
			if(o == null || getClass() != o.getClass()){
				return false;
			}
			DocImposter that = (DocImposter) o;
			if(this.document.equals(that.document)){
				return true;
			}else{
				return false;
			}

		}

		@Override
		public int hashCode() {
			return Objects.hash(document);
		}

		private Document getDocumentofImposter(){
			return this.document;
		}
		private void setDocumentofImposter(Document doc){
			this.document = doc;
		}
	}

	private void reHeapMe(DocImposter docImposter){
    	Document doc = docImposter.getDocumentofImposter();
    	doc.setLastUseTime(System.nanoTime());
    	docImposter.setDocumentofImposter(doc);
    	minHeap.reHeapify(docImposter);
	}
	private void heapInsert(DocImposter docImposter){
		Document document = docImposter.getDocumentofImposter();
		DocumentImpl doc =(DocumentImpl) document;
    	doc.setLastUseTime(System.nanoTime());
		docImposter.setDocumentofImposter(doc);
    	minHeap.insert(docImposter);
    	this.updateNEWDOCCurrentMemoryOfBytesAndDocsInStore(doc);
	}

	private void heapReplace(DocImposter docImposterToBeErased, DocImposter docImposterToBeInserted){
		Document docToBeErased = docImposterToBeErased.getDocumentofImposter();
    	docToBeErased.setLastUseTime(0);
    	docImposterToBeErased.setDocumentofImposter(docToBeErased);
    	minHeap.reHeapify(docImposterToBeErased);
    	minHeap.remove();
		Document docToBeInserted = docImposterToBeInserted.getDocumentofImposter();
		docToBeInserted.setLastUseTime(System.nanoTime());
		docImposterToBeInserted.setDocumentofImposter(docToBeInserted);
    	minHeap.insert(docImposterToBeInserted);
    	DocumentImpl docToBeErasedd = (DocumentImpl) docToBeErased;
    	DocumentImpl docToBeInsertedd = (DocumentImpl) docToBeInserted;
    	this.updateREPLACEDDOCCurrentMemoryOfBytesAndDocsInStore(docToBeErasedd, docToBeInsertedd);
	}

	private void heapDelete(DocImposter docImposter){
		Document document = docImposter.getDocumentofImposter();
		document.setLastUseTime(0);
		docImposter.setDocumentofImposter(document);
		minHeap.reHeapify(docImposter);
		minHeap.remove();
		this.updateDELETEDOCCurrentMemoryOfBytesAndDocsInStore((DocumentImpl) document);
	}

	private void limitCheck() {//Gonna have to do this after every put or undo
    	while((currentMemoryOfBytes > maxDocumentBytes) || (currentAmountOfDocsInStore > maxDocumentCount)){
			DocumentImpl document = (DocumentImpl) minHeap.remove().getDocumentofImposter(); //Removed from Heap
			URI uri = document.getKey();
			try {
				Btree.moveToDisk(uri); //Removes it from BTree
			} catch (Exception e) {
				e.printStackTrace();
			}
			//this.deleteDocument(uri); //STAGE5 don't need to Removed from Trie and HashTable
			this.updateDELETEDOCCurrentMemoryOfBytesAndDocsInStore(document);
			// this.myStack.pop(); //STAGE5 don't need this Will Take away the UNDO Command of this deleteDocument
			//Don't put in undo LOGIC STAGE5
			//Will null out all the documents that were ever at that URI
			//StackImpl<DocumentImpl> stack = this.uriToStack.get(uri);
			//StackImpl<DocumentImpl> documentStack = this.clearStackOfDoc(stack, document);
			//this.uriToStack.put(uri, documentStack);
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
    	boolean cameFromMem = true;
    	if(uri == null){
    		throw new IllegalArgumentException("null URI");
		}

    	if(this.Btree.get(uri) != null){
    		DocumentImpl gotDoc = (DocumentImpl) this.Btree.get(uri);

    		try{ // able to reheap, then it was in mem
    			this.reHeapMe(new DocImposter(gotDoc));
    			this.heapDelete(new DocImposter(gotDoc));
    			cameFromMem = true;
    		}catch(NullPointerException e){ // if it came from disk, it'll throw a NPE
    			cameFromMem = false;
    			//No undo statement needed
    		}


			Set<String> wordsInDoc = gotDoc.getWords();
			for(String s: wordsInDoc) {
				if(this.deleteAllACTIVATED){
				}else{
					this.trie.delete(s, uri);
				}
			}
    		this.uriToStack.get(uri).push(gotDoc);
		}
    	if(this.Btree.put(uri, null) != null){
    		if(this.deleteAllACTIVATED){
    			if(cameFromMem){
					this.commandSetForDeleteAll.addCommand(new GenericCommand(uri, undoDelete));
				}else{
					this.commandSetForDeleteAll.addCommand(new GenericCommand(uri, undoDeleteFromDisk));
				}

			}else{
    			if(cameFromMem){
					this.myStack.push(new GenericCommand(uri, undoDelete));
				}else{
					this.myStack.push(new GenericCommand(uri, undoDeleteFromDisk));
				}

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
			this.trie.delete(s, uri);
		}
		this.heapDelete(new DocImposter(poppedDoc));
		this.Btree.put(uri, null);
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
			this.trie.delete(s, uri);
		}
		Set<String> wordsInPeekedDoc = peekedDoc.getWords();
		for(String s : wordsInPeekedDoc){
			this.trie.put(s,uri);
		}
		this.heapReplace(new DocImposter(poppedDoc), new DocImposter(peekedDoc));

		this.Btree.put(uri, peekedDoc);
		return true;
	};


    private Function<URI, Boolean> undoPutReplaceFromDisk = uri -> {
		if(this.uriToStack.get(uri) == null){
			return true;
		}
		DocumentImpl poppedDoc = this.uriToStack.get(uri).pop();
		DocumentImpl peekedDoc = this.uriToStack.get(uri).peek();
		Set<String> wordsInPoppedDoc = poppedDoc.getWords();
		for(String s : wordsInPoppedDoc){
			this.trie.delete(s, uri);
		}
		Set<String> wordsInPeekedDoc = peekedDoc.getWords();
		for(String s : wordsInPeekedDoc){
			this.trie.put(s,uri);
		}
		this.heapReplace(new DocImposter(poppedDoc), new DocImposter(peekedDoc));

		this.Btree.put(uri, peekedDoc);
		try {
			this.Btree.moveToDisk(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

    	return true;
	};


    private Function<URI, Boolean> undoDelete = uri -> {
    	if(this.uriToStack.get(uri) == null){
    		return true;
		}
		DocumentImpl poppedDoc = this.uriToStack.get(uri).pop();
		Set<String> wordsInPoppedDoc = poppedDoc.getWords();
		for(String s : wordsInPoppedDoc){
			this.trie.put(s, uri);
		}
		this.heapInsert(new DocImposter(poppedDoc));
		this.Btree.put(uri, poppedDoc);
		return true;
	};

    private Function<URI, Boolean> undoDeleteFromDisk = uri -> {
		if(this.uriToStack.get(uri) == null){
			return true;
		}
		DocumentImpl poppedDoc = this.uriToStack.get(uri).pop();
		Set<String> wordsInPoppedDoc = poppedDoc.getWords();
		for(String s : wordsInPoppedDoc){
			this.trie.put(s, uri);
		}
		//Don't put it into Heap
		this.Btree.put(uri, poppedDoc);
		try {
			this.Btree.moveToDisk(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	private class compareByKeyWord implements Comparator<URI> {
		String keyword = DocumentStoreImpl.this.keyword;

		@Override
		public int compare(URI doc1, URI doc2) {
			int doc1Num = Btree.get(doc1).wordCount(keyword);
			int doc2Num = Btree.get(doc2).wordCount(keyword);

			return doc2Num - doc1Num;


		}

	}

	private class compareByPrefix implements Comparator<URI>{
		String keyword = DocumentStoreImpl.this.keyword;

		@Override
		public int compare(URI doc1, URI doc2) {
			int doc1Num = 0;
			int doc2Num = 0;
			Document doc11 = Btree.get(doc1);
			Document doc22 = Btree.get(doc2);
			if (doc11 instanceof DocumentImpl) {
				doc1Num = ((DocumentImpl) doc11).prefixWordCount(keyword);
			}
			if(doc22 instanceof  DocumentImpl){
				doc2Num = ((DocumentImpl) doc22).prefixWordCount(keyword);
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
		List<URI> uriList = this.trie.getAllSorted(this.keyword, new compareByKeyWord());
		List<Document> docs = new ArrayList<>();
		for(URI uri : uriList){
			docs.add(Btree.get(uri));
		}

		for(Document d: docs){
			this.reHeapMe(new DocImposter(d));
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

		List<URI> uriList = this.trie.getAllWithPrefixSorted(this.keyword, new compareByPrefix());
		List<Document> docs = new ArrayList<>();
		for(URI uri : uriList){
			docs.add(Btree.get(uri));
		}

		for(Document d: docs){
			this.reHeapMe(new DocImposter(d));
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

		Set<URI> deletedURIset = this.trie.deleteAll(keyword);
		Set<Document> deletedDocs = new HashSet<>();
		for(URI uri : deletedURIset){
			deletedDocs.add(Btree.get(uri));
		}

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
		Set<URI> deletedURIset = this.trie.deleteAllWithPrefix(keywordPrefix);
		Set<Document> deletedDocs = new HashSet<>();
		for(URI uri : deletedURIset){
			deletedDocs.add(Btree.get(uri));
		}
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











