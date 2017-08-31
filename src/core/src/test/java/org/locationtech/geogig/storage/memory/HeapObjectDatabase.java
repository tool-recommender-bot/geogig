/* Copyright (c) 2012-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.storage.memory;

import java.nio.file.Path;

import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Platform;
import org.locationtech.geogig.storage.BlobStore;
import org.locationtech.geogig.storage.GraphDatabase;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.geogig.storage.impl.ConnectionManager;
import org.locationtech.geogig.storage.impl.ForwardingObjectStore;
import org.locationtech.geogig.storage.impl.SynchronizedGraphDatabase;

import com.google.inject.Inject;

/**
 * Provides an implementation of a GeoGig object database that utilizes the heap for the storage of
 * objects.
 * 
 * @see ForwardingObjectStore
 */
public class HeapObjectDatabase extends ForwardingObjectStore implements ObjectDatabase {

    static HeapObjectDatabaseConnectionManager CONN_MANAGER = new HeapObjectDatabaseConnectionManager();

    private HeapBlobStore blobs;

    private HeapGraphDatabase graph;

    private Platform platform;

    public HeapObjectDatabase() {
        super(new HeapObjectStore(), false);
    }

    @Inject
    public HeapObjectDatabase(Platform platform, Hints hints) {
        super(connect(platform), readOnly(hints));
        this.platform = platform;
    }

    private static HeapObjectStore connect(Platform platform) {
        Path path = platform.pwd().toPath();
        HeapObjectStore store = CONN_MANAGER.acquire(path);
        return store;
    }

    private static boolean readOnly(Hints hints) {
        return hints == null ? false : hints.getBoolean(Hints.OBJECTS_READ_ONLY);
    }

    /**
     * Closes the database.
     * 
     * @see org.locationtech.geogig.storage.ObjectDatabase#close()
     */
    @Override
    public void close() {
        super.close();
        if (graph != null) {
            graph.close();
            graph = null;
        }
    }

    /**
     * Opens the database for use by GeoGig.
     */
    @Override
    public void open() {
        if (isOpen()) {
            return;
        }
        super.open();
        blobs = new HeapBlobStore();
        graph = new HeapGraphDatabase(platform);
        graph.open();
    }

    @Override
    public boolean isReadOnly() {
        return !super.canWrite;
    }

    @Override
    public BlobStore getBlobStore() {
        return blobs;
    }

    @Override
    public GraphDatabase getGraphDatabase() {
        return new SynchronizedGraphDatabase(graph);
    }

    @Override
    public void configure() {
        // No-op
    }

    @Override
    public boolean checkConfig() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static class HeapObjectDatabaseConnectionManager
            extends ConnectionManager<Path, HeapObjectStore> {

        @Override
        protected HeapObjectStore connect(Path address) {
            return new HeapObjectStore();
        }

        @Override
        protected void disconnect(HeapObjectStore c) {
            c.close();
        }

    }
}
