package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;


public class StackImpl<T> implements Stack<T> {

	private class StackEntry<T>{
        private T value;
        private StackEntry<T> next;

        private StackEntry(T v){
            if(v == null){
                throw new IllegalArgumentException("Value is null");
            }
            this.value = v;
            this.next = null;
        }
    }

    private StackEntry[] stack;
    private int sizeOfStack;

    public StackImpl(){
    	this.stack = new StackEntry[1];
    	this.sizeOfStack = 0;
    } 

	/**
     * @param element object to add to the Stack
     */
	@Override
    public void push(T element){
    	// add to head of list

    	if(element == null){
    		throw new IllegalArgumentException("tried pushing a null element");
    	}
    	StackEntry<T> newEntry = new StackEntry<T>(element);

    	if(this.stack[0] == null){
    		this.stack[0] = newEntry;
    		this.sizeOfStack++;
    	}else{
    		newEntry.next = this.stack[0];
    		this.stack[0] = newEntry;
    		this.sizeOfStack++;
    	}
    }

    /**
     * removes and returns element at the top of the stack
     * @return element at the top of the stack, null if the stack is empty
     */
    @Override
    public T pop(){
    	// remove whatever head points to and return it

    	if(this.stack[0] == null){
    		return null;
    	}else{
    		StackEntry<T> poppedEntry = this.stack[0];
    		this.stack[0] = this.stack[0].next;
    		this.sizeOfStack--;
    		return poppedEntry.value;
    	}
    	
    }

    /**
     *
     * @return the element at the top of the stack without removing it
     */
    @Override
    public T peek(){
    	//just return whatever head is, but do not remove it from the data structure
    	// dont allow it to be changed
    	if(this.stack[0] == null){
    		return null;
    	}else{
    		StackEntry<T> peekedEntry = this.stack[0];
    		return peekedEntry.value;
    	}
    }

    /**
     *
     * @return how many elements are currently in the stack
     */
    @Override
    public int size(){
        //everytime push is called, plus one to an instance variable called int sizeOfStack
        //everytime pop is called, minus one to instance variable
        //this method just returns that instance variable
        return this.sizeOfStack;
    }


}




















