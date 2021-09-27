package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {



    public MinHeapImpl() {
        this.elements = (E[]) new Comparable[5];

    }



    @Override
    public void reHeapify(E element) {
        if(element == null){
            throw new IllegalArgumentException("No Nulls! Cmon!");
        }
        int spot = this.getArrayIndex(element);
        this.upHeap(spot);
        this.downHeap(spot);
    }


    @Override
    protected int getArrayIndex(E element) {
        if(element == null){
            throw new IllegalArgumentException("No Nulls! Cmon!");
        }
        for(int i = 1; i<= this.elements.length; i++){
            if(this.elements[i].equals(element)){
                return i;
            }
        }
        throw new NoSuchElementException("No such element in the Heap");
    }

    @Override
    protected void doubleArraySize() {
        E[] newArray = (E[]) new Comparable[this.elements.length * 2];
        for(int i = 1; i < this.elements.length ; i++){
            newArray[i] = this.elements[i];
        }
        this.elements = newArray;
    }


}
