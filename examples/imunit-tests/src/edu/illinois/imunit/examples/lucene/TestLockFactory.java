package edu.illinois.imunit.examples.lucene;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;
import org.junit.Test;

import edu.illinois.imunit.Schedule;

@RunWith(IMUnitRunner.class)
public class TestLockFactory extends LuceneTestCase {

    // Verify: we can provide our own LockFactory implementation, the right
    // methods are called at the right time, locks are created, etc.

    // Verify: do stress test, by opening IndexReaders and
    // IndexWriters over & over in 2 threads and making sure
    // no unexpected exceptions are raised:
    @Test
    @Schedule(name = "stressLocks", value = "endWriter@writerThread->beforeCheck@main" + 
        ", endSearcher@searcherThread->beforeCheck@main")
    public void testStressLocks() throws Exception {
      _testStressLocks("stressLocks", null, _TestUtil.getTempDir("index.TestLockFactory6"));
    }

    // Verify: do stress test, by opening IndexReaders and
    // IndexWriters over & over in 2 threads and making sure
    // no unexpected exceptions are raised, but use
    // NativeFSLockFactory:
    @Test
    @Schedule(name = "stressLocksNativeFSLockFactory", value = "endWriter@writerThread->beforeCheck@main" + 
        ", endSearcher@searcherThread->beforeCheck@main")
    public void testStressLocksNativeFSLockFactory() throws Exception {
      File dir = _TestUtil.getTempDir("index.TestLockFactory7");
      _testStressLocks("stressLocksNativeFSLockFactory", new NativeFSLockFactory(dir), dir);
    }

    public void _testStressLocks(String scheduleName, LockFactory lockFactory, File indexDir) throws Exception {
        Directory dir = newFSDirectory(indexDir, lockFactory);

        // First create a 1 doc index:
        IndexWriter w = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer()).setOpenMode(OpenMode.CREATE));
        addDoc(w);
        w.close();

        WriterThread writer = new WriterThread(100, dir);
        writer.setName("writerThread");
        SearcherThread searcher = new SearcherThread(100, dir);
        searcher.setName("searcherThread");
        writer.start();
        searcher.start();

        //        while(writer.isAlive() || searcher.isAlive()) {
        //              Thread.sleep(5000);
        //}

        fireEvent("beforeCheck");
        assertTrue("IndexWriter hit unexpected exceptions", !writer.hitException);
        assertTrue("IndexSearcher hit unexpected exceptions", !searcher.hitException);

        dir.close();
        // Cleanup
        searcher.join();
        writer.join();
        _TestUtil.rmDir(indexDir);
    }

    private class WriterThread extends Thread { 
        private Directory dir;
        private int numIteration;
        public boolean hitException = false;
        public WriterThread(int numIteration, Directory dir) {
            this.numIteration = numIteration;
            this.dir = dir;
        }
        @Override
        public void run() {
            IndexWriter writer = null;
            for(int i=0;i<this.numIteration;i++) {
                try {
                    writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer()).setOpenMode(OpenMode.APPEND));
                } catch (IOException e) {
                    if (e.toString().indexOf(" timed out:") == -1) {
                        hitException = true;
                        System.out.println("Stress Test Index Writer: creation hit unexpected IOException: " + e.toString());
                        e.printStackTrace(System.out);
                    } else {
                        // lock obtain timed out
                        // NOTE: we should at some point
                        // consider this a failure?  The lock
                        // obtains, across IndexReader &
                        // IndexWriters should be "fair" (ie
                        // FIFO).
                    }
                } catch (Exception e) {
                    hitException = true;
                    System.out.println("Stress Test Index Writer: creation hit unexpected exception: " + e.toString());
                    e.printStackTrace(System.out);
                    break;
                }
                if (writer != null) {
                    try {
                        addDoc(writer);
                    } catch (IOException e) {
                        hitException = true;
                        System.out.println("Stress Test Index Writer: addDoc hit unexpected exception: " + e.toString());
                        e.printStackTrace(System.out);
                        break;
                    }
                    try {
                        writer.close();
                    } catch (IOException e) {
                        hitException = true;
                        System.out.println("Stress Test Index Writer: close hit unexpected exception: " + e.toString());
                        e.printStackTrace(System.out);
                        break;
                    }
                    writer = null;
                }
            }
            fireEvent("endWriter");
        }
    }

    private class SearcherThread extends Thread { 
        private Directory dir;
        private int numIteration;
        public boolean hitException = false;
        public SearcherThread(int numIteration, Directory dir) {
            this.numIteration = numIteration;
            this.dir = dir;
        }
        @Override
        public void run() {
            IndexSearcher searcher = null;
            Query query = new TermQuery(new Term("content", "aaa"));
            for(int i=0;i<this.numIteration;i++) {
                try{
                    searcher = new IndexSearcher(dir, false);
                } catch (Exception e) {
                    hitException = true;
                    System.out.println("Stress Test Index Searcher: create hit unexpected exception: " + e.toString());
                    e.printStackTrace(System.out);
                    break;
                }
                try {
                  searcher.search(query, null, 1000);
                } catch (IOException e) {
                  hitException = true;
                  System.out.println("Stress Test Index Searcher: search hit unexpected exception: " + e.toString());
                  e.printStackTrace(System.out);
                  break;
                }
                // System.out.println(hits.length() + " total results");
                try {
                  searcher.close();
                } catch (IOException e) {
                  hitException = true;
                  System.out.println("Stress Test Index Searcher: close hit unexpected exception: " + e.toString());
                  e.printStackTrace(System.out);
                  break;
                }
            }
            fireEvent("endSearcher");
        }
    }

    @RunWith(IMUnitRunner.class)
public class MockLockFactory extends LockFactory {

        public boolean lockPrefixSet;
        public Map<String,Lock> locksCreated = Collections.synchronizedMap(new HashMap<String,Lock>());
        public int makeLockCount = 0;

        @Override
        public void setLockPrefix(String lockPrefix) {    
            super.setLockPrefix(lockPrefix);
            lockPrefixSet = true;
        }

        @Override
        synchronized public Lock makeLock(String lockName) {
            Lock lock = new MockLock();
            locksCreated.put(lockName, lock);
            makeLockCount++;
            return lock;
        }

        @Override
        public void clearLock(String specificLockName) {}

        @RunWith(IMUnitRunner.class)
public class MockLock extends Lock {
            public int lockAttempts;

            @Override
            public boolean obtain() {
                lockAttempts++;
                return true;
            }
            @Override
            public void release() {
                // do nothing
            }
            @Override
            public boolean isLocked() {
                return false;
            }
        }
    }

    private void addDoc(IndexWriter writer) throws IOException {
        Document doc = new Document();
        doc.add(newField("content", "aaa", Field.Store.NO, Field.Index.ANALYZED));
        writer.addDocument(doc);
    }
}
