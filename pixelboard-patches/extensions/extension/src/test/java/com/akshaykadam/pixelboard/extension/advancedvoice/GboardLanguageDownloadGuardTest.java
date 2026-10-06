package com.akshaykadam.pixelboard.extension.advancedvoice;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public final class GboardLanguageDownloadGuardTest {
    @Before
    @After
    public void reset() {
        GboardLanguageDownloadGuard.resetForTest();
    }

    @Test
    public void firstRequestPassesAndIdenticalRepeatsAreDropped() {
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("en-US", "CHECKER"));
        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(GboardLanguageDownloadGuard.shouldSkip("en-US", "CHECKER"));
        }
    }

    @Test
    public void languagesAndSourcesAreTrackedIndependently() {
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("en-US", "A"));
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("it-IT", "A"));
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("en-US", "B"));
        Assert.assertTrue(GboardLanguageDownloadGuard.shouldSkip("en-US", "A"));
    }

    @Test
    public void nullLanguageIsNeverDropped() {
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip(null, "A"));
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip(null, "A"));
    }

    @Test
    public void concurrentIdenticalRequestsLetExactlyOneThrough() throws Exception {
        final int threads = 16;
        for (int round = 0; round < 200; round++) {
            GboardLanguageDownloadGuard.resetForTest();
            final AtomicInteger passed = new AtomicInteger();
            final CountDownLatch go = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(threads);
            for (int i = 0; i < threads; i++) {
                new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            go.await();
                        } catch (InterruptedException e) {
                            return;
                        }
                        if (!GboardLanguageDownloadGuard.shouldSkip("en-US", "A")) {
                            passed.incrementAndGet();
                        }
                        done.countDown();
                    }
                }).start();
            }
            go.countDown();
            done.await();
            Assert.assertEquals(1, passed.get());
        }
    }
}
