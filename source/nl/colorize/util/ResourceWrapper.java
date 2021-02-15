//-----------------------------------------------------------------------------
// Remember That
// Copyright 2015-2021 Colorize
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Wraps a resource that can be opened, closed, and reopened multiple times.
 * The wrapper can be used in conjuction with {@link AutoCloseable} and the
 * corresponding try-with-resources statement.
 *
 * @param <T> The type of resource that is provided.
 */
public final class ResourceWrapper<T extends AutoCloseable> implements Closeable, AutoCloseable {

    private Supplier<T> resourceProvider;
    private T resource;
    private AtomicBoolean status;

    private static final Logger LOGGER = LogHelper.getLogger(ResourceWrapper.class);

    private ResourceWrapper(Supplier<T> resourceProvider) {
        this.resourceProvider = resourceProvider;
        this.status = new AtomicBoolean(false);
    }

    public T open() {
        Preconditions.checkState(!status.get(), "Resource is already open");
        status.set(true);
        resource = resourceProvider.get();
        return resource;
    }

    @Override
    public void close() {
        if (status.get()) {
            status.set(false);
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.warning("Unable to close resource: " + e.getMessage());
            }
            resource = null;
        }
    }

    public boolean isOpen() {
        return status.get();
    }

    /**
     * Returns the currently open resource if it's already open, and opens it
     * when it's not yet open.
     */
    public T get() {
        if (status.get()) {
            return open();
        } else {
            return resource;
        }
    }

    public static <T extends AutoCloseable> ResourceWrapper<T> wrap(Supplier<T> resourceProvider) {
        return new ResourceWrapper<T>(resourceProvider);
    }
}
