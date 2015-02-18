package edu.illinois.imunit.examples.blockingbuffer;

import java.util.LinkedList;

/**
 * A simple blocking buffer implementation backed by a {@link LinkedList}.
 * 
 * 
 * @author Vilas Jagannath (vbangal2@illinois.edu), <br/>
 *         Milos Gligoric (gliga@illinois.edu), <br/>
 *         Dongyun Jin (djin3@illinois.edu), <br/>
 *         Qingzhou Luo (qluo2@illinois.edu).
 * 
 */
public class BlockingBuffer<T> {

    private LinkedList<T> buffer;

    private int max;

    public BlockingBuffer(int max) {
        this.max = max;
        buffer = new LinkedList<T>();
    }

    public synchronized void add(T e) {
        this.put(e);
    }

    public synchronized void put(T e) {
        while (buffer.size() >= max) {
            try {
                wait();
            } catch (Exception exp) {
            }
        }
        buffer.addLast(e);
        notifyAll();
    }

    public synchronized T remove() {
        return get();
    }

    public synchronized T get() {
        while (buffer.size() <= 0) {
            try {
                wait();
            } catch (Exception exp) {
            }
        }
        T ret = buffer.removeFirst();
        notifyAll();
        return ret;
    }

}
