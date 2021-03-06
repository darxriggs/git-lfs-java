package ru.bozaro.gitlfs.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.bozaro.gitlfs.client.auth.AuthProvider;

/**
 * Embedded LFS server for servlet testing.
 *
 * @author Artem V. Navrotskiy
 */
public class EmbeddedLfsServer implements AutoCloseable {
  @NotNull
  private final EmbeddedHttpServer server;
  @NotNull
  private final MemoryStorage storage;
  @Nullable
  private final LockManager lockManager;

  public EmbeddedLfsServer(@NotNull MemoryStorage storage, @Nullable LockManager lockManager) throws Exception {
    this.lockManager = lockManager;
    this.server = new EmbeddedHttpServer();
    this.storage = storage;
    server.addServlet("/foo/bar.git/info/lfs/objects/*", new PointerServlet(storage, "/foo/bar.git/info/lfs/storage/"));
    server.addServlet("/foo/bar.git/info/lfs/storage/*", new ContentServlet(storage));
    if (lockManager != null)
      server.addServlet("/foo/bar.git/info/lfs/locks/*", new LocksServlet(lockManager));
  }

  public AuthProvider getAuthProvider() {
    return storage.getAuthProvider(server.getBase().resolve("/foo/bar.git/info/lfs"));
  }

  @NotNull
  public MemoryStorage getStorage() {
    return storage;
  }

  @Nullable
  public LockManager getLockManager() {
    return lockManager;
  }

  @Override
  public void close() throws Exception {
    server.close();
  }
}
