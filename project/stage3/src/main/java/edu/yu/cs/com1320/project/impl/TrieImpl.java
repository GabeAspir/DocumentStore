package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value> {
    private final int alphabetSize = 91; //To accomodate for the usage of upper case letters and numbers
    private TrieNode root; // root of trie
    private Value valToBeDeleted;
    private Set<Value> setToBeDeleted;
    private Set<Value> setOfGottenValues;

    private class TrieNode<Value> {

        protected List<Value> collection;
        protected TrieNode[] links;

        private TrieNode() {
            this.links = new TrieNode[alphabetSize];
            this.collection = new ArrayList<>();
        }
    }

    public TrieImpl() {//Don't have to initialize the root to null, java does that for me
        this.root = new TrieNode();
        this.valToBeDeleted = null;
        this.setToBeDeleted = new HashSet<>();
        this.setOfGottenValues = new HashSet<>();
    }


    /**
     * add the given value at the given key
     *
     * @param key
     * @param val
     */
    @Override
    public void put(String key, Value val) {
        if (key == null || val == null) {
            throw new IllegalArgumentException("Null");
        }
        if (key.length() == 0) {
            return;
        }
        String fixedKey = fixString(key);
        this.root = put(this.root, fixedKey, val, 0);

    }

    private String fixString(String str) {
        String fixedString = str.replaceAll("[^a-zA-Z0-9\\s]", "").toUpperCase();
        return fixedString;
    }

    private TrieNode put(TrieNode node, String key, Value val, int d) {
        if(node == null){ //With each iteration we need to create a new node if there wasn't anything there before
            node = new TrieNode<>();
        }
        //Base Case
        if(d == key.length()){
            if(node.collection != null) {
                if(node.collection.contains(val)){
                    return node;
                }else{
                    node.collection.add(val);
                    return node;
                }
            }else{
                node.collection = new ArrayList();
                node.collection.add(val);
                return node;
            }
        }

        char c = key.charAt(d);

        node.links[c] = this.put(node.links[c], key, val, d + 1);
        return node;

    }

    /**
     * get all exact matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     *
     * @param key
     * @param comparator used to sort  values
     * @return a List of matching Values, in descending order
     */
    @Override
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        /*
        null checks
        Fix String
        Traverse through the trie to get the node, (create a private get method for the node)
        (To traverse it'll be very similar to private recursive put method)

        */
        // node.collection.sort(comparator);
        // tell it how to sort, from one list to another
        if(comparator == null){
            throw new IllegalArgumentException("Null Comparator");
        }
        if(key == null){
            throw new IllegalArgumentException("Null Key");
        }

        List<Value> empty = new ArrayList<>();

        if (key.length() == 0) {
            return empty;
        }
        String fixedKey = fixString(key);
        // Call private Get here
        TrieNode node = this.get(this.root, fixedKey, 0);
        if (node == null) {
            return empty;
        } else if (node.collection == null) {
            return empty;
        } else {
            node.collection.sort(comparator); // Don't need to set it to anything
            return node.collection;
        }
    }

    //Not putting any value, so you don't need the Value val
    private TrieNode get(TrieNode node, String key, int d) {
        // 2 base cases
        if (node == null) {//it missed the mark
            return null;
        }
        if (d == key.length()) { // found it!
            return node;
        }
        // recursion time!
        char c = key.charAt(d);

        return this.get(node.links[c], key, d + 1);

    }

    /**
     * get all matches which contain a String with the given prefix, sorted in descending order.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE INSENSITIVE.
     *
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order
     */
    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if(comparator == null){
            throw new IllegalArgumentException("Null Comparator");
        }
        if(prefix == null){
            throw new IllegalArgumentException("Null Prefix");
        }

        this.setOfGottenValues = new HashSet<>();

        List<Value> empty = new ArrayList<>();

        if(prefix.length() == 0){
            return empty;
        }

        String fixedKey = fixString(prefix);
        TrieNode prefixNode = this.get(this.root, fixedKey, 0);
        if (prefixNode == null) {
            return empty;
        }
        this.getAllWithPrefixSorted(prefixNode, new StringBuilder(fixedKey));

        ArrayList<Value> sortedList = new ArrayList<>();
        sortedList.addAll(this.setOfGottenValues);
        sortedList.sort(comparator);
        return sortedList;

    }

    private void getAllWithPrefixSorted(TrieNode node, StringBuilder prefix){
        //base case
        if(node.collection != null && !node.collection.isEmpty()){
            this.setOfGottenValues.addAll(node.collection);
        }

        for(char c = 0; c< this.alphabetSize; c++){
            if(node.links[c] != null){
                prefix.append(c);
                this.getAllWithPrefixSorted(node.links[c], prefix);
                prefix.deleteCharAt(prefix.length()-1);
            }
        }
    }




    /**
     * Delete the subtree rooted at the last character of the prefix.
     * Search is CASE INSENSITIVE.
     *
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Null Prefix");
        }
        this.setToBeDeleted = new HashSet<>();
        Set<Value> empty = new HashSet<>();

        if (prefix.length() == 0) {
            return empty;
        }
        String fixedKey = fixString(prefix);
        TrieNode prefixNode = this.get(this.root, fixedKey, 0);
        if (prefixNode == null) {
            return empty;
        }
        this.deleteAllWithPrefix(prefixNode, new StringBuilder(fixedKey));

        return this.setToBeDeleted;

    }

    private void deleteAllWithPrefix(TrieNode node, StringBuilder prefix){
        //base case
        if(node.collection != null && !node.collection.isEmpty()){
            this.setToBeDeleted.addAll(node.collection);
            node.collection = null;
        }

        for(char c = 0; c< this.alphabetSize; c++){
            if(node.links[c] != null){
                prefix.append(c);
                this.deleteAllWithPrefix(node.links[c], prefix);
                prefix.deleteCharAt(prefix.length()-1);
            }
        }


    }

    /**
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     *
     * @param key
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAll(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Null Key");
        }
        this.setToBeDeleted = new HashSet<>();
        Set<Value> empty = new HashSet<>();
        if (key == null) {
            return empty;
        }
        if (key.length() == 0) {
            return empty;
        }
        String fixedKey = fixString(key);
        this.root = deleteAll(this.root, fixedKey, 0);

        return this.setToBeDeleted;
    }

    private TrieNode deleteAll(TrieNode node, String key, int d){
        if (node == null) {
            return null;
        }
        if(d == key.length()){
            this.setToBeDeleted.addAll(node.collection);
            node.collection = new ArrayList();
        }else{
            char c = key.charAt(d);
            node.links[c] = this.deleteAll(node.links[c], key,d+1);
        }
        //Delete Subtree if need
        for (int c = 0; c < this.alphabetSize ; c++){
            if (node.links[c] != null)
            {
                return node; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }

    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     *
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    @Override
    public Value delete(String key, Value val) {
        if (key == null || val == null) {
            throw new IllegalArgumentException("Null");
        }
        this.valToBeDeleted = null;


        if (key.length() == 0) {
            return null;
        }
        String fixedKey = fixString(key);

        this.root = delete(this.root, fixedKey, val, 0);
        //Instance variable
        return this.valToBeDeleted;
    }

    private TrieNode delete(TrieNode node, String key, Value val, int d) {

        if(node == null){ //If there's a null node in the recursive iteration, that means there's no node with that value after.
            return null;
        }
        if (d == key.length()) {
            if(node.collection.contains(val)) {
                this.valToBeDeleted = val;
                node.collection.remove(val);
            }
        }else{
            char c = key.charAt(d);
            node.links[c] = this.delete(node.links[c], key, val, d+1);
        }

        if (node.collection != null && !node.collection.isEmpty() )
        {
            return node;
        }
        for (int c = 0; c < this.alphabetSize ; c++){
            if (node.links[c] != null)
            {
                return node; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;


    }




}
