package com.joyent.manta.client.multipart;

import com.joyent.manta.client.HttpHelper;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaHttpHeaders;
import com.joyent.manta.client.MantaJob;
import com.joyent.manta.client.MantaJobBuilder;
import com.joyent.manta.client.MantaJobPhase;
import com.joyent.manta.client.MantaMetadata;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.client.MantaObjectResponse;
import com.joyent.manta.client.MantaUtils;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.exception.MantaException;
import com.joyent.manta.exception.MantaIOException;
import com.joyent.manta.exception.MantaMultipartException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.joyent.manta.client.MantaClient.SEPARATOR;

/**
 * Class providing an API for multipart uploads to Manta.
 *
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 2.5.0
 */
public class MantaMultipartManager {
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HttpHelper.class);

    /**
     * Maximum number of parts for a single Manta object.
     */
    public static final int MAX_PARTS = 10_000;

    /**
     * Temporary storage directory on Manta for multipart data. This is a
     * randomly chosen UUID used so that we don't have directory naming
     * conflicts.
     */
    static final String MULTIPART_DIRECTORY =
            "stor/.multipart-6439b444-9041-11e6-9be2-9f622f483d01";

    /**
     * Metadata file containing information about final multipart file.
     */
    static final String METADATA_FILE = "metadata";

    /**
     * Default number of seconds to poll Manta to see if a job is complete.
     */
    private static final long DEFAULT_SECONDS_TO_POLL = 5L;

    /**
     * Number of times to check to see if a multipart transfer has completed.
     */
    private static final int NUMBER_OF_TIMES_TO_POLL = 20;

    /**
     * Reference to {@link MantaClient} Manta client object providing access to
     * Manta.
     */
    private final MantaClient mantaClient;

    /**
     * Full path on Manta to the upload directory.
     */
    private final String resolvedMultipartUploadDirectory;

    /**
     * Format for naming Manta jobs.
     */
    private static final String JOB_NAME_FORMAT = "append-%s";

    /**
     * Creates a new instance backed by the specified {@link MantaClient}.
     * @param mantaClient Manta client instance to use to communicate with server
     */
    public MantaMultipartManager(final MantaClient mantaClient) {
        if (mantaClient == null) {
            throw new IllegalArgumentException("Manta client must be present");
        }
        this.mantaClient = mantaClient;

        this.resolvedMultipartUploadDirectory =
                mantaClient.getContext().getMantaHomeDirectory()
                + SEPARATOR + MULTIPART_DIRECTORY;
    }

    /**
     * Lists multipart uploads that are currently in progress.
     *
     * @return stream of objects representing files being uploaded via multipart
     * @throws IOException thrown when there are network issues
     */
    public Stream<MantaMultipartUpload> listInProgress() throws IOException {
        final List<Throwable> exceptions = new ArrayList<>();

        /* This nesting structure is unfortunate, but an artifact of us needing
         * to close the stream when we have finished processing. */
        try (Stream<MantaObject> multipartDirList = mantaClient
                    .listObjects(this.resolvedMultipartUploadDirectory)
                    .filter(MantaObject::isDirectory)) {

            final Stream<MantaMultipartUpload> stream = multipartDirList
                    .map(object -> {
                        String idString = MantaUtils.lastItemInPath(object.getPath());
                        UUID id = UUID.fromString(idString);

                        try {
                            MultipartMetadata mantaMetadata = downloadMultipartMetadata(id);
                            return new MantaMultipartUpload(id, mantaMetadata.getPath());
                        } catch (MantaClientHttpResponseException e) {
                            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                                return null;
                            } else {
                                exceptions.add(e);
                                return null;
                            }
                        } catch (IOException | RuntimeException e) {
                            exceptions.add(e);
                            return null;
                        }
                    })
                    /* We explicitly filter out items that stopped existing when we
                     * went to get the multipart metadata because we encountered a
                     * race condition. */
                    .filter(value -> value != null);

            if (exceptions.isEmpty()) {
                return stream;
            }
        // This catches an exception on the initial listObjects call
        } catch (MantaClientHttpResponseException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return Stream.empty();
            } else {
                throw e;
            }
        }

        final MantaIOException aggregateException = new MantaIOException(
                "Problem(s) listing multipart uploads in progress");

        int count = 1;
        for (Throwable e : exceptions) {
            final String label = String.format("exception_%d", count++);
            aggregateException.setContextValue(label, e);
        }

        throw aggregateException;
    }

    /**
     * Initializes a new multipart upload for an object.
     *
     * @param path remote path of Manta object to be uploaded
     * @return unique id for the multipart upload
     * @throws IOException thrown when there are network issues
     */
    public MantaMultipartUpload initiateUpload(final String path) throws IOException {
        return initiateUpload(path, null, null);
    }

    /**
     * Initializes a new multipart upload for an object.
     *
     * @param path remote path of Manta object to be uploaded
     * @param mantaMetadata metadata to write to final Manta object
     * @return unique id for the multipart upload
     * @throws IOException thrown when there are network issues
     */
    public MantaMultipartUpload initiateUpload(final String path,
                                               final MantaMetadata mantaMetadata) throws IOException {
        return initiateUpload(path, mantaMetadata, null);
    }

    /**
     * Initializes a new multipart upload for an object.
     *
     * @param path remote path of Manta object to be uploaded
     * @param mantaMetadata metadata to write to final Manta object
     * @param httpHeaders HTTP headers to read from to write to final Manta object
     * @return unique id for the multipart upload
     * @throws IOException thrown when there are network issues
     */
    public MantaMultipartUpload initiateUpload(final String path,
                                               final MantaMetadata mantaMetadata,
                                               final MantaHttpHeaders httpHeaders) throws IOException {
        final UUID id = UUID.randomUUID();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating a new multipart upload [{}] for {}",
                    id, path);
        }

        final String uploadDir = multipartUploadDir(id);
        mantaClient.putDirectory(uploadDir, true);

        final String metadataPath = uploadDir + SEPARATOR + METADATA_FILE;

        final MultipartMetadata metadata = new MultipartMetadata()
                .setPath(path)
                .setObjectMetadata(mantaMetadata);

        if (httpHeaders != null) {
            metadata.setContentType(httpHeaders.getContentType());
        }

        final byte[] metadataBytes = SerializationUtils.serialize(metadata);

        mantaClient.put(metadataPath, metadataBytes);

        return new MantaMultipartUpload(id, path);
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param upload multipart upload object
     * @param partNumber part number to identify relative location in final file
     * @param contents String contents to be written in UTF-8
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart uploadPart(final MantaMultipartUpload upload,
                                                final int partNumber,
                                                final String contents)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return uploadPart(upload.getId(), partNumber, contents);
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param id multipart upload id
     * @param partNumber part number to identify relative location in final file
     * @param contents String contents to be written in UTF-8
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart uploadPart(final UUID id, final int partNumber,
                                               final String contents)
            throws IOException {

        final String path = multipartPath(id, partNumber);

        final MantaObjectResponse response = mantaClient.put(path, contents);
        return new MantaMultipartUploadPart(response);
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param upload multipart upload object
     * @param partNumber part number to identify relative location in final file
     * @param bytes byte array containing data of the part to be uploaded
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart uploadPart(final MantaMultipartUpload upload,
                                               final int partNumber,
                                               final byte[] bytes)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return uploadPart(upload.getId(), partNumber, bytes);
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param id multipart upload id
     * @param partNumber part number to identify relative location in final file
     * @param bytes byte array containing data of the part to be uploaded
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart uploadPart(final UUID id, final int partNumber,
                                               final byte[] bytes)
            throws IOException {
        final String path = multipartPath(id, partNumber);

        final MantaObjectResponse response = mantaClient.put(path, bytes);
        return new MantaMultipartUploadPart(response);
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param upload multipart upload object
     * @param partNumber part number to identify relative location in final file
     * @param file file containing data of the part to be uploaded
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart uploadPart(final MantaMultipartUpload upload,
                                               final int partNumber,
                                               final File file)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return uploadPart(upload.getId(), partNumber, file);
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param id multipart upload id
     * @param partNumber part number to identify relative location in final file
     * @param file file containing data of the part to be uploaded
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart uploadPart(final UUID id, final int partNumber,
                                               final File file)
            throws IOException {
        final String path = multipartPath(id, partNumber);

        final MantaObjectResponse response = mantaClient.put(path, file);
        return new MantaMultipartUploadPart(response);
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param upload multipart upload object
     * @param partNumber part number to identify relative location in final file
     * @param inputStream stream providing data for part to be uploaded
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart uploadPart(final MantaMultipartUpload upload,
                                               final int partNumber,
                                               final InputStream inputStream)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return uploadPart(upload.getId(), partNumber, inputStream);
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param id multipart upload id
     * @param partNumber part number to identify relative location in final file
     * @param inputStream stream providing data for part to be uploaded
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart uploadPart(final UUID id, final int partNumber,
                                               final InputStream inputStream)
            throws IOException {
        final String path = multipartPath(id, partNumber);
        final MantaObjectResponse response = mantaClient.put(path, inputStream);

        return new MantaMultipartUploadPart(response);
    }

    /**
     * Retrieves information about a single part of a multipart upload.
     *
     * @param upload multipart upload object
     * @param partNumber part number to identify relative location in final file
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart getPart(final MantaMultipartUpload upload,
                                            final int partNumber)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return getPart(upload.getId(), partNumber);
    }

    /**
     * Retrieves information about a single part of a multipart upload.
     *
     * @param id multipart upload id
     * @param partNumber part number to identify relative location in final file
     * @return multipart single part object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartUploadPart getPart(final UUID id, final int partNumber)
            throws IOException {
        final String path = multipartPath(id, partNumber);
        final MantaObjectResponse response = mantaClient.head(path);

        return new MantaMultipartUploadPart(response);
    }

    /**
     * Retrieves the state of a given Manta multipart upload.
     *
     * @param upload multipart upload object
     * @return enum representing the state / status of the multipart upload
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartStatus getStatus(final MantaMultipartUpload upload)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return getStatus(upload.getId());
    }

    /**
     * Retrieves the state of a given Manta multipart upload.
     *
     * @param id multipart upload id
     * @return enum representing the state / status of the multipart upload
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public MantaMultipartStatus getStatus(final UUID id) throws IOException {
        final String dir = multipartUploadDir(id);
        final MantaObjectResponse response;

        try {
            response = mantaClient.head(dir);
        } catch (MantaClientHttpResponseException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                final MantaJob job = findJob(id);

                if (job == null) {
                    return MantaMultipartStatus.UNKNOWN;
                } else if (job.getCancelled() != null && job.getCancelled()) {
                    return MantaMultipartStatus.ABORTED;
                } else if (job.getState().equals("done")) {
                    return MantaMultipartStatus.COMPLETED;
                } else if (job.getState().equals("running")) {
                    return MantaMultipartStatus.COMMITTING;
                } else {
                    MantaException mioe = new MantaException("Unexpected job state");
                    mioe.setContextValue("job_state", job.getState());
                    mioe.setContextValue("job_id", job.getId().toString());
                    mioe.setContextValue("multipart_id", id);
                    mioe.setContextValue("multipart_upload_dir", dir);

                    throw mioe;
                }
            }

            throw e;
        }

        if (!response.isDirectory()) {
            MantaMultipartException e = new MantaMultipartException(
                    "Remote path was a file and not a directory as expected");
            e.setContextValue("multipart_upload_dir", dir);
            throw e;
        }

        final MantaJob job = findJob(id);

        if (job == null) {
            return MantaMultipartStatus.CREATED;
        }

            /* If we still have the directory associated with the multipart
             * upload AND we are in the state of Cancelled. */
        if (job.getCancelled()) {
            return MantaMultipartStatus.ABORTING;
        }

        final String state = job.getState();

            /* If we still have the directory associated with the multipart
             * upload AND we have the job id, we are in a state where the
             * job hasn't finished clearing out the data files. */
        if (state.equals("done") || state.equals("running") || state.equals("queued")) {
            return MantaMultipartStatus.COMMITTING;
        } else {
            return MantaMultipartStatus.UNKNOWN;
        }
    }

    /**
     * Lists the parts that have already been uploaded.
     *
     * @param upload multipart upload object
     * @return stream of parts identified by integer part number
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public Stream<MantaMultipartUploadPart> listParts(final MantaMultipartUpload upload)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return listParts(upload.getId());
    }

    /**
     * Lists the parts that have already been uploaded.
     *
     * @param id multipart upload id
     * @return stream of parts identified by integer part number
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public Stream<MantaMultipartUploadPart> listParts(final UUID id) throws IOException {
        final String dir = multipartUploadDir(id);

        return mantaClient.listObjects(dir)
                .filter(value -> !Paths.get(value.getPath())
                        .getFileName().toString().equals(METADATA_FILE))
                .map(MantaMultipartUploadPart::new);
    }

    /**
     * Validates that there is no part missing from the sequence.
     *
     * @param upload multipart upload object
     * @throws IOException thrown if there is a problem connecting to Manta
     * @throws MantaMultipartException thrown went part numbers aren't sequential
     */
    public void validateThatThereAreSequentialPartNumbers(final MantaMultipartUpload upload)
            throws IOException, MantaMultipartException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        validateThatThereAreSequentialPartNumbers(upload.getId());
    }

    /**
     * Validates that there is no part missing from the sequence.
     *
     * @param id multipart upload id
     * @throws IOException thrown if there is a problem connecting to Manta
     * @throws MantaMultipartException thrown went part numbers aren't sequential
     */
    public void validateThatThereAreSequentialPartNumbers(final UUID id)
            throws IOException, MantaMultipartException {
        if (id == null) {
            throw new IllegalArgumentException("Upload id must be present");
        }

        listParts(id)
            .sorted()
            .map(MantaMultipartUploadPart::getPartNumber)
            .reduce(1, (memo, value) -> {
                if (!memo.equals(value)) {
                    MantaMultipartException e = new MantaMultipartException(
                            "Missing part of multipart upload");
                    e.setContextValue("missing_part", memo);
                    throw e;
                }

                return memo + 1;
            });
    }

    /**
     * Aborts a multipart transfer.
     *
     * @param upload multipart upload object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public void abort(final MantaMultipartUpload upload) throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        abort(upload.getId());
    }

    /**
     * Aborts a multipart transfer.
     *
     * @param id multipart upload id
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public void abort(final UUID id) throws IOException {
        final String dir = multipartUploadDir(id);

        final MantaJob job = findJob(id);

        if (job == null) {
            MantaMultipartException e = new MantaMultipartException("The multipart upload specified hasn't been "
                    + "committed/completed yet");
            e.setContextValue("upload_id", id.toString());

            String status = "N/A";

            // Safely try to get the status of the upload
            try {
                status = getStatus(id).toString();
            } finally {
                e.setContextValue("upload_status", status);
            }

            throw e;
        }

        if (job.getState().equals("running") || job.getState().equals("queued")) {
            mantaClient.cancelJob(job.getId());
        }

        mantaClient.deleteRecursive(dir);
    }

    /**
     * Completes a multipart transfer by assembling the parts on Manta.
     *
     * @param upload multipart upload object
     * @param partsStream stream of multipart part objects
     * @throws java.io.IOException thrown if there is a problem connecting to Manta
     */
    public void complete(final MantaMultipartUpload upload,
                         final Stream<? extends MantaMultipartUploadTuple> partsStream)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        complete(upload.getId(), partsStream);
    }

    /**
     * Completes a multipart transfer by assembling the parts on Manta.
     *
     * @param id multipart upload id
     * @param partsStream stream of multipart part objects
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public void complete(final UUID id,
                         final Stream<? extends MantaMultipartUploadTuple> partsStream)
            throws IOException {
        final String uploadDir = multipartUploadDir(id);
        final MultipartMetadata metadata = downloadMultipartMetadata(id);

        final Map<String, MantaMultipartUploadPart> listing = new HashMap<>();
        try (Stream<MantaMultipartUploadPart> listStream = listParts(id)
                .limit(MAX_PARTS)) {
            listStream.forEach(p -> listing.put(p.getEtag(), p));
        }

        final String path = metadata.getPath();

        final StringBuilder jobExecText = new StringBuilder("mget -q ");

        List<MantaMultipartUploadTuple> missingTuples = new ArrayList<>();

        final AtomicInteger count = new AtomicInteger(0);
        partsStream.sorted().forEach(part -> {
            if (count.incrementAndGet() > MAX_PARTS) {
                String msg = String.format("Too many multipart parts specified [%d]. "
                        + "The maximum number of parts is %d", MAX_PARTS, count.get());
                throw new IllegalArgumentException(msg);
            }

            final MantaMultipartUploadPart o = listing.get(part.getEtag());

            if (o != null) {
                jobExecText.append(o.getObjectPath()).append(" ");
            } else {
                missingTuples.add(part);
            }
        });

        if (!missingTuples.isEmpty()) {
            final MantaMultipartException e = new MantaMultipartException(
                    "Multipart part(s) specified couldn't be found");

            int missingCount = 0;
            for (MantaMultipartUploadTuple missingPart : missingTuples) {
                String key = String.format("missing_part_%d", ++missingCount);
                e.setContextValue(key, missingPart.toString());
            }

            throw e;
        }

        jobExecText.append("| mput -q ")
                   .append(path)
                   .append(" ");

        if (metadata.getContentType() != null) {
            jobExecText.append("-H 'Content-Type: ")
                       .append(metadata.getContentType())
                       .append("' ");
        }

        final MantaMetadata objectMetadata = metadata.getObjectMetadata();

        if (objectMetadata != null) {
            Set<Map.Entry<String, String>> entries = objectMetadata.entrySet();

            for (Map.Entry<String, String> entry : entries) {
                jobExecText.append("-H '")
                           .append(entry.getKey()).append(": ")
                           .append(entry.getValue())
                           .append("' ");
            }
        }

        final MantaJobPhase concatPhase = new MantaJobPhase()
                .setType("reduce")
                .setExec(jobExecText.toString());

        final MantaJobPhase cleanupPhase = new MantaJobPhase()
                .setType("reduce")
                .setExec("mrm -r " + uploadDir);

        MantaJobBuilder.Run run = mantaClient.jobBuilder()
                .newJob(String.format(JOB_NAME_FORMAT, id))
                .addPhase(concatPhase)
                .addPhase(cleanupPhase)
                .run();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Created job for concatenating parts: {}",
                    run.getJob().getId());
        }
    }

    /**
     * Downloads the serialized metadata object from Manta and deserializes it.
     *
     * @param id multipart upload id
     * @return metadata object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    protected MantaMultipartManager.MultipartMetadata downloadMultipartMetadata(final UUID id)
            throws IOException {
        final String uploadDir = multipartUploadDir(id);
        final String metadataPath = uploadDir + SEPARATOR + METADATA_FILE;

        try (InputStream in = mantaClient.getAsInputStream(metadataPath)) {
            return SerializationUtils.deserialize(in);
        }
    }

    /**
     * Waits for a multipart upload to complete. Polling every 5 seconds.
     *
     * @param <R> Return type for executeWhenTimesToPollExceeded
     * @param upload multipart upload object
     * @param executeWhenTimesToPollExceeded lambda executed when timesToPoll has been exceeded
     * @return null when under poll timeout, otherwise returns return value of executeWhenTimesToPollExceeded
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public <R> R waitForCompletion(final MantaMultipartUpload upload,
                                   final Function<UUID, R> executeWhenTimesToPollExceeded)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return waitForCompletion(upload.getId(), executeWhenTimesToPollExceeded);
    }

    /**
     * Waits for a multipart upload to complete. Polling every 5 seconds.
     *
     * @param <R> Return type for executeWhenTimesToPollExceeded
     * @param id multipart upload id
     * @param executeWhenTimesToPollExceeded lambda executed when timesToPoll has been exceeded
     * @return null when under poll timeout, otherwise returns return value of executeWhenTimesToPollExceeded
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public <R> R waitForCompletion(final UUID id,
                                   final Function<UUID, R> executeWhenTimesToPollExceeded)
            throws IOException {
        return waitForCompletion(id, Duration.ofSeconds(DEFAULT_SECONDS_TO_POLL),
                NUMBER_OF_TIMES_TO_POLL, executeWhenTimesToPollExceeded);
    }

    /**
     * Waits for a multipart upload to complete. Polling for set interval.
     *
     * @param <R> Return type for executeWhenTimesToPollExceeded
     * @param upload multipart upload object
     * @param pingInterval interval to poll
     * @param timesToPoll number of times to poll Manta to check for completion
     * @param executeWhenTimesToPollExceeded lambda executed when timesToPoll has been exceeded
     * @return null when under poll timeout, otherwise returns return value of executeWhenTimesToPollExceeded
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public <R> R waitForCompletion(final MantaMultipartUpload upload,
                                   final Duration pingInterval,
                                   final int timesToPoll,
                                   final Function<UUID, R> executeWhenTimesToPollExceeded)
            throws IOException {
        if (upload == null) {
            throw new IllegalArgumentException("Upload must be present");
        }

        return waitForCompletion(upload.getId(), pingInterval, timesToPoll, executeWhenTimesToPollExceeded);
    }

    /**
     * Waits for a multipart upload to complete. Polling for set interval.
     *
     * @param <R> Return type for executeWhenTimesToPollExceeded
     * @param id multipart upload id
     * @param pingInterval interval to poll
     * @param timesToPoll number of times to poll Manta to check for completion
     * @param executeWhenTimesToPollExceeded lambda executed when timesToPoll has been exceeded
     * @return null when under poll timeout, otherwise returns return value of executeWhenTimesToPollExceeded
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    public <R> R waitForCompletion(final UUID id, final Duration pingInterval,
                                   final int timesToPoll,
                                   final Function<UUID, R> executeWhenTimesToPollExceeded)
            throws IOException {
        if (timesToPoll <= 0) {
            String msg = String.format("times to poll should be set to a value greater than 1. "
                    + "Actual value: %d", timesToPoll);
            throw new IllegalArgumentException(msg);
        }

        final String dir = multipartUploadDir(id);
        final MantaJob job = findJob(id);

        if (job == null) {
            String msg = "Unable for find job associated with multipart upload. "
                    + "Was complete() run for upload or was it run so long ago "
                    + "that we no longer have a record for it?";
            MantaMultipartException e = new MantaMultipartException(msg);
            e.setContextValue("upload_id", id.toString());
            e.setContextValue("job_id", id.toString());

            throw e;
        }

        MantaJobBuilder.Run run = mantaClient.jobBuilder().lookupJob(job);

        if (LOG.isDebugEnabled()) {
            LOG.debug("No longer waiting for multipart to complete."
                    + " Actual job state: {}", run.getJob().getState());
        }

        final long waitMillis = pingInterval.toMillis();

        int timesPolled;

        /* We ping the upload directory and wait for it to be deleted because
         * there is the chance for a race condition when the job attempts to
         * delete the upload directory, but isn't finished. */
        for (timesPolled = 0; timesPolled < timesToPoll; timesPolled++) {
            try {
                final MantaMultipartStatus status = getStatus(id);
                final boolean finished = status.equals(MantaMultipartStatus.COMPLETED)
                        || status.equals(MantaMultipartStatus.ABORTED)
                        || status.equals(MantaMultipartStatus.UNKNOWN);

                // We do a check preemptively because we shouldn't sleep unless we need to
                if (finished) {
                    return null;
                }

                // Don't bother to sleep if we won't be doing a check
                if (timesPolled < timesToPoll + 1) {
                    Thread.sleep(waitMillis);
                }
            } catch (InterruptedException e) {
                /* We assume the client has written logic for when the polling operation
                 * doesn't complete within the time period as expected and we also make
                 * the assumption that that behavior would be acceptable when the thread
                 * has been interrupted. */
                return executeWhenTimesToPollExceeded.apply(id);
            }
        }

        if (timesPolled >= timesToPoll) {
            return executeWhenTimesToPollExceeded.apply(id);
        }

        return null;
    }

    /**
     * Builds the full remote path for a part of a multipart upload.
     *
     * @param id multipart upload id
     * @param partNumber part number to identify relative location in final file
     * @return temporary path on Manta to store part
     */
    String multipartPath(final UUID id, final int partNumber) {
        validatePartNumber(partNumber);
        final String dir = multipartUploadDir(id);
        return String.format("%s%d", dir, partNumber);
    }

    /**
     * Finds the directory in which to upload parts into.
     *
     * @param id multipart transaction id
     * @return temporary Manta directory in which to upload parts
     */
    String multipartUploadDir(final UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Transaction id must be present");
        }

        return this.resolvedMultipartUploadDirectory
                + SEPARATOR + id.toString() + SEPARATOR;
    }

    /**
     * Validates that the given part number is specified correctly.
     *
     * @param partNumber integer part number value
     * @throws IllegalArgumentException if partNumber is less than 1 or greater than MULTIPART_DIRECTORY
     */
    static void validatePartNumber(final int partNumber) {
        if (partNumber <= 0) {
            throw new IllegalArgumentException("Negative or zero part numbers are not valid");
        }

        if (partNumber > MAX_PARTS) {
            final String msg = String.format("Part number of [%d] exceeds maximum parts (%d)",
                    partNumber, MAX_PARTS);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Returns the Manta job used to concatenate multiple file parts.
     *
     * @param id multipart upload id
     * @return Manta job object
     * @throws IOException thrown if there is a problem connecting to Manta
     */
    MantaJob findJob(final UUID id) throws IOException {
        try (Stream<MantaJob> jobs = mantaClient.getJobsByName(String.format(JOB_NAME_FORMAT, id))) {
            return jobs.findFirst().orElse(null);
        }
    }

    /**
     * Inner class used only with the jobs-based multipart implementation for
     * storing header and metadata information.
     */
    static class MultipartMetadata implements Serializable {
        private static final long serialVersionUID = -4410867990710890357L;

        /**
         * Path to final object on Manta.
         */
        private String path;

        /**
         * Metadata of final object.
         */
        private HashMap<String, String> objectMetadata;

        /**
         * HTTP content type to write to the final object.
         */
        private String contentType;

        /**
         * Creates a new instance.
         */
        MultipartMetadata() {
        }

        String getPath() {
            return path;
        }

        /**
         * Sets the path to the final object on Manta.
         *
         * @param path remote Manta path
         * @return this instance
         */
        MultipartMetadata setPath(final String path) {
            this.path = path;
            return this;
        }

        /**
         * Gets the metadata associated with the final Manta object.
         *
         * @return new instance of {@link MantaMetadata} with data populated
         */
        MantaMetadata getObjectMetadata() {
            if (this.objectMetadata == null) {
                return null;
            }

            return new MantaMetadata(this.objectMetadata);
        }

        /**
         * Sets the metadata to be written to the final object on Manta.
         *
         * @param objectMetadata metadata to write
         * @return this instance
         */
        MultipartMetadata setObjectMetadata(final MantaMetadata objectMetadata) {
            if (objectMetadata != null) {
                this.objectMetadata = new HashMap<>(objectMetadata);
            } else {
                this.objectMetadata = null;
            }

            return this;
        }

        String getContentType() {
            return contentType;
        }

        /**
         * Sets http headers to write to the final object on Manta. Actually,
         * we only consume Content-Type for now.
         *
         * @param contentType HTTP content type to set for the object
         * @return this instance
         */
        MultipartMetadata setContentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }
    }
}
