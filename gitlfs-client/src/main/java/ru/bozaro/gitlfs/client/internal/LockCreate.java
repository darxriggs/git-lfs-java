package ru.bozaro.gitlfs.client.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.bozaro.gitlfs.common.data.*;

import java.io.IOException;

import static ru.bozaro.gitlfs.common.Constants.HEADER_ACCEPT;
import static ru.bozaro.gitlfs.common.Constants.MIME_LFS_JSON;

public final class LockCreate implements Request<LockCreate.Res> {
  @NotNull
  private final String path;
  @Nullable
  private final Ref ref;

  public LockCreate(@NotNull String path, @Nullable Ref ref) {
    this.path = path;
    this.ref = ref;
  }

  @NotNull
  @Override
  public HttpUriRequest createRequest(@NotNull ObjectMapper mapper, @NotNull String url) throws JsonProcessingException {
    final HttpPost req = new HttpPost(url);
    req.addHeader(HEADER_ACCEPT, MIME_LFS_JSON);

    final CreateLockReq createLockReq = new CreateLockReq(path, ref);
    final AbstractHttpEntity entity = new ByteArrayEntity(mapper.writeValueAsBytes(createLockReq));
    entity.setContentType(MIME_LFS_JSON);

    req.setEntity(entity);
    return req;
  }

  @Override
  public LockCreate.Res processResponse(@NotNull ObjectMapper mapper, @NotNull HttpResponse response) throws IOException {
    switch (response.getStatusLine().getStatusCode()) {
      case HttpStatus.SC_CREATED:
        return new Res(true, mapper.readValue(response.getEntity().getContent(), CreateLockRes.class).getLock(), null);
      case HttpStatus.SC_CONFLICT:
        final LockConflictRes res = mapper.readValue(response.getEntity().getContent(), LockConflictRes.class);
        return new Res(false, res.getLock(), res.getMessage());
      default:
        throw new IllegalStateException();
    }
  }

  @NotNull
  @Override
  public int[] statusCodes() {
    return new int[]{
        HttpStatus.SC_CREATED,
        HttpStatus.SC_CONFLICT,
    };
  }

  public static final class Res {

    private final boolean success;
    @Nullable
    private final String message;
    @NotNull
    private final Lock lock;

    private Res(boolean success, @NotNull Lock lock, @Nullable String message) {
      this.success = success;
      this.lock = lock;
      this.message = message;
    }

    public boolean isSuccess() {
      return success;
    }

    @NotNull
    public Lock getLock() {
      return lock;
    }

    @Nullable
    public String getMessage() {
      return message;
    }
  }
}
