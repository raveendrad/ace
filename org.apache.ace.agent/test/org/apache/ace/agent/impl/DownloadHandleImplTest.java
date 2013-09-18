/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ace.agent.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.ace.agent.ConnectionHandler;
import org.apache.ace.agent.DownloadHandle;
import org.apache.ace.agent.DownloadHandle.DownloadProgressListener;
import org.apache.ace.agent.DownloadHandler;
import org.apache.ace.agent.DownloadResult;
import org.apache.ace.agent.testutil.BaseAgentTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test cases for {@link DownloadHandleImpl}.
 */
public class DownloadHandleImplTest extends BaseAgentTest {
    private AgentContextImpl m_agentContext;

    private URL m_testContentURL;
    private String m_digest;
    private long m_contentLength;

    @BeforeClass
    public void setUp() throws Exception {
        m_agentContext = mockAgentContext();
        m_agentContext.setHandler(DownloadHandler.class, new DownloadHandlerImpl(m_agentContext.getWorkDir()));
        m_agentContext.setHandler(ConnectionHandler.class, new ConnectionHandlerImpl());
        replayTestMocks();
        m_agentContext.start();
    }

    @BeforeMethod
    public void setUpTestCase() throws Exception {
        File file = File.createTempFile("test", ".bin", new File("generated"));
        file.deleteOnExit();

        DigestOutputStream dos = null;
        try {
            dos = new DigestOutputStream(new FileOutputStream(file), MessageDigest.getInstance("MD5"));

            for (int i = 0; i < 10000; i++) {
                dos.write(String.valueOf(System.currentTimeMillis()).getBytes());
                dos.write(" Lorum Ipsum Lorum Ipsum Lorum Ipsum Lorum Ipsum Lorum Ipsum\n".getBytes());
            }
            dos.flush();
        }
        finally {
            if (dos != null) {
                dos.close();
            }
        }

        m_testContentURL = file.toURI().toURL();
        m_contentLength = file.length();
        m_digest = new String(dos.getMessageDigest().digest());
    }

    @AfterClass
    public void tearDown() throws Exception {
        m_agentContext.stop();
        verifyTestMocks();
        clearTestMocks();
    }

    @Test
    public void testCreateDownloadHandleGeneratesSameTemporaryFilenameOk() throws Exception {
        DownloadHandler downloadHandler = m_agentContext.getHandler(DownloadHandler.class);

        DownloadHandleImpl handle;

        handle = (DownloadHandleImpl) downloadHandler.getHandle(m_testContentURL);
        String tempFilename = handle.getDownloadFile().getAbsolutePath();

        handle = (DownloadHandleImpl) downloadHandler.getHandle(m_testContentURL);
        assertEquals(handle.getDownloadFile().getAbsolutePath(), tempFilename);

        handle = (DownloadHandleImpl) downloadHandler.getHandle(m_testContentURL);
        assertEquals(handle.getDownloadFile().getAbsolutePath(), tempFilename);
    }

    @Test
    public void testDownloadOk() throws Exception {
        DownloadHandler downloadHandler = m_agentContext.getHandler(DownloadHandler.class);

        DownloadResult downloadResult;
        Future<DownloadResult> future;

        final DownloadHandle handle = downloadHandler.getHandle(m_testContentURL);
        future = handle.start(null);

        downloadResult = future.get(5, TimeUnit.SECONDS);
        assertNotNull(downloadResult, "Failed to finish download?!");
        assertTrue(downloadResult.isComplete());

        File file = ((DownloadHandleImpl) handle).getDownloadFile();
        long fileLength = file.length();

        assertTrue(file.exists(), file.getName() + " does not exist?!");
        assertTrue(fileLength > 0 && fileLength == m_contentLength, "Nothing downloaded yet for " + file.getName() + "?");

        // Verify the contents of the downloaded file is what we expect...
        assertEquals(getDigest(file), m_digest);
    }

