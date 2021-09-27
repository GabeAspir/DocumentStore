package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

public class HashTableImpl<Key, Value> implements HashTable<Key,Value> {



    private class Entry<Key, Value>{
        private Key key;
        private Value value;
        private Entry<Key, Value> next;



        private Entry(Key k, Value v){
            if(k == null){
                throw new IllegalArgumentException("Key is null");
            }
            this.key = k;
            this.value = v;
            this.next = null;
        }
    }

    private Entry<?,?>[] table;

    public HashTableImpl(){
        this.table = new Entry[5];
    }

    private int hashFunction(Key key)
    {
        return (key.hashCode() & 0x7fffffff) % this.table.length;
    }


    /**
     * @param k the key whose value should be returned
     * @return the value that is stored in the HashTable for k, or null if there is no such key in the table
     */
    @Override
    public Value get(Key k){
        if(k == null){
            throw new IllegalArgumentException("Null key");
        }
        int index = this.hashFunction(k);
        Entry current = this.table[index]; // pointer
        while(current != null){
            if(current.key.equals(k)){
                return (Value)current.value;
            }else{
                current = current.next;

            }
        }
        return null;//when current is null meaning, there was no such key in the table
    }


    /**
     * @param k the key at which to store the value
     * @param v the value to store.
     * To delete an entry, put a null value.
     * @return if the key was already present in the HashTable, return the previous value stored for the key. If the key was not already present, return null.
     */
    public Value put(Key k, Value v) {

        int index = this.hashFunction(k);
        if (this.table[index] == null) {
            if (v == null) {// means key wasn't present
                return null;
            }
            Entry<Key, Value> newEntry = new Entry<Key, Value>(k, v);
            this.table[index] = newEntry;
            return null;
        }
        if (v == null) {
            try {//Definitely check this try catch
                Value deleted = (Value) this.delete(k, index);
                return deleted;
            } catch (NullPointerException e) {
                return null;
            }
        } else {
            Entry current = this.table[index];
            if(current.key.equals(k)){
                Value replacedValue = (Value) current.value;
                current.value = v;
                return replacedValue;
            }
            while (current.next != null && !current.next.key.equals(k)) {
                current = current.next;
            }
            if (current.next == null) {//reached the end
                Entry<Key, Value> newEntry = new Entry<Key, Value>(k, v);
                current.next = newEntry;
                return null;
            }
            if (current.next.key.equals(k)) {//found a good looking key
                Value returnedValue = (Value) current.next.value;
                current.next.value = v;
                return returnedValue;
            }

        }
        return null;
    }



    private Value delete(Key k, int index){

        Entry current = this.table[index];

        if(current.key.equals(k) && current.next== null){
            Entry old = this.table[index];
            this.table[index] = null;
            return (Value)old.value;
        }else if(current.key.equals(k) && current.next!= null){
            Entry old = this.table[index];
            this.table[index] = current.next;
            return (Value)old.value;
        }else if(current != null){
            while(current.next != null && !current.next.key.equals(k)){
                current = current.next;
            }
            if(current.next != null){
                Entry old = current.next;
                current.next = current.next.next;
                return (Value)old.value;
            }
            if(current.next == null){ //couldn't delete bc key was not present
                return null;
            }
        }
        return null;
    }

}









