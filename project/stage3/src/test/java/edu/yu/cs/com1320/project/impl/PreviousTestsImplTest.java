package edu.yu.cs.com1320.project.impl;

import static org.junit.jupiter.api.Assertions.*;
import edu.yu.cs.com1320.project.stage3.*;
import edu.yu.cs.com1320.project.stage3.impl.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import edu.yu.cs.com1320.project.impl.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;

import edu.yu.cs.com1320.project.stage3.impl.DocumentStoreImpl;
import org.junit.jupiter.api.Test;

import edu.yu.cs.com1320.project.stage3.DocumentStore.DocumentFormat;
class PreviousTestsImplTest {
    private URI textUri;
    private String textString;

    private URI binaryUri;
    private byte[] binaryData;

    @BeforeEach
    public void setUp() throws Exception {
        this.textUri = new URI("http://edu.yu.cs/com1320/txt");
        this.textString = "This is text content. Lots of it.";

        this.binaryUri = new URI("http://edu.yu.cs/com1320/binary");
        this.binaryData = "This is a PDF, brought to you by Adobe.".getBytes();
    }



    @Test
    public void constructorExists() {
        try {
            new DocumentStoreImpl();
        } catch (Exception e) {}
    }

    @Test
    public void putDocumentExists() throws URISyntaxException{
        try {
            new DocumentStoreImpl().putDocument(null, new URI("hi"), DocumentFormat.BINARY);
        } catch (Exception e) {}
    }

    @Test
    public void getDocumentExists() throws URISyntaxException{
        try {
            new DocumentStoreImpl().getDocument(new URI("hi"));
        } catch (Exception e) {}
    }

    @Test
    public void deleteDocumentExists() throws URISyntaxException {
        try {
            new DocumentStoreImpl().deleteDocument(new URI("hi"));
        } catch (Exception e) {}
    }

    @Test
    public void testGetTextDocumentAsTxt() {
        DocumentImpl textDocument = new DocumentImpl(this.textUri, this.textString);
        assertEquals(this.textString, textDocument.getDocumentTxt());
    }

    @Test
    public void testGetDocumentBinaryData() {
        DocumentImpl binaryDocument = new DocumentImpl(this.binaryUri, this.binaryData);
        assertArrayEquals(this.binaryData,binaryDocument.getDocumentBinaryData());
    }

    @Test
    public void testGetTextDocumentTextHashCode() {
        DocumentImpl textDocument = new DocumentImpl(this.textUri, this.textString);
        int code = Utils.calculateHashCode(this.textUri, this.textString,null);
        assertEquals(code, textDocument.hashCode());
    }

    @Test
    public void testGetBinaryDocumentTextHashCode() {
        DocumentImpl binaryDocument = new DocumentImpl(this.binaryUri, this.binaryData);
        int code = Utils.calculateHashCode(this.binaryUri, null, this.binaryData);
        assertEquals(code, binaryDocument.hashCode());
    }

    @Test
    public void testGetTextDocumentKey() {
        DocumentImpl textDocument = new DocumentImpl(this.textUri, this.textString);
        assertEquals(this.textUri, textDocument.getKey());
    }

    @Test
    public void testGetBinaryDocumentKey() {
        DocumentImpl binaryDocument = new DocumentImpl(this.binaryUri, this.binaryData);
        assertEquals(this.binaryUri, binaryDocument.getKey());
    }



    @Test
    public void constructor1Exists() throws URISyntaxException {
        URI uri = new URI("https://this.com");
        try {
            new DocumentImpl(uri, "hi");
        } catch (RuntimeException e) {}
    }

    @Test
    public void constructor2Exists() throws URISyntaxException {
        URI uri = new URI("https://this.com");
        byte[] ary = {0,0,0};
        try {
            new DocumentImpl(uri, ary );
        } catch (RuntimeException e) {}
    }

    @Test
    public void getDocumentBinaryDataExists() throws URISyntaxException{
        URI uri = new URI("https://this.com");
        try {
            new DocumentImpl(uri, "hi".getBytes()).getDocumentBinaryData();
        } catch (RuntimeException e) {}
    }

    @Test
    public void getDocumentTxtExists() throws URISyntaxException{
        URI uri = new URI("https://this.com");
        try {
            new DocumentImpl(uri, "hi").getDocumentTxt();
        } catch (RuntimeException e) {}
    }

    @Test
    public void getKeyExists() throws URISyntaxException {
        URI uri = new URI("https://this.com");
        try {
            new DocumentImpl(uri, "hi").getKey();
        } catch (RuntimeException e) {}
    }




}