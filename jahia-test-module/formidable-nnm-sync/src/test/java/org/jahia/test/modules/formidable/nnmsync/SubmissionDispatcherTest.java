package org.jahia.test.modules.formidable.nnmsync;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SubmissionDispatcherTest {

    private final ConfigSynchronizer synchronizer = mock(ConfigSynchronizer.class);

    private SubmissionDispatcher dispatcher(SubmissionDispatcher.Gateway gateway) {
        // an executor that runs the work in the caller, which keeps the test deterministic
        ExecutorService executor = mock(ExecutorService.class);
        doAnswer(call -> {
            ((Runnable) call.getArgument(0)).run();
            return null;
        }).when(executor).execute(any());
        return new SubmissionDispatcher(gateway, executor, synchronizer);
    }

    @Test
    void anAcceptedOrAlreadyKnownSubmissionIsEnough() {
        List<String> sent = new ArrayList<>();
        dispatcher(json -> {
            sent.add(json);
            return 201;
        }).send("f", "s1", "{}");
        dispatcher(json -> 409).send("f", "s1", "{}");

        assertEquals(1, sent.size());
        verify(synchronizer, never()).syncLater(anyString(), anyBoolean());
    }

    @Test
    void anUnknownConfigurationIsSentAgain() {
        dispatcher(json -> 404).send("f", "s1", "{}");

        verify(synchronizer).syncLater("f", true);
    }

    @Test
    void failuresAreSwallowed() {
        assertDoesNotThrow(() -> dispatcher(json -> {
            throw new IOException("timeout");
        }).send("f", "s1", "{}"));
        assertDoesNotThrow(() -> dispatcher(json -> 500).send("f", "s1", "{}"));
        assertDoesNotThrow(() -> dispatcher(json -> 413).send("f", "s1", "{}"));
    }

    @Test
    void aFullQueueDropsTheSubmissionInsteadOfFailingTheVisitor() {
        ExecutorService full = mock(ExecutorService.class);
        doThrow(new RejectedExecutionException()).when(full).execute(any());

        assertDoesNotThrow(() -> new SubmissionDispatcher(json -> 201, full, synchronizer).send("f", "s1", "{}"));
    }
}
