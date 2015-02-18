#!/bin/bash

# args validation
if [ $# -lt 1 ]; then
    echo "usage: " $0 "test_class|all"
    echo "example: " $0 " edu.illinois.imunit.examples.jbosscache.ReadWriteLockWithUpgradeTest"    
    exit 1
fi

# variables
test_class=$1
imunit_dir=../imunit-light
all_test_classes="edu.illinois.imunit.examples.jbosscache.ReadWriteLockWithUpgradeTest
edu.illinois.imunit.examples.jbosscache.IdentityLockTest
edu.illinois.imunit.examples.jbosscache.NonBlockingWriterLockTest
edu.illinois.imunit.examples.jbosscache.LockTest
edu.illinois.imunit.examples.jbosscache.ReentrantWriterPreferenceReadWriteLockTest
edu.illinois.imunit.examples.sysunit.TaskQueueTest
edu.illinois.imunit.examples.sysunit.SynchronizerTest
edu.illinois.imunit.examples.sysunit.CommandGroupTest
edu.illinois.imunit.examples.sysunit.BarrierTest
edu.illinois.imunit.examples.apache.pool.TestGenericObjectPool
edu.illinois.imunit.examples.apache.pool.TestGenericKeyedObjectPool
edu.illinois.imunit.examples.lucene.TestLockFactory
edu.illinois.imunit.examples.tck.AbstractQueuedLongSynchronizerTest
edu.illinois.imunit.examples.tck.AbstractQueuedSynchronizerTest
edu.illinois.imunit.examples.tck.ArrayBlockingQueueTest
edu.illinois.imunit.examples.tck.ReentrantLockTest
edu.illinois.imunit.examples.tck.ReentrantReadWriteLockTest
edu.illinois.imunit.examples.tck.PhaserTest
edu.illinois.imunit.examples.tck.SemaphoreTest
edu.illinois.imunit.examples.tck.SynchronousQueueTest
edu.illinois.imunit.examples.tck.PriorityBlockingQueueTest
edu.illinois.imunit.examples.tck.LinkedTransferQueueTest
edu.illinois.imunit.examples.tck.LinkedBlockingDequeTest
edu.illinois.imunit.examples.tck.LinkedBlockingQueueTest
edu.illinois.imunit.examples.tck.LockSupportTest
edu.illinois.imunit.examples.tck.ExchangerTest
edu.illinois.imunit.examples.tck.CyclicBarrierTest
edu.illinois.imunit.examples.tck.CountDownLatchTest
edu.illinois.imunit.examples.hadoop.TestRPC
edu.illinois.imunit.examples.apache.collections.TestBlockingBuffer
edu.illinois.imunit.examples.mina.DatagramConfigTest"

# functions
# $1 -- test class
function runTests() {
    echo "Running tests from: ${1}"
    java -Xbootclasspath/p:lib/jsr166.jar -cp 'bin:lib/*' org.junit.runner.JUnitCore ${1}
}

# run the test(s)
if [[ "${test_class}" == "all" ]]
then
    for test_class in ${all_test_classes}
    do
        runTests ${test_class}
    done    
else
    runTests ${test_class}
fi
