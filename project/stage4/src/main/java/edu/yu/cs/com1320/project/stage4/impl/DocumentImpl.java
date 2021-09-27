package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.stage4.Document;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;



public class DocumentImpl implements Document {
	private URI uri;
	private String txt;
	private byte[] binaryData;
	private String[] allWordsAsStringArray;
	private Set<String> wordsInDoc;
	private HashMap<String, Integer> hashedWordsinDoc;
	private long lastUsedTime;
	private int memoryUsage;



	public DocumentImpl(URI uri, String txt){ //txtDoc
		if(uri == null){
            throw new IllegalArgumentException("URI is null");
        }
        if(txt == null){
            throw new IllegalArgumentException("txt is null");
        }

        if(txt.length() == 0){
        	throw new IllegalArgumentException("txt is empty");
        }

        if(uri.toString().length() == 0){
        	throw new IllegalArgumentException("URI is null");
        }

		this.uri = uri;
		this.txt = txt;
		this.binaryData = null;
		this.allWordsAsStringArray = stringToFixedStringArray(this.txt);
		this.wordsInDoc = this.setWords();
		this.hashedWordsinDoc = this.setHashMap();
		this.memoryUsage = txt.getBytes().length;

		
	}

	public DocumentImpl(URI uri, byte[] binaryData){ //byteArray Doc

		if(uri == null){
            throw new IllegalArgumentException("URI is null");
        }
        if(binaryData == null){
            throw new IllegalArgumentException("binaryData is null");
        }
        if(binaryData.length == 0){
        	throw new IllegalArgumentException("binaryData is empty");
        }

        if(uri.toString().length() == 0){
        	throw new IllegalArgumentException("URI is null");
        }

		this.uri = uri;
		this.binaryData = binaryData;
		this.txt = null;
		this.wordsInDoc = null;
		this.allWordsAsStringArray = null;
		this.hashedWordsinDoc = null;
		this.memoryUsage = binaryData.length;
	}


	/**
     * @return content of text document
     */
    public String getDocumentTxt(){
    	return this.txt;
    }

    /**
     * @return content of binary data document
     */
    public byte[] getDocumentBinaryData(){
    	return this.binaryData;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    public URI getKey(){
    	return this.uri;
    }


	@Override
	public int hashCode() {
	int result = uri.hashCode();
	result = 31 * result + (txt != null ? txt.hashCode() : 0);
	result = 31 * result + Arrays.hashCode(binaryData);
	return result;
	}


	@Override
	public boolean equals(Object obj){
		if(obj instanceof DocumentImpl){
			DocumentImpl document = (DocumentImpl) obj;
			if(document.hashCode() == this.hashCode()){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}

	private String fixKeyWord(String str) {
		String fixedKeyWord = str.replaceAll("[^a-zA-Z0-9\\s]", "").toUpperCase();
		return fixedKeyWord;
	}

	/**
	 * how many times does the given word appear in the document?
	 * @param word
	 * @return the number of times the given words appears in the document. If it's a binary document, return 0.
	 */
	public int wordCount(String word){
		if(this.binaryData != null){ //meaning its a binaryDoc and not a txt
			return 0;
		}else{
			try{
				String keyword = this.fixKeyWord(word);
				return this.hashedWordsinDoc.get(keyword);
			}catch(NullPointerException e){
				return 0;
			}


		}
	}

	protected int prefixWordCount(String prefix){
		int i = 0;
		if(this.binaryData != null) { //meaning its a binaryDoc and not a txt
			return 0;
		}else{
			String keyword = this.fixKeyWord(prefix);
			for(String s : this.allWordsAsStringArray){
				if(s.startsWith(keyword)){
					i++;
				}
			}
			return i;
		}
	}


	protected int getMemoryUsage(){
		return this.memoryUsage;
	}


	private HashMap<String, Integer> setHashMap(){
		HashMap<String, Integer> mappedWords = new HashMap<>();
		String[] words = this.allWordsAsStringArray;
		for(int i = 0; i < words.length; i++){
			if(mappedWords.containsKey(words[i])){
				mappedWords.put(words[i], (mappedWords.get(words[i]) + 1) );
			}else{
				mappedWords.put(words[i], 1);
			}
		}
		return mappedWords;
	}


	/**
	 * @return all the words that appear in the document
	 */
	public Set<String> getWords(){
		if(this.binaryData != null){ //meaning its a binaryDoc and not a txt
			Set<String> emptySet = new HashSet<>();
			return emptySet;
		}else{
			return this.wordsInDoc;
		}

	}

	/**
	 * return the last time this document was used, via put/get or via a search result
	 * (for stage 4 of project)
	 */
	@Override
	public long getLastUseTime() {
		return this.lastUsedTime;
	}

	@Override
	public void setLastUseTime(long timeInNanoseconds) {
		this.lastUsedTime = timeInNanoseconds;
	}

	private Set<String> setWords(){
		Set<String> setWords = new HashSet<>();
		String[] words = this.allWordsAsStringArray;
		for(int i = 0; i < words.length; i++){
			if(!(setWords.contains(words[i]))){
				setWords.add(words[i]);
			}
		}
		return setWords;
	}

	private String[] stringToFixedStringArray(String str) {

		String fixedString = str.replaceAll("[^a-zA-Z0-9\\s]", "").toUpperCase().trim();
		String[] words = fixedString.split("\\s+");
		return words;
	}



	@Override
	public int compareTo(Document doc2) {
		if(this.getLastUseTime() > doc2.getLastUseTime()){
			return 1;
		}else if(this.getLastUseTime() < doc2.getLastUseTime()){
			return -1;
		}else{
			return 0;
		}
	}
}