    @Test
    public void testRestartDownloadOk() throws Exception {
        DownloadHandler downloadHandler = m_agentContext.getHandler(DownloadHandler.class);

        DownloadResult downloadResult;
        Future<DownloadResult> future;

        final DownloadHandle handle = downloadHandler.getHandle(m_testContentURL);
        future = handle.start(new DownloadProgressListener() {
            @Override
            public void progress(long bytesRead, long totalBytes) {
                handle.stop();
            }
        });

        try {
            future.get(5, TimeUnit.SECONDS);
        }
        catch (CancellationException exception) {
            // Ok; expected...
        }
        assertTrue(future.isCancelled());

        File file = ((DownloadHandleImpl) handle).getDownloadFile();
        long fileLength = file.length();

        // Discard the result...
        handle.discard();

        // Restart & finish the download...
        DownloadHandle handle2 = downloadHandler.getHandle(m_testContentURL);
        future = handle2.start(null);

        downloadResult = future.get(5, TimeUnit.SECONDS);
        assertNotNull(downloadResult, "Failed to finish download?!");
        assertTrue(downloadResult.isComplete());

        fileLength = file.length();

        assertTrue(file.exists(), file.getName() + " does not exist?!");
        assertTrue(fileLength == m_contentLength, "Nothing downloaded yet for " + file.getName() + "?");

        // Verify the contents of the downloaded file is what we expect...
        assertEquals(getDigest(file), m_digest);
    }

    @Test
    public void testResumeDownloadOk() throws Exception {
        DownloadHandler downloadHandler = m_agentContext.getHandler(DownloadHandler.class);

        DownloadResult downloadResult;
        Future<DownloadResult> future;

        final DownloadHandle handle = downloadHandler.getHandle(m_testContentURL);
        // Start the download, but interrupt it after reading the first chunk of data...
        future = handle.start(new DownloadProgressListener() {
            @Override
            public void progress(long bytesRead, long totalBytes) {
                Thread.currentThread().interrupt();
            }
        });

        downloadResult = future.get(5, TimeUnit.SECONDS);
        assertNotNull(downloadResult, "Failed to stop download?!");
        assertFalse(downloadResult.isComplete());

        File file = ((DownloadHandleImpl) handle).getDownloadFile();
        long firstFileLength = file.length();

        assertTrue(file.exists(), file.getName() + " does not exist?!");
        assertTrue(firstFileLength > 0 && firstFileLength < m_contentLength, "Nothing downloaded yet for " + file.getName() + "?");

        final DownloadHandle handle2 = downloadHandler.getHandle(m_testContentURL);
        // Resume the download, but stop it after reading the first chunk of data...
        future = handle2.start(new DownloadProgressListener() {
            @Override
            public void progress(long bytesRead, long totalBytes) {
                handle2.stop();
            }
        });

        try {
            future.get(5, TimeUnit.SECONDS);
        }
        catch (CancellationException exception) {
            // Ok; expected...
        }
        assertTrue(future.isCancelled());

        long secondFileLength = file.length();

        assertTrue(secondFileLength > firstFileLength && secondFileLength < m_contentLength, "Nothing downloaded yet for " + file.getName() + "?");

        DownloadHandle handle3 = downloadHandler.getHandle(m_testContentURL);
        // Resume the download, and finish it...
        future = handle3.start(null);

        downloadResult = future.get(5, TimeUnit.SECONDS);
        assertNotNull(downloadResult, "Failed to complete download?!");
        assertTrue(downloadResult.isComplete());

        assertEquals(file.length(), m_contentLength, "Not all content downloaded for " + file.getName() + "?");

        // Verify the contents of the downloaded file is what we expect...
        assertEquals(getDigest(file), m_digest);
    }

    private String getDigest(File file) throws Exception {
        DigestInputStream dis = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("MD5"));
        while (dis.read() != -1) {
        }
        dis.close();
        return new String(dis.getMessageDigest().digest());
    }
}
