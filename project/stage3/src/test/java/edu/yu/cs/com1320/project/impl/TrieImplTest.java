package edu.yu.cs.com1320.project.impl;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage3.impl.DocumentStoreImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage3.impl.DocumentImpl;

public class TrieImplTest {

    //variables to hold possible values for doc1
    private URI uri1;
    private String txt1;

    //variables to hold possible values for doc2
    private URI uri2;
    String txt2;

    private URI uri3;
    String txt3;

    @BeforeEach
    public void init() throws Exception {
        //init possible values for doc1
        this.uri1 = new URI("http://edu.yu.cs/com1320/project/doc1");
        this.txt1 = "Apple Apple Pizza Fish Pie Pizza Apple";

        //init possible values for doc2
        this.uri2 = new URI("http://edu.yu.cs/com1320/project/doc2");
        this.txt2 = "Pizza Pizza Pizza Pizza Pizza";

        //init possible values for doc3
        this.uri3 = new URI("http://edu.yu.cs/com1320/project/doc3");
        this.txt3 = "Penguin Park Piccalo Pants Pain Possum";
    }

    @Test
    public void basicSearchAndOrganizationTest() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt3.getBytes()),this.uri3, DocumentStore.DocumentFormat.TXT);
        assertEquals(1, store.search("PiE").size());
        assertEquals(3, store.searchByPrefix("p").size());
        assertEquals(0, store.searchByPrefix("x").size());
        assertEquals(3, store.searchByPrefix("pi").size());
        assertEquals(5, store.search("PiZzA").get(0).wordCount("pizza"));
        assertEquals(6, store.searchByPrefix("p").get(0).getWords().size());
    }
    @Test
    public void basicSearchDeleteTest() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt3.getBytes()),this.uri3, DocumentStore.DocumentFormat.TXT);
        assertEquals(1, store.search("PiE").size());
        assertEquals(3, store.searchByPrefix("p").size());
        assertEquals(1, store.search("possum").size());
        store.deleteDocument(this.uri3);
        DocumentImpl doc1 = new DocumentImpl(this.uri1, this.txt1);
        DocumentImpl doc2 = new DocumentImpl(this.uri2, this.txt2);
        DocumentImpl doc3 = new DocumentImpl(this.uri3, this.txt3);
        for (char c = 'a'; c<='z'; c++) {
            List<Document> list = store.searchByPrefix(Character.toString(c));
            if (list.size()!=0) {
                assertNotEquals(doc3, list.get(0));
                if ((!list.get(0).equals(doc1))&&(!list.get(0).equals(doc2))) {
                    fail();
                }
            }
        }
        for (char c = '0'; c<='9'; c++) {
            List<Document> list = store.searchByPrefix(Character.toString(c));
            if (list.size()!=0) {
                assertNotEquals(doc3, list.get(0));
                if ((!list.get(0).equals(doc1))&&(!list.get(0).equals(doc2))) {
                    fail();
                }
            }
        }
        assertEquals(0, store.search("possum").size());
        assertEquals(2, store.search("pizza").size());
        store.deleteDocument(this.uri2);
        assertEquals(1, store.search("pizza").size());
    }
    @Test
    public void basicPutOverwriteTest() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        assertEquals(2, store.search("pizza").size());
        store.putDocument(new ByteArrayInputStream(this.txt3.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        assertEquals(1, store.search("pizza").size());
    }
    @Test
    public void testDeleteAndDeleteAll() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt3.getBytes()),this.uri3, DocumentStore.DocumentFormat.TXT);
        assertEquals(2, store.search("pizza").size());
        store.deleteAll("PiZZa");
        assertEquals(0, store.search("pizza").size());
        assertNull(store.getDocument(this.uri1));
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        assertEquals(2, store.search("pizza").size());
        assertNotNull(store.getDocument(this.uri1));assertNotNull(store.getDocument(this.uri2));assertNotNull(store.getDocument(this.uri3));
        store.deleteAllWithPrefix("p");
        assertNull(store.getDocument(this.uri1));assertNull(store.getDocument(this.uri2));assertNull(store.getDocument(this.uri3));
    }
    @Test
    public void testUndoNoArgs() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt3.getBytes()),this.uri3, DocumentStore.DocumentFormat.TXT);
        store.undo();
        assertEquals(null, store.getDocument(this.uri3));
        assertEquals(0, store.search("penguin").size());
        store.putDocument(new ByteArrayInputStream(this.txt3.getBytes()),this.uri3, DocumentStore.DocumentFormat.TXT);
        store.deleteAll("pizza");
        assertEquals(0, store.search("pizza").size());
        assertNull(store.getDocument(this.uri1));
        store.undo();
        assertEquals(2, store.search("pizza").size());
    }
    @Test
    public void testUndoWithArgs() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt3.getBytes()),this.uri3, DocumentStore.DocumentFormat.TXT);
        assertEquals(1, store.search("apple").size());
        assertEquals(1, store.searchByPrefix("a").size());
        store.undo(this.uri1);
        assertEquals(0, store.search("apple").size());
        assertEquals(0, store.searchByPrefix("a").size());
    }
    @Test
    public void wordCountAndGetWordsTest() throws URISyntaxException {
        DocumentImpl txtDoc = new DocumentImpl(new URI("placeholder"), " The!se ARE? sOme   W@o%$rds with^ s**ymbols (m)ixed [in]. Hope    this test test passes!");
        assertEquals(0, txtDoc.wordCount("bundle"));
        assertEquals(1, txtDoc.wordCount("these"));
        assertEquals(1, txtDoc.wordCount("WORDS"));
        assertEquals(1, txtDoc.wordCount("S-Y-M-B-O-??-LS"));
        assertEquals(1, txtDoc.wordCount("p@A$$sse$s"));
        assertEquals(2, txtDoc.wordCount("tEst"));
        Set<String> words = txtDoc.getWords();
        assertEquals(12, words.size());


        DocumentImpl binaryDoc = new DocumentImpl(new URI("0110"), new byte[] {0,1,1,0});
        assertEquals(0, binaryDoc.wordCount("anythingYouPutHereShouldBeZero"));
        Set<String> words2 = binaryDoc.getWords();
        assertEquals(0, words2.size());
    }
    @Test
    public void testUndoCommandSet() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        assertEquals(2, store.deleteAll("pizza").size());
        store.putDocument(new ByteArrayInputStream(this.txt3.getBytes()),this.uri3, DocumentStore.DocumentFormat.TXT);
        assertNotNull(store.getDocument(this.uri3));
        assertEquals(0, store.search("pizza").size());
        store.undo(uri1);
        assertEquals(1, store.search("pizza").size());
        assertEquals(4, store.search("pizza").get(0).getWords().size());
        store.undo(uri2);
        assertEquals(2, store.search("pizza").size());
        assertEquals(1, store.search("pizza").get(0).getWords().size());
        store.undo();
        assertNull(store.getDocument(this.uri3));
        assertEquals(0, store.search("penguin").size());
    }
    @Test
    public void testUndoCommandSet2() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        store.deleteAll("pizza");
        assertEquals(0, store.search("pizza").size());
        store.undo(uri2);
        assertEquals(1, store.search("pizza").size());
        store.undo(uri2);
        assertEquals(0, store.search("pizza").size());
        boolean test = false;
        try {
            store.undo(uri2);
        } catch (IllegalStateException e) {
            test = true;
        }
        assertTrue(test);
        assertEquals(0, store.search("pizza").size());
        store.undo(uri1);
        assertEquals(1, store.searchByPrefix("app").size());
        assertEquals(1, store.search("pizza").size());
    }
    @Test
    public void removeCommandSet() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        store.putDocument(new ByteArrayInputStream(this.txt1.getBytes()),this.uri1, DocumentStore.DocumentFormat.TXT);
        store.putDocument(new ByteArrayInputStream(this.txt2.getBytes()),this.uri2, DocumentStore.DocumentFormat.TXT);
        store.deleteAll("pizza");
        assertEquals(0, store.search("pizza").size());
        store.undo(uri2);
        assertEquals(1, store.search("pizza").size());
        store.undo(uri1);
        assertEquals(2, store.search("pizza").size());
        store.undo();
        assertNull(store.getDocument(uri2));
        assertNotNull(store.getDocument(uri1));
        assertEquals(1, store.search("pizza").size());
    }

    @Test
    public void simpleTrieTest() {
        Trie trie = new TrieImpl<Integer>();
        trie.put("APPLE123", 1);
        trie.put("APPLE123", 2);
        trie.put("APPLE123", 3);
        trie.put("WORD87", 8);
        trie.put("WORD87", 7);

        List<Integer> apple123List = trie.getAllSorted("apple123", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});//this comparator will order integers from lowest to highest
        List<Integer> word87List = trie.getAllSorted("word87", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});

        assertEquals(3, apple123List.size());
        assertEquals(2, word87List.size());
        assertEquals(1, apple123List.get(0));
        assertEquals(2, apple123List.get(1));
        assertEquals(3, apple123List.get(2));
        assertEquals(7, word87List.get(0));
        assertEquals(8, word87List.get(1));

        trie.put("app", 12);
        trie.put("app", 5);
        trie.put("ap", 4);

        List<Integer> apList = trie.getAllWithPrefixSorted("AP", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});
        List<Integer> appList = trie.getAllWithPrefixSorted("APP", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});

        assertEquals(6, apList.size());
        assertEquals(5, appList.size());
        assertEquals(12, apList.get(5));
        assertEquals(12, appList.get(4));

        Set<Integer> deletedAppPrefix = trie.deleteAllWithPrefix("aPp");
        assertEquals(5, deletedAppPrefix.size());
        assertTrue(deletedAppPrefix.contains(3));
        assertTrue(deletedAppPrefix.contains(5));

        apList = trie.getAllWithPrefixSorted("AP", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});
        appList = trie.getAllWithPrefixSorted("APP", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});

        assertEquals(1, apList.size());
        assertEquals(0, appList.size());

        trie.put("deleteAll", 100);
        trie.put("deleteAll", 200);
        trie.put("deleteAll", 300);

        List<Integer> deleteList = trie.getAllSorted("DELETEALL", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});

        assertEquals(3, deleteList.size());
        Set<Integer> thingsActuallyDeleted = trie.deleteAll("DELETEall");
        assertEquals(3, thingsActuallyDeleted.size());
        assertTrue(thingsActuallyDeleted.contains(100));

        deleteList = trie.getAllSorted("DELETEALL", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});

        assertEquals(0, deleteList.size());

        trie.put("deleteSome", 100);
        trie.put("deleteSome", 200);
        trie.put("deleteSome", 300);

        List<Integer> deleteList2 = trie.getAllSorted("DELETESOME", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});

        assertEquals(3, deleteList2.size());
        Integer twoHundred = (Integer) trie.delete("deleteSome", 200);
        Integer nullInt = (Integer) trie.delete("deleteSome", 500);

        assertEquals(200, twoHundred);
        assertNull(nullInt);

        deleteList2 = trie.getAllSorted("DELETESOME", (int1, int2) -> {
            if ((int) int1 < (int) int2) {
                return -1;
            } else if ((int) int2 < (int) int1) {
                return 1;
            }
            return 0;});

        assertEquals(2, deleteList2.size());
        assertFalse(deleteList2.contains(200));
    }
}