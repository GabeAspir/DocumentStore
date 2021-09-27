package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.stage2.Document;

import java.net.URI;
import java.util.Arrays;

public class DocumentImpl implements Document {
	private URI uri;
	private String txt;
	private byte[] binaryData;



	public DocumentImpl(URI uri, String txt){
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

		
	}

	public DocumentImpl(URI uri, byte[] binaryData){

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




}