package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinHeapImplTest {

    public class minHeapPracticeEntry implements Comparable<minHeapPracticeEntry> {
        private int value;
        private char character;

        public minHeapPracticeEntry(char c, int value){
            this.value = value;
            this.character = c;
        }


        @Override
        public int compareTo(minHeapPracticeEntry o) {
            if(this.value>o.value){
                return 1;
            }else if(this.value<o.value){
                return -1;
            }else{
                return 0;
            }
        }
        public int getValue(){
            return this.value;
        }
        public void setValue(int v){
            this.value = v;
        }
    }





    @Test
    void reHeapify() {
        MinHeapImpl<minHeapPracticeEntry> minHeap = new MinHeapImpl<>();
        minHeap.insert(new minHeapPracticeEntry('a',1));
        minHeap.insert(new minHeapPracticeEntry('b',6));
        minHeap.insert(new minHeapPracticeEntry('c',4));
        minHeap.insert(new minHeapPracticeEntry('d',17));
        minHeapPracticeEntry e = new minHeapPracticeEntry('e',18);
        minHeap.insert(e);
        minHeap.insert(new minHeapPracticeEntry('i',100));
        minHeap.insert(new minHeapPracticeEntry('f',82));
        minHeap.insert(new minHeapPracticeEntry('g',2));
        minHeap.insert(new minHeapPracticeEntry('h',55));
        e.setValue(3);
        minHeap.reHeapify(e);
        minHeap.remove();
        minHeap.remove();
        minHeapPracticeEntry removed = (minHeapPracticeEntry) minHeap.remove();
        assertEquals(removed.value, 3);
    }

}